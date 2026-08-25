package com.kaizzinho.cobbletunes

import com.kaizzinho.cobbletunes.config.CobbleTunesServerConfig
import com.kaizzinho.cobbletunes.event.CobblemonBattleListener
import com.kaizzinho.cobbletunes.network.BattleMusicEndPayload
import com.kaizzinho.cobbletunes.network.BattleMusicStartPayload
import com.kaizzinho.cobbletunes.network.BattleVictoryPayload
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.kaizzinho.cobbletunes.network.StructureZonePayload
import com.kaizzinho.cobbletunes.event.StructureZoneDetector
import com.kaizzinho.cobbletunes.network.PlayerDeathPayload
import com.kaizzinho.cobbletunes.network.PokemonCapturedPayload
import com.kaizzinho.cobbletunes.network.ClientBridgeProbePayload
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking

const val MOD_ID = "cobbletunes"

val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

class CobbleTunes : ModInitializer {
    override fun onInitialize() {
        CobbleTunesServerConfig.load()

        PayloadTypeRegistry.playS2C().register(BattleMusicStartPayload.ID, BattleMusicStartPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(BattleMusicEndPayload.ID, BattleMusicEndPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(BattleVictoryPayload.ID, BattleVictoryPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(StructureZonePayload.ID, StructureZonePayload.CODEC)
        PayloadTypeRegistry.playS2C().register(PlayerDeathPayload.ID, PlayerDeathPayload.CODEC)
        PayloadTypeRegistry.playS2C().register(PokemonCapturedPayload.ID, PokemonCapturedPayload.CODEC)
        PayloadTypeRegistry.playC2S().register(ClientBridgeProbePayload.ID, ClientBridgeProbePayload.CODEC)

        // channel presence lets clients detect the optional server bridge
        ServerPlayNetworking.registerGlobalReceiver(ClientBridgeProbePayload.ID) { _, _ -> }

        StructureZoneDetector.register()
        CobblemonBattleListener.register()

        LOGGER.info("[$MOD_ID] Common init complete — client fallback active with optional server authority.")
    }
}


