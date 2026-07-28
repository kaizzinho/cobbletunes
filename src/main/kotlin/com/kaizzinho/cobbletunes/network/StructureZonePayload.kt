package com.kaizzinho.cobbletunes.network

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

/**
 * Pillar 9: sent server -> client whenever the player's current "zone" changes
 * — entering or leaving a gym structure, a Poké Center trigger block, or a
 * Poké Mart trigger block. Also sent when returning to normal (zoneId = "").
 *
 * @param zoneId one of:
 *   "cobbleverse:brock" / "cobbleverse:misty" / ... — a worldgen gym structure,
 *     mapped on the client to its RegionOfOrigin for GYM_AMBIENCE resolution.
 *   "cobbletunes:pokecenter" — within range of a MusicTriggerBlock (POKECENTER).
 *   "cobbletunes:pokemart"   — within range of a MusicTriggerBlock (POKEMART).
 *   ""                       — no zone; resume normal biome ambience.
 *
 * !! VERIFY PacketCodecs.STRING against your jar !! Same caveat as VAR_INT —
 * confirmed present in 1.21.1 vanilla but not genSources-verified here.
 */
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
