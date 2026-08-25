package com.kaizzinho.cobbletunes.network

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data object ClientBridgeProbePayload : CustomPayload {
    val ID = CustomPayload.Id<ClientBridgeProbePayload>(Identifier.of(MOD_ID, "client_bridge_probe"))
    val CODEC: PacketCodec<RegistryByteBuf, ClientBridgeProbePayload> = PacketCodec.unit(ClientBridgeProbePayload)

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}
