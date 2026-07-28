package com.kaizzinho.cobbletunes

import com.kaizzinho.cobbletunes.event.CobblemonBattleListener
import com.kaizzinho.cobbletunes.network.BattleMusicEndPayload
import com.kaizzinho.cobbletunes.network.BattleMusicStartPayload
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.kaizzinho.cobbletunes.network.StructureZonePayload
import com.kaizzinho.cobbletunes.event.StructureZoneDetector
import com.kaizzinho.cobbletunes.world.MusicTriggerBlock

const val MOD_ID = "cobbletunes"

val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

/**
 * Common entrypoint — runs on BOTH client and server now (no longer client-only,
 * see fabric.mod.json). Battle classification needs the real
 * ServerPlayerEntity/PokemonBattle, which only exist server-side — including the
 * integrated server in singleplayer — so CobblemonBattleListener lives here and
 * networks its results to whichever client(s) are in the battle. Client-side
 * playback logic still lives in com.kaizzinho.cobbletunes.client (src/client),
 * which just receives those packets and decides final music via its own config.
 */
class CobbleTunes : ModInitializer {
    override fun onInitialize() {
        PayloadTypeRegistry.playS2C().register(BattleMusicStartPayload.ID, BattleMusicStartPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(BattleMusicEndPayload.ID, BattleMusicEndPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(StructureZonePayload.ID, StructureZonePayload.CODEC)

        MusicTriggerBlock.register()
        StructureZoneDetector.register()
        CobblemonBattleListener.register()

        LOGGER.info("[$MOD_ID] Common init complete — battle classification runs server-side, playback client-side.")
    }
}


