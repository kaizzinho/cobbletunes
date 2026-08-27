package com.kaizzinho.cobbletunes.client

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import com.kaizzinho.cobbletunes.client.compat.lootmenu.LootMenuVictoryBridge
import com.kaizzinho.cobbletunes.client.compat.standalone.ClientStandaloneBridge
import com.kaizzinho.cobbletunes.client.evolution.ClientEvolutionWatcher
import com.kaizzinho.cobbletunes.client.sound.ClientMusicPlayer
import com.kaizzinho.cobbletunes.client.sound.MusicContext
import com.kaizzinho.cobbletunes.client.sound.RegionOfOrigin
import com.kaizzinho.cobbletunes.client.sound.TrackRegistry
import com.kaizzinho.cobbletunes.client.sound.VictoryKind
import com.kaizzinho.cobbletunes.client.sound.VictoryRequest
import com.kaizzinho.cobbletunes.network.BattleMusicEndPayload
import com.kaizzinho.cobbletunes.network.BattleMusicStartPayload
import com.kaizzinho.cobbletunes.network.BattleVictoryPayload
import com.kaizzinho.cobbletunes.network.PlayerDeathPayload
import com.kaizzinho.cobbletunes.network.PokemonCapturedPayload
import com.kaizzinho.cobbletunes.network.StructureZonePayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class CobbleTunesClient : ClientModInitializer {
    private var onDeathScreen = false
    private lateinit var standaloneBridge: ClientStandaloneBridge
    private lateinit var evolutionWatcher: ClientEvolutionWatcher

    private fun debugLog(message: String) {
        if (config.debugLogging) {
            LOGGER.info("[$MOD_ID] [Debug] $message")
        }
    }

    companion object {
        lateinit var config: CobbleTunesClientConfig
            private set
        lateinit var musicPlayer: ClientMusicPlayer
            private set

        private const val AMBIENCE_CHECK_INTERVAL_TICKS = 20
        private const val BATTLE_VICTORY_MILLIS = 3_000L
        private const val CAPTURE_VICTORY_MILLIS = 3_000L
        private const val RAID_VICTORY_MILLIS = 5_000L
        private const val VICTORY_FADE_OUT_DELAY_MILLIS = 2_000L

        private val VILLAGE_STRUCTURE_CATEGORIES = setOf(
            "bca_village_large",
            "bca_village_mid",
            "bca_village_small",
            "village_desert",
            "village_plains",
            "village_savanna",
            "village_snowy",
            "village_taiga"
        )

        fun applyConfig(updated: CobbleTunesClientConfig): Boolean {
            if (!updated.save()) return false
            config.copyFrom(updated)
            return true
        }

        private val STRUCTURE_TO_REGION: Map<String, RegionOfOrigin> = mapOf(
            "cobbleverse:brock"              to RegionOfOrigin.KANTO,
            "cobbleverse:misty"              to RegionOfOrigin.KANTO,
            "cobbleverse:ltsurge"            to RegionOfOrigin.KANTO,
            "cobbleverse:erika"              to RegionOfOrigin.KANTO,
            "cobbleverse:koga"               to RegionOfOrigin.KANTO,
            "cobbleverse:sabrina"            to RegionOfOrigin.KANTO,
            "cobbleverse:blaine"             to RegionOfOrigin.KANTO,
            "cobbleverse:giovanni"           to RegionOfOrigin.KANTO,
            "cobbleverse:kanto_league"       to RegionOfOrigin.KANTO,
            "cobbleverse:team_rocket_tower"  to RegionOfOrigin.KANTO,
            "cobbleverse:crown_spire"        to RegionOfOrigin.KANTO,
            "cobbleverse:dawn_tower"         to RegionOfOrigin.KANTO,
            "cobbleverse:dusk_tower"         to RegionOfOrigin.KANTO,
            "cobbleverse:valerio"            to RegionOfOrigin.JOHTO,
            "cobbleverse:chiara"             to RegionOfOrigin.JOHTO,
            "cobbleverse:angelo"             to RegionOfOrigin.JOHTO,
            "cobbleverse:alfredo"            to RegionOfOrigin.JOHTO,
            "cobbleverse:furio"              to RegionOfOrigin.JOHTO,
            "cobbleverse:jasmine"            to RegionOfOrigin.JOHTO,
            "cobbleverse:raffaello"          to RegionOfOrigin.JOHTO,
            "cobbleverse:sandra"             to RegionOfOrigin.JOHTO,
            "cobbleverse:johto_league"       to RegionOfOrigin.JOHTO,
            "cobbleverse:rocket_radio_tower" to RegionOfOrigin.JOHTO,
            "cobbleverse:rudi"               to RegionOfOrigin.HOENN,
            "cobbleverse:adriano"            to RegionOfOrigin.HOENN,
            "cobbleverse:tell_pat"           to RegionOfOrigin.HOENN,
            "cobbleverse:alice"              to RegionOfOrigin.HOENN,
            "cobbleverse:norman"             to RegionOfOrigin.HOENN,
            "cobbleverse:fiammetta"          to RegionOfOrigin.HOENN,
            "cobbleverse:walter"             to RegionOfOrigin.HOENN,
            "cobbleverse:petra"              to RegionOfOrigin.HOENN,
            "cobbleverse:hoenn_league"       to RegionOfOrigin.HOENN,
            "cobbleverse:gardenia"           to RegionOfOrigin.SINNOH,
            "cobbleverse:ferruccio"          to RegionOfOrigin.SINNOH,
            "cobbleverse:marzia"             to RegionOfOrigin.SINNOH,
            "cobbleverse:fannie"             to RegionOfOrigin.SINNOH,
            "cobbleverse:corrado"            to RegionOfOrigin.SINNOH,
            "cobbleverse:bianca"             to RegionOfOrigin.SINNOH,
            "cobbleverse:omar"               to RegionOfOrigin.SINNOH,
            "cobbleverse:pedro"              to RegionOfOrigin.SINNOH,
            "cobbleverse:sinnoh_league"      to RegionOfOrigin.SINNOH,
            "cobbleverse:team_galactic_hq"   to RegionOfOrigin.SINNOH,
        )
    }

    private var ambienceCheckCounter = 0
    private var lastWorld: net.minecraft.client.world.ClientWorld? = null
    private var pendingMenuTrack: com.kaizzinho.cobbletunes.client.sound.MusicTrack? = null
    private var menuReadyTicks = 0

    private var lastBattleVictoryRequest: VictoryRequest? = null
    private var lastBattleWasRaid = false
    private var battleVictoryTriggered = false
    private var battleVictoryActive = false
    private var battleVictoryEndsAtMillis: Long? = null
    private var battleVictoryTrack: com.kaizzinho.cobbletunes.client.sound.MusicTrack? = null
    private var lootVictoryActive = false
    private var lootMenuWasOpen = false
    private var lootVictoryFadeOutAtMillis: Long? = null
    private var captureVictoryActive = false
    private var captureVictoryEndsAtMillis: Long? = null
    private var raidVictoryActive = false
    private var raidVictoryEndsAtMillis: Long? = null
    private val lastVillageTrackByCategory = mutableMapOf<String, String>()

    private var lowHpBeepsRemaining = 0
    private var lowHpBeepCooldownTicks = 0
    private var lowHpCheckCounter = 0
    private var lastLowHpPokemonUuid: java.util.UUID? = null
    private val LOW_HP_BEEP_INTERVAL_TICKS = 20
    private val LOW_HP_CHECK_INTERVAL_TICKS = 10
    private val LOW_HP_VOLUME_MULTIPLIER = 0.80f

    override fun onInitializeClient() {
        LOGGER.info("[$MOD_ID] Initializing client music system...")

        config = CobbleTunesClientConfig.load()
        TrackRegistry.bootstrap()
        musicPlayer = ClientMusicPlayer(config)

        registerNetworkReceivers()
        standaloneBridge = ClientStandaloneBridge(
            debugLog = ::debugLog,
            onBattleStart = { handleBattleStart(it, "client") },
            onBattleEnd = { handleBattleEnd("client") },
            onBattleVictory = { handleBattleVictory("client") },
            shouldUseEarlyFaintVictory = { !lastBattleWasRaid },
            onCapture = { dex, regional -> handlePokemonCaptured(dex, regional, "client") }
        )
        standaloneBridge.register()
        evolutionWatcher = ClientEvolutionWatcher(
            config = config,
            musicPlayer = musicPlayer,
            debugLog = ::debugLog
        )
        evolutionWatcher.register()
        registerVanillaMusicSuppression()
        registerBiomeAmbienceWatcher()
        registerDeathScreenWatcher()
        registerMenuMusicWatcher()
        registerLootMenuVictoryWatcher()
        registerBattleVictoryWatcher()
        registerCaptureVictoryWatcher()
        registerRaidVictoryWatcher()
        registerLowHpWatcher()

        LOGGER.info("[$MOD_ID] Client init complete.")
    }

    private fun registerNetworkReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(BattleMusicStartPayload.ID) { payload, context ->
            context.client().execute { handleBattleStart(payload, "server") }
        }
        ClientPlayNetworking.registerGlobalReceiver(BattleMusicEndPayload.ID) { _, context ->
            context.client().execute { handleBattleEnd("server") }
        }
        ClientPlayNetworking.registerGlobalReceiver(BattleVictoryPayload.ID) { _, context ->
            context.client().execute { handleBattleVictory("server") }
        }
        ClientPlayNetworking.registerGlobalReceiver(PokemonCapturedPayload.ID) { payload, context ->
            context.client().execute {
                handlePokemonCaptured(payload.dexNumber, payload.regionalVariant, "server")
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(PlayerDeathPayload.ID) { _, context ->
            context.client().execute {
                clearVictoryState(finishMusic = false)
                musicPlayer.handlePlayerDeath()
            }
        }
        ClientPlayNetworking.registerGlobalReceiver(StructureZonePayload.ID) { payload, context ->
            context.client().execute { handleStructureZone(payload.zoneId, "server") }
        }
    }

    private fun handleBattleStart(payload: BattleMusicStartPayload, source: String) {
        // keep string routes for old packet compat
        val routeParts = payload.trainerTier.split('|', limit = 2)
        val routeHead = routeParts.firstOrNull().orEmpty()
        val routeValue = routeParts.getOrNull(1).orEmpty()

        clearVictoryState(finishMusic = false)
        lastBattleWasRaid = routeHead == "raid"
        lastBattleVictoryRequest = buildVictoryRequest(payload, routeHead, routeValue)

        if (routeHead == "boss" || routeHead == "raid") {
            val isRaid = routeHead == "raid"
            debugLog(
                "[${if (isRaid) "Raid" else "Boss"} route] source=$source raw='${payload.trainerTier}' " +
                    "tier='${routeValue.ifBlank { "unknown" }}' opposingDex=${payload.opposingDexNumbers}"
            )
            if (isRaid) {
                musicPlayer.playRaidBattle(
                    tierName = routeValue,
                    opposingDexNumbers = payload.opposingDexNumbers,
                    opposingRegionalVariants = payload.opposingRegionalVariants
                )
            } else {
                musicPlayer.playBossBattle(
                    tierName = routeValue,
                    opposingDexNumbers = payload.opposingDexNumbers,
                    opposingRegionalVariants = payload.opposingRegionalVariants
                )
            }
            return
        }

        val specialPoolRoute = parseSpecialTrainerPoolRoute(routeHead)
        if (specialPoolRoute != null) {
            val track = TrackRegistry.specialTrainerTrackFor(specialPoolRoute.poolId)
            if (track != null) {
                debugLog(
                    "[Special trainer pool] source=$source raw='${payload.trainerTier}' " +
                        "pool='${specialPoolRoute.poolId}' track='${track.id}' " +
                        "role='${specialPoolRoute.roleId}' region='${routeValue.ifBlank { "unknown" }}'"
                )
                musicPlayer.playExactTrainerBattle(track)
                return
            }
            debugLog(
                "[Special trainer pool] Missing/empty pool '${specialPoolRoute.poolId}' using normal trainer fallback"
            )
        }

        val specialRoute = parseSpecialTrainerRoute(routeHead)
        if (specialRoute != null) {
            val track = TrackRegistry.trackById(specialRoute.trackId)
            if (track != null) {
                debugLog(
                    "[Special trainer route] source=$source raw='${payload.trainerTier}' " +
                        "track='${specialRoute.trackId}' role='${specialRoute.roleId}' " +
                        "region='${routeValue.ifBlank { "unknown" }}'"
                )
                musicPlayer.playExactTrainerBattle(track)
                return
            }
            debugLog(
                "[Special trainer route] Missing track '${specialRoute.trackId}' using normal trainer fallback"
            )
        }

        val factionTheme = routeHead
            .takeIf { it.startsWith("faction:") }
            ?.substringAfter("faction:")
            ?.takeIf { it.isNotBlank() }
        val trainerTier = routeHead.takeUnless { factionTheme != null }.orEmpty()
        val routeRegion = routeValue.takeIf { it.isNotBlank() }?.let { regionId ->
            RegionOfOrigin.entries.firstOrNull {
                it.name.equals(regionId, ignoreCase = true)
            }
        }
        val variantRegion = RegionOfOrigin.fromRegionalVariant(payload.primaryRegionalVariant)
        val preferredRegion = routeRegion ?: variantRegion.takeIf { payload.isWild }

        val musicContext = when {
            payload.isWild -> if (payload.isLegendary) MusicContext.LEGENDARY_BATTLE else MusicContext.WILD_BATTLE
            payload.isTrainer && factionTheme != null -> MusicContext.FACTION_BATTLE
            payload.isTrainer -> when (trainerTier) {
                "leader" -> MusicContext.GYM_LEADER_BATTLE
                "e4" -> MusicContext.ELITE_FOUR_BATTLE
                "champ" -> MusicContext.CHAMPION_BATTLE
                "rival" -> MusicContext.PVP_BATTLE
                "frontier" -> MusicContext.FRONTIER_BRAIN_BATTLE
                else -> MusicContext.TRAINER_BATTLE
            }
            else -> MusicContext.PVP_BATTLE
        }
        val dexNumber = payload.dexNumber.takeIf { it >= 0 }

        debugLog(
            "[Battle route] source=$source raw='${payload.trainerTier}' tier='$trainerTier' " +
                "factionTheme='${factionTheme.orEmpty()}' " +
                "region=${preferredRegion?.name ?: "roster-vote"} " +
                "variant='${payload.primaryRegionalVariant.ifEmpty { "standard" }}' context=$musicContext"
        )

        musicPlayer.playBattleContext(
            context = musicContext,
            dexNumber = dexNumber,
            opposingDexNumbers = payload.opposingDexNumbers,
            opposingRegionalVariants = payload.opposingRegionalVariants,
            primaryRegionalVariant = payload.primaryRegionalVariant,
            legendaryForm = payload.legendaryForm,
            preferredRegion = preferredRegion,
            factionTheme = factionTheme
        )
    }

    private fun handleBattleEnd(source: String) {
        if (captureVictoryActive) {
            debugLog("[Capture victory] Ignoring battle end while capture theme is active")
            return
        }
        if (raidVictoryActive) {
            debugLog("[Raid victory] Ignoring battle end while raid Victory is active")
            return
        }
        if (battleVictoryActive || lootVictoryActive) {
            debugLog("[Victory] Ignoring battle end while Victory theme is active")
            return
        }
        if (LootMenuVictoryBridge.available && battleVictoryTrack != null) {
            debugLog("[Victory] Preserving Victory route in case Loot Menu opens")
            musicPlayer.playAmbience()
            return
        }
        debugLog("[Battle end] source=$source resuming world music")
        clearVictoryState(finishMusic = false)
        musicPlayer.playAmbience()
    }

    private fun handleBattleVictory(source: String) {
        if (raidVictoryActive) {
            debugLog("[Raid victory] Ignoring duplicate victory source=$source")
            return
        }
        if (battleVictoryTriggered || battleVictoryActive || lootVictoryActive) {
            debugLog("[Victory] Ignoring duplicate victory source=$source because Victory already started")
            return
        }

        if (lastBattleWasRaid) {
            startRaidVictory()
            return
        }

        val request = lastBattleVictoryRequest
        lastBattleWasRaid = false

        if (request != null && config.replaceBattleMusic) {
            val track = TrackRegistry.victoryTrackFor(request)
            if (track != null) {
                val now = System.currentTimeMillis()
                battleVictoryTriggered = true
                battleVictoryTrack = track
                battleVictoryActive = true
                lootVictoryFadeOutAtMillis = null
                battleVictoryEndsAtMillis = now + BATTLE_VICTORY_MILLIS + VICTORY_FADE_OUT_DELAY_MILLIS

                val lootOpen = LootMenuVictoryBridge.isLootScreen(
                    net.minecraft.client.MinecraftClient.getInstance().currentScreen
                )
                if (lootOpen) {
                    lootMenuWasOpen = true
                    lootVictoryActive = true
                    battleVictoryActive = false
                    battleVictoryEndsAtMillis = null
                }

                musicPlayer.playVictoryTheme(track)
                debugLog(
                    if (lootOpen) {
                        "[Victory] source=$source starting ${track.id} immediately and holding for open Loot Menu"
                    } else {
                        "[Victory] source=$source starting ${track.id} immediately for ${BATTLE_VICTORY_MILLIS / 1000}s " +
                            "then holding ${VICTORY_FADE_OUT_DELAY_MILLIS / 1000}s before fade-out"
                    }
                )
                return
            }
            debugLog("[Victory] No theme for ${request.region}/${request.kind}")
            battleVictoryTriggered = true
            musicPlayer.playAmbience()
            return
        }
        if (request == null) {
            debugLog("[Victory] source=$source arrived before a routable Victory request; waiting for fallback event")
        }
    }

    private fun handlePokemonCaptured(dexNumber: Int, regionalVariant: String, source: String) {
        clearVictoryState(finishMusic = false)

        if (!config.replaceBattleMusic) {
            musicPlayer.playAmbience()
            return
        }

        val region = RegionOfOrigin.fromRegionalVariant(regionalVariant)
            ?: RegionOfOrigin.fromDexNumber(dexNumber)
        val request = region?.let { VictoryRequest(it, VictoryKind.WILD) }
        val track = request?.let(TrackRegistry::victoryTrackFor)

        if (track == null) {
            debugLog(
                "[Capture victory] source=$source no theme for dex=$dexNumber " +
                    "regional='${regionalVariant.ifEmpty { "standard" }}'"
            )
            musicPlayer.playAmbience()
            return
        }

        captureVictoryActive = true
        captureVictoryEndsAtMillis = System.currentTimeMillis() +
            CAPTURE_VICTORY_MILLIS + VICTORY_FADE_OUT_DELAY_MILLIS
        musicPlayer.playVictoryTheme(track)
        debugLog(
            "[Capture victory] source=$source playing ${track.id} for ${CAPTURE_VICTORY_MILLIS / 1000}s " +
                "then holding ${VICTORY_FADE_OUT_DELAY_MILLIS / 1000}s before fade-out"
        )
    }

    private fun handleStructureZone(zoneId: String, source: String) {
        if (zoneId.isBlank()) {
            val mc = net.minecraft.client.MinecraftClient.getInstance()
            val pos = mc.player?.blockPos
            val biomeId = if (pos != null) {
                mc.world?.getBiome(pos)?.key?.orElse(null)?.value?.toString()
            } else null
            musicPlayer.clearZone(biomeId)
            return
        }

        debugLog("[Zone] source=$source id='$zoneId'")
        when (zoneId) {
            "cobbletunes:pokecenter" -> {
                val track = TrackRegistry.tracksFor(MusicContext.POKECENTER).randomOrNull()
                if (track != null) musicPlayer.playZoneAmbience(MusicContext.POKECENTER, track)
                return
            }
            "cobbletunes:pokemart" -> {
                val track = TrackRegistry.tracksFor(MusicContext.POKEMART).randomOrNull()
                if (track != null) musicPlayer.playZoneAmbience(MusicContext.POKEMART, track)
                return
            }
            "cobbletunes:game_corner",
            "cobbletunes:casino" -> {
                musicPlayer.enterGameCorner()
                return
            }
            "cobbletunes:gym_kanto",
            "cobbletunes:gym_johto",
            "cobbletunes:gym_hoenn",
            "cobbletunes:gym_sinnoh",
            "cobbletunes:gym_unova" -> {
                val region = parseRegion(zoneId.substringAfter("cobbletunes:gym_"))
                val track = region?.let(TrackRegistry::gymAmbienceTrackFor)
                if (track != null) musicPlayer.playZoneAmbience(MusicContext.GYM_AMBIENCE, track)
                return
            }
        }

        // tower floors get their own zone picks
        if (TrackRegistry.isBattleTowerZone(zoneId)) {
            val track = TrackRegistry.battleTowerTrackFor(zoneId)
            if (track != null) musicPlayer.playZoneAmbience(MusicContext.BATTLE_TOWER, track)
            return
        }

        // exact structures beat gym fallbacks
        val specialTrack = TrackRegistry.specialStructureTrackFor(zoneId)
        if (specialTrack != null) {
            musicPlayer.playZoneAmbience(MusicContext.SPECIAL_STRUCTURE, specialTrack)
            return
        }

        val region = STRUCTURE_TO_REGION[zoneId]
        if (region != null) {
            val track = TrackRegistry.gymAmbienceTrackFor(region)
            if (track != null) musicPlayer.playZoneAmbience(MusicContext.GYM_AMBIENCE, track)
            return
        }

        // vanilla bca structures use pools
        if (zoneId.startsWith("cobbletunes:vanilla_structure:")) {
            val category = zoneId.removePrefix("cobbletunes:vanilla_structure:")
            val vanillaTrack = if (category in VILLAGE_STRUCTURE_CATEGORIES) {
                pickVillageStructureTrack(category)
            } else {
                TrackRegistry.vanillaStructureTrackFor(category)
            }
            if (vanillaTrack != null) {
                if (category in VILLAGE_STRUCTURE_CATEGORIES) {
                    musicPlayer.playVillageAmbience(vanillaTrack)
                } else {
                    musicPlayer.playZoneAmbience(MusicContext.VANILLA_STRUCTURE, vanillaTrack)
                }
            } else {
                LOGGER.warn("[$MOD_ID] No tracks registered for vanilla structure category '$category'")
            }
            return
        }

        LOGGER.warn("[$MOD_ID] Unknown zone id '$zoneId' — ignoring")
    }

    private fun registerVanillaMusicSuppression() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            musicPlayer.tick()
            if (config.replaceAmbience) {
                client.musicTracker.stop()
            }
        }
    }

    private fun registerBiomeAmbienceWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!config.replaceAmbience) return@register
            if (client.isPaused) return@register

            val world = client.world
            if (world !== lastWorld) {
                val keepRaidVictory = raidVictoryActive && lastWorld != null && world != null
                if (!keepRaidVictory) {
                    clearVictoryState(finishMusic = false)
                }
                lastWorld = world
                ambienceCheckCounter = 0
                if (world != null) {
                    // tick order can change
                    pendingMenuTrack = null
                    if (keepRaidVictory) {
                        debugLog("[Raid victory] Preserving 5s cue across dimension change")
                    } else {
                        musicPlayer.beginWorldJoinSilence()
                    }
                } else {
                    // menu returns skip startup wait
                    menuReadyTicks = 200
                }
                return@register
            }

            ambienceCheckCounter++
            if (ambienceCheckCounter < AMBIENCE_CHECK_INTERVAL_TICKS) return@register
            ambienceCheckCounter = 0

            val player = client.player ?: return@register
            val currentWorld = world ?: return@register

            val biomeId = currentWorld.getBiome(player.blockPos).key
                .map { it.value.toString() }
                .orElse(null)
                ?: return@register

            // sky light and height keep surface nights out
            val skyLight = currentWorld.getLightLevel(
                net.minecraft.world.LightType.SKY, player.blockPos
            )
            // sealed areas use the cave pool
            val isCave = skyLight == 0 && player.blockPos.y <= 50

            if (isCave) {
                musicPlayer.updateAmbienceBiome("cobbletunes:cave")
            } else {
                musicPlayer.updateAmbienceBiome(biomeId)
            }
        }
    }

    private fun registerMenuMusicWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!config.replaceMenuMusic) return@register

            val onMenu = client.world == null

            if (onMenu) {
                menuReadyTicks++
                if (menuReadyTicks < 200) return@register

                if (pendingMenuTrack == null) {
                    pendingMenuTrack = TrackRegistry.tracksFor(MusicContext.MENU).randomOrNull()
                }
                val track = pendingMenuTrack ?: return@register

                if (!musicPlayer.isMenuThemeAudible()) {
                    musicPlayer.playMenuTheme(track)
                }
                client.musicTracker.stop()
            } else {
                // sound engine is warm after world load
                menuReadyTicks = 200
                if (pendingMenuTrack != null) {
                    musicPlayer.stopMenuTheme()
                    pendingMenuTrack = null
                }
            }
        }
    }
    private fun registerLootMenuVictoryWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!LootMenuVictoryBridge.available) return@register

            val lootOpen = LootMenuVictoryBridge.isLootScreen(client.currentScreen)
            if (lootOpen) {
                if (!lootMenuWasOpen) {
                    lootMenuWasOpen = true
                    val track = battleVictoryTrack
                        ?: lastBattleVictoryRequest?.let(TrackRegistry::victoryTrackFor)
                    if (track != null && config.replaceBattleMusic) {
                        val alreadyPlaying = battleVictoryActive || lootVictoryActive
                        battleVictoryTrack = track
                        battleVictoryTriggered = true
                        battleVictoryActive = false
                        battleVictoryEndsAtMillis = null
                        lootVictoryActive = true
                        lootVictoryFadeOutAtMillis = null

                        if (!alreadyPlaying) {
                            musicPlayer.playVictoryTheme(track)
                        }
                        debugLog(
                            if (alreadyPlaying) {
                                "[Victory] Loot menu opened; extending ${track.id} until the screen closes"
                            } else {
                                "[Victory] Loot menu opened; starting ${track.id} immediately"
                            }
                        )
                    }
                }
                return@register
            }

            if (lootMenuWasOpen) {
                lootMenuWasOpen = false
                if (lootVictoryActive) {
                    lootVictoryFadeOutAtMillis = System.currentTimeMillis() + VICTORY_FADE_OUT_DELAY_MILLIS
                    debugLog(
                        "[Victory] Loot menu closed; holding Victory for " +
                            "${VICTORY_FADE_OUT_DELAY_MILLIS / 1000}s before fade-out"
                    )
                }
                return@register
            }

            if (lootVictoryActive) {
                val fadeOutAt = lootVictoryFadeOutAtMillis ?: return@register
                if (System.currentTimeMillis() < fadeOutAt) return@register

                lootVictoryActive = false
                lootVictoryFadeOutAtMillis = null
                battleVictoryActive = false
                battleVictoryEndsAtMillis = null
                battleVictoryTrack = null
                lastBattleVictoryRequest = null
                musicPlayer.finishVictoryTheme()
                debugLog("[Victory] Loot Menu tail ended; starting fade-out and resuming world music")
            }

        }
    }

    private fun registerBattleVictoryWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (!battleVictoryActive || lootVictoryActive) return@register
            val endsAt = battleVictoryEndsAtMillis ?: return@register
            if (System.currentTimeMillis() < endsAt) return@register

            battleVictoryActive = false
            battleVictoryEndsAtMillis = null
            debugLog("[Victory] Battle Victory tail ended; starting fade-out and resuming current world music")
            musicPlayer.finishVictoryTheme()

            if (!LootMenuVictoryBridge.available) {
                battleVictoryTrack = null
                lastBattleVictoryRequest = null
            }
        }
    }

    private fun registerCaptureVictoryWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (!captureVictoryActive) return@register
            val endsAt = captureVictoryEndsAtMillis ?: return@register
            if (System.currentTimeMillis() < endsAt) return@register

            captureVictoryActive = false
            captureVictoryEndsAtMillis = null
            debugLog("[Capture victory] Tail ended; starting fade-out and resuming current world music")
            musicPlayer.finishVictoryTheme()
        }
    }

    private fun startRaidVictory() {
        val request = lastBattleVictoryRequest
        lastBattleVictoryRequest = null
        lastBattleWasRaid = false
        battleVictoryActive = false
        battleVictoryEndsAtMillis = null
        battleVictoryTrack = null
        lootVictoryActive = false
        lootMenuWasOpen = false
        lootVictoryFadeOutAtMillis = null

        if (!config.replaceBattleMusic || request == null) {
            musicPlayer.playAmbience()
            return
        }

        val track = TrackRegistry.victoryTrackFor(request)
        if (track == null) {
            debugLog("[Raid victory] No theme for ${request.region}/${request.kind}")
            musicPlayer.playAmbience()
            return
        }

        raidVictoryActive = true
        raidVictoryEndsAtMillis = System.currentTimeMillis() +
            RAID_VICTORY_MILLIS + VICTORY_FADE_OUT_DELAY_MILLIS
        musicPlayer.playVictoryTheme(track)
        debugLog(
            "[Raid victory] Playing ${track.id} for ${RAID_VICTORY_MILLIS / 1000}s " +
                "then holding ${VICTORY_FADE_OUT_DELAY_MILLIS / 1000}s before fade-out"
        )
    }

    private fun registerRaidVictoryWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            if (!raidVictoryActive) return@register
            val endsAt = raidVictoryEndsAtMillis ?: return@register
            if (System.currentTimeMillis() < endsAt) return@register

            raidVictoryActive = false
            raidVictoryEndsAtMillis = null
            debugLog("[Raid victory] Tail ended; starting fade-out and resuming current world music")
            musicPlayer.finishVictoryTheme()
        }
    }

    private fun pickVillageStructureTrack(category: String): com.kaizzinho.cobbletunes.client.sound.MusicTrack? {
        val pool = TrackRegistry.structureTracksFor(category)
        if (pool.isEmpty()) return null

        val lastTrackId = lastVillageTrackByCategory[category]
        val candidates = if (pool.size > 1) {
            pool.filter { it.id != lastTrackId }.ifEmpty { pool }
        } else {
            pool
        }
        return candidates.randomOrNull()?.also { lastVillageTrackByCategory[category] = it.id }
    }

    private fun buildVictoryRequest(
        payload: BattleMusicStartPayload,
        routeHead: String,
        routeValue: String
    ): VictoryRequest? {
        if (!payload.isWild && !payload.isTrainer) return null

        val explicitRegion = parseRegion(routeValue)
        val variantRegion = RegionOfOrigin.fromRegionalVariant(payload.primaryRegionalVariant)
        val rosterRegion = TrackRegistry.resolveRosterRegion(
            payload.opposingDexNumbers,
            payload.opposingRegionalVariants
        )
        val primaryRegion = payload.dexNumber.takeIf { it >= 0 }?.let(RegionOfOrigin::fromDexNumber)
        val region = if (payload.isWild) {
            explicitRegion ?: variantRegion ?: primaryRegion ?: rosterRegion
        } else {
            explicitRegion ?: rosterRegion ?: primaryRegion
        } ?: return null

        if (payload.isWild) return VictoryRequest(region, VictoryKind.WILD)

        val factionTheme = routeHead
            .takeIf { it.startsWith("faction:") }
            ?.substringAfter("faction:")
            ?.takeIf { it.isNotBlank() }

        val specialRole = parseSpecialTrainerPoolRoute(routeHead)?.roleId
            ?: parseSpecialTrainerRoute(routeHead)?.roleId
        val kind = when {
            factionTheme != null -> VictoryKind.FACTION
            specialRole == "leader" || routeHead == "leader" -> VictoryKind.GYM_LEADER
            specialRole == "e4" || routeHead == "e4" -> VictoryKind.ELITE_FOUR
            specialRole == "champ" || routeHead == "champ" -> VictoryKind.CHAMPION
            specialRole == "rival" || routeHead == "rival" -> VictoryKind.RIVAL
            specialRole == "frontier" || routeHead == "frontier" -> VictoryKind.FRONTIER_BRAIN
            else -> VictoryKind.TRAINER
        }

        return VictoryRequest(region, kind, factionTheme)
    }

    private data class SpecialTrainerPoolRoute(
        val poolId: String,
        val roleId: String
    )

    private fun parseSpecialTrainerPoolRoute(routeHead: String): SpecialTrainerPoolRoute? {
        if (!routeHead.startsWith("specialpool:")) return null
        val parts = routeHead.removePrefix("specialpool:").split(':', limit = 2)
        val poolId = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val roleId = parts.getOrNull(1).orEmpty()
        return SpecialTrainerPoolRoute(poolId, roleId)
    }

    private data class SpecialTrainerRoute(
        val trackId: String,
        val roleId: String
    )

    private fun parseSpecialTrainerRoute(routeHead: String): SpecialTrainerRoute? {
        if (!routeHead.startsWith("special:")) return null
        val parts = routeHead.removePrefix("special:").split(':', limit = 2)
        val trackId = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val roleId = parts.getOrNull(1).orEmpty()
        return SpecialTrainerRoute(trackId, roleId)
    }

    private fun parseRegion(value: String): RegionOfOrigin? =
        value.takeIf { it.isNotBlank() }?.let { id ->
            RegionOfOrigin.entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
        }

    private fun clearVictoryState(finishMusic: Boolean) {
        if (finishMusic && (battleVictoryActive || lootVictoryActive || captureVictoryActive || raidVictoryActive)) {
            musicPlayer.finishVictoryTheme()
        }
        lastBattleVictoryRequest = null
        lastBattleWasRaid = false
        battleVictoryTriggered = false
        battleVictoryActive = false
        battleVictoryEndsAtMillis = null
        battleVictoryTrack = null
        lootVictoryActive = false
        lootMenuWasOpen = false
        lootVictoryFadeOutAtMillis = null
        captureVictoryActive = false
        captureVictoryEndsAtMillis = null
        raidVictoryActive = false
        raidVictoryEndsAtMillis = null
    }

    private fun registerDeathScreenWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // world swaps can fake death screens
            if (client.world == null || client.player == null) return@register

            val isDeathScreen = try {
                client.currentScreen is net.minecraft.client.gui.screen.DeathScreen
            } catch (e: Exception) {
                // bad mappings should not kill music
                false
            }

            if (isDeathScreen && !onDeathScreen) {
                onDeathScreen = true
                musicPlayer.stopEverything()
                debugLog("[Death screen] Stopping all music")
            } else if (!isDeathScreen && onDeathScreen) {
                onDeathScreen = false
            }
        }
    }

    private fun registerLowHpWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (musicPlayer.isVolumeSuspended()) {
                lowHpBeepsRemaining = 0
                lowHpBeepCooldownTicks = 0
                return@register
            }

            if (lowHpBeepsRemaining > 0) {
                if (lowHpBeepCooldownTicks > 0) {
                    lowHpBeepCooldownTicks--
                } else {
                    val soundEvent = TrackRegistry.lowHpSoundEvent()
                    client.soundManager.play(
                        net.minecraft.client.sound.PositionedSoundInstance.master(
                            soundEvent,
                            1.0f,
                            (config.musicVolume * LOW_HP_VOLUME_MULTIPLIER)
                        )
                    )
                    lowHpBeepsRemaining--
                    lowHpBeepCooldownTicks = LOW_HP_BEEP_INTERVAL_TICKS
                }
                return@register
            }

            // hp checks stay cheap
            lowHpCheckCounter++
            if (lowHpCheckCounter < LOW_HP_CHECK_INTERVAL_TICKS) return@register
            lowHpCheckCounter = 0

            // leaving battle resets low hp state
            val battle = com.cobblemon.mod.common.client.CobblemonClient.battle
                ?: run { lastLowHpPokemonUuid = null; return@register }

            val playerUuid = client.player?.uuid ?: return@register

            // player actor may be on either side
            val playerActor = (battle.side1.actors + battle.side2.actors)
                .firstOrNull { it.uuid == playerUuid } ?: return@register

            // only active living low hp mons count
            val lowHpMon = playerActor.activePokemon
                .mapNotNull { it.battlePokemon }
                .firstOrNull { mon ->
                    val ratio = if (mon.isHpFlat) {
                        if (mon.maxHp > 0f) mon.hpValue / mon.maxHp else 1f
                    } else {
                        mon.hpValue // hp value is already a ratio
                    }
                    ratio in 0.001f..0.25f
                }

            if (lowHpMon == null) {
                // rearm after hp recovers
                lastLowHpPokemonUuid = null
                return@register
            }

            // same mon does not restart beeps
            if (lowHpMon.uuid == lastLowHpPokemonUuid) return@register

            lastLowHpPokemonUuid = lowHpMon.uuid
            lowHpBeepsRemaining = 1
            lowHpBeepCooldownTicks = 0
            debugLog("[Low HP] Pokémon ${lowHpMon.uuid} at low HP — beeping $lowHpBeepsRemaining times")
        }
    }
}
