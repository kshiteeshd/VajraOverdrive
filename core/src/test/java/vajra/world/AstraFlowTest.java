package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.weapon.Bullet;

/** THE FLOW (world/AstraFlow): Agni's pyre bloom and Vayu's tornado, as a simulation. See docs/ASTRA_FLOW_PLAN.md. */
class AstraFlowTest {
    private static final float DT = 1 / 60f;

    private static World arena() {
        World w = new World();
        w.enemies.clear();
        w.maps = new TileMap[0];                                         // open ground: no rock to turn the flow aside
        w.ship.x = 2600;
        w.ship.y = 2700;
        w.ship.facing = (float) (Math.PI / 2);
        return w;
    }

    private static Enemy foe(World w, float x, float y, float hp) {
        Enemy e = new Enemy(Enemy.Kind.MAYAVI, x, y);
        e.hp = e.maxHp = hp;
        e.active = true;
        w.enemies.add(e);
        return e;
    }

    private static void run(World w, float seconds) {
        for (float t = 0; t < seconds; t += DT) w.updateAstra(DT);
    }

    private static float dist(World w, Enemy e) {
        return (float) Math.hypot(e.x - w.ship.x, e.y - w.ship.y);
    }

    private static float angle(World w, Enemy e) {
        return (float) Math.atan2(e.y - w.ship.y, e.x - w.ship.x);
    }

    // ---- AGNI ----------------------------------------------------------------------------------------------------------------------------

    @Test
    void eighteenStreamsOpenOneAfterAnotherAndRunOutToAScreen() {
        World w = arena();
        w.flow.startAgni(w.ship, 0, 1f, 1f, 11L);
        run(w, 0.25f);
        int opened = 0;
        for (int i = 0; i < w.flow.n; i++) opened = Math.max(opened, w.flow.stream[i] + 1);
        assertTrue(opened > 3 && opened < 12, "early on only the first streams are open: " + opened);
        float far = 0f;
        int most = 0;
        for (float t = 0.25f; t < 2.0f; t += DT) {
            w.updateAstra(DT);
            for (int i = 0; i < w.flow.n; i++) {
                far = Math.max(far, (float) Math.hypot(w.flow.px[i] - w.ship.x, w.flow.py[i] - w.ship.y));
                most = Math.max(most, w.flow.stream[i] + 1);
            }
        }
        assertEquals(AstraFlow.STREAMS, most, "all eighteen streams open");
        assertTrue(far > 450f, "the fire runs out toward a screen's reach: " + far);
        assertTrue(far < AstraFlow.REACH + 100f, "and no farther than its reach: " + far);
    }

    @Test
    void aFoeInTheFireIgnitesAndKeepsBurningAfterTheStreamsEnd() {
        World w = arena();
        Enemy e = foe(w, 2600, 3050, 900);
        w.flow.startAgni(w.ship, 0, 1f, 1f, 5L);
        float burned = 0f;
        for (float t = 0; t < 1.4f; t += DT) {
            w.updateAstra(DT);
            burned = Math.max(burned, e.burnT);
        }
        assertTrue(burned > 0f, "the streams ignited it");
        for (int i = 0; i < 600 && w.flow.active(); i++) w.updateAstra(DT);
        assertFalse(w.flow.active(), "the streams are over");
        float hp = e.hp;
        e.burnT = 2f;                                                    // it is still alight (refresh, as a late touch would)
        run(w, 1f);
        assertTrue(e.hp < hp, "a foe still burns after the fire has gone: " + hp + " -> " + e.hp);
        run(w, 4f);
        assertEquals(0f, e.burnT, 0.001f, "and the burn runs out");
        assertTrue(e.hp < 900f - 60f, "it was hurt by the fire: " + e.hp);
    }

    @Test
    void aFoeThatDiesBurningIgnitesItsNeighbourAndPaysACoin() {
        World w = arena();
        Enemy a = foe(w, 2600, 3200, 1f), b = foe(w, 2660, 3200, 900);
        a.burnT = AstraFlow.BURN_TIME;
        a.burnOwner = 0;
        int coins = w.hangar.coins;
        run(w, 0.3f);
        assertTrue(a.dead, "the burning foe died");
        assertTrue(b.burnT > 0f, "and its neighbour caught fire");
        assertEquals(coins + 1, w.hangar.coins, "a foe that dies burning pays one extra coin");
    }

    @Test
    void theFlowReplaysFromItsSeedAndNeverExceedsItsCap() {
        World a = arena(), b = arena();
        a.flow.startAgni(a.ship, 0, 1f, 1f, 99L);
        b.flow.startAgni(b.ship, 0, 1f, 1f, 99L);
        for (int i = 0; i < 100; i++) { a.updateAstra(DT); b.updateAstra(DT); }
        assertEquals(a.flow.n, b.flow.n, "the same seed makes the same fire");
        for (int i = 0; i < a.flow.n; i++) assertEquals(a.flow.px[i], b.flow.px[i], 0.001f);
        for (int i = 0; i < 200; i++) { a.updateAstra(DT); assertTrue(a.flow.n <= AstraFlow.CAP); }
    }

    // ---- VAYU ----------------------------------------------------------------------------------------------------------------------------

    @Test
    void aFoeIsCarriedRoundTheFunnelNotDraggedInALine() {
        World w = arena();
        Enemy e = foe(w, 2900, 2700, 9000);
        float r0 = dist(w, e), a0 = angle(w, e);
        w.flow.startVayu(w.ship, 0, 1f, 1f, 3L);
        run(w, 1.0f);
        assertTrue(e.caught > 0f, "it is held by the tornado");
        assertTrue(dist(w, e) < r0 - 60f, "it was drawn in: " + r0 + " -> " + dist(w, e));
        float turned = Math.abs(World.wrapAngle(angle(w, e) - a0));
        assertTrue(turned > 0.6f, "and carried round it: it has turned " + turned + " rad");
    }

    @Test
    void heavierFoesLagBehindTheLight() {
        World w = arena();
        Enemy light = foe(w, 2900, 2700, 9000), heavy = foe(w, 2300, 2700, 9000);
        heavy.elite = true;
        w.flow.startVayu(w.ship, 0, 1f, 1f, 3L);
        run(w, 1.0f);
        assertTrue(dist(w, heavy) > dist(w, light) + 8f, "the elite resists the draw: " + dist(w, heavy) + " vs " + dist(w, light));
    }

    @Test
    void theWindCutsThemOneByOneNearestFirst() {
        World w = arena();
        Enemy[] f = new Enemy[4];
        for (int i = 0; i < 4; i++) f[i] = foe(w, 2600 + 150 + 50 * i, 2700, 100);
        w.flow.startVayu(w.ship, 0, 1f, 1f, 3L);
        float[] firstSeen = {-1, -1, -1, -1};
        int seen = 0;
        float t = 0;
        while (t < 1.7f) {
            w.updateAstra(DT);
            t += DT;
            while (seen < w.flow.blades.size() + cutsSoFar(w) && seen < 4 && firstSeen[seen] < 0) firstSeen[seen++] = t;
        }
        assertTrue(f[0].hp < 100f - 40f, "the nearest was cut: " + f[0].hp);
        assertTrue(f[0].hp >= 100f * 0.40f || f[0].dead, "a cut takes 55% of its health: " + f[0].hp);
        assertTrue(firstSeen[0] > 0.5f && firstSeen[0] < 0.65f, "the first cut lands about 0.55 s in: " + firstSeen[0]);
        assertTrue(firstSeen[1] - firstSeen[0] > 0.15f && firstSeen[1] - firstSeen[0] < 0.25f, "one every 0.2 s: " + (firstSeen[1] - firstSeen[0]));
    }

    /** Blades that have already faded (their cut happened earlier). */
    private static int cutsSoFar(World w) {
        int dead = 0;
        for (Enemy e : w.enemies) if (e.hp < e.maxHp) dead++;
        return Math.max(0, dead - w.flow.blades.size());
    }

    @Test
    void anEnemyRoundInTheStormIsDrawnInHarmlessAndThenThrownOut() {
        World w = arena();
        w.spawnEnemyBullet(w.ship.x + 300, w.ship.y, (float) Math.PI, Bullet.HOSTILE, 1f);
        Bullet b = w.bullets.get(w.bullets.size() - 1);
        w.flow.startVayu(w.ship, 0, 1f, 1f, 3L);
        float d0 = (float) Math.hypot(b.x - w.ship.x, b.y - w.ship.y);
        for (int i = 0; i < 20; i++) { w.updateAstra(DT); b.update(w, DT); }
        assertEquals(0f, b.damage, 0.001f, "a round inside the storm cannot hurt the pilot");
        for (float t = 0.33f; t < AstraFlow.T_EYE + 0.6f; t += DT) { w.updateAstra(DT); b.update(w, DT); }
        float dOut = (float) Math.hypot(b.x - w.ship.x, b.y - w.ship.y);
        assertTrue(dOut > d0 || b.dead, "and the throw blows it outward, away from the ship: " + d0 + " -> " + dOut);
    }

    @Test
    void theThrowSendsSurvivorsOutwardAndABossIsNeverCaught() {
        World w = arena();
        Enemy e = foe(w, 2850, 2700, 90000);
        Enemy boss = foe(w, 2700, 2900, 90000);
        boss.boss = true;
        w.flow.startVayu(w.ship, 0, 1f, 1f, 3L);
        run(w, AstraFlow.T_EYE - 0.02f);
        float rEye = dist(w, e);
        assertEquals(0f, boss.caught, 0.001f, "a ruler is not caught");
        assertEquals(2700f, boss.x, 0.01f, "and is not moved");
        run(w, 0.6f);
        assertTrue(dist(w, e) > rEye + 100f, "the throw sends it outward: " + rEye + " -> " + dist(w, e));
        run(w, 1.2f);
        assertEquals(0f, e.caught, 0.31f, "and then it is let go");
    }
}
