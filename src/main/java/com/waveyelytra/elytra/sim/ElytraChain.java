package com.waveyelytra.elytra.sim;

public final class ElytraChain {
	private static final float BODY_HALF_X = 0.26F;
	private static final float BODY_MIN_Y = -0.1F;
	private static final float BODY_MAX_Y = 1.5F;
	private static final float BODY_BACK_Z = 0.14F;
	private static final float BODY_CENTER_Y = 0.4F;
	private static final float MAX_INERTIA = 0.06F;
	private static final float SIDEWAYS_HOLD = 0.3F;
	private static final float MIN_STIFFNESS = 0.03F;
	private static final float CONSTRAINT_DAMPING = 0.6F;

	public final float side;
	public final int segments;
	public final int points;

	public final float[] pos;
	public final float[] tickPos;
	public final float[] rest;
	private final float[] prev;
	private final float[] restFrom;
	private final float[] unconstrained;

	private final float[] rotation = new float[9];
	private final float[] pivot = new float[3];
	private float segmentLength;
	private boolean initialized;
	private int age;

	public ElytraChain(float side, int segments) {
		this.side = side;
		this.segments = Math.max(2, segments);
		this.points = this.segments + 1;
		pos = new float[points * 3];
		tickPos = new float[points * 3];
		rest = new float[points * 3];
		prev = new float[points * 3];
		restFrom = new float[points * 3];
		unconstrained = new float[points * 3];
	}

	public void computeRest(ElytraPose pose, float flap, float sweep, float scale) {
		ElytraShape.rigidPose(side, pose.rotX(), pose.rotY(), pose.rotZ(), pose.crouch(), flap, sweep, pose.extra(side),
				pose.heli(), rotation, pivot);
		spinePoints(side, segments, scale, rotation, pivot, rest);
		segmentLength = (ElytraShape.SPINE_MAX_Y - ElytraShape.SPINE_MIN_Y) * scale / 16.0F / segments;
	}

	public static void spinePoints(float side, int segments, float scale, float[] r, float[] pivot, float[] out) {
		float lx = ElytraShape.spineX(side) * scale / 16.0F;
		float lz = ElytraShape.SPINE_Z * scale / 16.0F;
		float y0 = ElytraShape.SPINE_MIN_Y * scale / 16.0F;
		float y1 = ElytraShape.SPINE_MAX_Y * scale / 16.0F;
		for (int i = 0; i <= segments; i++) {
			float ly = y0 + (y1 - y0) * i / segments;
			int o = i * 3;
			out[o] = pivot[0] + r[0] * lx + r[1] * ly + r[2] * lz;
			out[o + 1] = pivot[1] + r[3] * lx + r[4] * ly + r[5] * lz;
			out[o + 2] = pivot[2] + r[6] * lx + r[7] * ly + r[8] * lz;
		}
	}

	public void reset() {
		System.arraycopy(rest, 0, pos, 0, pos.length);
		System.arraycopy(rest, 0, prev, 0, prev.length);
		System.arraycopy(rest, 0, tickPos, 0, tickPos.length);
		initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public void step(WingInputs in, SimParams params, float rigidity) {
		if (!initialized) {
			reset();
			return;
		}
		age++;
		System.arraycopy(pos, 0, tickPos, 0, pos.length);
		System.arraycopy(pos, 0, restFrom, 0, 3);

		float nx = rotation[2], ny = rotation[5], nz = rotation[8];
		float stiffness = Math.max(params.stiffness, MIN_STIFFNESS);

		int substeps = params.substeps;
		float h = 1.0F / substeps;
		float h2 = h * h;
		float wave = in.fallFlying ? 0.0F : params.flutter * Math.min(in.speed, 2.0F) + params.idleFlutter;

		for (int s = 0; s < substeps; s++) {
			float t = (s + 1.0F) / substeps;
			pos[0] = restFrom[0] + (rest[0] - restFrom[0]) * t;
			pos[1] = restFrom[1] + (rest[1] - restFrom[1]) * t;
			pos[2] = restFrom[2] + (rest[2] - restFrom[2]) * t;
			float time = age + t;

			for (int i = 1; i < points; i++) {
				int o = i * 3;
				float px = pos[o], py = pos[o + 1], pz = pos[o + 2];
				float vx = (px - prev[o]) * params.damping;
				float vy = (py - prev[o + 1]) * params.damping;
				float vz = (pz - prev[o + 2]) * params.damping;
				float tvx = vx / h, tvy = vy / h, tvz = vz / h;

				float wx = in.omegaX, wy = in.omegaY, wz = in.omegaZ;
				float rx = px, ry = py - BODY_CENTER_Y, rz = pz;
				float wrx = wy * rz - wz * ry, wry = wz * rx - wx * rz, wrz = wx * ry - wy * rx;

				float ax = -(wy * wrz - wz * wry) - 2.0F * (wy * tvz - wz * tvy);
				float ay = -(wz * wrx - wx * wrz) - 2.0F * (wz * tvx - wx * tvz);
				float az = -(wx * wry - wy * wrx) - 2.0F * (wx * tvy - wy * tvx);

				ax += in.gravX * params.gravity - clamp(in.accX * params.inertia, MAX_INERTIA);
				ay += in.gravY * params.gravity - clamp(in.accY * params.inertia, MAX_INERTIA);
				az += in.gravZ * params.gravity - clamp(in.accZ * params.inertia, MAX_INERTIA);

				float airX = -(in.velX + tvx), airY = -(in.velY + tvy), airZ = -(in.velZ + tvz);
				float push = (airX * nx + airY * ny + airZ * nz) * params.aeroStrength;
				if (push > params.maxAero) push = params.maxAero;
				if (push < -params.maxAero) push = -params.maxAero;
				float along = (float) i / segments;
				float ripple = wave * along * (float) Math.sin(time * 0.9F - i * 0.7F + (side > 0.0F ? 0.0F : 1.3F));
				ax += nx * (push + ripple);
				ay += ny * (push + ripple);
				az += nz * (push + ripple);

				prev[o] = px;
				prev[o + 1] = py;
				prev[o + 2] = pz;
				px += vx + ax * h2;
				py += vy + ay * h2;
				pz += vz + az * h2;

				float k = stiffness * (1.0F - 0.65F * along) * h;
				px += (rest[o] - px) * Math.max(k, SIDEWAYS_HOLD * h);
				py += (rest[o + 1] - py) * k;
				pz += (rest[o + 2] - pz) * k;

				pos[o] = px;
				pos[o + 1] = py;
				pos[o + 2] = pz;
			}

			System.arraycopy(pos, 0, unconstrained, 0, pos.length);
			for (int it = 0; it < params.iterations; it++) {
				solveLengths();
				limitBend(params.maxBendCos);
				collideBody();
			}
			for (int i = 3; i < pos.length; i++) {
				prev[i] += (pos[i] - unconstrained[i]) * CONSTRAINT_DAMPING;
			}
		}

		if (rigidity > 0.0F) {
			for (int i = 0; i < pos.length; i++) {
				float v = pos[i] - prev[i];
				pos[i] += (rest[i] - pos[i]) * rigidity;
				prev[i] = pos[i] - v * (1.0F - rigidity);
			}
		}

		if (!isFinite()) {
			reset();
		}
	}

	private void solveLengths() {
		for (int i = 1; i < points; i++) {
			int a = (i - 1) * 3, b = i * 3;
			float dx = pos[b] - pos[a], dy = pos[b + 1] - pos[a + 1], dz = pos[b + 2] - pos[a + 2];
			float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (d < 1.0E-6F) {
				dx = rest[b] - rest[a];
				dy = rest[b + 1] - rest[a + 1];
				dz = rest[b + 2] - rest[a + 2];
				d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			}
			float k = segmentLength / d;
			pos[b] = pos[a] + dx * k;
			pos[b + 1] = pos[a + 1] + dy * k;
			pos[b + 2] = pos[a + 2] + dz * k;
		}
	}

	private void limitBend(float maxBendCos) {
		float px = rest[3] - rest[0], py = rest[4] - rest[1], pz = rest[5] - rest[2];
		float pl = (float) Math.sqrt(px * px + py * py + pz * pz);
		px /= pl;
		py /= pl;
		pz /= pl;
		for (int i = 1; i < points; i++) {
			int a = (i - 1) * 3, b = i * 3;
			float dx = pos[b] - pos[a], dy = pos[b + 1] - pos[a + 1], dz = pos[b + 2] - pos[a + 2];
			float d = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (d < 1.0E-6F) {
				continue;
			}
			dx /= d;
			dy /= d;
			dz /= d;
			float cos = px * dx + py * dy + pz * dz;
			if (cos < maxBendCos) {
				float w = (maxBendCos - cos) / Math.max(1.0E-4F, 1.0F - cos);
				dx += (px - dx) * w;
				dy += (py - dy) * w;
				dz += (pz - dz) * w;
				float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
				dx /= l;
				dy /= l;
				dz /= l;
				pos[b] = pos[a] + dx * d;
				pos[b + 1] = pos[a + 1] + dy * d;
				pos[b + 2] = pos[a + 2] + dz * d;
			}
			px = dx;
			py = dy;
			pz = dz;
		}
	}

	private void collideBody() {
		for (int i = 1; i < points; i++) {
			int o = i * 3;
			float x = pos[o], y = pos[o + 1];
			if (x > -BODY_HALF_X && x < BODY_HALF_X && y > BODY_MIN_Y && y < BODY_MAX_Y) {
				float minZ = Math.min(BODY_BACK_Z, rest[o + 2]);
				if (pos[o + 2] < minZ) {
					pos[o + 2] = minZ;
				}
			}
		}
	}

	private static float clamp(float v, float max) {
		return v < -max ? -max : Math.min(v, max);
	}

	private boolean isFinite() {
		for (float v : pos) {
			if (!Float.isFinite(v) || Math.abs(v) > 16.0F) {
				return false;
			}
		}
		return true;
	}
}
