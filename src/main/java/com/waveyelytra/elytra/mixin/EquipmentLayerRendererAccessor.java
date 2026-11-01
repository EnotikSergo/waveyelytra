package com.waveyelytra.elytra.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentAssetManager;

@Mixin(EquipmentLayerRenderer.class)
public interface EquipmentLayerRendererAccessor {
	@Accessor("equipmentAssets")
	EquipmentAssetManager waveyelytra$getEquipmentAssets();
}
