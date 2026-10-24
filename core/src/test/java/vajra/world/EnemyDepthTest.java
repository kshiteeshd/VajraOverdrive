package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;

/** The shaft has a floor, and a second population lives there: a leader's call raises reserves out of it, and a scout can dive and
 *  surface at the hull's side. A body that is down cannot be touched. */
class EnemyDepthTest {
    private static Enemy aLeader(World w) {
        for (Enemy e : w.enemies) if (e.rank >= 1 && !e.boss && !e.kind.keeper() && !e.voice && !e.parley) return e;
        throw new AssertionError("no leader in the world");
    }

    @Test
    void aLeadersCallRaisesTwoReservesFromTheFloor() {
        World w = new World();
        Enemy leader = aLeader(w);
        leader.active = true;
        leader.callT = 0f;
        w.ship.x = leader.x + 220;
        w.ship.y = leader.y;
        w.ship.hp = w.ship.maxHp();
        int before = w.enemies.size();
        w.update(1 / 60f, ShipControls.NONE);
        assertEquals(before + 2, w.enemies.size(), "two reserves answer the call");
        int below = 0;
        for (Enemy e : w.enemies)
            if (e.commander == leader && e.depth > 0.9f) {
                below++;
                assertEquals(3, e.diveState, "they are rising");
                assertTrue(e.submerged());
                assertFalse(e.hurt(999f, w), "a body down at the floor cannot be touched");
                assertFalse(e.dead);
            }
        assertEquals(2, below);
        assertEquals(1, leader.backups, "a leader has two calls; one is spent");
        for (int i = 0; i < 90; i++) {                                                   // a second and a half later they have surfaced
            w.ship.hp = w.ship.maxHp();
            w.update(1 / 60f, ShipControls.NONE);
        }
        int up = 0;
        for (Enemy e : w.enemies) if (e.commander == leader && !e.dead && e.depth == 0f && e.diveState == 0) up++;
        assertTrue(up >= 1, "the reserves have broken the surface and are in the fight: " + up);
    }

    @Test
    void aLeaderOutOfTheFightCallsNobody() {
        World w = new World();
        Enemy leader = aLeader(w);
        leader.active = false;
        leader.callT = 0f;
        int before = w.enemies.size();
        w.update(1 / 60f, ShipControls.NONE);
        assertEquals(before, w.enemies.size(), "a sleeper calls nobody");
    }

    @Test
    void aScoutDivesAndSurfacesAtTheHullsSide() {
        World w = new World();
        w.enemies.clear();
        w.ship.x = 2600;
        w.ship.y = 300;
        Enemy s = new Enemy(Enemy.Kind.AURIS, 2600, 700);
        s.active = true;
        s.diveCd = 0f;
        w.add(s);
        boolean sank = false, below = false, surfaced = false;
        float distAtSurface = -1;
        for (int i = 0; i < 60 * 6; i++) {
            w.ship.hp = w.ship.maxHp();
            w.update(1 / 60f, ShipControls.NONE);
            sank |= s.diveState == 1;
            below |= s.submerged();
            if (s.diveState == 3 && distAtSurface < 0) distAtSurface = (float) Math.hypot(s.x - w.ship.x, s.y - w.ship.y);
            if (below && s.diveState == 0 && s.depth == 0f) { surfaced = true; break; }
        }
        assertTrue(sank && below && surfaced, "it sank, ran below and came up: " + sank + " " + below + " " + surfaced);
        assertTrue(distAtSurface > 250f && distAtSurface < 480f, "it surfaced a few hundred px from the hull: " + distAtSurface);
    }

    @Test
    void aLeaderOrAKeeperNeverDives() {
        World w = new World();
        Enemy leader = aLeader(w);
        leader.active = true;
        leader.diveCd = 0f;
        for (int i = 0; i < 120; i++) w.update(1 / 60f, ShipControls.NONE);
        assertEquals(0, leader.diveState, "rank 1 holds the line");
        assertEquals(0f, leader.depth, 1e-6f);
    }
}
