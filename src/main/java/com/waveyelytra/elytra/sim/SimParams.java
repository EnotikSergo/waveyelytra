package com.waveyelytra.elytra.sim;

public final class SimParams {
	public int substeps = 3;
	public int iterations = 3;
	public float damping = 0.93F;
	public static final float BASE_GRAVITY = 0.025F;
	public float gravity = BASE_GRAVITY;
	public float inertia = 1.0F;
	public float stiffness = 0.35F;
	public float aeroStrength = 0.25F;
	public float maxAero = 0.15F;
	public float flutter = 0.06F;
	public float idleFlutter = 0.004F;
	public float maxBendCos = (float) Math.cos(Math.toRadians(18.0));
	public float scale = 1.0F;
}
