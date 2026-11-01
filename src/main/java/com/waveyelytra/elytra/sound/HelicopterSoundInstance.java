package com.waveyelytra.elytra.sound;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import com.waveyelytra.elytra.ElytraClient;
import com.waveyelytra.elytra.config.ElytraConfig;

public final class HelicopterSoundInstance extends AbstractTickableSoundInstance {
	public static final SoundEvent HELICOPTER = SoundEvent.createVariableRangeEvent(
			Identifier.fromNamespaceAndPath(ElytraClient.MOD_ID, "item.elytra.helicopter"));
	private static final int DELAY = 20;
	private static final float LANDING_FADE = 0.55F;

	private final LocalPlayer player;
	private int time;

	public HelicopterSoundInstance(LocalPlayer player) {
		super(HELICOPTER, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
		this.player = player;
		this.looping = true;
		this.delay = 0;
		this.volume = 0.1F;
		this.x = player.getX();
		this.y = player.getY();
		this.z = player.getZ();
	}

	public static boolean active() {
		ElytraConfig config = ElytraConfig.get();
		return config.enabled && config.helicopter && config.helicopterSound;
	}

	@Override
	public void tick() {
		time++;
		if (player.isRemoved() || !active()) {
			stop();
			return;
		}
		x = player.getX();
		y = player.getY();
		z = player.getZ();
		if (time > DELAY && !player.isFallFlying()) {
			volume *= LANDING_FADE;
			if (volume < 0.01F) {
				stop();
			}
			return;
		}
		pitch = 1.0F;
		float speed = (float) player.getDeltaMovement().lengthSqr();
		volume = Mth.clamp(0.3F + speed / 4.0F, 0.3F, 1.0F);
		if (time < DELAY) {
			volume = 0.0F;
		} else if (time < DELAY * 2) {
			volume *= (time - DELAY) / (float) DELAY;
		}
	}
}
