package vajra.ship;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import vajra.weapon.WeaponDef;
import vajra.world.Balance;
import vajra.world.LokaTable;
import vajra.world.World;

/** The economy as a design, pinned: one hull and a few patterns open per loka, prices climb with the
 *  loka's pay, ranks add power and an ability, and a pilot's best damage keeps pace with the hull
 *  the enemy carries (docs/ECONOMY.md has the reasoning and the tables). */
class EconomyTest {
    /** Coins a thoroughly cleared loka pays, by the model in docs/ECONOMY.md (vajra.world.Balance; BalanceTest measures the built lokas against it). */
    private static double income(int loka) {
        return Balance.gross(loka);
    }

    private static double keep(int loka) { return Balance.keep(loka); } // a typical pilot never takes it all

    private static double dps(WeaponDef d) {
        return d.damage * d.count * Math.max(1, d.burst) / Math.max(0.05f, d.cooldown + d.chargeTime);
    }

    private static String signature(WeaponDef d) {
        return d.aimAssist + "/" + d.pierceEnemies + "/" + d.pierceWalls + "/" + d.homingTurn + "/"
                + d.aoeRadius + "/" + d.range + "/" + d.count + "/" + d.rearDamage + "/" + d.speed + "/" + d.life + "/" + d.burnDps
                + "/" + d.burnTime + "/" + d.slow + "/" + d.slowTime + "/" + d.bounces + "/" + d.chain + "/" + d.aoeRadius + "/" + d.zoneRadius
                + "/" + d.pullRadius + "/" + d.pull + "/" + d.stun + "/" + d.leech + "/" + d.splitCount + "/" + d.spreadDeg + "/" + d.grow + "/" + d.executeBelow;
    }

    @Test
    void oneHullOpensPerLokaAndThePricesClimb() {
        for (int i = 1; i < ShipDef.ALL.length; i++) {
            ShipDef d = ShipDef.ALL[i];
            assertEquals(Math.max(0, i - 1), d.minLoka, d.name + " opens one loka after the last");
            assertTrue(LokaTable.shipUnlocked(d.minLoka, i), d.name + " is open at its own loka");
            if (d.minLoka > 0) assertFalse(LokaTable.shipUnlocked(d.minLoka - 1, i), d.name + " is shut a loka earlier");
            assertTrue(d.cost > ShipDef.ALL[i - 1].cost, d.name + " costs more than the hull before it");
        }
        for (ShipDef d : ShipDef.ALL) {
            assertTrue(d.upgrades[2].cost > d.upgrades[1].cost, d.name + ": x.3 costs more than x.2");
            assertTrue(d.upgrades[1].cost > 0);
        }
    }

    @Test
    void aHullWithItsTiersCostsAboutOneLokasKeep() {
        for (int i = 1; i < ShipDef.ALL.length; i++) {
            ShipDef d = ShipDef.ALL[i];
            double all = d.cost + d.upgrades[1].cost + d.upgrades[2].cost;
            double k = keep(d.minLoka);
            assertTrue(all >= 0.6 * k && all <= 2.6 * k,
                    d.name + " costs " + all + " against a loka's keep of " + Math.round(k));
        }
    }

    @Test
    void theLastHullsHoldMoreThanTheFirst() {
        double firstHp = ShipDef.ALL[0].maxHp, lastHp = ShipDef.ALL[6].maxHp / (1 - ShipDef.ALL[6].damageReduction);
        assertTrue(lastHp >= 2.0 * firstHp, "Vajra-7 should hold at least twice the first blade (" + lastHp + ")");
        double enemyFire = LokaTable.at(6).dmgScale;
        assertTrue(lastHp / firstHp >= enemyFire * 0.8, "hull growth keeps pace with the enemy's fire");
    }

    @Test
    void patternsOpenFiveThreeTwoTwoTwoOne() {
        int[] want = {8, 6, 5, 5, 5, 4, 3};   // the standard patterns' five, three, two, two, two, one - and the lokas' three each
        int[] got = new int[7];
        for (WeaponDef d : WeaponDef.ALL) got[d.minLoka]++;
        assertArrayEquals(want, got);
        double prev = 0;
        for (int l = 0; l < 7; l++) { // the cheapest pattern a loka offers never costs less than the last loka's
            double min = Double.MAX_VALUE;
            for (WeaponDef d : WeaponDef.ALL) if (d.minLoka == l && d.cost > 0) min = Math.min(min, d.cost);
            if (min == Double.MAX_VALUE) continue;
            assertTrue(min >= prev, "loka " + l + " opens cheaper patterns than loka " + (l - 1));
            prev = min;
        }
    }

    @Test
    void aDeeperPatternIsNotOfferedUntilItsLoka() {
        Hangar h = new Hangar();
        h.coins = 99999;
        assertFalse(h.weaponOffered(WeaponDef.LASER.idx));
        assertFalse(h.buyWeapon(WeaponDef.LASER.idx), "the Laser is not for sale in Atala");
        h.loka = WeaponDef.LASER.minLoka;
        assertTrue(h.weaponOffered(WeaponDef.LASER.idx));
        assertTrue(h.buyWeapon(WeaponDef.LASER.idx));
    }

    @Test
    void ranksAddPowerThenAnAbility() {
        for (WeaponDef d : WeaponDef.ALL) {
            assertSame(d, d.ranked(0), "rank I is the pattern itself");
            assertEquals(d.damage * 1.25f, d.ranked(1).damage, 0.001f, d.name + " rank II");
            assertEquals(d.damage * 1.5f, d.ranked(2).damage, 0.001f, d.name + " rank III");
            assertEquals(signature(d), signature(d.ranked(1)), d.name + " rank II adds no ability");
            assertNotEquals(signature(d), signature(d.ranked(2)), d.name + " rank III gains its ability");
            assertFalse(d.ability.isBlank(), d.name + " names its ability");
        }
    }

    @Test
    void temperingCostsCoinsThenShardsAndStopsAtThree() {
        Hangar h = new Hangar();
        h.coins = 5000;
        WeaponDef single = WeaponDef.SINGLE;
        assertTrue(h.buyRank(single.idx), "rank II");
        assertEquals(1, h.weaponRank[single.idx]);
        assertFalse(h.buyRank(single.idx), "rank III wants shards on top of the coins");
        h.shards[0] = 2;
        int coins = h.coins;
        assertTrue(h.buyRank(single.idx));
        assertEquals(2, h.weaponRank[single.idx]);
        assertEquals(0, h.shardCount(), "two shards spent");
        assertEquals(coins - single.rankCost(1), h.coins);
        assertFalse(h.buyRank(single.idx), "there is no rank IV");
        assertFalse(h.buyRank(WeaponDef.LASER.idx), "an unowned pattern cannot be tempered");
        assertTrue(single.rankCost(0) >= 60 && single.rankCost(1) >= 150);
    }

    @Test
    void theGunsFireTheTemperedPattern() {
        World w = new World();
        w.hangar.coins = 5000;
        w.hangar.shards[0] = 2;
        w.selectWeapon(0);
        assertEquals(WeaponDef.SINGLE.damage, w.ship.weapon.def.damage, 0.001f);
        w.hangar.buyRank(0);
        w.hangar.buyRank(0);
        w.selectWeapon(0); // refit
        assertEquals(WeaponDef.SINGLE.damage * 1.5f, w.ship.weapon.def.damage, 0.001f);
        assertTrue(w.ship.weapon.def.aimAssist, "rank III Single Bolt is a true bolt");
    }

    @Test
    void aPilotsBestDamageKeepsPaceWithTheEnemysHull() {
        double base = dps(WeaponDef.SINGLE);
        for (int l = 0; l < LokaTable.COUNT; l++) {
            double best = 0;
            for (WeaponDef d : WeaponDef.ALL) if (d.minLoka <= l) best = Math.max(best, dps(d.ranked(2)));
            double player = best * 1.35 / base; // a rank III pattern and a damage tier, over the starter
            assertTrue(player >= 0.7 * LokaTable.at(l).hpScale,
                    LokaTable.at(l).name + ": player " + player + "x against hulls " + LokaTable.at(l).hpScale + "x");
        }
    }

    @Test
    void aClearedLokaPaysMoreThanTheOneBefore() {
        for (int l = 1; l < LokaTable.COUNT; l++) assertTrue(income(l) > income(l - 1));
        assertTrue(income(6) / income(0) > 2.0, "Patala pays over twice Atala");
    }
}
