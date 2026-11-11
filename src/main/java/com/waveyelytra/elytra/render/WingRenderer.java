package com.waveyelytra.elytra.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.equipment.Equippable;

import com.waveyelytra.elytra.config.ElytraConfig;
import com.waveyelytra.elytra.mixin.EquipmentLayerRendererAccessor;
import com.waveyelytra.elytra.sim.SimulationManager;
import com.waveyelytra.elytra.sim.WingSimulation;

public final class WingRenderer {
	private static final Geometry[] POOL = new Geometry[256];
	private static int poolIndex;
	private static final Map<EquipmentClientInfo.Layer, Identifier> TEXTURES = new HashMap<>();

	static {
		for (int i = 0; i < POOL.length; i++) {
			POOL[i] = new Geometry();
		}
	}

	private WingRenderer() {
	}

	public static boolean submit(PoseStack poseStack, SubmitNodeCollector collector, int light, AvatarRenderState state,
			EquipmentLayerRenderer equipmentRenderer) {
		ElytraConfig config = ElytraConfig.get();
		if (!config.enabled) {
			return false;
		}
		ItemStack stack = state.chestEquipment;
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null || equippable.assetId().isEmpty()) {
			return false;
		}
		EquipmentClientInfo info = ((EquipmentLayerRendererAccessor) equipmentRenderer).waveyelytra$getEquipmentAssets()
				.get(equippable.assetId().get());
		List<EquipmentClientInfo.Layer> layers = info.getLayers(EquipmentClientInfo.LayerType.WINGS);
		if (layers.isEmpty()) {
			return false;
		}

		WingSimulation sim = SimulationManager.get().getForRender(state.id);
		if (!sim.isInitialized()) {
			return false;
		}

		float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
		Identifier playerTexture = playerElytraTexture(state);
		int dyeColor = DyedItemColor.getOrDefault(stack, 0);
		boolean glint = stack.hasFoil();
		float scale = SimulationManager.get().params().scale;

		int order = 0;
		for (EquipmentClientInfo.Layer layer : layers) {
			int color = colorForLayer(layer, dyeColor);
			if (color == 0) {
				continue;
			}
			Identifier texture = layer.usePlayerTexture() && playerTexture != null ? playerTexture : texture(layer);
			RenderType renderType = RenderTypes.armorCutoutNoCull(texture);
			Geometry geometry = POOL[poolIndex];
			poolIndex = (poolIndex + 1) % POOL.length;
			geometry.set(sim, WingMask.forTexture(texture), partialTick, state, scale, light, color);
			collector.order(order++).submitCustomGeometry(poseStack, renderType, geometry);
			if (glint) {
				collector.order(order++).submitCustomGeometry(poseStack, RenderTypes.armorEntityGlint(), geometry);
			}
			glint = false;
		}
		return true;
	}

	private static Identifier texture(EquipmentClientInfo.Layer layer) {
		return TEXTURES.computeIfAbsent(layer, l -> l.getTextureLocation(EquipmentClientInfo.LayerType.WINGS));
	}

	private static @Nullable Identifier playerElytraTexture(AvatarRenderState state) {
		PlayerSkin skin = state.skin;
		if (skin.elytra() != null) {
			return skin.elytra().texturePath();
		}
		if (skin.cape() != null && state.showCape) {
			return skin.cape().texturePath();
		}
		return null;
	}

	private static int colorForLayer(EquipmentClientInfo.Layer layer, int dyeColor) {
		Optional<EquipmentClientInfo.Dyeable> dyeable = layer.dyeable();
		if (dyeable.isPresent()) {
			int undyed = dyeable.get().colorWhenUndyed().map(ARGB::opaque).orElse(0);
			return dyeColor != 0 ? dyeColor : undyed;
		}
		return -1;
	}

	public static void clearCaches() {
		TEXTURES.clear();
		WingMask.clear();
	}

	private static final class Geometry implements SubmitNodeCollector.CustomGeometryRenderer {
		private WingSimulation sim;
		private WingMask mask;
		private float partialTick;
		private float flap, sweep;
		private float rotX, rotY, rotZ;
		private float crouch;
		private float extraLeft, extraRight, heli, rigid;
		private float scale;
		private int light;
		private int color;

		void set(WingSimulation sim, WingMask mask, float partialTick, AvatarRenderState state, float scale, int light,
				int color) {
			this.sim = sim;
			this.mask = mask;
			this.partialTick = partialTick;
			this.flap = sim.flight.flap(partialTick);
			this.sweep = sim.flight.sweep(partialTick);
			this.rotX = sim.pose.rotX(partialTick);
			this.rotY = sim.pose.rotY(partialTick);
			this.rotZ = sim.pose.rotZ(partialTick);
			this.crouch = sim.pose.crouch(partialTick);
			this.extraLeft = sim.pose.extra(1.0F, partialTick);
			this.extraRight = sim.pose.extra(-1.0F, partialTick);
			this.heli = sim.pose.heli(partialTick);
			this.rigid = sim.pose.rigid(partialTick);
			this.scale = scale;
			this.light = light;
			this.color = color;
		}

		@Override
		public void render(PoseStack.Pose pose, VertexConsumer buffer) {
			if (sim != null) {
				WingMeshBuilder.build(sim, mask, partialTick, rotX, rotY, rotZ, crouch, flap, sweep, extraLeft, extraRight, heli,
						rigid, scale, pose, buffer, light, OverlayTexture.NO_OVERLAY, color);
			}
		}
	}
}
