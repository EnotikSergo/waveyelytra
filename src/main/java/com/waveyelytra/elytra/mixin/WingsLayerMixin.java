package com.waveyelytra.elytra.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import com.waveyelytra.elytra.render.WingRenderer;

@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin {
	@Shadow
	@Final
	private EquipmentLayerRenderer equipmentRenderer;

	@Inject(
			method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
			at = @At("HEAD"),
			cancellable = true
	)
	private void waveyelytra$submitWings(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
			HumanoidRenderState state, float yRot, float xRot, CallbackInfo ci) {
		if (state instanceof AvatarRenderState avatar
				&& WingRenderer.submit(poseStack, collector, lightCoords, avatar, this.equipmentRenderer)) {
			ci.cancel();
		}
	}
}
