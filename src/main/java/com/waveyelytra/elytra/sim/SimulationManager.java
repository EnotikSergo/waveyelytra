package com.waveyelytra.elytra.sim;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.waveyelytra.elytra.config.ElytraConfig;

public final class SimulationManager {
	private static final SimulationManager INSTANCE = new SimulationManager();
	private static final long FORGET_AFTER = 100;

	private final Int2ObjectOpenHashMap<Entry> entries = new Int2ObjectOpenHashMap<>();
	private final SimParams params = new SimParams();
	private @Nullable ClientLevel lastLevel;
	private long tick;

	public static SimulationManager get() {
		return INSTANCE;
	}

	public SimParams params() {
		return params;
	}

	public WingSimulation getForRender(int entityId) {
		Entry entry = entries.get(entityId);
		if (entry == null) {
			entry = new Entry(new WingSimulation(WingSimulation.segmentsFor(ElytraConfig.get().meshQuality)));
			entries.put(entityId, entry);
		}
		entry.lastRender = tick;
		return entry.sim;
	}

	public void tick(Minecraft client) {
		tick++;
		ClientLevel level = client.level;
		if (level != lastLevel) {
			clear();
			lastLevel = level;
		}
		if (level == null || entries.isEmpty()) {
			return;
		}

		ElytraConfig config = ElytraConfig.get();
		config.apply(params);
		float range2 = config.simulationRange * config.simulationRange;
		float lowRange2 = config.lowQualityRange * config.lowQualityRange;
		Entity viewer = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;

		ObjectIterator<Int2ObjectMap.Entry<Entry>> it = entries.int2ObjectEntrySet().fastIterator();
		while (it.hasNext()) {
			Int2ObjectMap.Entry<Entry> mapEntry = it.next();
			Entry entry = mapEntry.getValue();
			if (tick - entry.lastRender > FORGET_AFTER) {
				it.remove();
				continue;
			}

			Entity entity = level.getEntity(mapEntry.getIntKey());
			if (!(entity instanceof LivingEntity living)) {
				if (!entry.sim.isInitialized()) {
					entry.inputs.reset();
					entry.inputs.onGround = true;
					entry.sim.step(entry.inputs, params);
				}
				continue;
			}

			double dist2 = viewer != null && viewer != living ? living.distanceToSqr(viewer) : 0.0;
			if (entry.sim.isInitialized() && dist2 > range2) {
				entry.tracker.reset();
				continue;
			}

			int segments = WingSimulation.segmentsFor(dist2 > lowRange2 ? 0 : config.meshQuality);
			if (entry.sim.segments != segments) {
				entry.sim = new WingSimulation(segments);
			}

			entry.tracker.sample(living.getX(), living.getY(), living.getZ(), living.yBodyRot, living.getXRot(),
					living.isFallFlying(), entry.inputs);
			entry.inputs.onGround = living.onGround();
			entry.inputs.crouching = living.isCrouching();
			entry.inputs.inWater = living.isInWater();
			entry.inputs.helicopter = config.helicopter;
			Vec3 motion = living.getDeltaMovement();
			float ratio = 1.0F;
			if (motion.y < 0.0) {
				ratio = 1.0F - (float) Math.pow(-motion.normalize().y, 1.5);
			}
			entry.inputs.glideRatio = ratio;
			entry.sim.step(entry.inputs, params);
		}
	}

	public void clear() {
		entries.clear();
	}

	public @Nullable WingSimulation peek(int entityId) {
		Entry entry = entries.get(entityId);
		return entry != null ? entry.sim : null;
	}

	private static final class Entry {
		WingSimulation sim;
		final MotionTracker tracker = new MotionTracker();
		final WingInputs inputs = new WingInputs();
		long lastRender;

		Entry(WingSimulation sim) {
			this.sim = sim;
		}
	}
}
