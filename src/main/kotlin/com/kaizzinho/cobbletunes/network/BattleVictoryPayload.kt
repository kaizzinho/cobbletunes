package com.kaizzinho.cobbletunes.network

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

data object BattleVictoryPayload : CustomPayload {
    val ID = CustomPayload.Id<BattleVictoryPayload>(Identifier.of(MOD_ID, "battle_victory"))
    val CODEC: PacketCodec<RegistryByteBuf, BattleVictoryPayload> = PacketCodec.unit(BattleVictoryPayload)

    override fun getId(): CustomPayload.Id<out CustomPayload> = ID
}
