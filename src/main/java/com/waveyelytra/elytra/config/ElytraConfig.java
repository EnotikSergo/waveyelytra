package com.waveyelytra.elytra.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;

import com.waveyelytra.elytra.sim.SimParams;

public final class ElytraConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("waveyelytra");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static ElytraConfig instance = new ElytraConfig();

	public boolean enabled = true;
	public boolean helicopter = false;
	public boolean helicopterSound = true;
	public float wingScale = 1.0F;
	public float stiffness = 0.35F;
	public float wingWeight = 1.0F;
	public float airDrag = 0.25F;
	public float flutter = 0.06F;
	public float maxBendDegrees = 18.0F;
	public int meshQuality = 1;
	public float simulationRange = 64.0F;
	public float lowQualityRange = 24.0F;

	public static ElytraConfig get() {
		return instance;
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("waveyelytra.json");
	}

	public static void load() {
		Path path = path();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				ElytraConfig loaded = GSON.fromJson(reader, ElytraConfig.class);
				if (loaded != null) {
					instance = loaded;
				}
			} catch (IOException | JsonParseException e) {
				LOGGER.warn("Could not read {}, using defaults", path, e);
			}
		}
		instance.sanitize();
		save();
	}

	public static void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			LOGGER.warn("Could not write {}", path, e);
		}
	}

	public static void saveSanitized() {
		instance.sanitize();
		save();
	}

	private void sanitize() {
		wingScale = clamp(wingScale, 0.5F, 2.0F);
		stiffness = clamp(stiffness, 0.0F, 1.0F);
		wingWeight = clamp(wingWeight, 0.0F, 2.0F);
		airDrag = clamp(airDrag, 0.0F, 1.0F);
		flutter = clamp(flutter, 0.0F, 0.15F);
		maxBendDegrees = clamp(maxBendDegrees, 2.0F, 60.0F);
		meshQuality = Math.max(0, Math.min(2, meshQuality));
		simulationRange = clamp(simulationRange, 8.0F, 256.0F);
		lowQualityRange = clamp(lowQualityRange, 0.0F, simulationRange);
	}

	public void apply(SimParams params) {
		params.scale = wingScale;
		params.stiffness = stiffness;
		params.gravity = SimParams.BASE_GRAVITY * wingWeight;
		params.aeroStrength = airDrag;
		params.flutter = flutter;
		params.maxBendCos = (float) Math.cos(Math.toRadians(maxBendDegrees));
	}

	private static float clamp(float v, float min, float max) {
		return v < min ? min : Math.min(v, max);
	}
}
