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
    private var fadingOut = false
    private var fadeOutTicks = 1
    private var fadeOutElapsed = 0
    private var done = false

    init {
        this.repeat = looping
        this.repeatDelay = 0
        this.relative = true
        this.attenuationType = SoundInstance.AttenuationType.NONE
        this.pitch = 1f
        this.volume = if (fadeInSeconds <= 0f) {
            targetVolume
        } else {
            0.05f  // small floor so quiet tracks still get an audio channel
        }
    }

    fun beginFadeOut(seconds: Float) {
        fadingOut = true
        fadeOutTicks = max(1, (seconds * 20f).toInt())
        fadeOutElapsed = 0
    }

    fun finished(): Boolean = done

    override fun tick() {
        if (!fadingOut) {
            if (fadeInSeconds <= 0f) {
                volume = targetVolume
            } else {
                elapsedTicks++
                val fadeInTicks = max(1, (fadeInSeconds * 20f).toInt())
                volume = min(1f, elapsedTicks.toFloat() / fadeInTicks) * targetVolume
            }
        } else {
            fadeOutElapsed++
            val outFraction = 1f - min(1f, fadeOutElapsed.toFloat() / fadeOutTicks)
            volume = outFraction * targetVolume
            if (fadeOutElapsed >= fadeOutTicks) {
                done = true
            }
        }
    }

    override fun isDone(): Boolean = done
}