package com.waveyelytra.elytra;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

import com.waveyelytra.elytra.config.ElytraConfig;
import com.waveyelytra.elytra.render.WingRenderer;
import com.waveyelytra.elytra.sim.SimulationManager;

public final class ElytraClient implements ClientModInitializer {
	public static final String MOD_ID = "waveyelytra";

	@Override
	public void onInitializeClient() {
		ElytraConfig.load();
		ClientTickEvents.END_CLIENT_TICK.register(client -> SimulationManager.get().tick(client));
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> {
			SimulationManager.get().clear();
			WingRenderer.clearCaches();
		});
		ClientPlayConnectionEvents.DISCONNECT.register((listener, client) -> SimulationManager.get().clear());
	}
}
