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

    // zone state stays separate from playback
    private var activeZoneContext: MusicContext? = null
    private var activeZoneTrack: MusicTrack? = null

    // latest biome target survives battle and victory
    private var overrideBiomeId: String? = null
    private var overrideBiomeTrack: MusicTrack? = null

    private val biomeTrackMemory: MutableMap<String, MusicTrack> = mutableMapOf()
    private val trackBudgetStartMillis: MutableMap<String, Long> = mutableMapOf()
    private val trackBudgetDurationMillis: MutableMap<String, Long> = mutableMapOf()
    private var ambienceSegmentStartedAtMillis: Long? = null

    private var pendingBiomeId: String? = null
    private var pendingTrack: MusicTrack? = null
    private var pendingSilenceEndsAtMillis: Long? = null

    private var debounceBiomeId: String? = null
    private var debounceEndsAtMillis: Long? = null


    fun playBattleContext(
        context: MusicContext,
        dexNumber: Int? = null,
        opposingDexNumbers: List<Int> = emptyList(),
        opposingRegionalVariants: List<String> = emptyList(),
        primaryRegionalVariant: String = "",
        legendaryForm: String = "",
        preferredRegion: RegionOfOrigin? = null,
        factionTheme: String? = null
    ) {
        if (!config.replaceBattleMusic) return

        // keep zone state for battle resume
        val routedContext = if (
            context == MusicContext.TRAINER_BATTLE &&
            activeZoneContext == MusicContext.BATTLE_TOWER
        ) MusicContext.BATTLE_TOWER_BATTLE else context

        val track = when (routedContext) {
            MusicContext.LEGENDARY_BATTLE -> {
                if (dexNumber == null) {
                    LOGGER.warn("[$MOD_ID] LEGENDARY_BATTLE without dex number, falling back to WILD_BATTLE pool")
                    pickFrom(TrackRegistry.tracksFor(MusicContext.WILD_BATTLE))
                } else TrackRegistry.legendaryTrackFor(
                    dexNumber,
                    legendaryForm,
                    primaryRegionalVariant
                )
            }
            MusicContext.WILD_BATTLE -> dexNumber?.let {
                TrackRegistry.wildTrackFor(it, primaryRegionalVariant)
            }
                ?: pickFrom(TrackRegistry.tracksFor(context))
            MusicContext.TRAINER_BATTLE,
            MusicContext.GYM_LEADER_BATTLE,
            MusicContext.ELITE_FOUR_BATTLE,
            MusicContext.CHAMPION_BATTLE,
            MusicContext.PVP_BATTLE,
            MusicContext.FRONTIER_BRAIN_BATTLE ->
                TrackRegistry.regionalBattleTrackFor(
                    context = routedContext,
                    dexNumbers = opposingDexNumbers,
                    preferredRegion = preferredRegion,
                    regionalVariants = opposingRegionalVariants
                ) ?: pickFrom(TrackRegistry.tracksFor(routedContext))
            MusicContext.FACTION_BATTLE -> factionTheme
                ?.let(TrackRegistry::factionBattleTrackFor)
                ?: TrackRegistry.regionalBattleTrackFor(
                    MusicContext.TRAINER_BATTLE,
                    opposingDexNumbers,
                    preferredRegion,
                    opposingRegionalVariants
                )
            MusicContext.BATTLE_TOWER_BATTLE -> TrackRegistry.battleTowerBattleTrack()
            else -> pickFrom(TrackRegistry.tracksFor(routedContext))
        } ?: run {
            LOGGER.warn("[$MOD_ID] No track for $context, leaving current music")
            return
        }

        debugLog(
            "[Battle start] Picked: ${track.id} for $routedContext" +
                (factionTheme?.let { " (factionTheme=$it)" }
                    ?: preferredRegion?.let { " (preferredRegion=$it)" }
                    ?: "")
        )
        // keep newest biome for resume
        play(routedContext, track)
    }

    fun playBossBattle(
        tierName: String,
        opposingDexNumbers: List<Int>,
        opposingRegionalVariants: List<String> = emptyList()
    ) {
        if (!config.replaceBattleMusic) return

        val pick = TrackRegistry.bossTrackFor(
            opposingDexNumbers,
            tierName,
            opposingRegionalVariants
        ) ?: run {
            LOGGER.warn("[$MOD_ID] No regional Boss track available, leaving current music")
            return
        }

        debugLog(
            "[Boss battle] tier=${tierName.uppercase()} region=${pick.region?.name ?: "UNKNOWN"} " +
                "source=${pick.source} track=${pick.track.id}"
        )
        play(pick.context, pick.track)
    }

    fun playVictoryTheme(track: MusicTrack) {
        if (!config.replaceBattleMusic) return
        debugLog("[Victory] Playing immediately: ${track.id}")
        play(
            context = MusicContext.VICTORY,
            track = track,
            force = true,
            fadeInSeconds = 0.20f,
            fadeOutSeconds = 0.25f
        )
    }

    fun finishVictoryTheme() {
        if (currentContext != MusicContext.VICTORY) return
        debugLog("[Victory] Ending; resuming latest world music")
        playAmbience()
    }


    fun playAmbience() {
        if (!config.replaceAmbience) {
            stopCurrent(); currentContext = MusicContext.AMBIENCE; return
        }

        // zone beats biome on resume
        val zoneContext = activeZoneContext
        val zoneTrack = activeZoneTrack
        if (zoneContext != null && zoneTrack != null) {
            overrideBiomeId = null
            overrideBiomeTrack = null
            debugLog("[Battle end resume] Restoring active zone: ${zoneTrack.id} ($zoneContext)")
            play(zoneContext, zoneTrack, force = true)
            return
        }

        // latest biome beats stale ambience
        val latestOverrideTrack = overrideBiomeTrack
        if (latestOverrideTrack != null) {
            val latestOverrideBiome = overrideBiomeId
            overrideBiomeId = null
            overrideBiomeTrack = null
            pendingTrack = null
            pendingBiomeId = null
            pendingSilenceEndsAtMillis = null
            debounceBiomeId = null
            debounceEndsAtMillis = null
            currentBiomeId = latestOverrideBiome
            currentAmbienceTrack = latestOverrideTrack
            val budgetMillis = randomRotationBudgetMillis()
            trackBudgetStartMillis[latestOverrideTrack.id] = System.currentTimeMillis()
            trackBudgetDurationMillis[latestOverrideTrack.id] = budgetMillis
            debugLog("[Battle end resume] Using latest observed biome: ${latestOverrideTrack.id} ($latestOverrideBiome)")
            play(MusicContext.AMBIENCE, latestOverrideTrack, force = true)
            return
        }

        // queued track beats stale ambience
        val track = if (pendingTrack != null) {
            val t = pendingTrack!!
            debugLog("[Battle end resume] Using pending track: ${t.id}")
            pendingTrack = null
            pendingSilenceEndsAtMillis = null
            currentBiomeId = pendingBiomeId
            pendingBiomeId = null
            currentAmbienceTrack = t
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

        // respawn clears old world state
        currentBiomeId = null
        currentAmbienceTrack = null
        activeZoneContext = null
        activeZoneTrack = null
        overrideBiomeId = null
        overrideBiomeTrack = null
        biomeTrackMemory.clear()
        trackBudgetStartMillis.clear()
        trackBudgetDurationMillis.clear()
        debounceBiomeId = null
        debounceEndsAtMillis = null

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
        overrideBiomeId = null; overrideBiomeTrack = null
        currentContext = MusicContext.AMBIENCE
        debugLog("[World join] Silence for ${config.worldJoinSilenceSeconds}s")
    }

    fun updateAmbienceBiome(biomeId: String) {
        if (!config.replaceAmbience) return

        // battle victory blocks playback not detection
        if (isWorldOverrideContext(currentContext)) {
            val track = resolveTrackForBiome(biomeId) ?: return
            if (overrideBiomeId != biomeId || overrideBiomeTrack?.id != track.id) {
                debugLog("[Biome update during override] Remembering: ${track.id} ($biomeId)")
            }
            overrideBiomeId = biomeId
            overrideBiomeTrack = track
            return
        }

        // zone state stays authoritative
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


    private var currentSoundStartedAtMillis: Long = 0L
    private var worldJoinReadyTicks = 0
    private val WORLD_JOIN_READY_TICKS = 10

    fun playZoneAmbience(context: MusicContext, track: MusicTrack) {
        if (!config.replaceAmbience) return

        activeZoneContext = context
        activeZoneTrack = track
        pendingSilenceEndsAtMillis = null; pendingBiomeId = null; pendingTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null

        // zone updates survive battle and victory
        if (isWorldOverrideContext(currentContext)) {
            debugLog("[Zone update during override] Remembering: ${track.id} ($context)")
            return
        }

        debugLog("[Zone enter] Playing: ${track.id} ($context)")
        play(context, track)
    }

    fun clearZone(currentBiomeId: String?) {
        activeZoneContext = null
        activeZoneTrack = null
        if (!config.replaceAmbience) return

        // zone exits survive battle and victory
        if (isWorldOverrideContext(currentContext)) {
            debugLog("[Zone exit during override] Cleared active zone; override music continues")
            return
        }
        // stop current is needed on zone exit
        stopCurrent()
        currentContext = MusicContext.AMBIENCE
        this.currentBiomeId = null
        this.currentAmbienceTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null
        pendingSilenceEndsAtMillis = null; pendingTrack = null; pendingBiomeId = null
        debugLog("[Zone exit] Forcing fresh biome re-detection (was in zone, now at biome $currentBiomeId)")
        // start debounce right after zone exit
        if (currentBiomeId != null) {
            handleBiomeTransitionDebounce(currentBiomeId)
        }
    }


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

        // timer starts when track commits
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

        // retry startup audio until engine is ready
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
        MusicContext.FACTION_BATTLE,
        MusicContext.FRONTIER_BRAIN_BATTLE,
        MusicContext.BATTLE_TOWER_BATTLE,
        MusicContext.LEGENDARY_BATTLE
    )

    private fun isWorldOverrideContext(context: MusicContext): Boolean =
        isBattleContext(context) || context == MusicContext.VICTORY

    private fun pickFrom(candidates: List<MusicTrack>): MusicTrack? =
        if (config.shuffleAmbienceTracks) candidates.randomOrNull() else candidates.firstOrNull()

    private fun play(
        context: MusicContext,
        track: MusicTrack,
        force: Boolean = false,
        fadeInSeconds: Float = config.crossfadeSeconds,
        fadeOutSeconds: Float = config.crossfadeSeconds
    ) {
        if (!force && context == currentContext && track.id == currentTrackId) return

        if (currentContext == MusicContext.AMBIENCE) ambienceSegmentStartedAtMillis = null

        stopCurrent(fadeOutSeconds)
        val instance = FadingSoundInstance(
            soundEvent = track.soundEvent,
            targetVolume = config.musicVolume,
            fadeInSeconds = fadeInSeconds,
            looping = track.loop
        )
        // missing oggs stay silent
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

    private fun stopCurrent(fadeOutSeconds: Float = config.crossfadeSeconds) {
        val sound = currentSound ?: return
        sound.beginFadeOut(fadeOutSeconds)
        currentSound = null
        currentTrackId = null
    }

    fun stopEverything() {
        stopCurrent()
        currentContext = MusicContext.AMBIENCE
        activeZoneContext = null
        activeZoneTrack = null
        overrideBiomeId = null
        overrideBiomeTrack = null
        pendingSilenceEndsAtMillis = null
        pendingTrack = null
        pendingBiomeId = null
        debounceBiomeId = null
        debounceEndsAtMillis = null
    }


}
