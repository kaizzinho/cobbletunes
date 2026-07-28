package com.kaizzinho.cobbletunes.network

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

/**
 * Sent server -> client on BATTLE_VICTORY or BATTLE_FLED. No fields needed —
 * the client always just falls back to ambience regardless of which one fired.
 */
object BattleMusicEndPayload : CustomPayload {
    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    val ID: CustomPayload.Id<BattleMusicEndPayload> =
        CustomPayload.Id(Identifier.of(MOD_ID, "battle_music_end"))

    val CODEC: PacketCodec<RegistryByteBuf, BattleMusicEndPayload> =
        PacketCodec.unit(BattleMusicEndPayload)
}
