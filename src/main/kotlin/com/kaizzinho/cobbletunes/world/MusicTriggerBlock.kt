package com.kaizzinho.cobbletunes.world

import com.kaizzinho.cobbletunes.MOD_ID
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * Pillar 9: a visible but clearly "technical" block that marks the location of
 * a hand-placed structure (Poké Center, Poké Mart) for music zone detection.
 * Carries a zoneId string in its block entity, which StructureZoneDetector reads
 * when scanning nearby blocks.
 *
 * Placement: put one inside each WorldEdit schematic (e.g. hidden under the
 * floor or inside a wall). The block's appearance is a solid, distinctly colored
 * block — easy to find if you need to move it, but small enough to hide easily.
 *
 * zoneId values this block is expected to carry:
 *   "cobbletunes:pokecenter"  → routes client to POKECENTER context
 *   "cobbletunes:pokemart"    → routes client to POKEMART context
 *
 * !! VERIFY BlockWithEntity / BlockEntityType registration against your jar !!
 * This follows the standard Fabric 1.21.1 pattern for blocks with block entities
 * (BlockWithEntity → createBlockEntity(), getRenderType() = MODEL) but hasn't
 * been genSources-confirmed. If BlockEntityType.Builder.create() has a different
 * signature in your mappings, swap it here — nothing else in this file changes.
 */
class MusicTriggerBlock(settings: Settings) : BlockWithEntity(settings) {

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        Entity(pos, state)

    override fun getRenderType(state: BlockState): BlockRenderType =
        BlockRenderType.MODEL

    // ── Block entity ─────────────────────────────────────────────────────────

    class Entity(pos: BlockPos, state: BlockState) :
        BlockEntity(TYPE, pos, state) {

        var zoneId: String = ""

        override fun writeNbt(nbt: NbtCompound) {
            super.writeNbt(nbt)
            nbt.putString("ZoneId", zoneId)
        }

        override fun readNbt(nbt: NbtCompound) {
            super.readNbt(nbt)
            zoneId = nbt.getString("ZoneId")
        }

        companion object {
            lateinit var TYPE: BlockEntityType<Entity>
        }
    }

    // ── Registration helpers — called from CobbleTunes.onInitialize() ────────

    companion object {
        lateinit var INSTANCE: MusicTriggerBlock
            private set

        fun register() {
            INSTANCE = Registry.register(
                Registries.BLOCK,
                Identifier.of(MOD_ID, "music_trigger"),
                MusicTriggerBlock(Settings.create().strength(1.5f).requiresTool())
            )
            Entity.TYPE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                Identifier.of(MOD_ID, "music_trigger"),
                BlockEntityType.Builder.create(::Entity, INSTANCE).build(null)
            )
        }
    }
}
