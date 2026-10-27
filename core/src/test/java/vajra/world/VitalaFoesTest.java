package vajra.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;
import vajra.ship.ShipControls;
import vajra.weapon.WeaponDef;

/** Vitala's units: they never dive or rise from a floor (there is none), their reserves FLY IN, and a quick one sidesteps a round that is coming at it. */
class VitalaFoesTest {
    private static World hall() {
        World w = World.vitala(0);
        w.enemies.clear();
        w.cues.clear();
        return w;
    }

    @Test
    void noVitalaUnitEverDivesOrSinksAndTheReservesFlyInAlreadyInTheAir() {
        World w = hall();
        w.ship.x = Hataki.centre(Hataki.START[2] + 600f);
        w.ship.y = Hataki.START[2] + 600f;
        Enemy leader = new Enemy(Enemy.Kind.VAKRA, w.ship.x, w.ship.y + 380f);
        leader.rank = 1;
        leader.active = true;
        leader.callT = 0f;
        w.add(leader);
        assertTrue(leader.vitala);
        for (int i = 0; i < 60 * 14; i++) {
            w.update(1 / 60f, ShipControls.NONE);
            for (Enemy e : w.enemies) assertTrue(e.depth == 0f && !e.submerged() || e.kind == Enemy.Kind.AURIS || e.kind == Enemy.Kind.HATAKA, "nothing in Vitala goes under (" + e.kind + " depth " + e.depth + ")");
        }
        int reserves = 0;
        for (Enemy e : w.enemies) if (e != leader && e.commander == leader) reserves++;
        assertTrue(reserves >= 2, "the leader's call was answered: " + reserves);
    }

    @Test
    void aQuickUnitSidestepsARoundComingAtItButArmouredTamraDoesNot() {
        for (Enemy.Kind kind : new Enemy.Kind[] {Enemy.Kind.VAKRA, Enemy.Kind.BHAVI}) {
            World w = hall();
            w.ship.x = Hataki.centre(Hataki.START[2] + 900f);
            w.ship.y = Hataki.START[2] + 900f;
            Enemy e = new Enemy(kind, w.ship.x + 300f, w.ship.y + 200f);
            e.rank = 1;
            e.active = true;
            w.add(e);
            float sideMax = 0f;
            float ex = e.x;
            for (int shot = 0; shot < 6; shot++) {
                // a round of the pilot's flying straight at it along x
                w.spawnBullet(e.x - 220f, e.y, 0f, WeaponDef.SINGLE, 1f, 1f);
                for (int i = 0; i < 30; i++) { w.update(1 / 60f, ShipControls.NONE); sideMax = Math.max(sideMax, Math.abs(e.dodgeVy)); }
            }
            if (kind == Enemy.Kind.VAKRA) assertTrue(sideMax > 100f, "Agneya throws itself aside (" + sideMax + ")");
            else assertEquals(0f, sideMax, 1e-3f, "Tamra, heavy and plated, does not try");
        }
    }
}
