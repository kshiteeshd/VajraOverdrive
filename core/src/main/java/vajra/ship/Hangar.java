package vajra.ship;

import vajra.weapon.WeaponDef;
import vajra.world.LokaTable;

/** The purse, the shard stock, and what you own.
 *
 *  <p>A RUN OWNS NOTHING THAT OUTLIVES IT. Coins, tiers, patterns and pieces are all built
 *  inside a run and die with it (a run left mid-flight is PARKED whole, hangar included, in the pilot's
 *  profile - vajra.world.RunSave - and CONTINUE wakes it; ending the run discards it) — starting a new game resets the hangar to its defaults. The
 *  only thing that outlives a run is {@link #best}, the high score, which is a record rather
 *  than progress.
 *
 *  <p>Coins buy magnitudes (hulls and their tiers). Shards are the fuel of the astras: five shards of an element
 *  are what one release of its astra costs, paid when it is loosed (vajra.world.Astra). There is nothing to forge.
 */
public class Hangar {
    public static final int SHARD_COST = 3;  // x.3 tiers want shards on top of their coin price
    public static final int SHARD_SELL = 20; // coins per shard
    // tatva indexes, == World.TATVA order
    public static final int AKASHA = 0, VAYU = 1, AGNI = 2, JALA = 3, PRITHVI = 4;

    public int coins = 0; // a new run is broke — every coin is earned inside it
    public long best;     // the one thing that persists: best run score
    public int loka = 0;  // deepest loka this run has reached (index into LokaTable)
    // Magnetism: the pickup tug, fixed so loot reaches the hull.
    public float magnet = 15f;
    public final int[] shards = new int[5];         // Pancha Tatva stock (index = World.TATVA)
    public final boolean[] pieces = new boolean[7]; // Vajrayudha: one per loka
    public final boolean[][] owned = new boolean[ShipDef.ALL.length][3];
    public final boolean[] weaponOwned = new boolean[WeaponDef.ALL.length];
    /** Each pattern's temper: 0 = rank I, 1 = II, 2 = III (see WeaponDef.ranked). */
    public final int[] weaponRank = new int[WeaponDef.ALL.length];
    /** Which tier's LOOK each hull wears: -1 = the highest owned, the only choice the game offers. Stats follow the tiers owned. */
    public final int[] look = new int[ShipDef.ALL.length];
    public int shipIndex;
    public int weaponIndex;

    /** ASTRA TESTING STOCK: shards of every element a new run starts with, so every astra can be forged-by-spending and tried from the hangar's ASTRA tab.
     *  A test switch (-Dvajra.teststock=0 turns it off; the unit tests do): set it to 0 here before the astra economy ships. */
    public static final int TEST_STOCK = Integer.getInteger("vajra.teststock", 100);

    public Hangar() {
        java.util.Arrays.fill(look, -1);
        java.util.Arrays.fill(shards, TEST_STOCK);
        owned[0][0] = true;   // the starting hull comes with the run
        weaponOwned[0] = true; // and the Single Bolt
    }

    /** Take over another hangar's whole state: the purse, the stock, the hulls and tiers, the patterns, the looks, the chosen hull and pattern
     *  (a pilot carrying a run from one loka into the next). */
    public void copyFrom(Hangar o) {
        coins = o.coins;
        best = o.best;
        loka = o.loka;
        magnet = o.magnet;
        System.arraycopy(o.shards, 0, shards, 0, shards.length);
        System.arraycopy(o.pieces, 0, pieces, 0, pieces.length);
        for (int i = 0; i < owned.length; i++) System.arraycopy(o.owned[i], 0, owned[i], 0, owned[i].length);
        System.arraycopy(o.weaponOwned, 0, weaponOwned, 0, weaponOwned.length);
        System.arraycopy(o.weaponRank, 0, weaponRank, 0, weaponRank.length);
        System.arraycopy(o.look, 0, look, 0, look.length);
        shipIndex = o.shipIndex;
        weaponIndex = o.weaponIndex;
    }

    /** Own everything and pay for nothing: all hulls and tiers, all patterns at rank III, 999999 coins, 99 of every shard (the tests' fixture). */
    public void unlockAll() {
        for (boolean[] hull : owned) java.util.Arrays.fill(hull, true);
        java.util.Arrays.fill(weaponOwned, true);
        java.util.Arrays.fill(weaponRank, WeaponDef.RANKS - 1);
        coins = Math.max(coins, 999_999);
        java.util.Arrays.fill(shards, 99);
    }

    /** The sprite variation (1..3) hull `id` wears: the chosen look if it is owned, else the highest tier bought. */
    public int variation(int id) {
        int chosen = look[id];
        if (chosen >= 0 && chosen < 3 && owned[id][chosen]) return chosen + 1;
        return owned[id][2] ? 3 : owned[id][1] ? 2 : 1;
    }

    /** Only a hull's 2nd and 3rd variants can forge and loose an astra; the base variant (x.1) cannot, on any ship. */
    public boolean canForge() {
        return shipIndex >= 0 && shipIndex < owned.length && owned[shipIndex][1];
    }

    /** True when this run has bought the hull (x.1 is the ship itself). */
    public boolean ownsShip(int i) {
        return i >= 0 && i < owned.length && owned[i][0];
    }

    /** N in the hangar: buy a hull outright. The loka must have opened it, and its price is
     *  on top of whatever tier the ship already has. Returns what it cost, 0 for a no-op. */
    public int buyShip(int i, int lokaReached) {
        if (i <= 0 || i >= owned.length || ownsShip(i)) return 0; // 0 is the starting hull
        if (!LokaTable.shipUnlocked(lokaReached, i)) return 0;
        ShipDef d = ShipDef.ALL[i];
        if (coins < d.cost) return 0;
        coins -= d.cost;
        owned[i][0] = true;
        return d.cost;
    }

    /** N in the hangar: unlock that pattern at its price. */
    public boolean buyWeapon(int i) {
        if (i < 0 || i >= weaponOwned.length || weaponOwned[i] || !weaponOffered(i)) return false;
        int cost = WeaponDef.ALL[i].cost;
        if (coins < cost) return false;
        coins -= cost;
        weaponOwned[i] = true;
        return true;
    }

    /** A pattern is offered once the pilot has reached the loka that forges it - or already owns it. */
    public boolean weaponOffered(int i) {
        return i >= 0 && i < weaponOwned.length && (weaponOwned[i] || WeaponDef.ALL[i].minLoka <= loka);
    }

    /** Temper an owned pattern one rank: coins, and shards for rank III. */
    public boolean buyRank(int i) {
        if (i < 0 || i >= weaponOwned.length || !weaponOwned[i]) return false;
        int r = weaponRank[i];
        if (r >= WeaponDef.RANKS - 1) return false;
        WeaponDef d = WeaponDef.ALL[i];
        int shards = d.rankShards(r);
        if (coins < d.rankCost(r) || shardCount() < shards) return false;
        coins -= d.rankCost(r);
        takeShards(shards);
        weaponRank[i]++;
        return true;
    }

    /** The pattern as the pilot actually fires it: its rank's damage and, at III, its ability. */
    public WeaponDef weapon(int i) {
        return WeaponDef.ALL[i].ranked(weaponRank[i]);
    }

    /** The next owned pattern in dir (+1/-1), wrapping — what the V/B hotkeys walk. */
    public int cycleWeapon(int from, int dir) {
        int n = WeaponDef.ALL.length;
        for (int step = 1; step <= n; step++) {
            int i = Math.floorMod(from + dir * step, n);
            if (weaponOwned[i]) return i;
        }
        return from;
    }

    /** The next OWNED hull in dir, wrapping — the 1-7 hotkey only ever lands on something bought. */
    public int cycleShip(int from, int dir) {
        int n = ShipDef.ALL.length;
        for (int step = 1; step <= n; step++) {
            int i = Math.floorMod(from + dir * step, n);
            if (ownsShip(i)) return i;
        }
        return from;
    }

    public int shardCount() {
        int n = 0;
        for (int s : shards) n += s;
        return n;
    }

    /** Shards a hull's third tier takes: the deeper the hull, the more of the loka's metal. */
    public static int tierShards(ShipDef d) {
        return SHARD_COST + d.minLoka / 2;
    }

    public boolean buy(ShipDef d, int tier) {
        if (tier < 1 || tier > 2 || !ownsShip(d.id) || owned[d.id][tier]) return false;
        if (tier == 2 && !owned[d.id][1]) return false; // x.2 before x.3: the chain is the point
        int cost = d.upgrades[tier].cost;
        if (coins < cost) return false;
        if (tier == 2 && shardCount() < tierShards(d)) return false;
        coins -= cost;
        if (tier == 2) takeShards(tierShards(d));
        owned[d.id][tier] = true;
        return true;
    }

    /** Spend from the deepest stack first, so mixed stock drains evenly. */
    private void takeShards(int n) {
        while (n > 0) {
            int best = 0;
            for (int i = 1; i < shards.length; i++) if (shards[i] > shards[best]) best = i;
            shards[best]--;
            n--;
        }
    }

    /** X in the hangar: dump the whole stock at the flat rate. Returns the payout. */
    public int sellShards() {
        int paid = shardCount() * SHARD_SELL;
        coins += paid;
        for (int i = 0; i < shards.length; i++) shards[i] = 0;
        return paid;
    }

    /** A shard of `tatva` drops into the pile. True when that shard completes another set of
     *  {@link vajra.world.Astra#COST}: one more release of that element's astra is now in hand. */
    public boolean addShard(int tatva) {
        if (tatva < 0 || tatva >= shards.length) return false;
        shards[tatva]++;
        return shards[tatva] % vajra.world.Astra.COST == 0;
    }

    public float cooldownMult(ShipDef d) {
        float m = 1;
        for (int t = 1; t <= 2; t++) if (owned[d.id][t]) m *= d.upgrades[t].cooldownMult;
        return m;
    }

    public float damageMult(ShipDef d) {
        float m = 1;
        for (int t = 1; t <= 2; t++) if (owned[d.id][t]) m *= d.upgrades[t].damageMult;
        return m;
    }

    public float speedBonus(ShipDef d) {
        float s = 0;
        for (int t = 1; t <= 2; t++) if (owned[d.id][t]) s += d.upgrades[t].speedBonus;
        return s;
    }

    public float turnBonus(ShipDef d) {
        float s = 0;
        for (int t = 1; t <= 2; t++) if (owned[d.id][t]) s += d.upgrades[t].turnBonus;
        return s;
    }

    public float hullBonus(ShipDef d) {
        float h = 0;
        for (int t = 1; t <= 2; t++) if (owned[d.id][t]) h += d.upgrades[t].hullBonus;
        return h;
    }

    /** Extra mitigation the owned tiers give, and how much of FIRE they turn aside (Kite's 2.3 is made for the burning loka). */
    public float armor(ShipDef d) {
        float a = 0;
        for (int t = 1; t <= 2; t++) if (owned[d.id][t]) a += d.upgrades[t].armor;
        return a;
    }

    public float burnResist(ShipDef d) {
        float a = 0;
        for (int t = 1; t <= 2; t++) if (owned[d.id][t]) a = Math.max(a, d.upgrades[t].burnResist);
        return a;
    }

    public float shieldBonus(ShipDef d) {
        float s = 0;
        for (int t = 1; t <= 2; t++) if (owned[d.id][t]) s += d.upgrades[t].shieldBonus;
        return s;
    }
}