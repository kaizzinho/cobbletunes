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

    /**
     * Routine trace logging (death screen trigger, low HP trigger) — only
     * prints when config.debugLogging is enabled. Startup logs and
     * LOGGER.warn calls elsewhere in this file are NOT gated by this.
     */
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
    private var pendingMenuTrack: com.kaizzinho.cobbletunes.client.sound.MusicTrack? = null
    private var menuReadyTicks = 0

    // Low HP beep state
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

        ClientPlayNetworking.registerGlobalReceiver(PlayerDeathPayload.ID) { _, context ->
            context.client().execute { musicPlayer.handlePlayerDeath() }
        }

        // Pillar 9: zone change from server
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

                // Vanilla/BCA structure — category-pool lookup (random pick per
                // category, unlike SPECIAL_STRUCTURE's 1:1 lookup below).
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

    private fun registerBiomeAmbienceWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (!config.replaceAmbience) return@register
            if (client.isPaused) return@register

            val world = client.world
            if (world !== lastWorld) {
                lastWorld = world
                ambienceCheckCounter = 0
                if (world != null) {
                    // Explicitly reset menu state before world join silence —
                    // the menu watcher's else branch fires on the same tick
                    // but order isn't guaranteed, and menu music must be stopped
                    // before beginWorldJoinSilence() or it keeps restarting.
                    pendingMenuTrack = null
                    musicPlayer.beginWorldJoinSilence()
                } else {
                    // Returned to menu — reset menu tick counter so music restarts.
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

            // Underground cave detection: sky light=0 (no sky access) AND Y≤50.
            // Torches/lava raise block light but NOT sky light, so they don't
            // interfere. Night surface has sky light=0 but Y>50, so excluded.
            // Player houses are typically built above Y=50, so also excluded.
            // Only activates when the surface biome has no registered tracks —
            // named cave biomes (dripstone_caves, lush_caves etc.) keep their
            // own specific pools via normal biome resolution.
            val skyLight = currentWorld.getLightLevel(
                net.minecraft.world.LightType.SKY, player.blockPos
            )
            // Cave override: sky light=0 (sealed, no direct sky access) AND Y≤50.
            // Applies regardless of what the surface biome is — underground desert,
            // underground forest etc. all play cave music when genuinely sealed in.
            // Named cave biomes (dripstone_caves etc.) at Y>50 still use their own
            // biome tracks via normal resolution since sky light may not be 0 there.
            val isCave = skyLight == 0 && player.blockPos.y <= 50

            if (isCave) {
                musicPlayer.updateAmbienceBiome("cobbletunes:cave")
            } else {
                musicPlayer.updateAmbienceBiome(biomeId)
            }
        }
    }

    /**
     * Pillar 10: menu music. Plays while no world is loaded — covers the title
     * screen and all submenus (singleplayer list, options, etc.) as one session.
     * Retries every tick until audible, which covers the fresh-launch case where
     * the sound engine isn't ready on the very first attempt.
     */
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
            }else {
                // World loaded — engine is already running, so skip the wait
                // if the player later returns to the menu.
                menuReadyTicks = 200
                if (pendingMenuTrack != null) {
                    musicPlayer.stopMenuTheme()
                    pendingMenuTrack = null
                }
            }
        }
    }
    /**
     * Stops all music the moment the death screen appears — before the player
     * presses Respawn. The actual silence window + biome re-detection is handled
     * by handlePlayerDeath() which fires after respawn via PlayerDeathPayload.
     * This purely handles the "silence during the death screen" part.
     *
     * !! VERIFY net.minecraft.client.gui.screen.DeathScreen's exact class name
     * against your Yarn mappings !! Standard 1.21.1 name but not genSources-confirmed.
     */
    private fun registerDeathScreenWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // Only relevant while actually in a world — skip entirely on
            // title screen, loading screens, and dimension transitions.
            // This prevents the loading screen from being misidentified
            // as a death screen during world join.
            if (client.world == null || client.player == null) return@register

            val isDeathScreen = try {
                client.currentScreen is net.minecraft.client.gui.screen.DeathScreen
            } catch (e: Exception) {
                // If DeathScreen class name differs in Yarn mappings,
                // fail silently rather than breaking ambience.
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

    /**
     * Plays the low-HP beep sound 3–5 times whenever the player's active
     * Pokémon drops to ≤25% HP, or a low-HP mon is switched in.
     * Uses Cobblemon's client-side battle API — no server packet needed.
     * Tracks the last low-HP Pokémon by UUID so the beep only retriggers
     * on a switch-in or recovery-then-drop, not on every tick while low.
     *
     * !! VERIFY CobblemonClient.battle / side1.actors / activePokemon chains !!
     * All confirmed via decompilation of Cobblemon 1.7.3 jar, but worth a
     * ctrl-click check in IntelliJ after adding this.
     */
    private fun registerLowHpWatcher() {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            // Tick down and fire ongoing beep sequence
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

            // Throttle the HP check to every 10 ticks (~0.5s)
            lowHpCheckCounter++
            if (lowHpCheckCounter < LOW_HP_CHECK_INTERVAL_TICKS) return@register
            lowHpCheckCounter = 0

            // No battle active — reset trigger state
            val battle = com.cobblemon.mod.common.client.CobblemonClient.battle
                ?: run { lastLowHpPokemonUuid = null; return@register }

            val playerUuid = client.player?.uuid ?: return@register

            // Find the player's actor across both sides
            val playerActor = (battle.side1.actors + battle.side2.actors)
                .firstOrNull { it.uuid == playerUuid } ?: return@register

            // Find any active Pokémon at ≤25% HP that is still alive
            val lowHpMon = playerActor.activePokemon
                .mapNotNull { it.battlePokemon }
                .firstOrNull { mon ->
                    val ratio = if (mon.isHpFlat) {
                        if (mon.maxHp > 0f) mon.hpValue / mon.maxHp else 1f
                    } else {
                        mon.hpValue  // already a 0–1 ratio
                    }
                    ratio in 0.001f..0.25f
                }

            if (lowHpMon == null) {
                // No low-HP mon — reset so retrigger works if HP drops again
                lastLowHpPokemonUuid = null
                return@register
            }

            // Same mon as last check — don't retrigger
            if (lowHpMon.uuid == lastLowHpPokemonUuid) return@register

            lastLowHpPokemonUuid = lowHpMon.uuid
            lowHpBeepsRemaining = 2
            lowHpBeepCooldownTicks = 0
            debugLog("[Low HP] Pokémon ${lowHpMon.uuid} at low HP — beeping $lowHpBeepsRemaining times")
        }
    }
}