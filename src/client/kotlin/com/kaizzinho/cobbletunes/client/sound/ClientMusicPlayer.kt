package com.kaizzinho.cobbletunes.client.sound

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import net.minecraft.client.MinecraftClient

/**
 * Owns "what's currently playing."
 *
 * AMBIENCE ROTATION MODEL (Pillar 7):
 * - All ambience tracks are registered with loop=false and played non-looping.
 * - Each fresh pick rolls a random "budget" between trackEndSilenceMin/MaxSeconds.
 *   The track rotates when EITHER the budget expires OR the sound engine reports
 *   the track has naturally finished (isPlaying() returns false), whichever first.
 * - Per-biome memory: biomeTrackMemory remembers which track was picked per biome,
 *   so re-entering a biome resumes the same pick (if not yet expired) rather than
 *   re-rolling. Budget time banks while not actively playing (battle, zone music).
 *
 * SILENCE GATING (Pillar 7 polish):
 * Two distinct mechanisms with opposite reset behavior:
 * 1. PENDING SILENCE (world-join/track-end): once started, does NOT reset on
 *    biome changes — just updates which track is queued. "Still count the timer."
 * 2. BIOME-TRANSITION DEBOUNCE: cuts audio immediately on any biome change,
 *    resets countdown every time the biome changes again. Only commits once
 *    the biome has been stable for the full random window.
 *
 * ZONE MUSIC (Pillar 9): gym/pokecenter/pokemart/special structure music plays
 * looping, bypasses all ambience state, resumes normal ambience on zone exit.
 *
 * MENU MUSIC (Pillar 10): plays while no world is loaded, bypasses everything.
 */
class ClientMusicPlayer(private val config: CobbleTunesClientConfig) {

    /**
     * Routine state-transition logging (silence countdowns, rotation, zone
     * enter/exit, battle resume) — only prints when config.debugLogging is
     * enabled. LOGGER.warn calls elsewhere in this file are NOT gated by
     * this, since those indicate real problems worth seeing regardless.
     */
    private fun debugLog(message: String) {
        if (config.debugLogging) {
            LOGGER.info("[$MOD_ID] [Debug] $message")
        }
    }

    private var currentContext: MusicContext = MusicContext.AMBIENCE
    private var currentTrackId: String? = null
    private var currentSound: FadingSoundInstance? = null
    private var currentAmbienceTrack: MusicTrack? = null
    private var currentBiomeId: String? = null

    // Per-biome track memory and time-budget tracking.
    private val biomeTrackMemory: MutableMap<String, MusicTrack> = mutableMapOf()
    private val trackBudgetStartMillis: MutableMap<String, Long> = mutableMapOf()
    private val trackBudgetDurationMillis: MutableMap<String, Long> = mutableMapOf()
    private var ambienceSegmentStartedAtMillis: Long? = null

    // Mechanism 1 — pending silence.
    private var pendingBiomeId: String? = null
    private var pendingTrack: MusicTrack? = null
    private var pendingSilenceEndsAtMillis: Long? = null

    // Mechanism 2 — biome-transition debounce.
    private var debounceBiomeId: String? = null
    private var debounceEndsAtMillis: Long? = null

    // ── Battle ────────────────────────────────────────────────────────────────

    fun playBattleContext(
        context: MusicContext,
        dexNumber: Int? = null,
        opposingDexNumbers: List<Int> = emptyList()
    ) {
        if (!config.replaceBattleMusic) return

        val track = when (context) {
            MusicContext.LEGENDARY_BATTLE -> {
                if (dexNumber == null) {
                    LOGGER.warn("[$MOD_ID] LEGENDARY_BATTLE without dex number, falling back to WILD_BATTLE pool")
                    pickFrom(TrackRegistry.tracksFor(MusicContext.WILD_BATTLE))
                } else TrackRegistry.legendaryTrackFor(dexNumber)
            }
            MusicContext.WILD_BATTLE -> dexNumber?.let { TrackRegistry.wildTrackFor(it) }
                ?: pickFrom(TrackRegistry.tracksFor(context))
            MusicContext.TRAINER_BATTLE -> TrackRegistry.trainerTrackFor(opposingDexNumbers)
                ?: pickFrom(TrackRegistry.tracksFor(context))
            else -> pickFrom(TrackRegistry.tracksFor(context))
        } ?: run {
            LOGGER.warn("[$MOD_ID] No track for $context, leaving current music")
            return
        }

        debugLog("[Battle start] Picked: ${track.id} for $context")
        // Suspend any running pending-silence countdown — it should not keep
        // ticking while battle music is playing. It will resume naturally
        // when the biome watcher next fires after the battle ends (since
        // updateAmbienceBiome is only called when no battle is active).
        // We deliberately do NOT clear pendingTrack/pendingBiomeId so that
        // when the battle ends, playAmbience() can find the right next track.
        play(context, track)
    }

    // ── Ambience ──────────────────────────────────────────────────────────────

    /** Called on battle end — resumes ambience immediately, no silence gate. */
    fun playAmbience() {
        if (!config.replaceAmbience) {
            stopCurrent(); currentContext = MusicContext.AMBIENCE; return
        }
        // Prefer pendingTrack if one is queued (means a rotation happened during
        // the battle — the old track finished, a new one was picked, and the
        // pending-silence countdown was running). Resume the fresh pick rather
        // than the stale currentAmbienceTrack (the track that already finished).
        // Also clears the pending countdown since we're resuming immediately —
        // the battle-end resume IS the "after silence" moment in this case.
        val track = if (pendingTrack != null) {
            val t = pendingTrack!!
            debugLog("[Battle end resume] Using pending track: ${t.id}")
            pendingTrack = null
            pendingSilenceEndsAtMillis = null
            currentBiomeId = pendingBiomeId
            pendingBiomeId = null
            currentAmbienceTrack = t
            // Start budget clock for this track since we're skipping the silence.
            val budgetMillis = randomRotationBudgetMillis()
            trackBudgetStartMillis[t.id] = System.currentTimeMillis()
            trackBudgetDurationMillis[t.id] = budgetMillis
            t
        } else {
            currentAmbienceTrack
                ?: pickFrom(TrackRegistry.tracksFor(MusicContext.AMBIENCE))
                ?: run { LOGGER.warn("[$MOD_ID] No ambience tracks registered"); return }
        }
        currentAmbienceTrack = track
        debugLog("[Battle end resume] Playing: ${track.id}")
        play(MusicContext.AMBIENCE, track)
    }

    /**
     * Called when the player dies (detected via ServerPlayerEvents.AFTER_RESPAWN
     * with alive=false). Stops battle music immediately and opens a 30-second
     * silence window before resuming ambience. Unlike playAmbience() (which
     * resumes the pre-battle track), death clears all ambience state so the
     * biome watcher re-detects and debounces the respawn biome from scratch —
     * the player may have respawned far from where they died.
     */
    fun handlePlayerDeath() {
        debugLog("[Death] Stopping battle music, 30s silence before biome re-detect")
        stopCurrent()
        currentContext = MusicContext.AMBIENCE

        // Clear all ambience state — force a full fresh biome detection on respawn
        currentBiomeId = null
        currentAmbienceTrack = null
        biomeTrackMemory.clear()
        trackBudgetStartMillis.clear()
        trackBudgetDurationMillis.clear()
        debounceBiomeId = null
        debounceEndsAtMillis = null

        // Open a 30-second pending silence — the biome watcher will queue up
        // the respawn biome's track behind it via refreshPendingTrack() and
        // it'll fade in naturally once the silence elapses.
        pendingBiomeId = null
        pendingTrack = null
        pendingSilenceEndsAtMillis = System.currentTimeMillis() + 30_000L
    }

    /** World join or dimension change — open pending silence, play nothing yet. */
    fun beginWorldJoinSilence() {
        stopCurrent()
        ambienceSegmentStartedAtMillis = null
        worldJoinReadyTicks = 0

        pendingSilenceEndsAtMillis = System.currentTimeMillis() +
                (config.worldJoinSilenceSeconds * 1000).toLong()
        pendingBiomeId = null; pendingTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null
        currentBiomeId = null; currentAmbienceTrack = null
        currentContext = MusicContext.AMBIENCE
        debugLog("[World join] Silence for ${config.worldJoinSilenceSeconds}s")
    }

    /** Called every throttled tick from CobbleTunesClient's biome watcher. */
    fun updateAmbienceBiome(biomeId: String) {
        if (!config.replaceAmbience) return
        // Don't advance any ambience state while battle music is playing —
        // pending silence timers should not tick down during a battle, and
        // biome debounce should not fire. Everything resumes from playAmbience().
        if (currentContext != MusicContext.AMBIENCE &&
            currentContext != MusicContext.MENU) return

        resolvePendingSilenceIfElapsed()

        if (pendingSilenceEndsAtMillis != null) {
            refreshPendingTrack(biomeId)
            return
        }

        if (debounceBiomeId != null) {
            handleBiomeTransitionDebounce(biomeId)
            return
        }

        if (biomeId == currentBiomeId) {
            checkAndRotateCurrentTrack(biomeId)
            return
        }

        handleBiomeTransitionDebounce(biomeId)
    }

    // ── Zone music (Pillar 9) ─────────────────────────────────────────────────

    private var currentSoundStartedAtMillis: Long = 0L
    private var worldJoinReadyTicks = 0
    private val WORLD_JOIN_READY_TICKS = 10 // ~10s, same as menu music delay

    /**
     * Called when entering a structure/trigger-block zone. Deliberately does
     * NOT try to snapshot "what to resume later" — see clearZone() below for
     * why that approach was removed. Just clears any in-flight silence/debounce
     * state (a zone entry is an explicit, server-confirmed event, not something
     * that should keep an old countdown running) and plays the zone track.
     */
    fun playZoneAmbience(context: MusicContext, track: MusicTrack) {
        if (!config.replaceAmbience) return
        pendingSilenceEndsAtMillis = null; pendingBiomeId = null; pendingTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null
        debugLog("[Zone enter] Playing: ${track.id} ($context)")
        play(context, track)
    }

    /**
     * Called when leaving a zone. Used to snapshot currentAmbienceTrack/
     * currentBiomeId on zone-entry and restore them here — but that snapshot
     * could go stale if a biome-transition debounce was IN FLIGHT the moment
     * the zone was entered (e.g. the player walked into a structure right as
     * they crossed into a new biome, mid-debounce). playZoneAmbience() clears
     * that debounce, but the snapshot still captured the OLD committed biome,
     * not the one the player was transitioning into — so on exit it could
     * resume a track tagged with the wrong biome entirely (e.g. a plains track
     * resumed as if the player were in a flower forest), which then got
     * immediately rotated out again since its budget/memory didn't match.
     *
     * The fix: don't try to resume cached state at all. Force a full fresh
     * biome resolution — exactly like a normal biome transition — using
     * whatever biome the player is ACTUALLY in right now (passed in from
     * CobbleTunesClient's live lookup). biomeTrackMemory itself is untouched,
     * so if the player's current biome was visited before, the normal
     * resolution path still resumes that same remembered track — this just
     * goes through the standard debounce instead of trying to shortcut it.
     */
    fun clearZone(currentBiomeId: String?) {
        if (!config.replaceAmbience) return
        // Unconditional stop — NOT stopAmbienceAudio(), which only acts when
        // currentContext == AMBIENCE. At this point currentContext is still
        // whatever the zone was (GYM_AMBIENCE, VANILLA_STRUCTURE, etc.), so
        // that guard would silently no-op and leave the zone track playing
        // forever. Must also reset currentContext to AMBIENCE explicitly here
        // — otherwise the debounce commit below's own
        // "if (currentContext == AMBIENCE) play(...)" check fails too, and
        // the timer fires with no audible result until something else
        // happens to call play() again (e.g. re-entering a zone).
        stopCurrent()
        currentContext = MusicContext.AMBIENCE
        this.currentBiomeId = null
        this.currentAmbienceTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null
        pendingSilenceEndsAtMillis = null; pendingTrack = null; pendingBiomeId = null
        debugLog("[Zone exit] Forcing fresh biome re-detection (was in zone, now at biome $currentBiomeId)")
        // Immediately feed the current biome through the normal transition
        // path if we know it, rather than waiting for the next throttled
        // watcher tick — keeps the silence gap as short as the debounce
        // window instead of also adding the watcher's own throttle delay.
        if (currentBiomeId != null) {
            handleBiomeTransitionDebounce(currentBiomeId)
        }
    }

    // ── Menu music (Pillar 10) ────────────────────────────────────────────────

    fun isMenuThemeAudible(): Boolean {
        val sound = currentSound ?: return false
        return currentContext == MusicContext.MENU &&
                MinecraftClient.getInstance().soundManager.isPlaying(sound)
    }

    fun playMenuTheme(track: MusicTrack) {
        if (!config.replaceMenuMusic) return
        if (isMenuThemeAudible() && currentTrackId == track.id) return
        debugLog("[Menu] Playing: ${track.id}")
        play(MusicContext.MENU, track, force = true)
    }

    fun stopMenuTheme() {
        if (currentContext == MusicContext.MENU) stopCurrent()
    }

    // ── Internal ambience logic ───────────────────────────────────────────────

    /**
     * Biome is stable and unchanged. Check if the current track has finished
     * (sound engine reports it done) OR its time budget has elapsed — either
     * triggers a rotation into pending silence then a fresh pick.
     */
    private fun checkAndRotateCurrentTrack(biomeId: String) {
        val track = biomeTrackMemory[biomeId] ?: run {
            handleBiomeTransitionDebounce(biomeId)
            return
        }

        val budgetStart = trackBudgetStartMillis[track.id]
        val budgetDuration = trackBudgetDurationMillis[track.id]
        val budgetElapsed = if (budgetStart != null) System.currentTimeMillis() - budgetStart else 0L
        val budgetExpired = budgetDuration != null && budgetElapsed >= budgetDuration

        val soundAge = System.currentTimeMillis() - currentSoundStartedAtMillis
        val soundFinished = currentContext == MusicContext.AMBIENCE &&
                currentTrackId == track.id &&
                soundAge > 2000L &&
                (currentSound == null || !MinecraftClient.getInstance().soundManager.isPlaying(currentSound!!))

        if (budgetExpired || soundFinished) {
            val reason = if (budgetExpired) "budget elapsed (${budgetElapsed / 1000}s)" else "track finished"
            debugLog("[Rotation] $reason — queuing next for biome $biomeId")
            stopAmbienceAudio()
            biomeTrackMemory.remove(biomeId)
            trackBudgetStartMillis.remove(track.id)
            trackBudgetDurationMillis.remove(track.id)

            val fresh = resolveTrackForBiome(biomeId) ?: return
            val silenceSeconds = randomSecondsInRange(
                config.trackEndSilenceMinSeconds, config.trackEndSilenceMaxSeconds
            )
            debugLog("[Silence] Track-end silence: ${silenceSeconds.toInt()}s")
            beginPendingSilence(biomeId, fresh, silenceSeconds)
        }
    }

    private fun handleBiomeTransitionDebounce(biomeId: String) {
        if (biomeId != debounceBiomeId) {
            stopAmbienceAudio()
            debounceBiomeId = biomeId
            val seconds = randomSecondsInRange(
                config.biomeTransitionSilenceMinSeconds, config.biomeTransitionSilenceMaxSeconds
            )
            debounceEndsAtMillis = System.currentTimeMillis() + (seconds * 1000).toLong()
            debugLog("[Debounce] Biome change → $biomeId, waiting ${seconds.toInt()}s")
            return
        }

        val endsAt = debounceEndsAtMillis ?: return
        if (System.currentTimeMillis() < endsAt) return

        debounceBiomeId = null; debounceEndsAtMillis = null
        val track = resolveTrackForBiome(biomeId) ?: return
        currentBiomeId = biomeId; currentAmbienceTrack = track

        // Start budget clock NOW — this is when we commit to playing it.
        val budgetMillis = randomRotationBudgetMillis()
        trackBudgetStartMillis[track.id] = System.currentTimeMillis()
        trackBudgetDurationMillis[track.id] = budgetMillis

        debugLog("[Debounce commit] Playing ${track.id} for biome $biomeId (budget: ${budgetMillis / 1000}s)")
        if (currentContext == MusicContext.AMBIENCE) play(MusicContext.AMBIENCE, track)
    }

    private fun resolveTrackForBiome(biomeId: String): MusicTrack? {
        val remembered = biomeTrackMemory[biomeId]
        if (remembered != null) return remembered
        val fresh = if (biomeId == "cobbletunes:cave") {
            TrackRegistry.caveAmbienceTrack()
        } else {
            TrackRegistry.ambienceTrackFor(biomeId, region = null)
        } ?: run {
            LOGGER.warn("[$MOD_ID] No ambience track for biome: $biomeId")
            return null
        }
        biomeTrackMemory[biomeId] = fresh
        return fresh
    }

    private fun refreshPendingTrack(biomeId: String) {
        val track = resolveTrackForBiome(biomeId) ?: return
        if (pendingBiomeId != biomeId || pendingTrack?.id != track.id) {
            debugLog("[Pending] Queued track updated to ${track.id} for biome $biomeId")
        }
        pendingBiomeId = biomeId; pendingTrack = track
    }

    private fun beginPendingSilence(biomeId: String, track: MusicTrack, seconds: Float) {
        pendingBiomeId = biomeId; pendingTrack = track
        pendingSilenceEndsAtMillis = System.currentTimeMillis() + (seconds * 1000).toLong()
    }

    private fun resolvePendingSilenceIfElapsed() {
        val endsAt = pendingSilenceEndsAtMillis ?: return

        // Don't fire play() until the sound engine is ready — on a fresh
        // launch the engine initializes several seconds after the client,
        // so play() calls before it's ready are silently discarded.
        worldJoinReadyTicks++
        if (worldJoinReadyTicks < WORLD_JOIN_READY_TICKS) return

        if (System.currentTimeMillis() < endsAt) {
            val remaining = (endsAt - System.currentTimeMillis()) / 1000
            if (remaining % 5L == 0L && remaining > 0L) {
                debugLog("[Silence] Waiting ${remaining}s...")
            }
            return
        }
        pendingSilenceEndsAtMillis = null

        val track = pendingTrack ?: run {
            LOGGER.warn("[$MOD_ID] [Silence elapsed] No pending track — nothing to play")
            return
        }
        val biomeId = pendingBiomeId
        pendingTrack = null; pendingBiomeId = null
        currentBiomeId = biomeId; currentAmbienceTrack = track

        val budgetMillis = randomRotationBudgetMillis()
        trackBudgetStartMillis[track.id] = System.currentTimeMillis()
        trackBudgetDurationMillis[track.id] = budgetMillis

        debugLog("[Silence elapsed] Playing ${track.id} (budget: ${budgetMillis / 1000}s)")
        if (currentContext == MusicContext.AMBIENCE) play(MusicContext.AMBIENCE, track)
    }

    private fun randomSecondsInRange(min: Float, max: Float): Float {
        val hi = max.coerceAtLeast(min)
        return if (hi > min) min + (hi - min) * Math.random().toFloat() else min
    }

    private fun randomRotationBudgetMillis(): Long {
        val minMs = (config.trackEndSilenceMinSeconds * 1000).toLong()
        val maxMs = (config.trackEndSilenceMaxSeconds * 1000).toLong()
        return if (maxMs > minMs) minMs + ((Math.random() * (maxMs - minMs)).toLong()) else minMs
    }

    private fun stopAmbienceAudio() {
        if (currentContext != MusicContext.AMBIENCE) return
        ambienceSegmentStartedAtMillis = null
        stopCurrent()
    }

    private fun pickFrom(candidates: List<MusicTrack>): MusicTrack? =
        if (config.shuffleAmbienceTracks) candidates.randomOrNull() else candidates.firstOrNull()

    private fun play(context: MusicContext, track: MusicTrack, force: Boolean = false) {
        if (!force && context == currentContext && track.id == currentTrackId) return

        if (currentContext == MusicContext.AMBIENCE) ambienceSegmentStartedAtMillis = null

        stopCurrent()
        val instance = FadingSoundInstance(
            soundEvent = track.soundEvent,
            targetVolume = config.musicVolume,
            fadeInSeconds = config.crossfadeSeconds,
            looping = track.loop
        )
        MinecraftClient.getInstance().soundManager.play(instance)
        currentSound = instance
        currentTrackId = track.id
        currentContext = context
        currentSoundStartedAtMillis = System.currentTimeMillis()

        if (context == MusicContext.AMBIENCE) {
            ambienceSegmentStartedAtMillis = System.currentTimeMillis()
        }
    }

    private fun stopCurrent() {
        val sound = currentSound ?: return
        sound.beginFadeOut(config.crossfadeSeconds)
        currentSound = null
        currentTrackId = null
    }

    /**
     * Hard stop of whatever is currently playing — used when the death screen
     * appears so music doesn't keep playing while the player is looking at
     * "You Died!". Unlike stopCurrent() which fades out, this cuts immediately
     * since the death screen itself is already a jarring enough transition.
     * handlePlayerDeath() handles the post-respawn silence and biome re-detect.
     */
    fun stopEverything() {
        stopCurrent()
        currentContext = MusicContext.AMBIENCE
        pendingSilenceEndsAtMillis = null
        pendingTrack = null
        pendingBiomeId = null
        debounceBiomeId = null
        debounceEndsAtMillis = null
    }

}