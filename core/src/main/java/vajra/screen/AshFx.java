package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import vajra.enemy.Enemy;
import vajra.world.AshCloud;
import vajra.world.Smoke;
import vajra.world.World;

/** BHAIRAVA'S SMOKE, drawn: a real fluid (world/Smoke: stable fluids on a 96 x 96 grid) fed when the cloud is called, turning slowly, stirred by every hull and body that moves
 *  through it, thinning when the cloud ends. The grid's density becomes one soft texture over the hall. It lies on the floor (WorldRenderer draws it before the hulls and the rounds),
 *  so everything in it stays plainly visible; the grey mandala at its heart is Mandala.drawAsh. */
final class AshFx {
    private static final int N = 96;
    private final Smoke sim = new Smoke(N);
    private final Pixmap pm = new Pixmap(N, N, Pixmap.Format.RGBA8888);
    private final Texture tex;
    private boolean live, seeded;
    private float cloudT = -1f, lastClock;

    AshFx() {
        pm.setBlending(Pixmap.Blending.None);
        tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        tex.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
    }

    /** Inside an open SpriteBatch, in world space. */
    void draw(SpriteBatch batch, World w, float l, float b, float vw, float vh) {
        AshCloud c = w.ash;
        float dt = Math.max(0f, Math.min(0.05f, w.clock - lastClock));                 // the fluid lives in the world's time: it holds when the world is held
        lastClock = w.clock;
        if (c.active && (!seeded || c.t < cloudT - 0.01f)) {                     // a cloud has just been called: seed the fluid at its heart
            sim.reset(c.x, c.y, c.r);
            sim.puff(c.x, c.y, 0.30f * c.r, 3.4f, 170f);
            for (int i = 0; i < 7; i++) {
                float a = i * 0.8976f;
                sim.puff(c.x + (float) Math.cos(a) * 0.32f * c.r, c.y + (float) Math.sin(a) * 0.32f * c.r, 0.16f * c.r, 1.6f, 90f);
            }
            seeded = true;
            live = true;
        }
        cloudT = c.active ? c.t : -1f;
        if (!live) return;
        float side = sim.side();
        if (sim.originX() + side < l - 100 || sim.originX() > l + vw + 100 || sim.originY() + side < b - 100 || sim.originY() > b + vh + 100) {
            if (!c.active) live = false;
            return;
        }
        boolean ending = !c.active || c.dur - c.t < AshCloud.FALL;
        sim.step(dt, ending ? 0f : 0.35f, ending ? 1.1f : 0.06f);
        // everything that moves through it drags it along
        vajra.ship.Ship s = w.ship;
        sim.stir(s.x, s.y, s.vx, s.vy, 70f, 1f);
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy e = w.enemies.get(i);
            if (e.dead || e.depth > 0.05f) continue;
            sim.stir(e.x, e.y, e.vx, e.vy, Math.max(40f, e.drawSize() * 0.45f), e.boss ? 2.2f : 1f);
        }
        float tot = 0f;
        float k = Math.min(1f, c.active ? 1f : sim.total() / 30f);
        for (int j = 0; j < N; j++)
            for (int i = 0; i < N; i++) {
                float d = sim.density(i, j);
                tot += d;
                float a = Math.max(0f, Math.min(0.86f, d * 0.62f));
                float mv = Math.min(1f, sim.speedAt(i, j) / 160f);
                float shade = 0.40f + 0.20f * Math.min(1f, d) + 0.10f * mv;                // dense smoke is paler, and where it moves quickest it catches the light
                int r = (int) (255 * Math.min(1f, shade + 0.035f)), g = (int) (255 * shade), bl = (int) (255 * (shade - 0.01f));
                pm.drawPixel(i, N - 1 - j, (r << 24) | (g << 16) | (bl << 8) | (int) (255 * a * k));
            }
        tex.draw(pm, 0, 0);
        if (!c.active && tot < 1.5f) live = false;
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(tex, sim.originX(), sim.originY(), side, side);
    }

    void dispose() {
        tex.dispose();
        pm.dispose();
    }
}
