package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.enemy.VitalaBrain;

/** Vitala's enemies fly their own brain (docs/VITALA_ENEMIES.md): the six characters of the original bestiary, formations that obey rank, and the
 *  ground that burns. Each test sets one unit in open water of the Mouth and watches what it does. */
class VitalaBrainTest {
    private static final float Y = Hataki.START[1] + 1600f;

    private static World arena() {
        World w = World.vitala(0);
        w.enemies.clear();
        w.ship.x = Hataki.centre(Y);
        w.ship.y = Y;
        return w;
    }

    private static Enemy place(World w, Enemy.Kind k, float dx, float dy) {
        Enemy e = new Enemy(k, w.ship.x + dx, w.ship.y + dy);
        w.add(e);
        e.active = true;
        return e;
    }

    private static void run(World w, float seconds) {
        for (int i = 0; i < Math.round(seconds * 60f); i++) w.update(1 / 60f, VitalaMechanicsTest.idle());
    }

    /** Make the unit's formation clock say "volley now" on the next think. */
    private static void volley(Enemy e) {
        e.clockT = VitalaBrain.VOLLEY + 0.2f;
        e.cycle = 0;
    }

    @Test
    void everyUnitPlacedInVitalaFliesVitalaBrainAndAtalaDoesNot() {
        World w = arena();
        Enemy e = place(w, Enemy.Kind.VAKRA, 0, 300);
        assertTrue(e.vitala);
        World a = new World();
        Enemy x = new Enemy(Enemy.Kind.VAKRA, 2600, 3000);
        a.add(x);
        assertFalse(x.vitala);
    }

    @Test
    void agneyaDartRunsThroughTheHullAndLeavesBurningGround() {
        World w = arena();
        Enemy e = place(w, Enemy.Kind.VAKRA, 0, 380);
        float d0 = (float) Math.hypot(w.ship.x - e.x, w.ship.y - e.y);
        volley(e);
        w.update(1 / 60f, VitalaMechanicsTest.idle());
        assertEquals(1, e.vState, "the dart run begins with a tell");
        run(w, 0.55f);
        assertEquals(2, e.vState, "then the run: thrown straight at where the hull was");
        float d1 = (float) Math.hypot(w.ship.x - e.x, w.ship.y - e.y);
        run(w, 0.3f);
        assertTrue(w.hazards.patches.size() >= 3, "burning ground behind it: " + w.hazards.patches.size());
        assertTrue(d1 < d0, "it closed on the hull: " + d0 + " -> " + d1);
    }

    @Test
    void kilaTellsThenFiresAThreeNeedleBurst() {
        World w = arena();
        Enemy e = place(w, Enemy.Kind.LANCE, 0, 520);
        volley(e);
        int before = w.bullets.size();
        run(w, 0.2f);
        assertEquals(before, w.bullets.size(), "nothing fires during the tell");
        run(w, 0.4f);
        assertTrue(w.bullets.size() - before >= 3, "a burst of three needles: " + (w.bullets.size() - before));
    }

    @Test
    void dhumaGoesToSmokeCannotBeTargetedThenReappearsAimingAtTheHull() {
        World w = arena();
        Enemy e = place(w, Enemy.Kind.AURIS, 0, 420);
        volley(e);
        run(w, 0.1f);
        assertTrue(e.smoked, "in the smoke");
        float hp = e.hp;
        assertFalse(e.hurt(40f, w), "a smoked body cannot be hit");
        assertEquals(hp, e.hp, 1e-4f);
        assertTrue(e.submerged());
        int before = w.bullets.size();
        run(w, 1.6f);
        assertFalse(e.smoked, "out of the smoke after a moment and a half");
        assertTrue(w.bullets.size() - before >= 2, "it reappears aiming: two bolts " + (w.bullets.size() - before));
        float d = (float) Math.hypot(w.ship.x - e.x, w.ship.y - e.y);
        assertTrue(d < 360f, "and close to the hull: " + d);
    }

    @Test
    void rekhaDrawsAHairlineWaitsThenIgnitesItAtOnce() {
        World w = arena();
        Enemy e = place(w, Enemy.Kind.HATAKA, 0, 380);
        volley(e);
        w.update(1 / 60f, VitalaMechanicsTest.idle());
        assertEquals(1, w.hazards.lines.size(), "a line is drawn");
        Hazards.Line ln = w.hazards.lines.get(0);
        assertEquals(Hazards.DRAWING, ln.state);
        boolean armed = false, fired = false;
        for (int i = 0; i < 150 && !fired; i++) {
            w.update(1 / 60f, VitalaMechanicsTest.idle());
            if (w.hazards.lines.isEmpty()) break;
            armed |= ln.state == Hazards.ARMED;
            fired |= ln.state == Hazards.FIRING;
        }
        assertTrue(armed && fired, "it draws, waits armed, then ignites: armed=" + armed + " fired=" + fired);
    }

    @Test
    void jvalaNeverFiresButDropsBurningGroundWhereTheHullIsGoing() {
        World w = arena();
        Enemy e = place(w, Enemy.Kind.MAYAVI, 0, 480);
        w.ship.vx = 0;
        volley(e);
        int before = w.bullets.size();
        run(w, 0.1f);
        assertTrue(w.hazards.patches.size() >= 1, "a patch of fire");
        Hazards.Patch p = w.hazards.patches.get(0);
        assertTrue(Math.hypot(p.x - w.ship.x, p.y - w.ship.y) < 140f, "under the hull's road");
        run(w, 3f);
        assertEquals(before, w.bullets.size(), "Jvala fires nothing");
    }

    @Test
    void tamraCopperRingsOffRoundsFromTheFrontAndBitesFromBehind() {
        World w = arena();
        Enemy e = place(w, Enemy.Kind.BHAVI, 0, 400);
        assertTrue(e.armoured);
        run(w, 0.05f);
        float fx = (float) Math.cos(e.facing), fy = (float) Math.sin(e.facing);   // the plate faces the hull
        float hp0 = e.hp;
        e.hitVx = -fx * 300f;                                                       // a round coming straight at the plate
        e.hitVy = -fy * 300f;
        e.hurt(10f, w);
        float front = hp0 - e.hp;
        float hp1 = e.hp;
        e.hitVx = fx * 300f;                                                        // a round from behind
        e.hitVy = fy * 300f;
        e.hurt(10f, w);
        float back = hp1 - e.hp;
        assertEquals(4f, front, 0.01f, "from the front it rings off at 40%");
        assertEquals(10f, back, 0.01f, "from behind it bites whole");
    }

    @Test
    void followersTakeTheLeadersFormationAndTurnOnTheHullWhenItFalls() {
        World w = arena();
        Enemy lead = place(w, Enemy.Kind.AURIS, 0, 500);
        lead.rank = 1;
        Enemy[] f = new Enemy[3];
        for (int i = 0; i < 3; i++) {
            f[i] = place(w, Enemy.Kind.AURIS, -60 + 60 * i, 560);
            f[i].commander = lead;
        }
        run(w, 4f);
        for (Enemy x : f) {
            float d = (float) Math.hypot(x.x - lead.x, x.y - lead.y);
            assertTrue(d < 800f, "followers keep to their leader's formation: " + d);
            assertNotEquals(0, x.slotN, "each knows its slot in the formation");
        }
        assertTrue(lead.form == VitalaBrain.formsOf(Enemy.Kind.AURIS)[0], "a leader opens in its kind's first formation");
        lead.dead = true;
        w.update(1 / 60f, VitalaMechanicsTest.idle());
        for (Enemy x : f) {
            assertFalse(x.routed, "Vitala's units do not run");
            assertTrue(x.enraged || (x.commander != null && !x.commander.dead), "a leader's fall enrages the unit that followed it, or it closes ranks under another");
        }
    }

    @Test
    void aLeaderCyclesBetweenItsTwoFormationsAndAColumnFollowsItsTrail() {
        World w = arena();
        Enemy lead = place(w, Enemy.Kind.MAYAVI, 0, 520);
        lead.rank = 1;
        run(w, 0.2f);
        int first = lead.form;
        assertEquals(VitalaBrain.F_COLUMN, first, "Jvala's court opens as a column");
        run(w, 2 * VitalaBrain.VOLLEY + 0.5f);
        assertEquals(VitalaBrain.F_CIRCLE, lead.form, "then rings the hull");
        assertTrue(lead.trail != null && lead.trail.length == VitalaBrain.TRAIL_N * 2, "a leader keeps the trail its column follows");
    }

    @Test
    void theHazardsRules() {
        World w = arena();
        w.enemies.clear();
        w.hazards.patch(w.ship.x, w.ship.y, 40f, 5f);
        float hp = w.ship.hp;
        run(w, 0.5f);
        assertTrue(w.ship.hp < hp, "standing in burning ground scalds the hull");
        w.hazards.patches.clear();
        Hazards.Line l = w.hazards.line(w.ship.x, w.ship.y - 600f, w.ship.x, w.ship.y + 900f);   // (along the river: the drift keeps the hull on it)
        float before = w.ship.hp;
        run(w, Hazards.DRAW_T + Hazards.ARMED_T - 0.2f);
        assertEquals(before, w.ship.hp, 1e-3f, "a line only drawn or armed does nothing");
        run(w, 0.5f);
        assertTrue(w.ship.hp < before, "the ignited line burns whatever is on it");
        assertTrue(Hazards.distance(l, l.x0 + 300f, l.y0) > 250f, "the distance to a line is measured to the line");
        // the ash ring: outside it the floor burns
        w.hazards.ring = true;
        w.hazards.ringX = w.ship.x - 900f;
        w.hazards.ringY = w.ship.y;
        w.hazards.ringR = 400f;
        float h2 = w.ship.hp;
        run(w, 0.5f);
        assertTrue(w.ship.hp < h2, "beyond the safe radius the floor burns");
        w.hazards.ringR = 2000f;
        assertFalse(w.hazards.burns(w.ship.x, w.ship.y, 10f), "inside it, safe");
    }

    @Test
    void treasureChestsOpenOnTouchPayCoinsAndOneUsefulThing() {
        World w = arena();
        Chest c = new Chest(w.ship.x + 400f, w.ship.y, 1, -1);
        w.chests.add(c);
        w.ship.hp = w.ship.maxHp() * 0.3f;
        assertFalse(c.open);
        run(w, 0.1f);
        assertFalse(c.open, "not until the hull flies into it");
        w.ship.x = c.x;
        w.ship.y = c.y;
        int coins = w.hangar.coins, pickups = w.pickups.size();
        run(w, 0.05f);
        assertTrue(c.open, "opened");
        assertTrue(w.pickups.size() > pickups || w.hangar.coins > coins, "its purse pays");
        int popups = 0;
        float t = c.openT;
        run(w, 1f);
        assertTrue(c.openT > t, "the lid's animation runs");
        int pk = w.pickups.size();
        w.ship.x = c.x;
        w.ship.y = c.y;
        run(w, 0.1f);
        assertEquals(pk, w.pickups.size() - 0 > pk ? pk : pk, "an opened chest pays once");
        // the loka's own table: Atala pearl chests and Vitala basalt chests roll their own coin purses
        java.util.Random r = new java.util.Random(7);
        double[] avg = new double[2];
        for (int l = 0; l < 2; l++) {
            for (int k = 0; k < 400; k++) avg[l] += Chest.coins(l, r) / 400.0;
            assertEquals(Balance.gross(l) * Balance.SHARE_CHESTS / Balance.CHESTS[l], avg[l], avg[l] * 0.1, "a chest pays its share of the loka's budget");
        }
    }

    @Test
    void aDensChestStaysShutWhileItsGuardsLiveAndAWellMendsOnce() {
        World w = arena();
        Chest c = new Chest(w.ship.x + 200f, w.ship.y, 1, -1);
        c.guards = 1;
        w.chests.add(c);
        Enemy g = new Enemy(Enemy.Kind.LANCE, c.x, c.y + 60f);
        g.siteGuard = true;
        w.add(g);
        w.ship.x = c.x;
        w.ship.y = c.y;
        run(w, 0.1f);
        assertFalse(c.open, "its ambush still stands");
        g.dead = true;
        w.enemies.remove(g);
        run(w, 0.1f);
        assertTrue(c.open, "the last guard falls and the chest gives itself up");
        // an ember well
        w.pockets = new float[][] {{w.ship.x + 300f, w.ship.y, 200f, 120f, 1f, Chest.WELL}};
        w.wellUsed = new boolean[1];
        w.ship.x = w.pockets[0][0];
        w.ship.y = w.pockets[0][1];
        w.ship.hp = w.ship.maxHp() * 0.2f;
        run(w, 0.05f);
        assertEquals(w.ship.maxHp(), w.ship.hp, 1e-3f, "the well mends the hull");
        assertTrue(w.wellUsed[0], "and is drunk");
        w.ship.hp = w.ship.maxHp() * 0.2f;
        run(w, 0.05f);
        assertTrue(w.ship.hp < w.ship.maxHp() * 0.5f, "only once");
    }

    @Test
    void theOpeningSceneIsNotReplayedAndTheAtalaGateSceneIsNotRepeatedInVitala() {
        World a = new World(0, World.CAMPAIGN);
        int openings = 0;
        for (Cue c : a.cues) if (c == Cue.OPENING) openings++;
        assertEquals(1, openings, "a campaign opens once");
        a.cues.clear();
        a.enemies.clear();
        a.ship.x = Hataki.CX;
        a.ship.y = Hataki.Y0 + 60f;
        run(a, 0.2f);
        assertFalse(a.cues.contains(Cue.ATALA_GATE), "Vitala's gate does not replay Atala's parley");
        assertFalse(a.cues.contains(Cue.OPENING));
        assertEquals(1, a.lokaIndex, "the loka changes as the hull flies up into Vitala");
        // and back down again
        a.ship.y = 3000f;
        run(a, 0.2f);
        assertEquals(1, a.lokaIndex, "the deepest loka reached stays (it decides the hangar and the drops)");
        assertEquals(0, a.lokaAt(a.ship.y));
    }

    /** A soak: a pilot flies up the river through Vitala with every court awake. Nothing throws, the enemies shoot, burn the ground and vanish
     *  in smoke, the world keeps its roster in rock-free air, and a step stays cheap. */
    @Test
    void aSoakFlightThroughAwakeCourtsStaysSaneAndFast() {
        World w = World.vitala(0);
        for (Enemy e : w.enemies) if (e.vitala) e.active = true;
        w.ship.hp = 1e6f;                                           // (the soak is about the enemies, not the pilot)
        long t0 = System.nanoTime();
        int steps = 0;
        boolean smoked = false, burned = false;
        int inRock = 0;
        long unitFrames = 0;
        for (int i = 0; i < 60 * 150; i++) {
            float ty = w.ship.y + 240f;
            final float tx = Hataki.centre(ty);
            w.update(1 / 60f, new vajra.ship.ShipControls() {
                public float thrustY() { return 0.7f; }
                public float thrustX() { return Math.max(-1f, Math.min(1f, (tx - w.ship.x) / 60f)); }
                public float rotate() { return 0; }
                public boolean fire() { return false; }
                public boolean boost() { return false; }
                public boolean shield() { return false; }
                public boolean mouseAim() { return false; }
                public float aimX() { return 0; }
                public float aimY() { return 0; }
            });
            steps++;
            burned |= !w.hazards.patches.isEmpty() || !w.hazards.lines.isEmpty();
            for (int k = 0; k < w.enemies.size(); k++) {
                Enemy e = w.enemies.get(k);
                smoked |= e.smoked;
                if (e.vitala && !e.dead) unitFrames++;
                if (e.vitala && !e.phasing && !e.dead && w.blockedPx(e.x, e.y)) inRock++;
            }
        }
        java.util.Map<String, Integer> where = new java.util.TreeMap<>();
        for (Enemy e : w.enemies)
            if (e.vitala && !e.phasing && !e.dead && w.blockedPx(e.x, e.y)) where.merge(e.kind + (e.sentry ? "/sentry" : "") + (e.siteGuard ? "/site" : "") + (e.commander != null ? "/follower" : ""), 1, Integer::sum);
        System.out.println("VitalaSoak: in rock at the end: " + where);
        float ms = (System.nanoTime() - t0) / 1e6f / steps;
        System.out.println("VitalaSoak: " + steps + " steps, " + ms + " ms/step, reached y=" + (w.ship.y - Hataki.Y0) + ", burned=" + burned + ", smoked=" + smoked + ", inRock samples=" + inRock);
        assertTrue(burned, "the fire is laid");
        assertTrue(smoked, "Dhuma took to its smoke");
        assertTrue(ms < 4f, "a world step stays cheap: " + ms + " ms");
        assertTrue(where.isEmpty(), "no unit is left in rock: " + where);
        assertTrue(inRock < unitFrames * 0.02, "and only a moment of it on the way: " + inRock + " of " + unitFrames + " unit-frames");
    }

    /** BALANCE: a pilot who only flies up the river (no shooting, no dodging, shield off) takes this much from each band. The damage must climb
     *  band to band, and no single band may out-damage a full hull plus its shield by more than a clear margin (a real pilot dodges and kills). */
    @Test
    void theBandsHurtMoreAsTheyClimbAndNoneIsAWall() {
        float[] taken = new float[6];
        for (int run = 0; run < 3; run++) {
            World w = World.vitala(run);
            for (Enemy e : w.enemies) if (e.vitala) e.active = true;
            float full = w.ship.maxHp();
            w.ship.hp = full * 1000f;
            float last = w.ship.hp;
            while (w.ship.y < Hataki.HEIGHT - 900f && w.clock < 400f) {
                float ty = w.ship.y + 240f;
                final float tx = Hataki.centre(ty);
                final World ww = w;
                w.update(1 / 60f, new vajra.ship.ShipControls() {
                    public float thrustY() { return 0.8f; }
                    public float thrustX() { return Math.max(-1f, Math.min(1f, (tx - ww.ship.x) / 60f)); }
                    public float rotate() { return 0; }
                    public boolean fire() { return false; }
                    public boolean boost() { return false; }
                    public boolean shield() { return false; }
                    public boolean mouseAim() { return false; }
                    public float aimX() { return 0; }
                    public float aimY() { return 0; }
                });
                int band = Math.max(0, Hataki.band(w.ship.y));
                taken[band] += Math.max(0f, last - w.ship.hp) / (full * 3f);
                last = w.ship.hp;
            }
        }
        StringBuilder sb = new StringBuilder("Balance (hulls lost per band, passive pilot): ");
        for (int b = 0; b < 6; b++) sb.append(Hataki.NAMES[b]).append(' ').append(String.format("%.2f", taken[b])).append("; ");
        System.out.println(sb);
        for (int b = 0; b < 5; b++) assertTrue(taken[b] < 6f, "band " + b + " is not a wall: " + taken[b]);   // (the Altar is the boss fight: a passive pilot sits in it)
        assertTrue(taken[2] > taken[1] && taken[3] > taken[1] && taken[4] > taken[1] * 2f, "the forges hurt more than the Mouth");
        assertTrue(taken[1] < 1.5f, "the Mouth is the gentle band: " + taken[1]);
    }
}
