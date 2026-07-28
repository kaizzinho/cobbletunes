package com.kaizzinho.cobbletunes.network

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.util.Identifier

/**
 * Verified against the real 1.21.1 jar: PacketCodecs.BOOL confirmed as-is;
 * PacketCodecs.VAR_INT for Int fields; PacketCodecs.STRING for the trainerTier
 * below (confirm this name before trusting it — same caveat as VAR_INT from
 * before). PacketCodec.tuple's 6-field overload — bump from the previous 5.
 * If vanilla's PacketCodec.tuple tops out before 6, swap to PacketCodec.of(...)
 * with manual encode/decode lambdas.
 *
 * Sent server -> client once per BATTLE_STARTED_POST, one packet per player in
 * the battle (their own personalized view of "who's opposing me"). Carries raw
 * facts rather than a final MusicContext — client applies config on top.
 *
 * @param dexNumber -1 sentinel for "no legendary detected / not applicable."
 * @param opposingDexNumbers Pillar 3: full opposing roster for majority-vote
 *   regional trainer resolver.
 * @param trainerTier Pillar 4: RCT trainer tier string resolved server-side
 *   from TrainerMobData.getType().id() — "leader", "e4", "champ", "rival", or
 *   null if the opponent is not an RCT-managed NPC (wild, PvP, or RCT absent).
 *   Null is serialized as an empty string to avoid nullable-codec complexity;
 *   client treats "" identically to null (falls through to TRAINER_BATTLE).
 */
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