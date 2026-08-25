package com.kaizzinho.cobbletunes.network

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.network.ServerPlayerEntity

object CobbleTunesNetworking {
    fun sendIfSupported(player: ServerPlayerEntity, payload: CustomPayload): Boolean {
        if (!ServerPlayNetworking.canSend(player, payload.id)) return false
        ServerPlayNetworking.send(player, payload)
        return true
    }
}
