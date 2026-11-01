package com.waveyelytra.elytra.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.waveyelytra.elytra.sim.ElytraChain;
import com.waveyelytra.elytra.sim.ElytraShape;
import com.waveyelytra.elytra.sim.WingSimulation;

public final class WingMeshBuilder {
	private static final float TEX_W = WingMask.TEX_W;
	private static final float TEX_H = WingMask.TEX_H;
	private static final int COLUMNS = 10;
	private static final int ROWS = 20;
	private static final int LAYERS = 2;
	private static final float UV_INSET = 0.3F;
	private static final int STRIDE = 9;

	private static float[] spine = new float[0];
	private static float[] frames = new float[0];
	private static final float[] rotation = new float[9];
	private static final float[] pivot = new float[3];
	private static float[] rotor = new float[0];

	private WingMeshBuilder() {
	}

	public static void build(WingSimulation sim, WingMask mask, float partialTick, float elytraRotX, float elytraRotY,
			float elytraRotZ, float crouch, float flap, float sweep, float extraLeft, float extraRight, float heli,
			float rigid, float scale,
			PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
		float[][] templates = template(mask, sim.segments);
		for (int w = 0; w < 2; w++) {
			ElytraChain chain = sim.chains[w];
			float extra = chain.side > 0.0F ? extraLeft : extraRight;
			ElytraShape.rigidPose(chain.side, elytraRotX, elytraRotY, elytraRotZ, crouch, flap, sweep, extra, heli, rotation,
					pivot);
			buildFrames(chain, partialTick, rigid, scale);
			emit(templates[w], chain, scale, pose, buffer, light, overlay, color);
		}
	}

	private static void buildFrames(ElytraChain chain, float pt, float rigid, float scale) {
		int n = chain.points;
		if (spine.length < n * 3) {
			spine = new float[n * 3];
			frames = new float[n * 9];
			rotor = new float[n * 3];
		}
		for (int i = 0; i < n * 3; i++) {
			spine[i] = chain.tickPos[i] + (chain.pos[i] - chain.tickPos[i]) * pt;
		}
		if (rigid > 0.0F) {
			ElytraChain.spinePoints(chain.side, chain.segments, scale, rotation, pivot, rotor);
			for (int i = 0; i < n * 3; i++) {
				spine[i] += (rotor[i] - spine[i]) * rigid;
			}
		}
		float rx = rotation[0], ry = rotation[3], rz = rotation[6];
		for (int i = 0; i < n; i++) {
			int a = Math.max(0, i - 1) * 3, b = Math.min(n - 1, i + 1) * 3;
			float tx = spine[b] - spine[a], ty = spine[b + 1] - spine[a + 1], tz = spine[b + 2] - spine[a + 2];
			float tl = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
			if (tl < 1.0E-6F) {
				tx = rotation[1];
				ty = rotation[4];
				tz = rotation[7];
			} else {
				tx /= tl;
				ty /= tl;
				tz /= tl;
			}
			float d = rx * tx + ry * ty + rz * tz;
			float ax = rx - tx * d, ay = ry - ty * d, az = rz - tz * d;
			float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
			ax /= al;
			ay /= al;
			az /= al;
			float bx = ay * tz - az * ty, by = az * tx - ax * tz, bz = ax * ty - ay * tx;
			int f = i * 9;
			frames[f] = ax;
			frames[f + 1] = ay;
			frames[f + 2] = az;
			frames[f + 3] = tx;
			frames[f + 4] = ty;
			frames[f + 5] = tz;
			frames[f + 6] = bx;
			frames[f + 7] = by;
			frames[f + 8] = bz;
		}
	}

	private static void emit(float[] template, ElytraChain chain, float scale, PoseStack.Pose pose, VertexConsumer buffer,
			int light, int overlay, int color) {
		float spineX = ElytraShape.spineX(chain.side);
		float spineZ = ElytraShape.SPINE_Z;
		float k = scale / 16.0F;
		int last = chain.points - 1;
		for (int v = 0; v < template.length; v += STRIDE) {
			float row = template[v + 8];
			int r0 = Math.min((int) row, last);
			int r1 = Math.min(r0 + 1, last);
			float t = row - r0;
			int s0 = r0 * 3, s1 = r1 * 3, f0 = r0 * 9, f1 = r1 * 9;
			float lx = (template[v] - spineX) * k;
			float lz = (template[v + 2] - spineZ) * k;
			float nx = template[v + 5], ny = template[v + 6], nz = template[v + 7];
			float x = 0.0F, y = 0.0F, z = 0.0F, wx = 0.0F, wy = 0.0F, wz = 0.0F;
			for (int c = 0; c < 3; c++) {
				float p = lerp(spine[s0 + c], spine[s1 + c], t);
				float a = lerp(frames[f0 + c], frames[f1 + c], t);
				float tn = lerp(frames[f0 + 3 + c], frames[f1 + 3 + c], t);
				float b = lerp(frames[f0 + 6 + c], frames[f1 + 6 + c], t);
				float position = p + a * lx + b * lz;
				float normal = a * nx + tn * ny + b * nz;
				if (c == 0) {
					x = position;
					wx = normal;
				} else if (c == 1) {
					y = position;
					wy = normal;
				} else {
					z = position;
					wz = normal;
				}
			}
			buffer.addVertex(pose, x, y, z)
					.setColor(color)
					.setUv(template[v + 3], template[v + 4])
					.setOverlay(overlay)
					.setLight(light)
					.setNormal(pose, wx, wy, wz);
		}
	}

	private static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	private static float[][] template(WingMask mask, int segments) {
		float[][] cached = mask.templates.get(segments);
		if (cached == null) {
			cached = new float[][]{buildTemplate(mask, 1.0F, segments), buildTemplate(mask, -1.0F, segments)};
			mask.templates.put(segments, cached);
		}
		return cached;
	}

	private static float xEdge(float side, int column) {
		float minX = ElytraShape.boxMinX(side);
		return side > 0.0F ? minX + column : minX + COLUMNS - column;
	}

	private static int southU(int c) { return 45 - c; }
	private static int northU(int c) { return 24 + c; }
	private static int westU(int layer) { return 23 - layer; }
	private static int eastU(int layer) { return 34 + layer; }
	private static int downU(int c) { return 24 + c; }
	private static int upU(int c) { return 34 + c; }
	private static int capV(int layer) { return 1 - layer; }
	private static int sideV(int j) { return 2 + j; }

	private static boolean exists(boolean[] voxels, int c, int j, int l) {
		return c >= 0 && c < COLUMNS && j >= 0 && j < ROWS && l >= 0 && l < LAYERS && voxels[(l * ROWS + j) * COLUMNS + c];
	}

	private static float[] buildTemplate(WingMask mask, float side, int segments) {
		boolean[] voxels = new boolean[COLUMNS * ROWS * LAYERS];
		int[] ownU = new int[voxels.length];
		int[] ownV = new int[voxels.length];
		for (int l = 0; l < LAYERS; l++) {
			for (int j = 0; j < ROWS; j++) {
				for (int c = 0; c < COLUMNS; c++) {
					int i = (l * ROWS + j) * COLUMNS + c;
					int[][] candidates = {
							{l == 1 ? southU(c) : northU(c), sideV(j)},
							{c == 0 ? westU(l) : -1, sideV(j)},
							{c == COLUMNS - 1 ? eastU(l) : -1, sideV(j)},
							{j == 0 ? downU(c) : -1, capV(l)},
							{j == ROWS - 1 ? upU(c) : -1, capV(l)},
					};
					for (int[] t : candidates) {
						if (t[0] >= 0 && mask.opaque(t[0], t[1])) {
							voxels[i] = true;
							ownU[i] = t[0];
							ownV[i] = t[1];
							break;
						}
					}
				}
			}
		}

		float columnDir = side > 0.0F ? 1.0F : -1.0F;
		boolean mirrored = side < 0.0F;
		float[] out = new float[voxels.length * 6 * 4 * STRIDE];
		int[] cursor = {0};
		for (int l = 0; l < LAYERS; l++) {
			for (int j = 0; j < ROWS; j++) {
				for (int c = 0; c < COLUMNS; c++) {
					int i = (l * ROWS + j) * COLUMNS + c;
					if (!voxels[i]) {
						continue;
					}
					float x0 = xEdge(side, c), x1 = xEdge(side, c + 1);
					float y0 = ElytraShape.BOX_MIN_Y + j, y1 = y0 + 1.0F;
					float z0 = ElytraShape.BOX_MIN_Z + l, z1 = z0 + 1.0F;
					int u = ownU[i], v = ownV[i];
					if (!exists(voxels, c, j, l + 1)) {
						boolean painted = l == LAYERS - 1 && mask.opaque(southU(c), sideV(j));
						int fu = painted ? southU(c) : u;
						int fv = painted ? sideV(j) : v;
						face(out, cursor, segments, mirrored, 0, 0, 1, fu, fv, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
					}
					if (!exists(voxels, c, j, l - 1)) {
						boolean painted = l == 0 && mask.opaque(northU(c), sideV(j));
						int fu = painted ? northU(c) : u;
						int fv = painted ? sideV(j) : v;
						face(out, cursor, segments, mirrored, 0, 0, -1, fu, fv, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
					}
					if (!exists(voxels, c - 1, j, l)) {
						boolean painted = c == 0 && mask.opaque(westU(l), sideV(j));
						int fu = painted ? westU(l) : u;
						int fv = painted ? sideV(j) : v;
						face(out, cursor, segments, mirrored, -columnDir, 0, 0, fu, fv, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
					}
					if (!exists(voxels, c + 1, j, l)) {
						boolean painted = c == COLUMNS - 1 && mask.opaque(eastU(l), sideV(j));
						int fu = painted ? eastU(l) : u;
						int fv = painted ? sideV(j) : v;
						face(out, cursor, segments, mirrored, columnDir, 0, 0, fu, fv, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
					}
					if (!exists(voxels, c, j - 1, l)) {
						boolean painted = j == 0 && mask.opaque(downU(c), capV(l));
						int fu = painted ? downU(c) : u;
						int fv = painted ? capV(l) : v;
						face(out, cursor, segments, mirrored, 0, -1, 0, fu, fv, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
					}
					if (!exists(voxels, c, j + 1, l)) {
						boolean painted = j == ROWS - 1 && mask.opaque(upU(c), capV(l));
						int fu = painted ? upU(c) : u;
						int fv = painted ? capV(l) : v;
						face(out, cursor, segments, mirrored, 0, 1, 0, fu, fv, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
					}
				}
			}
		}
		return java.util.Arrays.copyOf(out, cursor[0]);
	}

	private static void face(float[] out, int[] cursor, int segments, boolean mirrored, float nx, float ny, float nz,
			int u, int v, float ax, float ay, float az, float bx, float by, float bz,
			float cx, float cy, float cz, float dx, float dy, float dz) {
		float ua = u + 0.5F - UV_INSET, ub = u + 0.5F + UV_INSET;
		float va = v + 0.5F - UV_INSET, vb = v + 0.5F + UV_INSET;
		vertex(out, cursor, segments, ax, ay, az, ua, va, nx, ny, nz);
		if (mirrored) {
			vertex(out, cursor, segments, dx, dy, dz, ua, vb, nx, ny, nz);
			vertex(out, cursor, segments, cx, cy, cz, ub, vb, nx, ny, nz);
			vertex(out, cursor, segments, bx, by, bz, ub, va, nx, ny, nz);
		} else {
			vertex(out, cursor, segments, bx, by, bz, ub, va, nx, ny, nz);
			vertex(out, cursor, segments, cx, cy, cz, ub, vb, nx, ny, nz);
			vertex(out, cursor, segments, dx, dy, dz, ua, vb, nx, ny, nz);
		}
	}

	private static void vertex(float[] out, int[] cursor, int segments, float x, float y, float z, float u, float v,
			float nx, float ny, float nz) {
		int o = cursor[0];
		out[o] = x;
		out[o + 1] = y;
		out[o + 2] = z;
		out[o + 3] = u / TEX_W;
		out[o + 4] = v / TEX_H;
		out[o + 5] = nx;
		out[o + 6] = ny;
		out[o + 7] = nz;
		out[o + 8] = (y - ElytraShape.SPINE_MIN_Y) / (ElytraShape.SPINE_MAX_Y - ElytraShape.SPINE_MIN_Y) * segments;
		cursor[0] = o + STRIDE;
	}
}
