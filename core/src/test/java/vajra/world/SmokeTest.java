package vajra.world;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** The smoke is a fluid: it keeps its mass while it swirls, spreads from where it was put, is stirred by a body moving through it, and thins when it drains. */
class SmokeTest {
    private static Smoke fresh() {
        Smoke s = new Smoke(64);
        s.reset(0f, 0f, 800f);
        return s;
    }

    private static float spread(Smoke s) {
        float tot = 0f, m = 0f;
        for (int j = 0; j < s.n; j++) for (int i = 0; i < s.n; i++) { float d = s.density(i, j); float r = (float) Math.hypot(i - s.n / 2f, j - s.n / 2f); tot += d; m += d * r * r; }
        return tot > 0f ? m / tot : 0f;
    }

    @Test
    void aPuffSpreadsAndKeepsMostOfItsMassWhileItSwirls() {
        Smoke s = fresh();
        s.puff(0f, 0f, 120f, 3f, 80f);
        float m0 = s.total(), sp0 = spread(s);
        for (int i = 0; i < 180; i++) s.step(1 / 60f, 0f, 0f);
        assertTrue(s.total() > m0 * 0.5f, "it does not vanish: " + m0 + " -> " + s.total());
        assertTrue(spread(s) > sp0 * 1.3f, "it spreads from the heart: " + sp0 + " -> " + spread(s));
        for (int j = 0; j < s.n; j++) for (int i = 0; i < s.n; i++) assertTrue(Float.isFinite(s.density(i, j)) && s.density(i, j) >= -0.05f && s.density(i, j) < 5f);
    }

    @Test
    void aBodyMovingThroughItCutsAChannel() {
        Smoke a = fresh(), b = fresh();
        a.puff(0f, 0f, 400f, 1.5f, 0f);
        b.puff(0f, 0f, 400f, 1.5f, 0f);
        for (int i = 0; i < 90; i++) {
            a.step(1 / 60f, 0f, 0f);
            b.stir(-200f + i * 4f, 0f, 400f, 0f, 90f, 1f);
            b.step(1 / 60f, 0f, 0f);
        }
        float diff = 0f;
        for (int j = 0; j < a.n; j++) for (int i = 0; i < a.n; i++) diff += Math.abs(a.density(i, j) - b.density(i, j));
        assertTrue(diff > 1f, "the smoke that was stirred is not the smoke that was left alone: " + diff);
    }

    @Test
    void itThinsWhenItDrainsAndIsFedWhenItStands() {
        Smoke s = fresh();
        s.puff(0f, 0f, 300f, 2f, 0f);
        for (int i = 0; i < 120; i++) s.step(1 / 60f, 0f, 1.2f);
        assertTrue(s.total() < 0.35f * 2f * 40f, "it thins away: " + s.total());
        Smoke f = fresh();
        for (int i = 0; i < 240; i++) f.step(1 / 60f, 1f, 0f);
        assertTrue(f.total() > 1f, "a cloud that is fed fills: " + f.total());
    }
}
