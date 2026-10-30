package vajra.world;

/** SMOKE AS A SIMULATION (no Gdx in it): a 2-D stable-fluids grid (Stam) of density and velocity over a square that holds Bhairava's cloud. Velocity is advected by itself, made
 *  divergence-free by a pressure projection (so the smoke swirls and rolls instead of drifting apart), given back its curls by vorticity confinement, and fed, stirred and drained from
 *  outside: a puff at the heart when the cloud is called, a slow turning of the whole bank, small drifting eddies, the wake of every hull and body that moves through it.
 *  It is purely visual (the slowing of the hulls is {@link AshCloud}'s rule), and it is deterministic for a given sequence of calls. */
public final class Smoke {
    public final int n;                       // cells along a side
    private final int s;                      // n + 2: a border cell round the grid
    private final float[] u, v, u0, v0, d, d0, curl, p, div;
    private float cell = 16f, ox, oy, radius = 800f;
    private float time;
    private int seed;

    public Smoke(int n) {
        this.n = n;
        this.s = n + 2;
        int sz = s * s;
        u = new float[sz];
        v = new float[sz];
        u0 = new float[sz];
        v0 = new float[sz];
        d = new float[sz];
        d0 = new float[sz];
        curl = new float[sz];
        p = new float[sz];
        div = new float[sz];
    }

    private int ix(int i, int j) { return i + s * j; }

    /** Clear the grid and cover the square of half-side `radius` about (cx, cy). */
    public void reset(float cx, float cy, float radius) {
        java.util.Arrays.fill(u, 0f);
        java.util.Arrays.fill(v, 0f);
        java.util.Arrays.fill(d, 0f);
        this.radius = radius * 1.05f;
        this.cell = 2f * this.radius / n;
        this.ox = cx - this.radius;
        this.oy = cy - this.radius;
        this.time = 0f;
        this.seed = (int) (cx * 31 + cy * 17);
    }

    public float cell() { return cell; }
    public float originX() { return ox; }
    public float originY() { return oy; }
    public float side() { return 2f * radius; }

    /** The smoke's density at grid cell (i, j), 0..n-1 (not clamped). */
    public float density(int i, int j) { return d[ix(i + 1, j + 1)]; }

    public float speedAt(int i, int j) { int k = ix(i + 1, j + 1); return (float) Math.hypot(u[k], v[k]) * cell; }

    public float total() {
        float t = 0f;
        for (int j = 1; j <= n; j++) for (int i = 1; i <= n; i++) t += d[ix(i, j)];
        return t;
    }

    private int cellX(float wx) { return (int) Math.floor((wx - ox) / cell) + 1; }

    private int cellY(float wy) { return (int) Math.floor((wy - oy) / cell) + 1; }

    /** Put `amount` of smoke in a soft disc at a world point, with an outward push of `push` px/s. */
    public void puff(float wx, float wy, float rpx, float amount, float push) {
        int cx = cellX(wx), cy = cellY(wy), rc = Math.max(1, (int) Math.ceil(rpx / cell));
        for (int j = cy - rc; j <= cy + rc; j++)
            for (int i = cx - rc; i <= cx + rc; i++) {
                if (i < 1 || j < 1 || i > n || j > n) continue;
                float dx = i - cx, dy = j - cy, r = (float) Math.sqrt(dx * dx + dy * dy) / rc;
                if (r >= 1f) continue;
                float w = (1f - r) * (1f - r) * (3f - 2f * (1f - r));
                d[ix(i, j)] += amount * w;
                if (push != 0f && r > 0.01f) {
                    u[ix(i, j)] += dx / (r * rc) * push / cell * w;
                    v[ix(i, j)] += dy / (r * rc) * push / cell * w;
                }
            }
    }

    /** A body moving through the smoke drags it with it: velocity (px/s) added in a soft disc, with a little of the smoke scattered. */
    public void stir(float wx, float wy, float vx, float vy, float rpx, float strength) {
        int cx = cellX(wx), cy = cellY(wy), rc = Math.max(1, (int) Math.ceil(rpx / cell));
        for (int j = cy - rc; j <= cy + rc; j++)
            for (int i = cx - rc; i <= cx + rc; i++) {
                if (i < 1 || j < 1 || i > n || j > n) continue;
                float dx = i - cx, dy = j - cy, r = (float) Math.sqrt(dx * dx + dy * dy) / rc;
                if (r >= 1f) continue;
                float w = (1f - r) * (1f - r) * strength;
                u[ix(i, j)] += vx / cell * w * 0.1f;
                v[ix(i, j)] += vy / cell * w * 0.1f;
            }
    }

    /** Advance by dt seconds. `feed` (0..1) is how hard the cloud is being fed (0 once it is ending), `drain` how fast it thins (per second). */
    public void step(float dt, float feed, float drain) {
        dt = Math.min(dt, 1f / 30f);
        time += dt;
        // the bank turns slowly round its heart, and small eddies drift through it, so it is never still
        float cx = n * 0.5f + 1f, cy = n * 0.5f + 1f;
        for (int j = 1; j <= n; j++)
            for (int i = 1; i <= n; i++) {
                float dx = i - cx, dy = j - cy, r = (float) Math.sqrt(dx * dx + dy * dy) / (n * 0.5f);
                if (r > 1f) continue;
                float turn = 0.05f * (1f - r) * (1f - r) * n * 0.5f;                       // cells/s of swirl, strongest between the heart and the rim
                float wob = (float) Math.sin(i * 0.19f + time * 0.45f) * (float) Math.cos(j * 0.23f - time * 0.37f);
                int k = ix(i, j);
                u[k] += (-dy / Math.max(1f, r * n * 0.5f) * turn + 0.4f * wob) * dt * 6f;
                v[k] += (dx / Math.max(1f, r * n * 0.5f) * turn - 0.4f * (float) Math.cos(i * 0.21f + time * 0.4f) * (float) Math.sin(j * 0.17f + time * 0.5f)) * dt * 6f;
            }
        if (feed > 0.01f) {                                                                // small drifting puffs in the bank keep it fed while the cloud stands
            seed = seed * 1103515245 + 12345;
            float a = ((seed >>> 8) & 0xFFFF) / 65535f * 6.2831853f, r = (0.25f + 0.55f * (((seed >>> 4) & 0xFF) / 255f)) * radius * 0.8f;
            puff(ox + radius + (float) Math.cos(a) * r, oy + radius + (float) Math.sin(a) * r, 70f, 0.5f * feed * dt * 60f * 0.2f, 0f);
        }
        confine();
        vorticity(dt);
        advectVelocity(dt);
        project();
        advectDensity(dt);
        float keep = (float) Math.exp(-drain * dt);
        for (int k = 0; k < d.length; k++) d[k] = Math.min(2.5f, d[k] * keep);
        confine();
    }

    /** The walls of the hall: smoke and motion die away toward the edge of the square, so the cloud holds its place. */
    private void confine() {
        float cxx = n * 0.5f + 1f;
        for (int j = 1; j <= n; j++)
            for (int i = 1; i <= n; i++) {
                float dx = i - cxx, dy = j - cxx, r = (float) Math.sqrt(dx * dx + dy * dy) / (n * 0.5f);
                if (r < 0.80f) continue;
                float m = r >= 1f ? 0f : (1f - r) / 0.2f;
                m = m * m * (3f - 2f * m);
                int k = ix(i, j);
                d[k] *= 0.9f + 0.1f * m;
                u[k] *= 0.9f + 0.1f * m;
                v[k] *= 0.9f + 0.1f * m;
            }
    }

    private void vorticity(float dt) {
        for (int j = 2; j < n; j++)
            for (int i = 2; i < n; i++) curl[ix(i, j)] = (v[ix(i + 1, j)] - v[ix(i - 1, j)] - u[ix(i, j + 1)] + u[ix(i, j - 1)]) * 0.5f;
        float eps = 5.5f;
        for (int j = 3; j < n - 1; j++)
            for (int i = 3; i < n - 1; i++) {
                float dwx = (Math.abs(curl[ix(i + 1, j)]) - Math.abs(curl[ix(i - 1, j)])) * 0.5f, dwy = (Math.abs(curl[ix(i, j + 1)]) - Math.abs(curl[ix(i, j - 1)])) * 0.5f;
                float len = (float) Math.sqrt(dwx * dwx + dwy * dwy) + 1e-5f;
                float nx = dwx / len, ny = dwy / len, w = curl[ix(i, j)];
                u[ix(i, j)] += eps * ny * w * dt;
                v[ix(i, j)] -= eps * nx * w * dt;
            }
    }

    private void advectVelocity(float dt) {
        System.arraycopy(u, 0, u0, 0, u.length);
        System.arraycopy(v, 0, v0, 0, v.length);
        advect(u, u0, u0, v0, dt);
        advect(v, v0, u0, v0, dt);
    }

    private void advectDensity(float dt) {
        System.arraycopy(d, 0, d0, 0, d.length);
        advect(d, d0, u, v, dt);
    }

    private void advect(float[] out, float[] in, float[] uu, float[] vv, float dt) {
        for (int j = 1; j <= n; j++)
            for (int i = 1; i <= n; i++) {
                float x = i - dt * uu[ix(i, j)], y = j - dt * vv[ix(i, j)];
                x = Math.max(0.5f, Math.min(n + 0.5f, x));
                y = Math.max(0.5f, Math.min(n + 0.5f, y));
                int i0 = (int) x, j0 = (int) y;
                float sx = x - i0, sy = y - j0;
                out[ix(i, j)] = (1f - sx) * ((1f - sy) * in[ix(i0, j0)] + sy * in[ix(i0, j0 + 1)]) + sx * ((1f - sy) * in[ix(i0 + 1, j0)] + sy * in[ix(i0 + 1, j0 + 1)]);
            }
    }

    private void project() {
        for (int j = 1; j <= n; j++)
            for (int i = 1; i <= n; i++) {
                div[ix(i, j)] = -0.5f * (u[ix(i + 1, j)] - u[ix(i - 1, j)] + v[ix(i, j + 1)] - v[ix(i, j - 1)]);
                p[ix(i, j)] = 0f;
            }
        for (int it = 0; it < 18; it++)
            for (int j = 1; j <= n; j++)
                for (int i = 1; i <= n; i++) p[ix(i, j)] = (div[ix(i, j)] + p[ix(i - 1, j)] + p[ix(i + 1, j)] + p[ix(i, j - 1)] + p[ix(i, j + 1)]) * 0.25f;
        for (int j = 1; j <= n; j++)
            for (int i = 1; i <= n; i++) {
                u[ix(i, j)] -= 0.5f * (p[ix(i + 1, j)] - p[ix(i - 1, j)]);
                v[ix(i, j)] -= 0.5f * (p[ix(i, j + 1)] - p[ix(i, j - 1)]);
            }
    }
}
