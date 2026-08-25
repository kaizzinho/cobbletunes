package com.kaizzinho.cobbletunes.client.sound

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundCategory

class ClientMusicPlayer(private val config: CobbleTunesClientConfig) {

    private fun debugLog(message: String) {
        if (config.debugLogging) {
            LOGGER.info("[$MOD_ID] [Debug] $message")
        }
    }

    private var currentContext: MusicContext = MusicContext.AMBIENCE
    private var currentTrackId: String? = null
    private var currentTrack: MusicTrack? = null
    private var currentSound: FadingSoundInstance? = null
    private var currentAmbienceTrack: MusicTrack? = null
    private var currentBiomeId: String? = null

    // zone state stays separate from playback
    private var activeZoneContext: MusicContext? = null
    private var activeZoneTrack: MusicTrack? = null
    private var villageCooldownEndsAtMillis: Long? = null

    private var volumeSuspended = false
    private var evolutionDucking = false

    // latest biome target survives battle and victory
    private var overrideBiomeId: String? = null
    private var overrideBiomeTrack: MusicTrack? = null

    private val gameCornerQueue = ArrayDeque<MusicTrack>()
    private var lastGameCornerTrackId: String? = null

    private val biomeTrackMemory: MutableMap<String, MusicTrack> = mutableMapOf()
    private val trackBudgetStartMillis: MutableMap<String, Long> = mutableMapOf()
    private val trackBudgetDurationMillis: MutableMap<String, Long> = mutableMapOf()
    private var ambienceSegmentStartedAtMillis: Long? = null

    private var pendingBiomeId: String? = null
    private var pendingTrack: MusicTrack? = null
    private var pendingSilenceEndsAtMillis: Long? = null

    private var debounceBiomeId: String? = null
    private var debounceEndsAtMillis: Long? = null

    private val VILLAGE_COOLDOWN_MIN_SECONDS = 5f
    private val VILLAGE_COOLDOWN_MAX_SECONDS = 60f
    private val EVOLUTION_DUCK_MULTIPLIER = 0.68f


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

    fun playExactTrainerBattle(track: MusicTrack) {
        if (!config.replaceBattleMusic) return
        debugLog("[Special trainer] Playing exact track: ${track.id}")
        play(MusicContext.TRAINER_BATTLE, track)
    }

    fun playBossBattle(
        tierName: String,
        opposingDexNumbers: List<Int>,
        opposingRegionalVariants: List<String> = emptyList()
    ) {
        playTierWeightedBattle("Boss", tierName, opposingDexNumbers, opposingRegionalVariants)
    }

    fun playRaidBattle(
        tierName: String,
        opposingDexNumbers: List<Int>,
        opposingRegionalVariants: List<String> = emptyList()
    ) {
        playTierWeightedBattle("Raid", tierName, opposingDexNumbers, opposingRegionalVariants)
    }

    private fun playTierWeightedBattle(
        label: String,
        tierName: String,
        opposingDexNumbers: List<Int>,
        opposingRegionalVariants: List<String>
    ) {
        if (!config.replaceBattleMusic) return

        val pick = TrackRegistry.bossTrackFor(
            opposingDexNumbers,
            tierName,
            opposingRegionalVariants
        ) ?: run {
            LOGGER.warn("[$MOD_ID] No regional $label track available, leaving current music")
            return
        }

        debugLog(
            "[$label battle] tier=${tierName.uppercase()} region=${pick.region?.name ?: "UNKNOWN"} " +
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
            if (zoneContext == MusicContext.VILLAGE_STRUCTURE) {
                villageCooldownEndsAtMillis = null
            }
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
        debugLog(
            if (isMusicMutedNow()) "[Death] Music muted so respawn silence is skipped"
            else "[Death] Stopping battle music 30s silence before biome re-detect"
        )
        stopCurrent()
        currentContext = MusicContext.AMBIENCE

        // respawn clears old world state
        currentBiomeId = null
        currentAmbienceTrack = null
        activeZoneContext = null
        activeZoneTrack = null
        villageCooldownEndsAtMillis = null
        resetGameCornerQueue()
        lastGameCornerTrackId = null
        overrideBiomeId = null
        overrideBiomeTrack = null
        biomeTrackMemory.clear()
        trackBudgetStartMillis.clear()
        trackBudgetDurationMillis.clear()
        debounceBiomeId = null
        debounceEndsAtMillis = null

        pendingBiomeId = null
        pendingTrack = null
        pendingSilenceEndsAtMillis = if (isMusicMutedNow()) {
            null
        } else {
            System.currentTimeMillis() + 30_000L
        }
    }

    fun beginWorldJoinSilence() {
        stopCurrent()
        ambienceSegmentStartedAtMillis = null
        worldJoinReadyTicks = 0

        if (isMusicMutedNow()) {
            pendingSilenceEndsAtMillis = null
            pendingBiomeId = null
            pendingTrack = null
            debounceBiomeId = null
            debounceEndsAtMillis = null
            currentBiomeId = null
            currentAmbienceTrack = null
            activeZoneContext = null
            activeZoneTrack = null
            villageCooldownEndsAtMillis = null
            resetGameCornerQueue()
            lastGameCornerTrackId = null
            overrideBiomeId = null
            overrideBiomeTrack = null
            currentContext = MusicContext.AMBIENCE
            debugLog("[World join] Music muted so startup silence is skipped")
            return
        }

        pendingSilenceEndsAtMillis = System.currentTimeMillis() +
                (config.worldJoinSilenceSeconds * 1000).toLong()
        pendingBiomeId = null; pendingTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null
        currentBiomeId = null; currentAmbienceTrack = null
        activeZoneContext = null; activeZoneTrack = null
        resetGameCornerQueue(); lastGameCornerTrackId = null
        overrideBiomeId = null; overrideBiomeTrack = null
        currentContext = MusicContext.AMBIENCE
        debugLog("[World join] Silence for ${config.worldJoinSilenceSeconds}s")
    }

    fun updateAmbienceBiome(biomeId: String) {
        if (!config.replaceAmbience) return

        if (isMusicMutedNow() && !isWorldOverrideContext(currentContext) && activeZoneContext == null) {
            val track = resolveTrackForBiome(biomeId) ?: return
            currentBiomeId = biomeId
            currentAmbienceTrack = track
            pendingSilenceEndsAtMillis = null
            pendingBiomeId = null
            pendingTrack = null
            debounceBiomeId = null
            debounceEndsAtMillis = null
            currentContext = MusicContext.AMBIENCE
            currentTrack = track
            currentTrackId = track.id
            return
        }

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

    fun enterGameCorner() {
        if (!config.replaceAmbience) return
        if (activeZoneContext == MusicContext.GAME_CORNER) return

        val track = nextGameCornerTrack() ?: run {
            LOGGER.warn("[$MOD_ID] No Game Corner tracks registered")
            return
        }
        playZoneAmbience(MusicContext.GAME_CORNER, track)
    }

    fun tick() {
        syncVolumeSuspension()
        if (volumeSuspended) return

        when (currentContext) {
            MusicContext.GAME_CORNER -> tickGameCorner()
            MusicContext.VILLAGE_STRUCTURE -> tickVillageStructure()
            else -> Unit
        }
    }

    private fun tickGameCorner() {
        if (activeZoneContext != MusicContext.GAME_CORNER) return

        val sound = currentSound ?: return
        if (System.currentTimeMillis() - currentSoundStartedAtMillis < 1_000L) return
        if (MinecraftClient.getInstance().soundManager.isPlaying(sound)) return

        val next = nextGameCornerTrack() ?: return
        activeZoneTrack = next
        debugLog("[Game corner] Track finished; next: ${next.id}")
        play(
            context = MusicContext.GAME_CORNER,
            track = next,
            force = true,
            fadeInSeconds = 0.15f,
            fadeOutSeconds = 0f
        )
    }

    private fun tickVillageStructure() {
        if (activeZoneContext != MusicContext.VILLAGE_STRUCTURE) return
        val track = activeZoneTrack ?: return
        val now = System.currentTimeMillis()

        val cooldownEnds = villageCooldownEndsAtMillis
        if (cooldownEnds != null) {
            if (now < cooldownEnds) return
            villageCooldownEndsAtMillis = null
            debugLog("[Village] Cooldown ended replaying ${track.id}")
            play(
                context = MusicContext.VILLAGE_STRUCTURE,
                track = track,
                force = true,
                fadeInSeconds = 0.15f,
                fadeOutSeconds = 0f
            )
            return
        }

        if (now - currentSoundStartedAtMillis < 1_000L) return
        val sound = currentSound
        if (sound != null && MinecraftClient.getInstance().soundManager.isPlaying(sound)) return

        stopPhysicalSound(0f)
        val seconds = randomSecondsInRange(VILLAGE_COOLDOWN_MIN_SECONDS, VILLAGE_COOLDOWN_MAX_SECONDS)
        villageCooldownEndsAtMillis = now + (seconds * 1000).toLong()
        debugLog("[Village] Track finished waiting ${seconds.toInt()}s before replay")
    }

    private fun nextGameCornerTrack(): MusicTrack? {
        val pool = TrackRegistry.tracksFor(MusicContext.GAME_CORNER)
        if (pool.isEmpty()) return null

        if (gameCornerQueue.isEmpty()) {
            val shuffled = pool.shuffled().toMutableList()
            if (shuffled.size > 1 && shuffled.first().id == lastGameCornerTrackId) {
                val swapIndex = shuffled.indexOfFirst { it.id != lastGameCornerTrackId }
                if (swapIndex > 0) {
                    val first = shuffled[0]
                    shuffled[0] = shuffled[swapIndex]
                    shuffled[swapIndex] = first
                }
            }
            gameCornerQueue.addAll(shuffled)
        }

        return gameCornerQueue.removeFirst().also { lastGameCornerTrackId = it.id }
    }

    private fun resetGameCornerQueue() {
        gameCornerQueue.clear()
    }

    fun playZoneAmbience(context: MusicContext, track: MusicTrack) {
        if (!config.replaceAmbience) return

        if (activeZoneContext == MusicContext.GAME_CORNER && context != MusicContext.GAME_CORNER) {
            resetGameCornerQueue()
        }
        villageCooldownEndsAtMillis = null
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

    fun playVillageAmbience(track: MusicTrack) {
        if (!config.replaceAmbience) return

        if (activeZoneContext == MusicContext.GAME_CORNER) resetGameCornerQueue()
        val oneShotTrack = track.copy(loop = false)
        activeZoneContext = MusicContext.VILLAGE_STRUCTURE
        activeZoneTrack = oneShotTrack
        villageCooldownEndsAtMillis = null
        pendingSilenceEndsAtMillis = null; pendingBiomeId = null; pendingTrack = null
        debounceBiomeId = null; debounceEndsAtMillis = null

        if (isWorldOverrideContext(currentContext)) {
            debugLog("[Village update during override] Remembering: ${oneShotTrack.id}")
            return
        }

        debugLog("[Village enter] Playing once: ${oneShotTrack.id}")
        play(MusicContext.VILLAGE_STRUCTURE, oneShotTrack)
    }

    fun clearZone(currentBiomeId: String?) {
        if (activeZoneContext == MusicContext.GAME_CORNER) resetGameCornerQueue()
        activeZoneContext = null
        activeZoneTrack = null
        villageCooldownEndsAtMillis = null
        if (!config.replaceAmbience) return

        // zone exits survive battle and victory
        if (isWorldOverrideContext(currentContext)) {
            debugLog("[Zone exit during override] Cleared active zone; override music continues")
            return
        }

        if (isMusicMutedNow()) {
            stopCurrent(0f)
            currentContext = MusicContext.AMBIENCE
            pendingSilenceEndsAtMillis = null
            pendingTrack = null
            pendingBiomeId = null
            debounceBiomeId = null
            debounceEndsAtMillis = null
            this.currentBiomeId = currentBiomeId
            this.currentAmbienceTrack = currentBiomeId?.let(::resolveTrackForBiome)
            currentTrack = currentAmbienceTrack
            currentTrackId = currentAmbienceTrack?.id
            debugLog("[Zone exit] Music muted so biome resume is armed without debounce")
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

    fun isVolumeSuspended(): Boolean = isMusicMutedNow()

    fun setEvolutionDucking(active: Boolean) {
        if (evolutionDucking == active) return
        evolutionDucking = active
        val multiplier = evolutionVolumeMultiplierFor(currentContext)
        currentSound?.setExternalVolumeMultiplier(multiplier)
        debugLog("[Evolution] world music duck=${if (active) "on" else "off"}")
    }

    private fun evolutionVolumeMultiplierFor(context: MusicContext): Float =
        if (evolutionDucking && isEvolutionDuckable(context)) EVOLUTION_DUCK_MULTIPLIER else 1f

    private fun isEvolutionDuckable(context: MusicContext): Boolean =
        !isBattleContext(context) && context != MusicContext.VICTORY && context != MusicContext.MENU

    private fun isMusicMutedNow(): Boolean {
        val minecraftMusicVolume = MinecraftClient.getInstance()
            .options
            .getSoundVolumeOption(SoundCategory.MUSIC)
            .value
            .toFloat()
        return config.musicVolume <= 0.0001f || minecraftMusicVolume <= 0.0001f
    }

    private fun syncVolumeSuspension() {
        val muted = isMusicMutedNow()
        if (muted == volumeSuspended) return
        volumeSuspended = muted

        if (muted) {
            stopPhysicalSound(0f)
            pendingSilenceEndsAtMillis = null
            pendingBiomeId = null
            pendingTrack = null
            debounceBiomeId = null
            debounceEndsAtMillis = null
            villageCooldownEndsAtMillis = null
            trackBudgetStartMillis.clear()
            trackBudgetDurationMillis.clear()
            debugLog("[Volume] Music suspended at zero volume")
            return
        }

        debugLog("[Volume] Music restored resuming current context immediately")
        resumeImmediatelyAfterVolume()
    }

    private fun resumeImmediatelyAfterVolume() {
        if (MinecraftClient.getInstance().world == null) {
            if (currentContext == MusicContext.MENU && config.replaceMenuMusic) {
                currentTrack?.let {
                    play(MusicContext.MENU, it, force = true, fadeInSeconds = 0.15f, fadeOutSeconds = 0f)
                }
            }
            return
        }

        if (isWorldOverrideContext(currentContext)) {
            val track = currentTrack
            if (track != null) {
                play(currentContext, track, force = true, fadeInSeconds = 0.15f, fadeOutSeconds = 0f)
                return
            }
        }

        val zoneContext = activeZoneContext
        val zoneTrack = activeZoneTrack
        if (config.replaceAmbience && zoneContext != null && zoneTrack != null) {
            villageCooldownEndsAtMillis = null
            play(zoneContext, zoneTrack, force = true, fadeInSeconds = 0.15f, fadeOutSeconds = 0f)
            return
        }

        if (config.replaceAmbience) {
            val track = currentAmbienceTrack ?: overrideBiomeTrack
            if (track != null) {
                overrideBiomeId = null
                overrideBiomeTrack = null
                pendingSilenceEndsAtMillis = null
                pendingBiomeId = null
                pendingTrack = null
                debounceBiomeId = null
                debounceEndsAtMillis = null
                val budgetMillis = randomRotationBudgetMillis()
                trackBudgetStartMillis[track.id] = System.currentTimeMillis()
                trackBudgetDurationMillis[track.id] = budgetMillis
                play(MusicContext.AMBIENCE, track, force = true, fadeInSeconds = 0.15f, fadeOutSeconds = 0f)
                return
            }
        }

    }

    private fun pickFrom(candidates: List<MusicTrack>): MusicTrack? =
        if (config.shuffleAmbienceTracks) candidates.randomOrNull() else candidates.firstOrNull()

    private fun play(
        context: MusicContext,
        track: MusicTrack,
        force: Boolean = false,
        fadeInSeconds: Float = config.crossfadeSeconds,
        fadeOutSeconds: Float = config.crossfadeSeconds
    ) {
        val mc = MinecraftClient.getInstance()
        val sameTrackAudible = currentSound?.let(mc.soundManager::isPlaying) == true
        if (!force && context == currentContext && track.id == currentTrackId && sameTrackAudible) return

        if (currentContext == MusicContext.AMBIENCE) ambienceSegmentStartedAtMillis = null

        stopPhysicalSound(fadeOutSeconds)
        currentContext = context
        currentTrack = track
        currentTrackId = track.id
        currentSoundStartedAtMillis = System.currentTimeMillis()

        if (context == MusicContext.AMBIENCE) {
            ambienceSegmentStartedAtMillis = System.currentTimeMillis()
        }

        if (isMusicMutedNow()) {
            return
        }

        // missing oggs stay silent
        if (mc.soundManager.getKeys().none {
                it.toString() == track.soundEvent.id.toString()
            }) {
            debugLog("[Play] Skipping ${track.id} — no sounds registered in resource pack")
            return
        }

        val instance = FadingSoundInstance(
            soundEvent = track.soundEvent,
            targetVolume = config.musicVolume,
            fadeInSeconds = fadeInSeconds,
            looping = track.loop
        )
        instance.setExternalVolumeMultiplier(
            evolutionVolumeMultiplierFor(context),
            transitionSeconds = 0f
        )
        mc.soundManager.play(instance)
        currentSound = instance
    }

    private fun stopPhysicalSound(fadeOutSeconds: Float = config.crossfadeSeconds) {
        val sound = currentSound ?: return
        sound.beginFadeOut(fadeOutSeconds)
        currentSound = null
    }

    private fun stopCurrent(fadeOutSeconds: Float = config.crossfadeSeconds) {
        stopPhysicalSound(fadeOutSeconds)
        currentTrack = null
        currentTrackId = null
    }

    fun stopEverything() {
        stopCurrent()
        currentContext = MusicContext.AMBIENCE
        activeZoneContext = null
        activeZoneTrack = null
        villageCooldownEndsAtMillis = null
        resetGameCornerQueue()
        lastGameCornerTrackId = null
        overrideBiomeId = null
        overrideBiomeTrack = null
        pendingSilenceEndsAtMillis = null
        pendingTrack = null
        pendingBiomeId = null
        debounceBiomeId = null
        debounceEndsAtMillis = null
    }


}
