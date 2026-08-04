package com.kaizzinho.cobbletunes.client.sound

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import net.minecraft.client.MinecraftClient

class ClientMusicPlayer(private val config: CobbleTunesClientConfig) {

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

    // zone state lives separately so battles can safely borrow the audio slot
    private var activeZoneContext: MusicContext? = null
    private var activeZoneTrack: MusicTrack? = null

    // biome memory + rotation timers
    private val biomeTrackMemory: MutableMap<String, MusicTrack> = mutableMapOf()
    private val trackBudgetStartMillis: MutableMap<String, Long> = mutableMapOf()
    private val trackBudgetDurationMillis: MutableMap<String, Long> = mutableMapOf()
    private var ambienceSegmentStartedAtMillis: Long? = null

    // silence queued before the next track
    private var pendingBiomeId: String? = null
    private var pendingTrack: MusicTrack? = null
    private var pendingSilenceEndsAtMillis: Long? = null

    // short debounce while the biome settles
    private var debounceBiomeId: String? = null
    private var debounceEndsAtMillis: Long? = null

    // battle

    fun playBattleContext(
        context: MusicContext,
        dexNumber: Int? = null,
        opposingDexNumbers: List<Int> = emptyList(),
        preferredRegion: RegionOfOrigin? = null
    ) {
        if (!config.replaceBattleMusic) return

        // don't clear zone state here; we may need it right after the battle

        val track = when (context) {
            MusicContext.LEGENDARY_BATTLE -> {
                if (dexNumber == null) {
                    LOGGER.warn("[$MOD_ID] LEGENDARY_BATTLE without dex number, falling back to WILD_BATTLE pool")
                    pickFrom(TrackRegistry.tracksFor(MusicContext.WILD_BATTLE))
                } else TrackRegistry.legendaryTrackFor(dexNumber)
            }
            MusicContext.WILD_BATTLE -> dexNumber?.let { TrackRegistry.wildTrackFor(it) }
                ?: pickFrom(TrackRegistry.tracksFor(context))
            MusicContext.TRAINER_BATTLE,
            MusicContext.GYM_LEADER_BATTLE,
            MusicContext.ELITE_FOUR_BATTLE,
            MusicContext.CHAMPION_BATTLE,
            MusicContext.PVP_BATTLE ->
                TrackRegistry.regionalBattleTrackFor(
                    context = context,
                    dexNumbers = opposingDexNumbers,
                    preferredRegion = preferredRegion
                ) ?: pickFrom(TrackRegistry.tracksFor(context))
            else -> pickFrom(TrackRegistry.tracksFor(context))
        } ?: run {
            LOGGER.warn("[$MOD_ID] No track for $context, leaving current music")
            return
        }

        debugLog(
            "[Battle start] Picked: ${track.id} for $context" +
                    (preferredRegion?.let { " (preferredRegion=$it)" } ?: "")
        )
        // keep pending biome info around; battle-end resume will use it
        play(context, track)
    }

    // ambience

    fun playAmbience() {
        if (!config.replaceAmbience) {
            stopCurrent(); currentContext = MusicContext.AMBIENCE; return
        }

        // zone wins over biome here, incl. forfeits inside gyms
        val zoneContext = activeZoneContext
        val zoneTrack = activeZoneTrack
        if (zoneContext != null && zoneTrack != null) {
            debugLog("[Battle end resume] Restoring active zone: ${zoneTrack.id} ($zoneContext)")
            play(zoneContext, zoneTrack, force = true)
            return
        }

        // a queued pick is fresher than the old ambience track
        val track = if (pendingTrack != null) {
            val t = pendingTrack!!
            debugLog("[Battle end resume] Using pending track: ${t.id}")
            pendingTrack = null
            pendingSilenceEndsAtMillis = null
            currentBiomeId = pendingBiomeId
            pendingBiomeId = null
            currentAmbienceTrack = t
            // start its timer now since we skipped the gap
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

    fun handlePlayerDeath() {
        debugLog("[Death] Stopping battle music, 30s silence before biome re-detect")
        stopCurrent()
        currentContext = MusicContext.AMBIENCE

        // respawn may be far away, so toss all old world/zone state
        currentBiomeId = null
        currentAmbienceTrack = null
        activeZoneContext = null
        activeZoneTrack = null
        biomeTrackMemory.clear()
        trackBudgetStartMillis.clear()
        trackBudgetDurationMillis.clear()
        debounceBiomeId = null
        debounceEndsAtMillis = null

        // watcher queues the respawn biome while this 30s gap runs
        pendingBiomeId = null
        pendingTrack = null
        pendingSilenceEndsAtMillis = System.currentTimeMillis() + 30_000L
    }

    fun beginWorldJoinSilence() {
        stopCurrent()
        ambienceSegmentStartedAtMillis = null
        worldJoinReadyTicks = 0

        pendingSilenceEndsAtMillis = System.currentTimeMillis() +
                (config.worldJoinSilenceSeconds * 1000).toLong()
        pendingBiomeId = null; pendingTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null
        currentBiomeId = null; currentAmbienceTrack = null
        activeZoneContext = null; activeZoneTrack = null
        currentContext = MusicContext.AMBIENCE
        debugLog("[World join] Silence for ${config.worldJoinSilenceSeconds}s")
    }

    fun updateAmbienceBiome(biomeId: String) {
        if (!config.replaceAmbience) return
        // freeze ambience timers while a battle ctx owns the player
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

    // zone music

    private var currentSoundStartedAtMillis: Long = 0L
    private var worldJoinReadyTicks = 0
    private val WORLD_JOIN_READY_TICKS = 10 // ~10s, same as menu music delay

    fun playZoneAmbience(context: MusicContext, track: MusicTrack) {
        if (!config.replaceAmbience) return

        activeZoneContext = context
        activeZoneTrack = track
        pendingSilenceEndsAtMillis = null; pendingBiomeId = null; pendingTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null

        // zone packets during battle only update memory, not the audible track
        if (isBattleContext(currentContext)) {
            debugLog("[Zone update during battle] Remembering: ${track.id} ($context)")
            return
        }

        debugLog("[Zone enter] Playing: ${track.id} ($context)")
        play(context, track)
    }

    fun clearZone(currentBiomeId: String?) {
        activeZoneContext = null
        activeZoneTrack = null
        if (!config.replaceAmbience) return

        // same deal on exit: update state, leave battle audio alone
        if (isBattleContext(currentContext)) {
            debugLog("[Zone exit during battle] Cleared active zone; battle music continues")
            return
        }
        // stopCurrent is intentional; stopAmbienceAudio ignores zone ctxs
        stopCurrent()
        currentContext = MusicContext.AMBIENCE
        this.currentBiomeId = null
        this.currentAmbienceTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null
        pendingSilenceEndsAtMillis = null; pendingTrack = null; pendingBiomeId = null
        debugLog("[Zone exit] Forcing fresh biome re-detection (was in zone, now at biome $currentBiomeId)")
        // kick the debounce now instead of waiting for the next watcher tick
        if (currentBiomeId != null) {
            handleBiomeTransitionDebounce(currentBiomeId)
        }
    }

    // menu music

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

    // ambience internals

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

        // timer starts when the track actually commits
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

        // fresh launches can drop sounds until the engine is ready
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

    private fun isBattleContext(context: MusicContext): Boolean = context in setOf(
        MusicContext.WILD_BATTLE,
        MusicContext.TRAINER_BATTLE,
        MusicContext.GYM_LEADER_BATTLE,
        MusicContext.ELITE_FOUR_BATTLE,
        MusicContext.CHAMPION_BATTLE,
        MusicContext.PVP_BATTLE,
        MusicContext.LEGENDARY_BATTLE
    )

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
        // missing oggs should be a quiet no-op, not an empty-event warning
        val mc = MinecraftClient.getInstance()
        if (mc.soundManager.getKeys().none {
                it.toString() == track.soundEvent.id.toString()
            }) {
            debugLog("[Play] Skipping ${track.id} — no sounds registered in resource pack")
            return
        }


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