package com.kaizzinho.cobbletunes.client.sound

import net.minecraft.entity.Entity
import net.minecraft.client.sound.AbstractSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.client.sound.TickableSoundInstance
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import net.minecraft.util.math.random.Random

class EvolutionSoundInstance(
    soundEvent: SoundEvent,
    private val sourceEntity: Entity?,
    private val baseVolume: Float,
    private val local: Boolean = false
) : AbstractSoundInstance(soundEvent, SoundCategory.RECORDS, Random.create()),
    TickableSoundInstance {

    private var done = false

    init {
        repeat = false
        repeatDelay = 0
        relative = local
        attenuationType = if (local) {
            SoundInstance.AttenuationType.NONE
        } else {
            SoundInstance.AttenuationType.LINEAR
        }
        pitch = 1f
        updatePosition()
    }

    fun setVolumeScale(scale: Float) {
        volume = (baseVolume * scale).coerceIn(0f, 1f)
    }

    fun finish() {
        done = true
    }

    fun isAudibleTo(listener: Entity, maxDistanceSquared: Double): Boolean {
        if (local) return true
        val source = sourceEntity ?: return false
        return !source.isRemoved && source.squaredDistanceTo(listener) <= maxDistanceSquared
    }

    override fun tick() {
        updatePosition()
    }

    private fun updatePosition() {
        val entity = sourceEntity ?: return
        x = entity.x
        y = entity.y
        z = entity.z
    }

    override fun isDone(): Boolean = done
}
