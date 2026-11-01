package com.waveyelytra.elytra.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;

import com.waveyelytra.elytra.sound.HelicopterSoundInstance;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
	@ModifyArg(method = "onSyncedDataUpdated", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/sounds/SoundManager;play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;"))
	private SoundInstance waveyelytra$helicopterSound(SoundInstance sound) {
		if (sound instanceof ElytraOnPlayerSoundInstance && HelicopterSoundInstance.active()) {
			return new HelicopterSoundInstance((LocalPlayer) (Object) this);
		}
		return sound;
	}
}
