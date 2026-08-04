package com.kaizzinho.cobbletunes.client

import com.kaizzinho.cobbletunes.LOGGER
import com.kaizzinho.cobbletunes.MOD_ID
import com.kaizzinho.cobbletunes.client.config.CobbleTunesClientConfig
import com.kaizzinho.cobbletunes.client.sound.ClientMusicPlayer
import com.kaizzinho.cobbletunes.client.sound.MusicContext
import com.kaizzinho.cobbletunes.client.sound.RegionOfOrigin
import com.kaizzinho.cobbletunes.client.sound.TrackRegistry
import com.kaizzinho.cobbletunes.network.BattleMusicEndPayload
import com.kaizzinho.cobbletunes.network.BattleMusicStartPayload
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

        private val STRUCTURE_TO_REGION: Map<String, RegionOfOrigin> = mapOf(
            // kanto gyms
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
            // johto gyms
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
            // hoenn gyms
            "cobbleverse:rudi"               to RegionOfOrigin.HOENN,
            "cobbleverse:adriano"            to RegionOfOrigin.HOENN,
            "cobbleverse:tell_pat"           to RegionOfOrigin.HOENN,
            "cobbleverse:alice"              to RegionOfOrigin.HOENN,
            "cobbleverse:norman"             to RegionOfOrigin.HOENN,
            "cobbleverse:fiammetta"          to RegionOfOrigin.HOENN,
            "cobbleverse:walter"             to RegionOfOrigin.HOENN,
            "cobbleverse:petra"              to RegionOfOrigin.HOENN,
            "cobbleverse:hoenn_league"       to RegionOfOrigin.HOENN,
            // sinnoh gyms
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

    // low hp beep state
    private var lowHpBeepsRemaining = 0
    private var lowHpBeepCooldownTicks = 0
    private var lowHpCheckCounter = 0
    private var lastLowHpPokemonUuid: java.util.UUID? = null
    private val LOW_HP_BEEP_INTERVAL_TICKS = 20
    private val LOW_HP_CHECK_INTERVAL_TICKS = 10

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
        registerLowHpWatcher()

        LOGGER.info("[$MOD_ID] Client init complete.")
    }

    private fun registerNetworkReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(BattleMusicStartPayload.ID) { payload, context ->
            context.client().execute {
                // route stays string-based so old packets like "leader" still work
                val routeParts = payload.trainerTier.split('|', limit = 2)
                val trainerTier = routeParts.firstOrNull().orEmpty()
                val preferredRegion = routeParts.getOrNull(1)?.let { regionId ->
                    RegionOfOrigin.entries.firstOrNull {
                        it.name.equals(regionId, ignoreCase = true)
                    }
                }

                val musicContext = when {
                    payload.isWild -> if (payload.isLegendary) MusicContext.LEGENDARY_BATTLE else MusicContext.WILD_BATTLE
                    payload.isTrainer -> when (trainerTier) {
                        "leader" -> MusicContext.GYM_LEADER_BATTLE
                        "e4"     -> MusicContext.ELITE_FOUR_BATTLE
                        "champ"  -> MusicContext.CHAMPION_BATTLE
                        "rival"  -> MusicContext.PVP_BATTLE
                        else     -> MusicContext.TRAINER_BATTLE
                    }
                    else -> MusicContext.PVP_BATTLE
                }
                val dexNumber = payload.dexNumber.takeIf { it >= 0 }

                debugLog(
                    "[Battle route] raw='${payload.trainerTier}' tier='$trainerTier' " +
                            "region=${preferredRegion?.name ?: "roster-vote"} context=$musicContext"
                )

                musicPlayer.playBattleContext(
                    context = musicContext,
                    dexNumber = dexNumber,
                    opposingDexNumbers = payload.opposingDexNumbers,
                    preferredRegion = preferredRegion
                )
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(BattleMusicEndPayload.ID) { _, context ->
            context.client().execute { musicPlayer.playAmbience() }
        }

        ClientPlayNetworking.registerGlobalReceiver(PlayerDeathPayload.ID) { _, context ->
            context.client().execute { musicPlayer.handlePlayerDeath() }
        }

        // zone updates come from the server tracker
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

                // hand-placed pokecenter/pokemart zones
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
                }

                // worldgen gym: use its region for the ambience pick
                val region = STRUCTURE_TO_REGION[payload.zoneId]
                if (region != null) {
                    val track = TrackRegistry.gymAmbienceTrackFor(region)
                    if (track != null) musicPlayer.playZoneAmbience(MusicContext.GYM_AMBIENCE, track)
                    return@execute
                }

                // vanilla/bca structures use a small pool per category
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

                // special structures map straight to one track
                val specialTrack = TrackRegistry.specialStructureTrackFor(payload.zoneId)
                if (specialTrack != null) {
                    musicPlayer.playZoneAmbience(MusicContext.SPECIAL_STRUCTURE, specialTrack)
                } else {
                    LOGGER.warn("[$MOD_ID] Unknown zone id '${payload.zoneId}' — ignoring")
                }
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
                lastWorld = world
                ambienceCheckCounter = 0
                if (world != null) {
                    // reset this here too; tick callback order isn't guaranteed
                    pendingMenuTrack = null
                    musicPlayer.beginWorldJoinSilence()
                } else {
                    // no extra 10s wait when coming back to the menu
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

            // sky light ignores torches, and y<=50 keeps nighttime surfaces out
            val skyLight = currentWorld.getLightLevel(
                net.minecraft.world.LightType.SKY, player.blockPos
            )
            // sealed underground areas share the cave pool, whatever biome is above
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
                // the sound engine is warm now, so future menu returns can start asap
                menuReadyTicks = 200
                if (pendingMenuTrack != null) {
                    musicPlayer.stopMenuTheme()
                    pendingMenuTrack = null
                }
            }
        }
    }
    private fun registerDeathScreenWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // loading/menu screens can look like death during world swaps, so skip them
            if (client.world == null || client.player == null) return@register

            val isDeathScreen = try {
                client.currentScreen is net.minecraft.client.gui.screen.DeathScreen
            } catch (e: Exception) {
                // mapping mismatch shouldn't break the whole music loop
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
            // finish the current beep pair first
            if (lowHpBeepsRemaining > 0) {
                if (lowHpBeepCooldownTicks > 0) {
                    lowHpBeepCooldownTicks--
                } else {
                    val soundEvent = TrackRegistry.lowHpSoundEvent()
                    client.soundManager.play(
                        net.minecraft.client.sound.PositionedSoundInstance.master(
                            soundEvent, 1.0f, config.musicVolume
                        )
                    )
                    lowHpBeepsRemaining--
                    lowHpBeepCooldownTicks = LOW_HP_BEEP_INTERVAL_TICKS
                }
                return@register
            }

            // hp checks every ~0.5s are plenty
            lowHpCheckCounter++
            if (lowHpCheckCounter < LOW_HP_CHECK_INTERVAL_TICKS) return@register
            lowHpCheckCounter = 0

            // leaving battle arms the trigger again
            val battle = com.cobblemon.mod.common.client.CobblemonClient.battle
                ?: run { lastLowHpPokemonUuid = null; return@register }

            val playerUuid = client.player?.uuid ?: return@register

            // player actor can be on either side
            val playerActor = (battle.side1.actors + battle.side2.actors)
                .firstOrNull { it.uuid == playerUuid } ?: return@register

            // only active, living mons at 25% or less
            val lowHpMon = playerActor.activePokemon
                .mapNotNull { it.battlePokemon }
                .firstOrNull { mon ->
                    val ratio = if (mon.isHpFlat) {
                        if (mon.maxHp > 0f) mon.hpValue / mon.maxHp else 1f
                    } else {
                        mon.hpValue  // already 0..1 here
                    }
                    ratio in 0.001f..0.25f
                }

            if (lowHpMon == null) {
                // let it trigger again after hp recovers
                lastLowHpPokemonUuid = null
                return@register
            }

            // don't restart the beeps every check
            if (lowHpMon.uuid == lastLowHpPokemonUuid) return@register

            lastLowHpPokemonUuid = lowHpMon.uuid
            lowHpBeepsRemaining = 2
            lowHpBeepCooldownTicks = 0
            debugLog("[Low HP] Pokémon ${lowHpMon.uuid} at low HP — beeping $lowHpBeepsRemaining times")
        }
    }
}