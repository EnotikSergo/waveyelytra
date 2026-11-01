package com.waveyelytra.elytra.sim;

public final class MotionTracker {
	private static final float DEG = (float) (Math.PI / 180.0);
	private static final float ACCEL_SMOOTHING = 0.35F;
	private static final float MAX_ACCEL = 0.5F;
	private static final float MAX_OMEGA = 0.6F;
	private static final double TELEPORT = 10.0;

	private boolean hasSample;
	private double lastX, lastY, lastZ;
	private float velX, velY, velZ;
	private float accX, accY, accZ;
	private float flightBlend;
	private float lastYaw;
	private boolean hasYaw;

	private final float[] basis = new float[9];
	private final float[] prevBasis = new float[9];

	public void reset() {
		hasSample = false;
		velX = velY = velZ = 0.0F;
		accX = accY = accZ = 0.0F;
		flightBlend = 0.0F;
		hasYaw = false;
	}

	public void sample(double x, double y, double z, float bodyYaw, float pitch, boolean fallFlying, WingInputs out) {
		float nvx = 0.0F, nvy = 0.0F, nvz = 0.0F;
		if (hasSample) {
			double dx = x - lastX, dy = y - lastY, dz = z - lastZ;
			if (dx * dx + dy * dy + dz * dz < TELEPORT * TELEPORT) {
				nvx = (float) dx;
				nvy = (float) dy;
				nvz = (float) dz;
			}
		}
		float rawAx = hasSample ? nvx - velX : 0.0F;
		float rawAy = hasSample ? nvy - velY : 0.0F;
		float rawAz = hasSample ? nvz - velZ : 0.0F;
		accX += (rawAx - accX) * ACCEL_SMOOTHING;
		accY += (rawAy - accY) * ACCEL_SMOOTHING;
		accZ += (rawAz - accZ) * ACCEL_SMOOTHING;
		float accLen = (float) Math.sqrt(accX * accX + accY * accY + accZ * accZ);
		if (accLen > MAX_ACCEL) {
			float s = MAX_ACCEL / accLen;
			accX *= s;
			accY *= s;
			accZ *= s;
		}
		velX = nvx;
		velY = nvy;
		velZ = nvz;
		lastX = x;
		lastY = y;
		lastZ = z;

		flightBlend += ((fallFlying ? 1.0F : 0.0F) - flightBlend) * 0.25F;
		float theta = flightBlend * (90.0F + pitch) * DEG;
		float yaw = bodyYaw * DEG;
		float fx = -(float) Math.sin(yaw), fz = (float) Math.cos(yaw);
		float cos = (float) Math.cos(theta), sin = (float) Math.sin(theta);
		float ux = sin * fx, uy = cos, uz = sin * fz;
		float tfx = cos * fx, tfy = -sin, tfz = cos * fz;

		System.arraycopy(basis, 0, prevBasis, 0, 9);
		basis[0] = uy * tfz - uz * tfy;
		basis[1] = uz * tfx - ux * tfz;
		basis[2] = ux * tfy - uy * tfx;
		basis[3] = -ux;
		basis[4] = -uy;
		basis[5] = -uz;
		basis[6] = -tfx;
		basis[7] = -tfy;
		basis[8] = -tfz;

		float wx = 0.0F, wy = 0.0F, wz = 0.0F;
		if (hasSample) {
			for (int i = 0; i < 9; i += 3) {
				float ax = prevBasis[i], ay = prevBasis[i + 1], az = prevBasis[i + 2];
				float bx = basis[i], by = basis[i + 1], bz = basis[i + 2];
				wx += 0.5F * (ay * bz - az * by);
				wy += 0.5F * (az * bx - ax * bz);
				wz += 0.5F * (ax * by - ay * bx);
			}
		}
		hasSample = true;

		out.velX = dot(0, velX, velY, velZ);
		out.velY = dot(3, velX, velY, velZ);
		out.velZ = dot(6, velX, velY, velZ);
		out.accX = dot(0, accX, accY, accZ);
		out.accY = dot(3, accX, accY, accZ);
		out.accZ = dot(6, accX, accY, accZ);
		out.omegaX = clamp(dot(0, wx, wy, wz), MAX_OMEGA);
		out.omegaY = clamp(dot(3, wx, wy, wz), MAX_OMEGA);
		out.omegaZ = clamp(dot(6, wx, wy, wz), MAX_OMEGA);
		out.gravX = -basis[1];
		out.gravY = -basis[4];
		out.gravZ = -basis[7];

		float speed = (float) Math.sqrt(velX * velX + velY * velY + velZ * velZ);
		out.speed = speed;
		out.verticalSpeed = velY;
		out.forwardAccel = speed > 1.0E-4F ? (accX * velX + accY * velY + accZ * velZ) / speed : 0.0F;
		out.pitch = pitch;
		float yawDelta = hasYaw ? wrapDegrees(bodyYaw - lastYaw) : 0.0F;
		out.turnRate += (yawDelta - out.turnRate) * 0.5F;
		lastYaw = bodyYaw;
		hasYaw = true;
		out.fallFlying = fallFlying;
		out.thrust = fallFlying && (out.forwardAccel > 0.025F || velY > 0.15F);
	}

	private static float wrapDegrees(float deg) {
		deg %= 360.0F;
		if (deg >= 180.0F) deg -= 360.0F;
		if (deg < -180.0F) deg += 360.0F;
		return deg;
	}

	private float dot(int axis, float x, float y, float z) {
		return basis[axis] * x + basis[axis + 1] * y + basis[axis + 2] * z;
	}

	private static float clamp(float v, float max) {
		return v < -max ? -max : Math.min(v, max);
	}
}
