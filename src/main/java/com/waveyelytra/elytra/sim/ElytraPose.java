package com.waveyelytra.elytra.sim;

public final class ElytraPose {
	private static final float REST_X = (float) (Math.PI / 12);
	private static final float REST_Z = (float) (-Math.PI / 12);
	private static final float FLY_X = (float) (Math.PI / 9);
	private static final float FLY_Z = (float) (-Math.PI / 2);
	private static final float CROUCH_LEAN = 0.5F;
	private static final float RATE = 0.3F;
	private static final float HELI_X = 0.35F;
	private static final float SPIN_MIN = 0.3F;
	private static final float SPIN_PER_SPEED = 0.6F;
	private static final float SPIN_MAX = 1.5F;
	private static final float TWO_PI = (float) (Math.PI * 2.0);
	private static final float UNWIND_MAX = 0.45F;
	private static final float UNWIND_MIN = 0.03F;
	private static final int UNWIND_TURNS = 2;
	private static final float MIRROR_RATE = 0.2F;
	private static final float[] LIFT_SPEEDS = {0.0F, 0.1F, 0.215F, 0.28F, 0.55F, 1.0F};
	private static final float[] LIFT_ANGLES = {0.0F, 0.17F, 0.45F, 0.49F, 0.86F, 0.94F};
	private static final float LIFT_MAX = 1.0F;
	private static final float LIFT_SPREAD = 0.15F;
	private static final float LIFT_YAW = 0.3F;
	private static final float WIND = 0.08F;
	private static final float STAND_TILT = -0.133F;

	private float rotX = REST_X, rotY, rotZ = REST_Z, crouch;
	private float prevX = REST_X, prevY, prevZ = REST_Z, prevCrouch;
	private float heli, prevHeli, spin, prevSpin, spinRate;
	private boolean unwinding;
	private float unwind, prevUnwind, unwindRate, unwindDecel, unwindTarget;
	private float mirror, prevMirror;
	private float lift;
	private int age;

	public void tick(WingInputs in) {
		prevX = rotX;
		prevY = rotY;
		prevZ = rotZ;
		prevCrouch = crouch;
		prevHeli = heli;
		prevSpin = spin;
		prevUnwind = unwind;
		prevMirror = mirror;
		float targetX, targetZ, targetY = 0.0F;
		boolean crouched = in.crouching && !in.fallFlying;
		boolean rotor = in.helicopter && in.fallFlying;
		if (rotor) {
			targetX = HELI_X;
			targetZ = FLY_Z;
		} else if (in.fallFlying) {
			targetX = REST_X + (FLY_X - REST_X) * in.glideRatio;
			targetZ = REST_Z + (FLY_Z - REST_Z) * in.glideRatio;
		} else if (crouched) {
			targetX = REST_X + CROUCH_LEAN + tickLift(in) * 0.6F;
			targetZ = REST_Z - lift * LIFT_SPREAD * 0.6F;
			targetY = lift * LIFT_YAW * 0.6F;
		} else {
			targetX = REST_X + STAND_TILT + tickLift(in);
			targetZ = REST_Z - lift * LIFT_SPREAD;
			targetY = lift * LIFT_YAW;
		}
		rotX += (targetX - rotX) * RATE;
		rotY += (targetY - rotY) * RATE;
		rotZ += (targetZ - rotZ) * RATE;
		crouch += ((crouched ? 1.0F : 0.0F) - crouch) * RATE;
		if (rotor) {
			if (unwinding) {
				spin = unwind;
				prevSpin = prevUnwind;
				spinRate = unwindRate;
				unwinding = false;
				unwind = prevUnwind = mirror = prevMirror = 0.0F;
			}
			float rateTarget = Math.min(SPIN_MAX, SPIN_MIN + SPIN_PER_SPEED * in.speed);
			spinRate += (rateTarget - spinRate) * 0.1F;
			spin += spinRate;
			if (spin > TWO_PI) {
				spin -= TWO_PI;
				prevSpin -= TWO_PI;
			}
		} else if (spin != 0.0F) {
			startUnwind();
		}
		if (unwinding) {
			tickUnwind();
		}
		heli += ((rotor ? 1.0F : 0.0F) - heli) * RATE;
		if (heli < 1.0E-3F) {
			heli = 0.0F;
		}
	}

	private float tickLift(WingInputs in) {
		age++;
		float horizontal = (float) Math.sqrt(in.velX * in.velX + in.velZ * in.velZ);
		float air = horizontal + 0.6F * Math.max(0.0F, -in.verticalSpeed);
		float target = liftFor(air);
		lift += (target - lift) * (target > lift ? 0.25F : 0.1F);
		float gust = 0.6F + 0.4F * (float) Math.sin(age * 0.031F) * (float) Math.sin(age * 0.083F + 1.0F);
		return lift + WIND * gust * (1.0F - 0.5F * lift / LIFT_MAX);
	}

	private static float liftFor(float speed) {
		for (int i = 1; i < LIFT_SPEEDS.length; i++) {
			if (speed <= LIFT_SPEEDS[i]) {
				float t = (speed - LIFT_SPEEDS[i - 1]) / (LIFT_SPEEDS[i] - LIFT_SPEEDS[i - 1]);
				return LIFT_ANGLES[i - 1] + (LIFT_ANGLES[i] - LIFT_ANGLES[i - 1]) * t;
			}
		}
		return LIFT_ANGLES[LIFT_ANGLES.length - 1];
	}

	private void startUnwind() {
		unwinding = true;
		unwind = spin;
		prevUnwind = prevSpin;
		mirror = wrap(2.0F * spin);
		prevMirror = mirror - 2.0F * (spin - prevSpin);
		unwindRate = Math.min(UNWIND_MAX, Math.max(spinRate, 0.2F));
		unwindTarget = ((int) Math.ceil(spin / TWO_PI) + UNWIND_TURNS) * TWO_PI;
		unwindDecel = unwindRate * unwindRate / (2.0F * (unwindTarget - unwind));
		spin = prevSpin = spinRate = 0.0F;
	}

	private void tickUnwind() {
		mirror *= 1.0F - MIRROR_RATE;
		unwindRate = Math.max(UNWIND_MIN, unwindRate - unwindDecel);
		float left = unwindTarget - unwind;
		if (unwindRate >= left) {
			prevUnwind -= unwindTarget;
			unwind = 0.0F;
			mirror = prevMirror = 0.0F;
			unwindRate = 0.0F;
			unwinding = false;
			return;
		}
		unwind += unwindRate;
	}

	private static float wrap(float a) {
		a %= TWO_PI;
		if (a > Math.PI) a -= TWO_PI;
		if (a < -Math.PI) a += TWO_PI;
		return a;
	}

	public float extra(float side) {
		return unwinding ? (side > 0.0F ? unwind : mirror - unwind) : spin;
	}

	public float extra(float side, float pt) {
		if (unwinding || unwind != prevUnwind) {
			float u = prevUnwind + (unwind - prevUnwind) * pt;
			float m = prevMirror + (mirror - prevMirror) * pt;
			return side > 0.0F ? u : m - u;
		}
		return prevSpin + (spin - prevSpin) * pt;
	}

	public float rigid() {
		return unwinding ? 1.0F : heli;
	}

	public float rigid(float pt) {
		return unwinding || unwind != prevUnwind ? 1.0F : heli(pt);
	}

	public void reset(WingInputs in) {
		tick(in);
		rotX = prevX = in.fallFlying ? FLY_X : REST_X;
		rotZ = prevZ = in.fallFlying ? FLY_Z : REST_Z;
		heli = prevHeli = in.helicopter && in.fallFlying ? 1.0F : 0.0F;
	}

	public float rotX() { return rotX; }
	public float rotY() { return rotY; }
	public float rotZ() { return rotZ; }
	public float crouch() { return crouch; }
	public float heli() { return heli; }
	public float spin() { return spin; }
	public float spinRate() { return unwinding ? unwindRate : spinRate; }
	public boolean unwinding() { return unwinding; }

	public float rotX(float pt) { return prevX + (rotX - prevX) * pt; }
	public float rotY(float pt) { return prevY + (rotY - prevY) * pt; }
	public float rotZ(float pt) { return prevZ + (rotZ - prevZ) * pt; }
	public float crouch(float pt) { return prevCrouch + (crouch - prevCrouch) * pt; }
	public float heli(float pt) { return prevHeli + (heli - prevHeli) * pt; }
	public float spin(float pt) { return prevSpin + (spin - prevSpin) * pt; }
}
