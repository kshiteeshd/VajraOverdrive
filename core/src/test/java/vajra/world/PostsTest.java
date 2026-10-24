package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;

/** HOLD YOUR POST: an enemy belongs to the band it was placed in and the place it was put. It wakes when the hull comes near, fights from its post
 *  or its formation, and when the hull goes it goes back; it never follows the pilot out of its band, never crosses a gate, and never enters Bhu
 *  Loka (the open land below Atala's grand gate, y < 1600) - where the pilot starts and is safe. This test is also the analysis: it prints how far
 *  the garrison drifts when a hull sits in each band. */
class PostsTest {
    private static vajra.ship.ShipControls idle() {
        return new vajra.ship.ShipControls() {
            public float thrustY() { return 0; }
            public float thrustX() { return 0; }
            public float rotate() { return 0; }
            public boolean fire() { return false; }
            public boolean boost() { return false; }
            public boolean shield() { return false; }
            public boolean mouseAim() { return false; }
            public float aimX() { return 0; }
            public float aimY() { return 0; }
        };
    }

    /** Home region of a spawn point. */
    private static int regionOf(World w, float y) {
        for (int i = 0; i < w.regionStart.length; i++) if (y >= w.regionStart[i] && y < w.regionEnd[i]) return i;
        return w.regionStart.length - 1;
    }

    /** Park a hull at (x, y) for `seconds`, with the garrison alive and the hull immortal; return {max drift from spawn, units beyond 450 px of spawn,
     *  units in Bhu Loka, units outside their home region, awake units}. */
    private static String worst = "";

    private static float[] sit(World w, float x, float y, float seconds) {
        w.ship.x = x;
        w.ship.y = y;
        w.ship.hp = w.ship.maxHp() * 1000f;
        float maxDrift = 0f;
        worst = "";
        int beyond = 0, bhu = 0, strayed = 0, awake = 0;
        for (int i = 0; i < Math.round(seconds * 60f); i++) {
            w.ship.x = x;
            w.ship.y = y;
            w.ship.hp = w.ship.maxHp() * 1000f;
            w.update(1 / 60f, idle());
            if (i % 30 != 29) continue;
            beyond = bhu = strayed = awake = 0;
            for (Enemy e : w.enemies) {
                if (e.dead || e.voice || e.boss) continue;
                if (e.active) awake++;
                float d = (float) Math.hypot(e.x - e.spawnX, e.y - e.spawnY);
                if (d > maxDrift) {
                    maxDrift = d;
                    worst = e.kind + (e.vitala ? "/V" : "/A") + (e.hunt ? "/hunt" : "") + (e.routed ? "/routed" : "") + (e.commander != null ? "/follower" : "") + (e.sentry ? "/sentry" : "");
                }
                if (d > 450f) beyond++;
                if (e.y < 1600f) bhu++;
                if (regionOf(w, e.y) != regionOf(w, e.spawnY)) strayed++;
            }
        }
        return new float[] {maxDrift, beyond, bhu, strayed, awake};
    }

    @Test
    void theGarrisonHoldsItsPostsAndNeverEntersBhuLoka() {
        float[][] probes = {{2600, 1800}, {2600, 3000}, {1400, 5200}, {3800, 6600}, {2600, 9000}, {2600, 13000}, {2600, 17200},
                {Hataki.CX, Hataki.START[1] + 1500f}, {Hataki.CX, Hataki.START[2] + 2500f}, {Hataki.CX, Hataki.START[3] + 2500f}, {Hataki.CX, Hataki.START[4] + 1800f}};
        StringBuilder sb = new StringBuilder("Posts: ");
        float worstDrift = 0f;
        int bhuTotal = 0, strayTotal = 0;
        for (float[] p : probes) {
            World w = new World(0, World.CAMPAIGN);
            float y = p[1];
            float x = p[0];
            if (Hataki.in(y)) x = Hataki.centre(y);
            int n0 = w.enemies.size();
            float[] r = sit(w, x, y, 30f);
            sb.append("[lost ").append(n0 - w.enemies.size()).append("] ");
            sb.append(String.format("y=%d drift %d (%s), beyond450 %d, inBhu %d, strayed %d, awake %d | ", (int) y, (int) r[0], worst, (int) r[1], (int) r[2], (int) r[3], (int) r[4]));
            worstDrift = Math.max(worstDrift, r[0]);
            bhuTotal += r[2];
            strayTotal += r[3];
        }
        System.out.println(sb);
        assertEquals(0, bhuTotal, "no enemy ever enters Bhu Loka");
        assertEquals(0, strayTotal, "no enemy leaves the band it was placed in");
        assertTrue(worstDrift < 900f, "the garrison holds its ground: the worst drift is " + worstDrift);
    }

    @Test
    void whenTheHullLeavesEveryoneWhoWokeFliesHomeAndSleepsInItsFormation() {
        World w = new World(0, World.CAMPAIGN);
        sit(w, 2600f, 9000f, 15f);                                   // wake the court of Svapna Mandala
        int awake = 0;
        for (Enemy e : w.enemies) if (e.active && !e.voice && !e.boss) awake++;
        assertTrue(awake >= 8, "a court woke: " + awake);
        sit(w, 2600f, 1800f, 30f);                                   // and the hull is gone, a long way off
        int away = 0, stillAwake = 0;
        for (Enemy e : w.enemies) {
            if (e.dead || e.voice || e.boss || Math.abs(e.spawnY - 9000f) > 1300f) continue;
            if (Math.hypot(e.x - e.spawnX, e.y - e.spawnY) > 90f) {   // (sleepers a bar apart are eased off one another a few px)
                away++;
                if (false) System.out.println("Straggler: " + e.kind + " at " + (int) e.x + "," + (int) e.y + " spawn " + (int) e.spawnX + "," + (int) e.spawnY + " active " + e.active + " standDown " + e.standDown + " routed " + e.routed + " hunt " + e.hunt + " dive " + e.diveState + " depth " + e.depth + " parley " + e.parley + " sentry " + e.sentry);
            }
            if (e.active) stillAwake++;
        }
        assertEquals(0, away, "every unit is back on the post it was put on");
        assertEquals(0, stillAwake, "and asleep there");
    }

    /** PLACEMENT AUDIT: where the garrison was put. Prints the roster per region and counts the faults: spawns in rock, in the wrong band, on top of one
     *  another, or crowding a gate. */
    @Test
    void thePlacementOfTheGarrisonIsClean() {
        World w = new World(0, World.CAMPAIGN);
        int[] per = new int[w.regionStart.length];
        int inRock = 0, overlap = 0, offBand = 0, gateCrowd = 0;
        for (int i = 0; i < w.enemies.size(); i++) {
            Enemy a = w.enemies.get(i);
            if (a.boss || a.voice) continue;
            int r = regionOf(w, a.spawnY);
            per[r]++;
            if (w.blockedPx(a.spawnX, a.spawnY)) inRock++;
            if (a.spawnY < a.bandMin || a.spawnY > a.bandMax) offBand++;
            for (int g = 1; g < w.regionStart.length; g++)
                if (Math.abs(a.spawnY - w.regionStart[g]) < 120f && Math.abs(a.spawnX - 2600f) < 200f && !a.guard && !a.parley && !a.siteGuard) gateCrowd++;
            for (int j = i + 1; j < w.enemies.size(); j++) {
                Enemy b = w.enemies.get(j);
                if (b.boss || b.voice || Math.abs(a.spawnY - b.spawnY) > 60f) continue;
                float need = (a.w + b.w) * 0.5f * 0.8f;
                if (Math.hypot(a.spawnX - b.spawnX, a.spawnY - b.spawnY) < need) overlap++;
            }
        }
        StringBuilder sb = new StringBuilder("Placement: per region ");
        for (int r = 0; r < per.length; r++) sb.append(per[r]).append(r + 1 < per.length ? "/" : "");
        sb.append(" | in rock ").append(inRock).append(", off band ").append(offBand).append(", overlapping ").append(overlap).append(", crowding a gate ").append(gateCrowd);
        System.out.println(sb);
        assertEquals(0, per[0], "nobody is placed in Bhu Loka");
        assertEquals(0, inRock, "nobody is placed in rock");
        assertEquals(0, offBand, "everybody is inside the band it belongs to");
        assertEquals(0, overlap, "nobody is placed on top of anybody");
        assertEquals(0, gateCrowd, "the gates are kept clear");
    }
}
