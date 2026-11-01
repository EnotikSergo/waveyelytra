package com.waveyelytra.elytra.config;

import java.util.Locale;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public final class WaveyElytraConfigScreen extends OptionsSubScreen {
	private static final String PREFIX = "waveyelytra.options.";

	public WaveyElytraConfigScreen(Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable(PREFIX + "title"));
	}

	@Override
	protected void addOptions() {
		if (this.list == null) {
			return;
		}
		ElytraConfig c = ElytraConfig.get();
		this.list.addHeader(Component.translatable(PREFIX + "section.general"));
		this.list.addSmall(
				bool("enabled", c.enabled, v -> c.enabled = v),
				slider("wingScale", c.wingScale, 0.5F, 2.0F, 0.05F, "%.2f", v -> c.wingScale = v),
				quality(c),
				bool("helicopter", c.helicopter, v -> c.helicopter = v),
				bool("helicopterSound", c.helicopterSound, v -> c.helicopterSound = v));
		this.list.addHeader(Component.translatable(PREFIX + "section.physics"));
		this.list.addSmall(
				slider("stiffness", c.stiffness, 0.0F, 1.0F, 0.05F, "%.2f", v -> c.stiffness = v),
				percent("wingWeight", c.wingWeight, 2.0F, v -> c.wingWeight = v),
				slider("airDrag", c.airDrag, 0.0F, 1.0F, 0.05F, "%.2f", v -> c.airDrag = v),
				slider("flutter", c.flutter, 0.0F, 0.15F, 0.005F, "%.3f", v -> c.flutter = v),
				slider("maxBendDegrees", c.maxBendDegrees, 2.0F, 60.0F, 1.0F, "%.0f°", v -> c.maxBendDegrees = v));
		this.list.addHeader(Component.translatable(PREFIX + "section.performance"));
		this.list.addSmall(
				slider("simulationRange", c.simulationRange, 8.0F, 256.0F, 8.0F, "%.0f", v -> c.simulationRange = v),
				slider("lowQualityRange", c.lowQualityRange, 0.0F, 256.0F, 4.0F, "%.0f", v -> c.lowQualityRange = v));
	}

	@Override
	public void removed() {
		ElytraConfig.saveSanitized();
	}

	private static OptionInstance<Boolean> bool(String key, boolean value, Consumer<Boolean> setter) {
		OptionInstance.TooltipSupplier<Boolean> tooltip = tooltip(key);
		OptionInstance.CaptionBasedToString<Boolean> label = (caption, v) -> Component.literal(v ? "ON" : "OFF");
		return new OptionInstance<Boolean>(PREFIX + key, tooltip, label, OptionInstance.BOOLEAN_VALUES, Boolean.valueOf(value),
				setter::accept);
	}

	/** Float setting as an integer step slider; the label shows the real value. */
	private static OptionInstance<Integer> slider(String key, float value, float min, float max, float step, String format,
			Consumer<Float> setter) {
		int steps = Math.round((max - min) / step);
		int initial = Math.max(0, Math.min(steps, Math.round((value - min) / step)));
		return new OptionInstance<>(PREFIX + key, tooltip(key),
				(caption, v) -> Component.translatable("options.generic_value", caption,
						Component.literal(String.format(Locale.ROOT, format, min + v * step))),
				new OptionInstance.IntRange(0, steps, true), initial, v -> setter.accept(min + v * step));
	}

	/** 0..max multiplier shown in percent, 5% steps. */
	private static OptionInstance<Integer> percent(String key, float value, float max, Consumer<Float> setter) {
		int steps = Math.round(max * 20.0F);
		int initial = Math.max(0, Math.min(steps, Math.round(value * 20.0F)));
		return new OptionInstance<>(PREFIX + key, tooltip(key),
				(caption, v) -> Component.translatable("options.generic_value", caption, Component.literal(v * 5 + "%")),
				new OptionInstance.IntRange(0, steps, true), initial, v -> setter.accept(v / 20.0F));
	}

	private static OptionInstance<Integer> quality(ElytraConfig c) {
		return new OptionInstance<>(PREFIX + "meshQuality", tooltip("meshQuality"),
				(caption, v) -> Component.translatable("options.generic_value", caption,
						Component.translatable(PREFIX + "meshQuality." + v)),
				new OptionInstance.IntRange(0, 2, true), c.meshQuality, v -> c.meshQuality = v);
	}

	private static <T> OptionInstance.TooltipSupplier<T> tooltip(String key) {
		return OptionInstance.cachedConstantTooltip(Component.translatable(PREFIX + key + ".tooltip"));
	}
}
