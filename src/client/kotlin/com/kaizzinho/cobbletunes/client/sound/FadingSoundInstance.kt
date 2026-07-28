package com.kaizzinho.cobbletunes.client.sound

import net.minecraft.client.sound.AbstractSoundInstance
import net.minecraft.client.sound.SoundInstance
import net.minecraft.client.sound.TickableSoundInstance
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvent
import kotlin.math.max
import kotlin.math.min

/**
 * A looping music/battle track that ramps its volume in on start and out on stop
 * instead of hard-cutting, so ClientMusicPlayer can crossfade between contexts.
 *
 * AbstractSoundInstance exposes `volume` as a protected mutable field, which is
 * exactly the hook we need: we recompute it every tick and the sound engine picks
 * the new value up on its own without us touching OpenAL/SoundEngine directly.
 */
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
        this.volume = 0f
        this.pitch = 1f
    }

    /** Start ramping this track's volume down; ClientMusicPlayer stops it once finished(). */
    fun beginFadeOut(seconds: Float) {
        fadingOut = true
        fadeOutTicks = max(1, (seconds * 20f).toInt())
        fadeOutElapsed = 0
    }

    fun finished(): Boolean = done

    override fun tick() {
        if (!fadingOut) {
            elapsedTicks++
            val fadeInTicks = max(1, (fadeInSeconds * 20f).toInt())
            volume = min(1f, elapsedTicks.toFloat() / fadeInTicks) * targetVolume
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
