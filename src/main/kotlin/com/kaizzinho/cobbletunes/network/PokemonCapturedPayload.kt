package com.kaizzinho.cobbletunes.network

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class PokemonCapturedPayload(
    val dexNumber: Int,
    val regionalVariant: String
) : CustomPayload {
    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        val ID: CustomPayload.Id<PokemonCapturedPayload> =
            CustomPayload.Id(Identifier.of(MOD_ID, "pokemon_captured"))

        val CODEC: PacketCodec<RegistryByteBuf, PokemonCapturedPayload> = PacketCodec.of(
            { value, buf ->
                PacketCodecs.VAR_INT.encode(buf, value.dexNumber)
                PacketCodecs.STRING.encode(buf, value.regionalVariant)
            },
            { buf ->
                PokemonCapturedPayload(
                    dexNumber = PacketCodecs.VAR_INT.decode(buf),
                    regionalVariant = PacketCodecs.STRING.decode(buf)
                )
            }
        )
    }
}
