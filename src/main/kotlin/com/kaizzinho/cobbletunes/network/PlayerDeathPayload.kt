package com.kaizzinho.cobbletunes.network

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

/**
 * Sent server -> client when a player dies during or after a battle.
 * Distinct from BattleMusicEndPayload because death needs different
 * client behavior: stop battle music immediately, then apply a silence
 * window before resuming biome ambience — the player will respawn in a
 * potentially different biome, so we shouldn't resume whatever was
 * playing before the battle. Instead we let the biome watcher re-detect
 * and debounce normally after the silence elapses.
 *
 * Sent from ServerPlayerEvents.AFTER_RESPAWN (Fabric API) rather than
 * any Cobblemon event, since Cobblemon has no BATTLE_DEFEAT event —
 * death causes the battle to end server-side without firing
 * BATTLE_VICTORY or BATTLE_FLED.
 */
object PlayerDeathPayload : CustomPayload {
    override fun getId(): CustomPayload.Id<out CustomPayload> = ID

    val ID: CustomPayload.Id<PlayerDeathPayload> =
        CustomPayload.Id(Identifier.of(MOD_ID, "player_death"))

    val CODEC: PacketCodec<RegistryByteBuf, PlayerDeathPayload> =
        PacketCodec.unit(PlayerDeathPayload)
}