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
    val trainerTier: String
) : CustomPayload {
    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    companion object {
        val ID: CustomPayload.Id<BattleMusicStartPayload> =
            CustomPayload.Id(Identifier.of(MOD_ID, "battle_music_start"))

        val CODEC: PacketCodec<RegistryByteBuf, BattleMusicStartPayload> = PacketCodec.tuple(
            PacketCodecs.BOOL, BattleMusicStartPayload::isWild,
            PacketCodecs.BOOL, BattleMusicStartPayload::isTrainer,
            PacketCodecs.BOOL, BattleMusicStartPayload::isLegendary,
            PacketCodecs.VAR_INT, BattleMusicStartPayload::dexNumber,
            PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), BattleMusicStartPayload::opposingDexNumbers,
            PacketCodecs.STRING, BattleMusicStartPayload::trainerTier,
            ::BattleMusicStartPayload
        )
    }
}