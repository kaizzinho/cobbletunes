package com.kaizzinho.cobbletunes.network

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class StructureZonePayload(val zoneId: String) : CustomPayload {
    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        val ID: CustomPayload.Id<StructureZonePayload> =
            CustomPayload.Id(Identifier.of(MOD_ID, "structure_zone"))

        val CODEC: PacketCodec<RegistryByteBuf, StructureZonePayload> = PacketCodec.tuple(
            PacketCodecs.STRING, StructureZonePayload::zoneId,
            ::StructureZonePayload
        )
    }
}
