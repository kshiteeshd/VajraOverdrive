package vajra.world;

/** THE BALANCE: every number that says how much a loka pays and how hard it bites, in one place (docs/ECONOMY.md has the reasoning and the tables).
 *
 *  <p>Two curves run through the seven lokas. DIFFICULTY is {@link LokaTable}'s hull and fire multipliers (about x1.3 a loka), graded again INSIDE
 *  a loka by {@link #ramp}: its first band is gentler than its average, its last band harsher, so every loka teaches before it tests and the
 *  step from one loka into the next is never a cliff. INCOME is {@link #gross}: what a thorough clear of a loka pays in coins, growing x1.28 a
 *  loka; it is split into SHARES (kills, chests, caches, the ruler's purse), and each share is paid out by the thing that earns it, so a loka
 *  built with more chests pays each one less, and a loka built with more enemies pays each one less - the budget is the loka's, not the item's.
 *  A new loka is balanced by meeting its budget, which {@code BalanceTest} measures from the generated world. */
public final class Balance {
    private Balance() {}

    /** Coins a thorough clear of Atala pays, and the growth per loka after it. */
    public static final double GROSS_BASE = 1700, GROWTH = 1.28;
    /** Where a clear's coins come from. Kills are paid by LootSystem (2/4/6 by rank x sqrt of the loka's hull scale); the rest by the numbers below. */
    public static final double SHARE_KILLS = 0.34, SHARE_CHESTS = 0.26, SHARE_CACHES = 0.26, SHARE_PURSE = 0.14;
    /** A pilot who plays the loka the way people do - most of it, not all - keeps this share of a thorough clear. */
    public static final double TYPICAL = 0.45;

    /** Chests, caches and enemies the loka is BUILT with (a built loka's row is pinned by BalanceTest; an unbuilt loka's is the plan). */
    public static final int[] CHESTS = {10, 19, 16, 16, 16, 16, 16};
    public static final int CACHES = 5;

    /** The first and last band of each loka in the campaign world's region list (the difficulty ramp runs across them). */
    public static final int[] BAND_FIRST = {1, 6, 12, 18, 24, 30, 36};
    public static final int[] BAND_LAST = {5, 11, 17, 23, 29, 35, 41};

    /** Hull and fire multiplier at the first and last band of a loka, around 1.0 at its middle. */
    public static final float RAMP_LOW = 0.88f, RAMP_HIGH = 1.12f;
    /** Vitala's own hull multiplier, on top of its loka's: its units are forge-tempered (the first loka that hurts), a leader more so. */
    public static final float VITALA_HULL = 2.1f, VITALA_LEADER = 1.3f;

    /** Coins a thorough clear of loka `l` pays. */
    public static double gross(int l) {
        return GROSS_BASE * Math.pow(GROWTH, l);
    }

    /** Coins a typical pilot actually banks from loka `l`. */
    public static double keep(int l) {
        return gross(l) * TYPICAL;
    }

    /** The purse the ruler's fall pays. */
    public static int purse(int l) {
        return (int) Math.round(gross(l) * SHARE_PURSE / 5.0) * 5;
    }

    /** One hidden cache's coins. */
    public static int cacheCoins(int l) {
        return (int) Math.round(gross(l) * SHARE_CACHES / CACHES / 5.0) * 5;
    }

    /** The coins of one chest, +-30% around the loka's share spread over its chests. */
    public static int chestCoins(int l, java.util.Random rng) {
        double avg = gross(l) * SHARE_CHESTS / CHESTS[Math.max(0, Math.min(CHESTS.length - 1, l))];
        return Math.max(5, (int) Math.round(avg * (0.7 + 0.6 * rng.nextDouble())));
    }

    /** The same grade for the enemy's FIRE, at half strength: fire climbs only x1.2 a loka, so a full-strength ramp would make a loka's last band
     *  out-bite the next loka's first. */
    public static float rampFire(int loka, int region) {
        return 1f + (ramp(loka, region) - 1f) * 0.5f;
    }

    /** The in-loka grade (RAMP_LOW .. RAMP_HIGH) of a band: 0 at the loka's first band, 1 at its last. */
    public static float ramp(int loka, int region) {
        int first = BAND_FIRST[loka], last = BAND_LAST[loka];
        float t = Math.max(0f, Math.min(1f, (region - first) / (float) Math.max(1, last - first)));
        return RAMP_LOW + (RAMP_HIGH - RAMP_LOW) * t;
    }
}
