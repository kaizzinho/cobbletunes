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

class MusicTriggerBlock(settings: Settings) : BlockWithEntity(settings) {

    // vanilla needs a codec for custom blocks
    override fun getCodec(): MapCodec<out BlockWithEntity> =
        throw UnsupportedOperationException("MusicTriggerBlock is not data-driven")

    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        Entity(pos, state)

    override fun getRenderType(state: BlockState): BlockRenderType =
        BlockRenderType.MODEL


    class Entity(pos: BlockPos, state: BlockState) :
        BlockEntity(TYPE, pos, state) {

        var zoneId: String = ""

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
