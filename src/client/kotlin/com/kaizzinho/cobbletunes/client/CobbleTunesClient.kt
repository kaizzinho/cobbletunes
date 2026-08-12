package com.kaizzinho.cobbletunes.client

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import com.kaizzinho.cobbletunes.client.compat.lootmenu.LootMenuVictoryBridge
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
import com.kaizzinho.cobbletunes.network.StructureZonePayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class CobbleTunesClient : ClientModInitializer {
    private var onDeathScreen = false

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
        private const val VICTORY_LOOT_WAIT_MILLIS = 3_000L

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

    private data class PendingLootVictory(
        val request: VictoryRequest,
        val expiresAtMillis: Long
    )

    private var lastBattleVictoryRequest: VictoryRequest? = null
    private var pendingLootVictory: PendingLootVictory? = null
    private var lootVictoryActive = false
    private var lootMenuWasOpen = false

    private var lowHpBeepsRemaining = 0
    private var lowHpBeepCooldownTicks = 0
    private var lowHpCheckCounter = 0
    private var lastLowHpPokemonUuid: java.util.UUID? = null
    private val LOW_HP_BEEP_INTERVAL_TICKS = 20
    private val LOW_HP_CHECK_INTERVAL_TICKS = 10
    private val LOW_HP_VOLUME_MULTIPLIER = 1.35f

    override fun onInitializeClient() {
        LOGGER.info("[$MOD_ID] Initializing client music system...")

        config = CobbleTunesClientConfig.load()
        TrackRegistry.bootstrap()
        musicPlayer = ClientMusicPlayer(config)

        registerNetworkReceivers()
        registerVanillaMusicSuppression()
        registerBiomeAmbienceWatcher()
        registerDeathScreenWatcher()
        registerMenuMusicWatcher()
        registerLootMenuVictoryWatcher()
        registerLowHpWatcher()

        LOGGER.info("[$MOD_ID] Client init complete.")
    }

    private fun registerNetworkReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(BattleMusicStartPayload.ID) { payload, context ->
            context.client().execute {
                // keep string routes for old packet compat
                val routeParts = payload.trainerTier.split('|', limit = 2)
                val routeHead = routeParts.firstOrNull().orEmpty()
                val routeValue = routeParts.getOrNull(1).orEmpty()

                clearVictoryState(finishMusic = false)
                lastBattleVictoryRequest = buildVictoryRequest(payload, routeHead, routeValue)

                if (routeHead == "boss") {
                    debugLog(
                        "[Boss route] raw='${payload.trainerTier}' tier='${routeValue.ifBlank { "unknown" }}' " +
                            "opposingDex=${payload.opposingDexNumbers}"
                    )
                    musicPlayer.playBossBattle(
                        tierName = routeValue,
                        opposingDexNumbers = payload.opposingDexNumbers,
                        opposingRegionalVariants = payload.opposingRegionalVariants
                    )
                    return@execute
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
                        "e4"     -> MusicContext.ELITE_FOUR_BATTLE
                        "champ"  -> MusicContext.CHAMPION_BATTLE
                        "rival"    -> MusicContext.PVP_BATTLE
                        "frontier" -> MusicContext.FRONTIER_BRAIN_BATTLE
                        else       -> MusicContext.TRAINER_BATTLE
                    }
                    else -> MusicContext.PVP_BATTLE
                }
                val dexNumber = payload.dexNumber.takeIf { it >= 0 }

                debugLog(
                    "[Battle route] raw='${payload.trainerTier}' tier='$trainerTier' " +
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
        }

        ClientPlayNetworking.registerGlobalReceiver(BattleMusicEndPayload.ID) { _, context ->
            context.client().execute {
                clearVictoryState(finishMusic = false)
                musicPlayer.playAmbience()
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(BattleVictoryPayload.ID) { _, context ->
            context.client().execute {
                val request = lastBattleVictoryRequest
                lastBattleVictoryRequest = null
                pendingLootVictory = null
                lootVictoryActive = false
                lootMenuWasOpen = false

                if (LootMenuVictoryBridge.available && request != null) {
                    val pending = PendingLootVictory(
                        request = request,
                        expiresAtMillis = System.currentTimeMillis() + VICTORY_LOOT_WAIT_MILLIS
                    )
                    pendingLootVictory = pending

                    val track = TrackRegistry.victoryTrackFor(request)
                    if (track != null) {
                        lootVictoryActive = true
                        musicPlayer.playVictoryTheme(track)
                        debugLog(
                            "[Victory] Started at battle victory; waiting up to " +
                                "${VICTORY_LOOT_WAIT_MILLIS / 1000}s for loot menu"
                        )
                    } else {
                        pendingLootVictory = null
                        debugLog("[Victory] No theme for ${request.region}/${request.kind}")
                    }
                }

                if (!lootVictoryActive) {
                    musicPlayer.playAmbience()
                }
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(PlayerDeathPayload.ID) { _, context ->
            context.client().execute {
                clearVictoryState(finishMusic = false)
                musicPlayer.handlePlayerDeath()
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(StructureZonePayload.ID) { payload, context ->
            context.client().execute {
                if (payload.zoneId.isBlank()) {
                    val mc = net.minecraft.client.MinecraftClient.getInstance()
                    val pos = mc.player?.blockPos
                    val biomeId = if (pos != null) {
                        mc.world?.getBiome(pos)?.key?.orElse(null)?.value?.toString()
                    } else null
                    musicPlayer.clearZone(biomeId)
                    return@execute
                }

                when (payload.zoneId) {
                    "cobbletunes:pokecenter" -> {
                        val track = TrackRegistry.tracksFor(MusicContext.POKECENTER).randomOrNull()
                        if (track != null) musicPlayer.playZoneAmbience(MusicContext.POKECENTER, track)
                        return@execute
                    }
                    "cobbletunes:pokemart" -> {
                        val track = TrackRegistry.tracksFor(MusicContext.POKEMART).randomOrNull()
                        if (track != null) musicPlayer.playZoneAmbience(MusicContext.POKEMART, track)
                        return@execute
                    }
                    "cobbletunes:gym_kanto",
                    "cobbletunes:gym_johto",
                    "cobbletunes:gym_hoenn",
                    "cobbletunes:gym_sinnoh",
                    "cobbletunes:gym_unova" -> {
                        val region = parseRegion(payload.zoneId.substringAfter("cobbletunes:gym_"))
                        val track = region?.let(TrackRegistry::gymAmbienceTrackFor)
                        if (track != null) musicPlayer.playZoneAmbience(MusicContext.GYM_AMBIENCE, track)
                        return@execute
                    }
                }

                // tower floors get their own zone picks
                if (TrackRegistry.isBattleTowerZone(payload.zoneId)) {
                    val track = TrackRegistry.battleTowerTrackFor(payload.zoneId)
                    if (track != null) musicPlayer.playZoneAmbience(MusicContext.BATTLE_TOWER, track)
                    return@execute
                }

                // exact structures beat gym fallbacks
                val specialTrack = TrackRegistry.specialStructureTrackFor(payload.zoneId)
                if (specialTrack != null) {
                    musicPlayer.playZoneAmbience(MusicContext.SPECIAL_STRUCTURE, specialTrack)
                    return@execute
                }

                val region = STRUCTURE_TO_REGION[payload.zoneId]
                if (region != null) {
                    val track = TrackRegistry.gymAmbienceTrackFor(region)
                    if (track != null) musicPlayer.playZoneAmbience(MusicContext.GYM_AMBIENCE, track)
                    return@execute
                }

                // vanilla bca structures use pools
                if (payload.zoneId.startsWith("cobbletunes:vanilla_structure:")) {
                    val category = payload.zoneId.removePrefix("cobbletunes:vanilla_structure:")
                    val vanillaTrack = TrackRegistry.vanillaStructureTrackFor(category)
                    if (vanillaTrack != null) {
                        musicPlayer.playZoneAmbience(MusicContext.VANILLA_STRUCTURE, vanillaTrack)
                    } else {
                        LOGGER.warn("[$MOD_ID] No tracks registered for vanilla structure category '$category'")
                    }
                    return@execute
                }

                LOGGER.warn("[$MOD_ID] Unknown zone id '${payload.zoneId}' — ignoring")
            }
        }
    }

    private fun registerVanillaMusicSuppression() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
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
                clearVictoryState(finishMusic = false)
                lastWorld = world
                ambienceCheckCounter = 0
                if (world != null) {
                    // tick order can change
                    pendingMenuTrack = null
                    musicPlayer.beginWorldJoinSilence()
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
                    if (pendingLootVictory != null) {
                        pendingLootVictory = null
                        debugLog("[Victory] Loot menu confirmed; keeping Victory active")
                    }
                }
                return@register
            }

            if (lootMenuWasOpen) {
                lootMenuWasOpen = false
                pendingLootVictory = null
                if (lootVictoryActive) {
                    lootVictoryActive = false
                    musicPlayer.finishVictoryTheme()
                }
                return@register
            }

            val pending = pendingLootVictory
            if (pending != null && System.currentTimeMillis() > pending.expiresAtMillis) {
                pendingLootVictory = null
                debugLog("[Victory] Loot menu grace expired; resuming current world music")
                if (lootVictoryActive) {
                    lootVictoryActive = false
                    musicPlayer.finishVictoryTheme()
                }
            }
        }
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

        val kind = when {
            factionTheme != null -> VictoryKind.FACTION
            routeHead == "leader" -> VictoryKind.GYM_LEADER
            routeHead == "e4" -> VictoryKind.ELITE_FOUR
            routeHead == "champ" -> VictoryKind.CHAMPION
            routeHead == "rival" -> VictoryKind.RIVAL
            routeHead == "frontier" -> VictoryKind.FRONTIER_BRAIN
            else -> VictoryKind.TRAINER
        }

        return VictoryRequest(region, kind, factionTheme)
    }

    private fun parseRegion(value: String): RegionOfOrigin? =
        value.takeIf { it.isNotBlank() }?.let { id ->
            RegionOfOrigin.entries.firstOrNull { it.name.equals(id, ignoreCase = true) }
        }

    private fun clearVictoryState(finishMusic: Boolean) {
        if (finishMusic && lootVictoryActive) musicPlayer.finishVictoryTheme()
        lastBattleVictoryRequest = null
        pendingLootVictory = null
        lootVictoryActive = false
        lootMenuWasOpen = false
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
            if (lowHpBeepsRemaining > 0) {
                if (lowHpBeepCooldownTicks > 0) {
                    lowHpBeepCooldownTicks--
                } else {
                    val soundEvent = TrackRegistry.lowHpSoundEvent()
                    client.soundManager.play(
                        net.minecraft.client.sound.PositionedSoundInstance.master(
                            soundEvent,
                            1.0f,
                            (config.musicVolume * LOW_HP_VOLUME_MULTIPLIER).coerceAtMost(1.5f)
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
            lowHpBeepsRemaining = 2
            lowHpBeepCooldownTicks = 0
            debugLog("[Low HP] Pokémon ${lowHpMon.uuid} at low HP — beeping $lowHpBeepsRemaining times")
        }
    }
}
