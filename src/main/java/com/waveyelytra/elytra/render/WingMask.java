package com.waveyelytra.elytra.render;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

public final class WingMask {
	public static final int TEX_W = 64;
	public static final int TEX_H = 32;
	private static final int ALPHA_CUTOFF = 26;
	private static final Identifier VANILLA = Identifier.withDefaultNamespace("textures/entity/equipment/wings/elytra.png");
	private static final Logger LOGGER = LoggerFactory.getLogger("waveyelytra");

	private static final Map<Identifier, WingMask> CACHE = new HashMap<>();
	private static WingMask fallback;

	private final boolean[] opaque = new boolean[TEX_W * TEX_H];
	final Int2ObjectOpenHashMap<float[][]> templates = new Int2ObjectOpenHashMap<>();

	private WingMask() {
	}

	public boolean opaque(int u, int v) {
		return u >= 0 && u < TEX_W && v >= 0 && v < TEX_H && opaque[v * TEX_W + u];
	}

	public static WingMask forTexture(Identifier texture) {
		WingMask mask = CACHE.get(texture);
		if (mask == null) {
			mask = load(texture);
			if (mask == null) {
				mask = fallback();
			}
			CACHE.put(texture, mask);
		}
		return mask;
	}

	private static WingMask fallback() {
		if (fallback == null) {
			fallback = load(VANILLA);
			if (fallback == null) {
				fallback = new WingMask();
				for (int v = 2; v < 22; v++) {
					for (int u = 36; u < 46; u++) {
						fallback.opaque[v * TEX_W + u] = true;
					}
				}
			}
		}
		return fallback;
	}

	private static WingMask load(Identifier texture) {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
		if (resource.isEmpty()) {
			return loadDownloaded(texture);
		}
		try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
			return fromImage(image);
		} catch (Exception e) {
			LOGGER.warn("Could not read wing mask from {}", texture, e);
			return null;
		}
	}

	private static WingMask loadDownloaded(Identifier texture) {
		AbstractTexture loaded = Minecraft.getInstance().getTextureManager().getTexture(texture);
		if (!(loaded instanceof DynamicTexture dynamic) || dynamic.getPixels() == null || dynamic.getPixels().isClosed()) {
			return null;
		}
		return fromImage(dynamic.getPixels());
	}

	private static WingMask fromImage(NativeImage image) {
		float scaleX = image.getWidth() / (float) TEX_W;
		float scaleY = image.getHeight() / (float) TEX_H;
		WingMask mask = new WingMask();
		boolean any = false;
		for (int v = 0; v < 22; v++) {
			for (int u = 22; u < 46; u++) {
				int x = Math.min(image.getWidth() - 1, (int) ((u + 0.5F) * scaleX));
				int y = Math.min(image.getHeight() - 1, (int) ((v + 0.5F) * scaleY));
				boolean solid = (image.getPixel(x, y) >>> 24) >= ALPHA_CUTOFF;
				mask.opaque[v * TEX_W + u] = solid;
				any |= solid;
			}
		}
		return any ? mask : null;
	}

	public static void clear() {
		CACHE.clear();
		fallback = null;
	}
}
