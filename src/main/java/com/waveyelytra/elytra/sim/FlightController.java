package com.waveyelytra.elytra.sim;

public final class FlightController {
	private static final float BEAT_TICKS = 5.0F;
	private static final float UPSTROKE = 0.6F;
	private static final float BEAT_AMPLITUDE = 0.6F;
	private static final int BOOST_BEATS = 3;
	private static final int TURN_BEATS = 2;
	private static final float TURN_RATE = 6.0F;
	private static final float DIVE_PITCH = 35.0F;
	private static final float DIVE_SWEEP = 0.9F;
	private static final float DIVE_LIFT = -0.12F;
	private static final float FLIGHT_RIGIDITY = 0.93F;
	private static final int CROUCH_HOLD_TICKS = 8;
	private static final float CROUCH_TOGGLE_RIGIDITY = 0.5F;
	private static final float ROTOR_RIGIDITY = 1.0F;

	private float flap;
	private float prevFlap;
	private float sweep;
	private float prevSweep;
	private float rigidity;

	private float phase;
	private int beatsLeft;
	private int cooldown;
	private boolean prevThrust;
	private boolean prevCrouching;
	private int crouchHold;
	private float dive;
	private int age;

	public void tick(WingInputs in) {
		prevFlap = flap;
		prevSweep = sweep;
		age++;
		boolean rotor = in.fallFlying && in.helicopter;
		boolean flying = in.fallFlying && !rotor;

		boolean boostStarted = in.thrust && !prevThrust;
		prevThrust = in.thrust;
		if (cooldown > 0) {
			cooldown--;
		}
		if (!flying) {
			beatsLeft = 0;
		} else if (beatsLeft == 0) {
			if (boostStarted && in.pitch < DIVE_PITCH && dive < 0.3F) {
				startBeats(BOOST_BEATS);
			} else if (Math.abs(in.turnRate) > TURN_RATE && cooldown == 0) {
				startBeats(TURN_BEATS);
			}
		}

		float diveTarget = 0.0F;
		if (flying && in.pitch > DIVE_PITCH && in.verticalSpeed < -0.4F) {
			diveTarget = Math.min(1.0F, (in.pitch - DIVE_PITCH) / 30.0F);
		}
		dive += (diveTarget - dive) * 0.15F;

		float flapTarget = 0.0F;
		if (flying) {
			float amp = (0.025F + 0.015F * Math.min(in.speed, 2.0F)) * (1.0F - dive);
			flapTarget = amp * ((float) Math.sin(age * 2.7F) + 0.6F * (float) Math.sin(age * 4.3F + 1.0F));
			flapTarget += DIVE_LIFT * dive;
		}

		if (beatsLeft > 0) {
			phase += 1.0F / BEAT_TICKS;
			if (phase >= 1.0F) {
				phase -= 1.0F;
				beatsLeft--;
				if (beatsLeft == 0) {
					phase = 0.0F;
					cooldown = 10;
				}
			}
			flap = flapTarget + (beatsLeft > 0 ? beat(phase) : 0.0F);
		} else {
			flap += (flapTarget - flap) * 0.4F;
		}
		sweep += ((flying ? DIVE_SWEEP * dive : 0.0F) - sweep) * 0.25F;

		if (in.crouching != prevCrouching) {
			crouchHold = CROUCH_HOLD_TICKS;
		} else if (crouchHold > 0) {
			crouchHold--;
		}
		prevCrouching = in.crouching;
		float rigidTarget = rotor ? ROTOR_RIGIDITY : flying ? FLIGHT_RIGIDITY : crouchHold > 0 ? CROUCH_TOGGLE_RIGIDITY : 0.0F;
		rigidity = rigidTarget > rigidity ? rigidTarget : rigidity + (rigidTarget - rigidity) * 0.08F;
	}

	private void startBeats(int count) {
		beatsLeft = count;
		phase = 0.0F;
	}

	private static float beat(float p) {
		float q = p < UPSTROKE ? 0.5F * p / UPSTROKE : 0.5F + 0.5F * (p - UPSTROKE) / (1.0F - UPSTROKE);
		return -BEAT_AMPLITUDE * (float) Math.sin(q * Math.PI * 2.0);
	}

	public float flap(float partialTick) {
		return prevFlap + (flap - prevFlap) * partialTick;
	}

	public float sweep(float partialTick) {
		return prevSweep + (sweep - prevSweep) * partialTick;
	}

	public float flap() {
		return flap;
	}

	public float sweep() {
		return sweep;
	}

	public float rigidity() {
		return rigidity;
	}

	public boolean isBeating() {
		return beatsLeft > 0;
	}

	public float diveAmount() {
		return dive;
	}
}
