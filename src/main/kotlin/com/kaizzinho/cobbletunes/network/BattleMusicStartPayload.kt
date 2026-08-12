package com.kaizzinho.cobbletunes.network

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data class BattleMusicStartPayload(
    val isWild: Boolean,
    val isTrainer: Boolean,
    val isLegendary: Boolean,
    val dexNumber: Int,
    val opposingDexNumbers: List<Int>,
    val opposingRegionalVariants: List<String>,
    val primaryRegionalVariant: String,
    val legendaryForm: String,
    val trainerTier: String
) : CustomPayload {
    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        val ID: CustomPayload.Id<BattleMusicStartPayload> =
            CustomPayload.Id(Identifier.of(MOD_ID, "battle_music_start"))

        val CODEC: PacketCodec<RegistryByteBuf, BattleMusicStartPayload> = PacketCodec.of(
            { value, buf ->
                PacketCodecs.BOOL.encode(buf, value.isWild)
                PacketCodecs.BOOL.encode(buf, value.isTrainer)
                PacketCodecs.BOOL.encode(buf, value.isLegendary)
                PacketCodecs.VAR_INT.encode(buf, value.dexNumber)

                PacketCodecs.VAR_INT.encode(buf, value.opposingDexNumbers.size)
                value.opposingDexNumbers.forEach { dexNumber ->
                    PacketCodecs.VAR_INT.encode(buf, dexNumber)
                }

                PacketCodecs.VAR_INT.encode(buf, value.opposingRegionalVariants.size)
                value.opposingRegionalVariants.forEach { regionalVariant ->
                    PacketCodecs.STRING.encode(buf, regionalVariant)
                }

                PacketCodecs.STRING.encode(buf, value.primaryRegionalVariant)
                PacketCodecs.STRING.encode(buf, value.legendaryForm)
                PacketCodecs.STRING.encode(buf, value.trainerTier)
            },
            { buf ->
                val isWild = PacketCodecs.BOOL.decode(buf)
                val isTrainer = PacketCodecs.BOOL.decode(buf)
                val isLegendary = PacketCodecs.BOOL.decode(buf)
                val dexNumber = PacketCodecs.VAR_INT.decode(buf)

                val opposingCount = PacketCodecs.VAR_INT.decode(buf)
                require(opposingCount in 0..256) {
                    "Invalid opposing Pokédex number count: $opposingCount"
                }
                val opposingDexNumbers = List(opposingCount) {
                    PacketCodecs.VAR_INT.decode(buf)
                }

                val regionalCount = PacketCodecs.VAR_INT.decode(buf)
                require(regionalCount in 0..256) {
                    "Invalid opposing regional variant count: $regionalCount"
                }
                val opposingRegionalVariants = List(regionalCount) {
                    PacketCodecs.STRING.decode(buf)
                }

                BattleMusicStartPayload(
                    isWild = isWild,
                    isTrainer = isTrainer,
                    isLegendary = isLegendary,
                    dexNumber = dexNumber,
                    opposingDexNumbers = opposingDexNumbers,
                    opposingRegionalVariants = opposingRegionalVariants,
                    primaryRegionalVariant = PacketCodecs.STRING.decode(buf),
                    legendaryForm = PacketCodecs.STRING.decode(buf),
                    trainerTier = PacketCodecs.STRING.decode(buf)
                )
            }
        )
    }
}
