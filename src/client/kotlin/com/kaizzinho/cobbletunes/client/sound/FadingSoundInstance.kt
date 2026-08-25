package com.kaizzinho.cobbletunes.client.sound

import net.minecraft.client.sound.AbstractSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.client.sound.TickableSoundInstance
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import kotlin.math.max
import kotlin.math.min

class FadingSoundInstance(
    soundEvent: SoundEvent,
    private val targetVolume: Float,
    private val fadeInSeconds: Float,
    looping: Boolean
) : AbstractSoundInstance(soundEvent, SoundCategory.MUSIC, net.minecraft.util.math.random.Random.create()),
    TickableSoundInstance {

    private var elapsedTicks = 0
    private var baseVolume = 0f
    private var volumeMultiplier = 1f
    private var volumeMultiplierTarget = 1f
    private var volumeMultiplierTicks = 0
    private var volumeMultiplierStep = 0f
    private var fadingOut = false
    private var fadeOutTicks = 1
    private var fadeOutElapsed = 0
    private var fadeOutStartVolume = 0f
    private var done = false

    init {
        this.repeat = looping
        this.repeatDelay = 0
        this.relative = true
        this.attenuationType = SoundInstance.AttenuationType.NONE
        this.pitch = 1f
        this.baseVolume = if (fadeInSeconds <= 0f) {
            targetVolume
        } else {
            0.05f // small floor keeps quiet tracks alive
        }
        this.volume = baseVolume
    }

    fun setExternalVolumeMultiplier(multiplier: Float, transitionSeconds: Float = 0.35f) {
        val target = multiplier.coerceIn(0f, 1f)
        if (transitionSeconds <= 0f) {
            volumeMultiplier = target
            volumeMultiplierTarget = target
            volumeMultiplierTicks = 0
            volumeMultiplierStep = 0f
            if (!fadingOut) volume = baseVolume * volumeMultiplier
            return
        }

        volumeMultiplierTarget = target
        volumeMultiplierTicks = max(1, (transitionSeconds * 20f).toInt())
        volumeMultiplierStep = (volumeMultiplierTarget - volumeMultiplier) / volumeMultiplierTicks
    }

    fun beginFadeOut(seconds: Float) {
        fadeOutStartVolume = volume
        fadingOut = true
        fadeOutTicks = max(1, (seconds * 20f).toInt())
        fadeOutElapsed = 0
    }

    fun finished(): Boolean = done

    override fun tick() {
        if (!fadingOut) {
            tickVolumeMultiplier()
            if (fadeInSeconds <= 0f) {
                baseVolume = targetVolume
            } else {
                elapsedTicks++
                val fadeInTicks = max(1, (fadeInSeconds * 20f).toInt())
                baseVolume = min(1f, elapsedTicks.toFloat() / fadeInTicks) * targetVolume
            }
            volume = baseVolume * volumeMultiplier
        } else {
            fadeOutElapsed++
            val outFraction = 1f - min(1f, fadeOutElapsed.toFloat() / fadeOutTicks)
            volume = outFraction * fadeOutStartVolume
            if (fadeOutElapsed >= fadeOutTicks) {
                done = true
            }
        }
    }

    private fun tickVolumeMultiplier() {
        if (volumeMultiplierTicks <= 0) return
        volumeMultiplier += volumeMultiplierStep
        volumeMultiplierTicks--
        if (volumeMultiplierTicks <= 0) {
            volumeMultiplier = volumeMultiplierTarget
            volumeMultiplierStep = 0f
        }
    }

    override fun isDone(): Boolean = done
}
