package com.waveyelytra.elytra.sim;

public final class WingSimulation {
	public static final int LEFT = 0;
	public static final int RIGHT = 1;

	public final ElytraChain[] chains;
	public final FlightController flight = new FlightController();
	public final ElytraPose pose = new ElytraPose();
	public final int segments;
	private float scale = -1.0F;

	public WingSimulation(int segments) {
		this.segments = segments;
		chains = new ElytraChain[]{new ElytraChain(1.0F, segments), new ElytraChain(-1.0F, segments)};
	}

	public void step(WingInputs in, SimParams params) {
		boolean rescale = scale != params.scale;
		scale = params.scale;
		if (isInitialized()) {
			pose.tick(in);
		} else {
			pose.reset(in);
		}
		flight.tick(in);
		for (ElytraChain chain : chains) {
			chain.computeRest(pose, flight.flap(), flight.sweep(), params.scale);
			if (rescale || !chain.isInitialized()) {
				chain.reset();
			} else {
				chain.step(in, params, Math.max(flight.rigidity(), pose.rigid()));
			}
		}
	}

	public boolean isInitialized() {
		return chains[0].isInitialized();
	}

	public static int segmentsFor(int quality) {
		return switch (quality) {
			case 0 -> 6;
			case 2 -> 16;
			default -> 11;
		};
	}
}
