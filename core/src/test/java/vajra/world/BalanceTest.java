package vajra.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import vajra.enemy.Enemy;

/** The balance as a measurement (docs/ECONOMY.md): the lokas that are BUILT are measured, enemy by enemy and chest by chest, against the budget
 *  {@link Balance} sets for them; the ones still to build are checked as a curve. */
class BalanceTest {
    private static final int[] KILL_COINS = {2, 4, 6};

    /** {kill coins, chests, enemies} a thorough clear of each built loka pays, from the generated campaign world. */
    private static double[][] measure(World w) {
        double[][] m = new double[2][3];
        int[] chests = new int[2];
        for (Enemy e : w.enemies) {
            if (e.voice || e.boss) continue;
            int l = w.lokaAt(e.spawnY);
            int tier = e.elite ? 2 : e.rank == 1 ? 1 : 0;
            m[l][0] += Math.max(1, Math.round(KILL_COINS[tier] * (float) Math.sqrt(LokaTable.at(l).hpScale)));
            m[l][2]++;
        }
        for (Chest c : w.chests) chests[c.loka]++;
        for (int l = 0; l < 2; l++) m[l][1] = chests[l];
        return m;
    }

    @Test
    void theBuiltLokasPayWhatTheirBudgetSays() {
        World w = new World(0, World.CAMPAIGN);
        double[][] m = measure(w);
        StringBuilder sb = new StringBuilder("Balance: ");
        for (int l = 0; l < 2; l++) {
            double kills = m[l][0];
            double chests = m[l][1] * (Balance.gross(l) * Balance.SHARE_CHESTS / Balance.CHESTS[l]);
            double caches = Balance.CACHES * Balance.cacheCoins(l);
            double purse = Balance.purse(l);
            double total = kills + chests + caches + purse;
            sb.append(String.format("%s: %d enemies, kills %d chests %d caches %d purse %d = %d of %d (x%.2f) | ", LokaTable.at(l).name, (int) m[l][2], (int) kills, (int) chests, (int) caches,
                    (int) purse, (int) total, (int) Balance.gross(l), total / Balance.gross(l)));
            assertEquals((int) Balance.CHESTS[l], (int) m[l][1], LokaTable.at(l).name + " is built with the chests Balance plans for");
            assertEquals(Balance.gross(l), total, Balance.gross(l) * 0.18, LokaTable.at(l).name + " pays its budget");
            assertEquals(Balance.gross(l) * Balance.SHARE_KILLS, kills, Balance.gross(l) * Balance.SHARE_KILLS * 0.30,
                    LokaTable.at(l).name + "'s roster pays its share of the budget");
        }
        System.out.println(sb);
    }

    @Test
    void everyEnemyIsGradedByTheLokaItStandsIn() {
        World w = new World(0, World.CAMPAIGN);
        double[] hp = new double[2], fire = new double[2];
        int[] n = new int[2];
        for (Enemy e : w.enemies) {
            if (e.voice || e.boss || e.elite || e.rank != 0) continue;
            int l = w.lokaAt(e.spawnY);
            hp[l] += e.maxHp / e.kind.hp;
            fire[l] += e.dmgScale;
            n[l]++;
        }
        for (int l = 0; l < 2; l++) {
            double want = LokaTable.at(l).hpScale * (l == 1 ? Balance.VITALA_HULL : 1.0);   // Vitala's units carry the forge's own multiplier on top
            assertEquals(want, hp[l] / n[l], want * 0.05, LokaTable.at(l).name + " hull grade averages the loka's");
            assertEquals(LokaTable.at(l).dmgScale, fire[l] / n[l], LokaTable.at(l).dmgScale * 0.05, LokaTable.at(l).name + " fire grade averages the loka's");
        }
    }

    @Test
    void difficultyClimbsBandByBandAndNeverStepsBackAtALokaDoor() {
        double prevHp = 0, prevFire = 0;
        for (int l = 0; l < LokaTable.COUNT; l++)
            for (int r = Balance.BAND_FIRST[l]; r <= Balance.BAND_LAST[l]; r++) {
                double hp = LokaTable.at(l).hpScale * Balance.ramp(l, r), fire = LokaTable.at(l).dmgScale * Balance.rampFire(l, r);
                assertTrue(hp > prevHp, LokaTable.at(l).name + " band " + r + " hull " + hp + " after " + prevHp);
                assertTrue(fire > prevFire, LokaTable.at(l).name + " band " + r + " fire " + fire + " after " + prevFire);
                prevHp = hp;
                prevFire = fire;
            }
        assertEquals(1f, (Balance.ramp(0, 1) + Balance.ramp(0, 5)) / 2f, 0.001f, "a loka's average stays its table's number");
    }

    @Test
    void thePlanForTheSevenLokasHolds() {
        double total = 0;
        for (int l = 0; l < LokaTable.COUNT; l++) {
            assertTrue(l == 0 || Balance.gross(l) > Balance.gross(l - 1) * 1.2, "each loka pays over a fifth more than the last");
            assertTrue(Balance.gross(l) < LokaTable.at(l).hpScale * Balance.GROSS_BASE * 1.6, "pay never outruns the hulls it is earned from");
            total += Balance.keep(l);
        }
        double sinks = 0;
        for (vajra.ship.ShipDef d : vajra.ship.ShipDef.ALL) sinks += (d.cost + d.upgrades[1].cost + d.upgrades[2].cost);
        for (vajra.weapon.WeaponDef d : vajra.weapon.WeaponDef.ALL) sinks += d.cost + d.rankCost(0) + d.rankCost(1);
        double share = total / sinks;
        System.out.println("Balance: a typical pilot banks " + (int) total + " coins over seven lokas against " + (int) sinks + " of things to buy (" + Math.round(share * 100) + "%)");
        assertTrue(share > 0.2 && share < 0.8, "a typical run affords a build, not a checklist: " + share);
    }
}
