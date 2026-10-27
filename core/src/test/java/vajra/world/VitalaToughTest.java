package vajra.world;

import vajra.enemy.Enemy;
import vajra.ship.ShipControls;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/** Vitala's units are tempered (their own hull multiplier) and, when their leader falls, they turn on the hull instead of running. */
class VitalaToughTest {
    private static final ShipControls IDLE = new ShipControls() {
        public float thrustX() { return 0; }
        public float thrustY() { return 0; }
        public float rotate() { return 0; }
        public boolean fire() { return false; }
        public boolean boost() { return false; }
        public boolean shield() { return false; }
        public boolean mouseAim() { return false; }
        public float aimX() { return 0; }
        public float aimY() { return 0; }
    };

    private static World vitala() {
        World w = World.vitala(0);
        w.enemies.clear();
        return w;
    }

    private static Enemy foe(World w, Enemy.Kind k, float dx, float dy, int rank) {
        Enemy e = new Enemy(k, w.ship.x + dx, w.ship.y + dy);
        e.rank = rank;
        e.vitala = true;
        e.active = true;
        e.wakeRange = 2200f;
        w.enemies.add(e);
        return e;
    }

    @Test
    void vitalaUnitsAreMuchToughterThanAtalas() {
        World w = vitala();
        Enemy e = new Enemy(Enemy.Kind.VAKRA, w.ship.x, w.ship.y + 300);
        float base = e.maxHp;
        w.add(e);
        assertTrue(e.maxHp >= base * 1.35f * 0.88f * Balance.VITALA_HULL * 0.99f, "a Vitala fan-firer carries the forge's multiplier: " + e.maxHp + " from " + base);
        assertTrue(e.maxHp > base * 2f, "more than twice an Atala hull");
        Enemy lead = new Enemy(Enemy.Kind.VAKRA, w.ship.x + 100, w.ship.y + 300);
        lead.rank = 1;
        w.add(lead);
        assertTrue(lead.maxHp > e.maxHp * 1.2f, "a leader is tougher still");
    }

    @Test
    void aFallenLeaderMakesVitalasUnitsTurnOnTheHullNotRun() {
        World w = vitala();
        Enemy lead = foe(w, Enemy.Kind.VAKRA, 0, 500, 1);
        Enemy a = foe(w, Enemy.Kind.AURIS, -120, 560, 0);
        Enemy b = foe(w, Enemy.Kind.LANCE, 120, 560, 0);
        a.commander = lead;
        b.commander = lead;
        for (int i = 0; i < 30; i++) w.update(0.016f, IDLE);
        lead.hp = 0f;
        lead.dead = true;
        float d0 = (float) Math.hypot(a.x - w.ship.x, a.y - w.ship.y);
        float dmg0 = a.dmgScale;
        for (int i = 0; i < 90; i++) w.update(0.016f, IDLE);
        assertFalse(a.routed, "no unit of Vitala runs");
        assertFalse(b.routed);
        assertTrue(a.enraged || (a.commander != null && !a.commander.dead), "it took a new leader or turned on the hull");
        if (a.enraged) assertTrue(a.dmgScale > dmg0, "and hits harder");
        float d1 = (float) Math.hypot(a.x - w.ship.x, a.y - w.ship.y);
        assertTrue(d1 < d0 + 60f, "it did not flee: " + d0 + " -> " + d1);
    }

    @Test
    void theArenaVitalaScenariosStillBuild() {
        for (Arena.Scenario s : Arena.in(Arena.FOES)) {
            if (!s.name.startsWith("VITALA")) continue;
            World w = Arena.build(s);
            for (int i = 0; i < 120; i++) w.update(0.016f, IDLE);
            assertFalse(w.enemies.isEmpty(), s.name);
        }
    }
}
