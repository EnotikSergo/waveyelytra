package com.waveyelytra.elytra.sim;

public final class WingInputs {
	public float velX, velY, velZ;
	public float accX, accY, accZ;
	public float omegaX, omegaY, omegaZ;
	public float gravX, gravY = 1.0F, gravZ;

	public float speed;
	public float forwardAccel;
	public float verticalSpeed;
	public float turnRate;
	public float pitch;

	public float glideRatio = 1.0F;

	public boolean fallFlying;
	public boolean onGround;
	public boolean crouching;
	public boolean inWater;
	public boolean thrust;
	public boolean helicopter;

	public void reset() {
		velX = velY = velZ = 0.0F;
		accX = accY = accZ = 0.0F;
		omegaX = omegaY = omegaZ = 0.0F;
		gravX = 0.0F;
		gravY = 1.0F;
		gravZ = 0.0F;
		speed = forwardAccel = verticalSpeed = pitch = turnRate = 0.0F;
		fallFlying = onGround = crouching = inWater = thrust = helicopter = false;
		glideRatio = 1.0F;
	}
}
