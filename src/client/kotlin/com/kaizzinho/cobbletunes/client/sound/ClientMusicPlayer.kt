package com.kaizzinho.cobbletunes.client.sound

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import net.minecraft.client.MinecraftClient

/**
 * Owns "what's currently playing." Pillar 8 classifier model: CobblemonMusicListener
 * runs one classification pass per battle start and calls playBattleContext() once —
 * no stack, since only one battle context is ever live at a time. playAmbience() is
 * called on battle end/flee and always resumes ambience IMMEDIATELY — the silence
 * gating below is deliberately scoped to world-join/dimension-change and biome-driven
 * transitions only, not battle-end resumes.
 *
 * Pillar 7 (landed): ambience is BIOME-DRIVEN, resolved by CobbleTunesClient's
 * client-tick watcher calling updateAmbienceBiome() whenever the player's biome
 * is checked. Region was dropped from this resolution entirely — Cobbleverse has
 * no in-world "region" concept (pure datapack tweaks, no zone/dimension API), and
 * per-player RCT region-progress was deliberately ruled out as an ambience signal
 * (too sticky/repetitive over long play sessions). So this always calls
 * TrackRegistry.ambienceTrackFor() with region = null — the flat, region-blind
 * biome pool.
 *
 * Pillar 7 refinement — per-biome memory + random-window rotation:
 * biomeTrackMemory remembers WHICH track was selected for each exact biome id,
 * so briefly leaving a biome and coming back resumes that same selection instead
 * of re-rolling. trackProgressMillis banks real elapsed listening time per track
 * id, paused while that track isn't the audible one and resumed when it is again.
 * Each fresh selection rolls a random rotation target between
 * config.ambienceRotationMinSeconds/MaxSeconds — once that budget is used up, a
 * new track gets picked.
 *
 * Pillar 7 polish — TWO DISTINCT silence mechanisms, deliberately separate
 * because they need opposite reset behavior:
 *
 * 1. PENDING SILENCE (world-join/dimension-change, and a track finishing its
 *    rotation budget while the player stands still) — pendingSilenceEndsAtMillis
 *    / pendingBiomeId / pendingTrack. Once started, this does NOT reset if the
 *    biome changes during the wait — it just keeps updating what's queued up,
 *    so whichever biome the player is in WHEN IT ELAPSES is what plays. This is
 *    the "still count the timer" behavior.
 *
 * 2. BIOME-TRANSITION DEBOUNCE (walking into a new biome while the old track
 *    still had budget left) — debounceBiomeId / debounceEndsAtMillis. Audio is
 *    cut to silence THE MOMENT a biome change is detected. If the observed
 *    biome changes AGAIN before the debounce window elapses, the countdown
 *    RESETS against the newest biome — this is what stops a thin biome like a
 *    river from ever getting its own track started if the player's already
 *    walked past it by the time the window would've closed. Only commits (and
 *    fades in) once a biome has read the same for the FULL window.
 *
 * Whenever a pending-silence countdown (mechanism 1) is active, biome-transition
 * debounce (mechanism 2) is skipped entirely — the pending countdown already
 * guarantees silence and will resolve to whatever's current when it elapses.
 *
 * KNOWN LIMITATION: Minecraft's sound API has no seek/resume-from-position —
 * so "resuming" a remembered track restarts it from the beginning of the file,
 * not the exact moment it was interrupted. What IS preserved is the *selection*
 * (same song keeps coming back) and the *total listen-time budget*.
 */
class ClientMusicPlayer(private val config: CobbleTunesClientConfig) {

    private var currentContext: MusicContext = MusicContext.AMBIENCE
    private var currentTrackId: String? = null
    private var currentSound: FadingSoundInstance? = null
    private var currentAmbienceTrack: MusicTrack? = null
    private var currentBiomeId: String? = null

    // Pillar 7 refinement state — per-biome memory + rotation budget.
    private val biomeTrackMemory: MutableMap<String, MusicTrack> = mutableMapOf()
    private val trackProgressMillis: MutableMap<String, Long> = mutableMapOf()
    private val trackTargetMillis: MutableMap<String, Long> = mutableMapOf()
    private var ambienceSegmentTrackId: String? = null
    private var ambienceSegmentStartedAtMillis: Long? = null

    // Mechanism 1 — pending silence (world-join/dimension-change, track-end).
    // Does NOT reset on biome changes; first countdown started wins.
    private var pendingBiomeId: String? = null
    private var pendingTrack: MusicTrack? = null
    private var pendingSilenceEndsAtMillis: Long? = null

    // Mechanism 2 — biome-transition debounce. RESETS every time the observed
    // biome changes; only commits once stable for the full random window.
    private var debounceBiomeId: String? = null
    private var debounceEndsAtMillis: Long? = null

    /**
     * Called once from CobblemonMusicListener right after it classifies a battle start.
     * @param dexNumber the opposing side's primary species dex number — used for
     *   LEGENDARY_BATTLE (species/regional resolution) and WILD_BATTLE (regional
     *   resolution, Pillar 1).
     * @param opposingDexNumbers Pillar 3: the opposing trainer's full roster, used
     *   only by TRAINER_BATTLE for the majority-vote regional resolver. Ignored by
     *   every other context — empty list is the correct default for wild/PvP.
     */
    fun playBattleContext(
        context: MusicContext,
        dexNumber: Int? = null,
        opposingDexNumbers: List<Int> = emptyList()
    ) {
        if (!config.replaceBattleMusic) return

        val track = when (context) {
            MusicContext.LEGENDARY_BATTLE -> {
                if (dexNumber == null) {
                    LOGGER.warn("[$MOD_ID] LEGENDARY_BATTLE requested without a dex number, falling back to WILD_BATTLE pool")
                    pickFrom(TrackRegistry.tracksFor(MusicContext.WILD_BATTLE))
                } else {
                    TrackRegistry.legendaryTrackFor(dexNumber)
                }
            }
            MusicContext.WILD_BATTLE -> {
                dexNumber?.let { TrackRegistry.wildTrackFor(it) }
                    ?: pickFrom(TrackRegistry.tracksFor(context))
            }
            MusicContext.TRAINER_BATTLE -> {
                TrackRegistry.trainerTrackFor(opposingDexNumbers)
                    ?: pickFrom(TrackRegistry.tracksFor(context))
            }
            else -> pickFrom(TrackRegistry.tracksFor(context))
        }

        if (track == null) {
            LOGGER.warn("[$MOD_ID] No track resolved for context $context, leaving current music alone")
            return
        }
        play(context, track)
    }

    /**
     * Called on battle victory/flee. Resumes whatever currentAmbienceTrack was
     * last resolved IMMEDIATELY — deliberately not silence-gated, since the
     * silence behavior was only asked for on world-join/dimension-change and
     * biome transitions, not battle-end. Falls back to a flat pick across the
     * whole AMBIENCE pool only if nothing's been resolved yet.
     */
    fun playAmbience() {
        if (!config.replaceAmbience) {
            stopCurrent()
            currentContext = MusicContext.AMBIENCE
            return
        }

        val track = currentAmbienceTrack ?: pickFrom(TrackRegistry.tracksFor(MusicContext.AMBIENCE))
        if (track == null) {
            LOGGER.warn("[$MOD_ID] No ambience tracks registered, leaving current music alone")
            return
        }
        currentAmbienceTrack = track
        play(MusicContext.AMBIENCE, track)
    }

    /**
     * Pillar 7 polish: called by CobbleTunesClient whenever the client's world
     * reference changes — covers initial world join AND dimension changes with
     * one check. Immediately silences whatever was playing (dimension change
     * shouldn't carry the old world's ambience into the new one even for a
     * moment) and opens a pending-silence countdown (mechanism 1) — nothing
     * plays until it elapses, and any in-flight biome-transition debounce
     * (mechanism 2) is discarded since this supersedes it.
     */
    fun beginWorldJoinSilence() {
        stopAmbienceAudio()

        pendingSilenceEndsAtMillis = System.currentTimeMillis() + (config.worldJoinSilenceSeconds * 1000).toLong()
        pendingBiomeId = null
        pendingTrack = null
        debounceBiomeId = null
        debounceEndsAtMillis = null
        currentBiomeId = null
        currentAmbienceTrack = null
    }

    /**
     * Pillar 9: called when the server signals the player has entered a structure
     * zone (gym, Poké Center, Poké Mart). Plays the given track immediately,
     * bypassing biome debounce/pending-silence machinery — zone music is an
     * explicit, server-confirmed event, not a proximity guess that needs debouncing.
     * Also clears all pending/debounce state so that leaving the zone (clearZone)
     * starts fresh without an old pending countdown taking over.
     */
    fun playZoneAmbience(context: MusicContext, track: MusicTrack) {
        if (!config.replaceAmbience) return
        pendingSilenceEndsAtMillis = null
        pendingBiomeId = null
        pendingTrack = null
        debounceBiomeId = null
        debounceEndsAtMillis = null
        currentAmbienceTrack = track
        play(context, track)
    }

    /**
     * Pillar 9: called when the server signals the player has left all structure
     * zones. Resumes normal biome ambience immediately — no silence gap needed
     * since walking out of a gym into the overworld should feel seamless.
     * Forces a re-resolve of the current biome by clearing currentBiomeId so
     * the next updateAmbienceBiome() tick picks up the correct biome track
     * even if the biome hasn't changed since the player entered the zone.
     */
    fun clearZone() {
        if (!config.replaceAmbience) return
        currentBiomeId = null
        val track = currentAmbienceTrack ?: pickFrom(TrackRegistry.tracksFor(MusicContext.AMBIENCE))
        if (track != null) {
            currentAmbienceTrack = track
            play(MusicContext.AMBIENCE, track)
        }
    }

    /**
     * Pillar 7: called from CobbleTunesClient's client-tick biome watcher on
     * every throttled tick, not just on change.
     *
     * Region is intentionally NOT passed to TrackRegistry.ambienceTrackFor() —
     * Cobbleverse has no in-world region concept to supply one, so this always
     * resolves against the flat, region-blind biome pool.
     */
    fun updateAmbienceBiome(biomeId: String) {
        if (!config.replaceAmbience) return

        resolvePendingSilenceIfElapsed()

        if (pendingSilenceEndsAtMillis != null) {
            // A pending-silence countdown (world-join or track-end) is already
            // running — it always wins. Don't run biome-transition debounce at
            // all; just keep the queued track in sync with wherever the player
            // currently is, so whatever's current WHEN the countdown elapses is
            // what plays (see resolvePendingSilenceIfElapsed()).
            refreshPendingTrack(biomeId)
            return
        }

        if (debounceBiomeId != null) {
            // A biome-transition debounce is already in flight — audio is
            // currently silent and waiting to resolve. Always route through
            // it here, even if biomeId happens to equal currentBiomeId (e.g.
            // the player dipped into a different biome and walked straight
            // back before ever committing to it) — otherwise this would be
            // mistaken for "no change" below and leave the audio stuck silent
            // with nothing left to resume it.
            handleBiomeTransitionDebounce(biomeId)
            return
        }

        if (biomeId == currentBiomeId) {
            // Not entering a new biome — but the current track might have used
            // up its rotation budget just by standing here.
            rotateIfExpired(biomeId)
            return
        }

        handleBiomeTransitionDebounce(biomeId)
    }

    /**
     * Mechanism 2: biome-transition debounce. Cuts audio to silence the moment
     * ANY biome change is observed. If the observed biome changes again before
     * the window elapses, the countdown resets against the newest biome —
     * this is what stops a thin biome (a river cutting through a forest) from
     * ever starting its own theme if the player's already walked past it by
     * the time the window would've closed. Only commits once a biome has read
     * the same for the FULL randomly-rolled window.
     */
    private fun handleBiomeTransitionDebounce(biomeId: String) {
        if (biomeId != debounceBiomeId) {
            stopAmbienceAudio()
            debounceBiomeId = biomeId
            val seconds = randomSecondsInRange(
                config.biomeTransitionSilenceMinSeconds,
                config.biomeTransitionSilenceMaxSeconds
            )
            debounceEndsAtMillis = System.currentTimeMillis() + (seconds * 1000).toLong()
            return
        }

        val endsAt = debounceEndsAtMillis ?: return
        if (System.currentTimeMillis() < endsAt) return

        // Stable for the full window — commit to it.
        debounceBiomeId = null
        debounceEndsAtMillis = null

        val track = resolveTrackForBiome(biomeId) ?: return
        currentBiomeId = biomeId
        currentAmbienceTrack = track

        if (currentContext == MusicContext.AMBIENCE) {
            play(MusicContext.AMBIENCE, track)
        }
    }

    /**
     * Resolves the remembered (if not expired) or a freshly-picked track for
     * this biome, updating biomeTrackMemory/rotation bookkeeping as needed.
     * Does NOT touch currentBiomeId/currentAmbienceTrack or play anything —
     * callers decide what to do with the result.
     */
    private fun resolveTrackForBiome(biomeId: String): MusicTrack? {
        val remembered = biomeTrackMemory[biomeId]
        if (remembered != null && !isExpired(remembered)) return remembered

        val fresh = pickFreshAmbienceTrack(biomeId) ?: return null
        remembered?.let {
            trackProgressMillis.remove(it.id)
            trackTargetMillis.remove(it.id)
        }
        biomeTrackMemory[biomeId] = fresh
        return fresh
    }

    /** Keeps the pending-silence target in sync with wherever the player currently is. */
    private fun refreshPendingTrack(biomeId: String) {
        val track = resolveTrackForBiome(biomeId) ?: return
        pendingBiomeId = biomeId
        pendingTrack = track
    }

    private fun beginPendingSilence(biomeId: String, track: MusicTrack, seconds: Float) {
        pendingBiomeId = biomeId
        pendingTrack = track
        pendingSilenceEndsAtMillis = System.currentTimeMillis() + (seconds * 1000).toLong()
    }

    /** The only place mechanism 1 (pending silence) ever actually starts audible ambience. */
    private fun resolvePendingSilenceIfElapsed() {
        val endsAt = pendingSilenceEndsAtMillis ?: return
        if (System.currentTimeMillis() < endsAt) return
        pendingSilenceEndsAtMillis = null

        val track = pendingTrack ?: return
        val biomeId = pendingBiomeId
        pendingTrack = null
        pendingBiomeId = null

        currentBiomeId = biomeId
        currentAmbienceTrack = track

        if (currentContext == MusicContext.AMBIENCE) {
            play(MusicContext.AMBIENCE, track)
        }
    }

    /**
     * Vanilla itself (net.minecraft.sound.MusicSound's minDelay/maxDelay) rolls
     * a random silence length between two bounds rather than using a fixed
     * one — this mirrors that shape for both track-end and biome-transition
     * silence, just with much shorter windows appropriate to this mod's pace.
     */
    private fun randomSecondsInRange(minSeconds: Float, maxSeconds: Float): Float {
        val hi = maxSeconds.coerceAtLeast(minSeconds)
        return if (hi > minSeconds) minSeconds + (hi - minSeconds) * Math.random().toFloat() else minSeconds
    }

    private fun isExpired(track: MusicTrack): Boolean {
        val target = trackTargetMillis[track.id] ?: return true
        val totalElapsedMillis = (trackProgressMillis[track.id] ?: 0L) + currentSegmentElapsedMillis(track.id)
        return totalElapsedMillis >= target
    }

    private fun currentSegmentElapsedMillis(trackId: String): Long {
        if (ambienceSegmentTrackId != trackId) return 0L
        val startedAt = ambienceSegmentStartedAtMillis ?: return 0L
        return System.currentTimeMillis() - startedAt
    }

    /**
     * Called when still standing in the same biome — handles rotation-in-place.
     * A track legitimately finishing its rotation budget goes through
     * mechanism 1 (pending silence), NOT the debounce — the "still count the
     * timer through biome changes" rule applies here, matching the
     * river-track-ends-while-you're-in-it case.
     */
    private fun rotateIfExpired(biomeId: String) {
        val remembered = biomeTrackMemory[biomeId] ?: return
        if (!isExpired(remembered)) return

        stopAmbienceAudio()

        trackProgressMillis.remove(remembered.id)
        trackTargetMillis.remove(remembered.id)
        val fresh = pickFreshAmbienceTrack(biomeId) ?: return
        biomeTrackMemory[biomeId] = fresh

        beginPendingSilence(
            biomeId, fresh,
            randomSecondsInRange(config.trackEndSilenceMinSeconds, config.trackEndSilenceMaxSeconds)
        )
    }

    /**
     * Resolves a fresh track for this biome and figures out its rotation
     * target. If the track has a known durationSeconds, the target is
     * min(durationSeconds, a random pick in ambienceRotationMin/MaxSeconds) —
     * so a short track never gets forced to loop past its natural end just to
     * fill the random window, while a long track still gets capped by that
     * window instead of being allowed to play its full length uninterrupted.
     * Tracks without a known duration keep the old behavior: always the full
     * random window, since there's nothing shorter to respect.
     */
    private fun pickFreshAmbienceTrack(biomeId: String): MusicTrack? {
        val fresh = TrackRegistry.ambienceTrackFor(biomeId, region = null) ?: return null
        val randomTargetMillis = randomRotationTargetMillis()
        val knownDurationMillis = fresh.durationSeconds?.let { it * 1000L }
        trackTargetMillis[fresh.id] = if (knownDurationMillis != null) {
            minOf(knownDurationMillis, randomTargetMillis)
        } else {
            randomTargetMillis
        }
        return fresh
    }

    private fun randomRotationTargetMillis(): Long {
        val minSeconds = config.ambienceRotationMinSeconds
        val maxSeconds = config.ambienceRotationMaxSeconds.coerceAtLeast(minSeconds)
        val seconds = if (maxSeconds > minSeconds) (minSeconds..maxSeconds).random() else minSeconds
        return seconds * 1000L
    }

    /**
     * Banks elapsed real time for whatever ambience track was just playing into
     * trackProgressMillis, then clears the active segment. Called right before
     * switching away from an ambience track so its listen-time budget keeps
     * accumulating correctly across interruptions instead of resetting every
     * time it's paused.
     */
    private fun pauseAmbienceProgress() {
        val trackId = ambienceSegmentTrackId ?: return
        val startedAt = ambienceSegmentStartedAtMillis ?: return
        val elapsed = System.currentTimeMillis() - startedAt
        trackProgressMillis[trackId] = (trackProgressMillis[trackId] ?: 0L) + elapsed
        ambienceSegmentTrackId = null
        ambienceSegmentStartedAtMillis = null
    }

    /**
     * Stops whatever's currently audible WITHOUT starting anything new —
     * this is the actual "cut to silence" step, used by both silence
     * mechanisms the moment they're triggered. Idempotent: safe to call when
     * already silent.
     */
    private fun stopAmbienceAudio() {
        if (currentContext != MusicContext.AMBIENCE) return
        pauseAmbienceProgress()
        stopCurrent()
    }

    private fun pickFrom(candidates: List<MusicTrack>): MusicTrack? =
        if (config.shuffleAmbienceTracks) candidates.randomOrNull() else candidates.firstOrNull()

    private fun play(context: MusicContext, track: MusicTrack) {
        if (context == currentContext && track.id == currentTrackId) return

        if (currentContext == MusicContext.AMBIENCE) {
            pauseAmbienceProgress()
        }

        stopCurrent()
        val instance = FadingSoundInstance(
            soundEvent = track.soundEvent,
            targetVolume = config.musicVolume,
            fadeInSeconds = config.crossfadeSeconds,
            // A known durationSeconds means we're tracking its real length
            // ourselves (see pickFreshAmbienceTrack) — looping it would let
            // Minecraft's sound engine restart it mid-cycle before our own
            // timer ever gets a chance to cut it cleanly. track.loop only
            // applies when durationSeconds is unknown.
            looping = track.durationSeconds == null && track.loop
        )
        MinecraftClient.getInstance().soundManager.play(instance)
        currentSound = instance
        currentTrackId = track.id
        currentContext = context
        LOGGER.info("[$MOD_ID] Switched music context -> $context (${track.id})")

        if (context == MusicContext.AMBIENCE) {
            ambienceSegmentTrackId = track.id
            ambienceSegmentStartedAtMillis = System.currentTimeMillis()
        }
    }

    private fun stopCurrent() {
        val sound = currentSound ?: return
        sound.beginFadeOut(config.crossfadeSeconds)
        currentSound = null
        currentTrackId = null
    }
}
