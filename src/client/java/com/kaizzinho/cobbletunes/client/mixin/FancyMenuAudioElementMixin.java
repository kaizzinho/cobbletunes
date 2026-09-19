package com.kaizzinho.cobbletunes.client.mixin;

import com.kaizzinho.cobbletunes.client.compat.fancymenu.FancyMenuMusicSuppressor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "de.keksuccino.fancymenu.customization.element.elements.audio.AudioElement", remap = false)
public abstract class FancyMenuAudioElementMixin {
    @Inject(method = "renderTick", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void cobbletunes$suppressFancyMenuMusic(CallbackInfo ci) {
        if (FancyMenuMusicSuppressor.shouldSuppressAudioElement(this)) {
            ci.cancel();
        }
    }
}
