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
import com.kaizzinho.cobbletunes.network.StructureZonePayload
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking

class CobbleTunesClient : ClientModInitializer {

    companion object {
        lateinit var config: CobbleTunesClientConfig
            private set
        lateinit var musicPlayer: ClientMusicPlayer
            private set

        private const val AMBIENCE_CHECK_INTERVAL_TICKS = 20

        /**
         * Pillar 9: maps Cobbleverse structure IDs to the RegionOfOrigin used by
         * TrackRegistry.gymAmbienceTrackFor(). All known Cobbleverse gym structures
         * and their region. "team_rocket_tower" and the Kanto league / spires are
         * mapped to KANTO since that's the region their music belongs to.
         * Add Johto/Hoenn/Sinnoh entries here once those gym datapacks exist.
         */
        private val STRUCTURE_TO_REGION: Map<String, RegionOfOrigin> = mapOf(
            // Kanto gyms
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
            // Johto gyms
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
            // Hoenn gyms
            "cobbleverse:rudi"               to RegionOfOrigin.HOENN,
            "cobbleverse:adriano"            to RegionOfOrigin.HOENN,
            "cobbleverse:tell_pat"           to RegionOfOrigin.HOENN,
            "cobbleverse:alice"              to RegionOfOrigin.HOENN,
            "cobbleverse:norman"             to RegionOfOrigin.HOENN,
            "cobbleverse:fiammetta"          to RegionOfOrigin.HOENN,
            "cobbleverse:walter"             to RegionOfOrigin.HOENN,
            "cobbleverse:petra"              to RegionOfOrigin.HOENN,
            "cobbleverse:hoenn_league"       to RegionOfOrigin.HOENN,
            // Sinnoh gyms
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
    // add near the other instance fields:
    // add near menuMusicPlaying:
    private var pendingMenuTrack: com.kaizzinho.cobbletunes.client.sound.MusicTrack? = null
    private var menuMusicPlaying = false

    override fun onInitializeClient() {
        LOGGER.info("[$MOD_ID] Initializing client music system...")

        config = CobbleTunesClientConfig.load()
        TrackRegistry.bootstrap()
        musicPlayer = ClientMusicPlayer(config)

        registerNetworkReceivers()
        registerVanillaMusicSuppression()
        registerBiomeAmbienceWatcher()
        // call this from onInitializeClient() alongside the other register*() calls:
        registerMenuMusicWatcher()

        LOGGER.info("[$MOD_ID] Client init complete.")
    }

    /**
     * Pillar 4/6/9 routing. Battle contexts resolve from isWild/isTrainer/trainerTier.
     * Pillar 9: StructureZonePayload is received here and routed to the correct
     * proximity context (GYM_AMBIENCE, POKECENTER, POKEMART) or clears the zone
     * (empty string → resume normal biome ambience via musicPlayer.clearZone()).
     */
    private fun registerNetworkReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(BattleMusicStartPayload.ID) { payload, context ->
            context.client().execute {
                val musicContext = when {
                    payload.isWild -> if (payload.isLegendary) MusicContext.LEGENDARY_BATTLE else MusicContext.WILD_BATTLE
                    payload.isTrainer -> when (payload.trainerTier) {
                        "leader" -> MusicContext.GYM_LEADER_BATTLE
                        "e4"     -> MusicContext.ELITE_FOUR_BATTLE
                        "champ"  -> MusicContext.CHAMPION_BATTLE
                        "rival"  -> MusicContext.PVP_BATTLE
                        else     -> MusicContext.TRAINER_BATTLE
                    }
                    else -> MusicContext.PVP_BATTLE
                }
                val dexNumber = payload.dexNumber.takeIf { it >= 0 }
                musicPlayer.playBattleContext(musicContext, dexNumber, payload.opposingDexNumbers)
            }
        }

        ClientPlayNetworking.registerGlobalReceiver(BattleMusicEndPayload.ID) { _, context ->
            context.client().execute { musicPlayer.playAmbience() }
        }

        // Pillar 9: zone change from server
        ClientPlayNetworking.registerGlobalReceiver(StructureZonePayload.ID) { payload, context ->
            context.client().execute {
                if (payload.zoneId.isBlank()) {
                    musicPlayer.clearZone()
                    return@execute
                }

                // Trigger-block zones ("cobbletunes:pokecenter" / "cobbletunes:pokemart")
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

                // Worldgen gym structure — resolve region then pick gym track
                val region = STRUCTURE_TO_REGION[payload.zoneId]
                if (region != null) {
                    val track = TrackRegistry.gymAmbienceTrackFor(region)
                    if (track != null) musicPlayer.playZoneAmbience(MusicContext.GYM_AMBIENCE, track)
                    return@execute
                }

                // Special structure — direct 1:1 track lookup by structure ID
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


    private fun registerMenuMusicWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!config.replaceMenuMusic) return@register

            val onMenu = client.world == null

            if (onMenu) {
                if (pendingMenuTrack == null) {
                    pendingMenuTrack = TrackRegistry.tracksFor(MusicContext.MENU).randomOrNull()
                }
                val track = pendingMenuTrack
                if (track != null && !musicPlayer.isMenuThemeAudible()) {
                    // Retried every tick until genuinely audible -- covers the
                    // fresh-launch case where the sound engine isn't ready yet
                    // on the first few attempts.
                    musicPlayer.playMenuTheme(track)
                }
                client.musicTracker.stop()
            } else if (pendingMenuTrack != null) {
                musicPlayer.stopMenuTheme()
                pendingMenuTrack = null
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
                    musicPlayer.beginWorldJoinSilence()
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

            musicPlayer.updateAmbienceBiome(biomeId)
        }
    }
}
