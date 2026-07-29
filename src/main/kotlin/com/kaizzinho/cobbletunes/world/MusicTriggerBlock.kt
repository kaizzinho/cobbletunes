package com.kaizzinho.cobbletunes.world

import com.kaizzinho.cobbletunes.MOD_ID
import com.mojang.serialization.MapCodec
import net.minecraft.block.Block
import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

/**
 * Pillar 9: a visible but clearly "technical" block that marks the location of
 * a hand-placed structure (Poké Center, Poké Mart) for music zone detection.
 * Carries a zoneId string in its block entity, which StructureZoneDetector reads
 * when scanning nearby blocks.
 *
 * Placement: put one inside each WorldEdit schematic (hidden under floor or
 * inside a wall). Set the zone via:
 *   /setblock ~ ~ ~ cobbletunes:music_trigger{ZoneId:"cobbletunes:pokecenter"}
 *   /setblock ~ ~ ~ cobbletunes:music_trigger{ZoneId:"cobbletunes:pokemart"}
 */
class MusicTriggerBlock(settings: Settings) : BlockWithEntity(settings) {

    // Required by BlockWithEntity in 1.21.1 — codec is unused since we register
    // this block manually rather than through a data-driven registry, but the
    // abstract member must be implemented or the class won't compile.
    override fun getCodec(): MapCodec<out BlockWithEntity> =
        throw UnsupportedOperationException("MusicTriggerBlock is not data-driven")

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        Entity(pos, state)

    override fun getRenderType(state: BlockState): BlockRenderType =
        BlockRenderType.MODEL

    // ── Block entity ─────────────────────────────────────────────────────────

    class Entity(pos: BlockPos, state: BlockState) :
        BlockEntity(TYPE, pos, state) {

        var zoneId: String = ""

        // 1.21.1: writeNbt/readNbt both take a RegistryWrapper.WrapperLookup
        // second parameter — the lookup is unused here since ZoneId is a plain
        // string with no registry references, but the signature must match.
        override fun writeNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup) {
            super.writeNbt(nbt, registryLookup)
            nbt.putString("ZoneId", zoneId)
        }

        override fun readNbt(nbt: NbtCompound, registryLookup: RegistryWrapper.WrapperLookup) {
            super.readNbt(nbt, registryLookup)
            zoneId = nbt.getString("ZoneId")
        }

        companion object {
            lateinit var TYPE: BlockEntityType<Entity>
        }
    }

    // ── Registration — called from CobbleTunes.onInitialize() ───────────────

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