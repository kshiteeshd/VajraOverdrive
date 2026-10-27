package vajra.world;

/** BHAIRAVA'S ASH CLOUD (no Gdx in it): a bank of grey smoke that fills the hall round the altar. It rises over a second and a half, stands, and thins away over two seconds;
 *  while a hull is inside it, its movement is slowed to {@link #MUD} and it cannot boost. It hides nothing: the smoke lies on the floor, under every hull and round, so Bhairava
 *  and his court can be seen through it as clearly as ever (World draws a grey mandala at its heart; screen/AshFx paints both). */
public final class AshCloud {
    public static final float RISE = 1.6f, FALL = 2.2f, MUD = 0.55f;

    public boolean active;
    public float x, y, r, t, dur;

    public void start(float cx, float cy, float radius, float seconds) {
        active = true;
        x = cx;
        y = cy;
        r = radius;
        t = 0f;
        dur = seconds;
    }

    public void step(float dt) {
        if (!active) return;
        t += dt;
        if (t >= dur) active = false;
    }

    /** How present the cloud is, 0..1 (rising, standing, thinning). */
    public float k() {
        if (!active) return 0f;
        float in = Math.min(1f, t / RISE), out = Math.min(1f, (dur - t) / FALL), v = Math.max(0f, Math.min(in, out));
        return v * v * (3f - 2f * v);
    }

    /** The cloud's radius now: it spreads out from the heart as it rises. */
    public float radiusNow() { return r * (0.35f + 0.65f * Math.min(1f, t / RISE)); }

    /** A hull at (px, py) is in the smoke (and slowed by it) once it has thickened enough to matter. */
    public boolean holds(float px, float py) {
        if (!active || k() < 0.25f) return false;
        float dx = px - x, dy = py - y, rr = radiusNow();
        return dx * dx + dy * dy <= rr * rr;
    }
}
