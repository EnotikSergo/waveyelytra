package com.waveyelytra.elytra.sim;

public final class ElytraShape {
	public static final float GROW = 1.0F;
	public static final float BOX_MIN_Y = 0.0F;
	public static final float BOX_HEIGHT = 20.0F;
	public static final float BOX_MIN_Z = 0.0F;
	public static final float BOX_DEPTH = 2.0F;
	public static final float BOX_WIDTH = 10.0F;
	public static final float SPINE_Z = BOX_MIN_Z + BOX_DEPTH / 2.0F;
	public static final float SPINE_MIN_Y = BOX_MIN_Y - GROW;
	public static final float SPINE_MAX_Y = BOX_MIN_Y + BOX_HEIGHT + GROW;
	public static final float LAYER_Z = 2.0F;
	public static final float PIVOT_X = 5.0F;
	public static final float CROUCH_Y = 3.0F;
	public static final float MIDLINE_MARGIN = 0.5F;

	private ElytraShape() {
	}

	public static float boxMinX(float side) {
		return side > 0.0F ? -BOX_WIDTH : 0.0F;
	}

	public static float spineX(float side) {
		return boxMinX(side) + BOX_WIDTH / 2.0F;
	}

	public static void rotationZYX(float zRot, float yRot, float xRot, float[] out) {
		float cz = (float) Math.cos(zRot), sz = (float) Math.sin(zRot);
		float cy = (float) Math.cos(yRot), sy = (float) Math.sin(yRot);
		float cx = (float) Math.cos(xRot), sx = (float) Math.sin(xRot);
		float a00 = cz * cy, a01 = -sz, a02 = cz * sy;
		float a10 = sz * cy, a11 = cz, a12 = sz * sy;
		float a20 = -sy, a21 = 0.0F, a22 = cy;
		out[0] = a00;
		out[1] = a01 * cx + a02 * sx;
		out[2] = -a01 * sx + a02 * cx;
		out[3] = a10;
		out[4] = a11 * cx + a12 * sx;
		out[5] = -a11 * sx + a12 * cx;
		out[6] = a20;
		out[7] = a21 * cx + a22 * sx;
		out[8] = -a21 * sx + a22 * cx;
	}

	public static void rigidPose(float side, float elytraRotX, float elytraRotY, float elytraRotZ, float crouch,
			float flap, float sweep, float[] rotation, float[] pivot) {
		rigidPose(side, elytraRotX, elytraRotY, elytraRotZ, crouch, flap, sweep, 0.0F, 0.0F, rotation, pivot);
	}

	public static void rigidPose(float side, float elytraRotX, float elytraRotY, float elytraRotZ, float crouch,
			float flap, float sweep, float extra, float heli, float[] rotation, float[] pivot) {
		pivot[0] = side * PIVOT_X * (1.0F - heli) / 16.0F;
		pivot[1] = crouch * CROUCH_Y / 16.0F;
		pivot[2] = LAYER_Z / 16.0F;
		if (sweep > 0.0F && crossesMidline(side, elytraRotX, elytraRotY, elytraRotZ, flap, sweep, rotation)) {
			float lo = 0.0F, hi = sweep;
			for (int i = 0; i < 8; i++) {
				float mid = (lo + hi) * 0.5F;
				if (crossesMidline(side, elytraRotX, elytraRotY, elytraRotZ, flap, mid, rotation)) {
					hi = mid;
				} else {
					lo = mid;
				}
			}
			sweep = lo;
		}
		pose(side, elytraRotX, elytraRotY, elytraRotZ, flap, sweep, extra, rotation);
		if (heli > 0.0F) {
			float hub = heli * spineX(side) / 16.0F;
			pivot[0] -= rotation[0] * hub;
			pivot[1] -= rotation[3] * hub;
			pivot[2] -= rotation[6] * hub;
		}
	}

	private static void pose(float side, float elytraRotX, float elytraRotY, float elytraRotZ, float flap, float sweep,
			float[] rotation) {
		pose(side, elytraRotX, elytraRotY, elytraRotZ, flap, sweep, 0.0F, rotation);
	}

	private static void pose(float side, float elytraRotX, float elytraRotY, float elytraRotZ, float flap, float sweep,
			float extra, float[] rotation) {
		rotationZYX(elytraRotZ * side + extra, elytraRotY * side, elytraRotX, rotation);
		if (flap != 0.0F || sweep != 0.0F) {
			float cy = (float) Math.cos(side * flap), sy = (float) Math.sin(side * flap);
			float cz = (float) Math.cos(side * sweep), sz = (float) Math.sin(side * sweep);
			premultiply(cy * cz, -cy * sz, sy, sz, cz, 0.0F, -sy * cz, sy * sz, cy, rotation);
		}
	}

	private static boolean crossesMidline(float side, float elytraRotX, float elytraRotY, float elytraRotZ, float flap,
			float sweep, float[] rotation) {
		pose(side, elytraRotX, elytraRotY, elytraRotZ, flap, sweep, rotation);
		float lx = side > 0.0F ? -BOX_WIDTH : BOX_WIDTH;
		float ly = BOX_MIN_Y + BOX_HEIGHT;
		float lz = SPINE_Z;
		float x = side * PIVOT_X + rotation[0] * lx + rotation[1] * ly + rotation[2] * lz;
		return side * x < MIDLINE_MARGIN;
	}

	private static void premultiply(float a0, float a1, float a2, float a3, float a4, float a5, float a6, float a7, float a8,
			float[] m) {
		float b0 = m[0], b1 = m[1], b2 = m[2], b3 = m[3], b4 = m[4], b5 = m[5], b6 = m[6], b7 = m[7], b8 = m[8];
		m[0] = a0 * b0 + a1 * b3 + a2 * b6;
		m[1] = a0 * b1 + a1 * b4 + a2 * b7;
		m[2] = a0 * b2 + a1 * b5 + a2 * b8;
		m[3] = a3 * b0 + a4 * b3 + a5 * b6;
		m[4] = a3 * b1 + a4 * b4 + a5 * b7;
		m[5] = a3 * b2 + a4 * b5 + a5 * b8;
		m[6] = a6 * b0 + a7 * b3 + a8 * b6;
		m[7] = a6 * b1 + a7 * b4 + a8 * b7;
		m[8] = a6 * b2 + a7 * b5 + a8 * b8;
	}
}
