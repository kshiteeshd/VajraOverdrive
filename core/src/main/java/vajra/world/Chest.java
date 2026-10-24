package vajra.world;

/** A TREASURE CHEST (docs/TREASURE_CHESTS.md): every loka hides its own, in the places off the road. You open one by flying into it; it pays coins
 *  and ONE useful thing, rolled from the loka's own table. Atala's are pearl-and-gold; Vitala's are black basalt bound in molten gold, and they
 *  sit in the vaults and dens of the side caverns. */
public final class Chest {
    /** What a chest can give besides coins. */
    public static final int MEND = 0, SHARDS = 1, LIFE = 2, SURGE = 3;
    public static final String[] ITEM = {"HULL MENDED", "SHARDS", "A LIFE", "SURGE"};
    public static final float TOUCH_R = 34f;
    /** What a side cavern of Vitala holds (MapGen.pockets): a VAULT a chest, a WELL an ember well, a DEN a guarded chest. */
    public static final int VAULT = 0, WELL = 1, DEN = 2;

    public final float x, y;
    /** 0 Atala (pearl), 1 Vitala (basalt and molten gold). */
    public final int loka;
    /** A DEN's chest is guarded: it does not open until its ambush has fallen (0 = free to open). */
    public int guards;
    public boolean open;
    /** Seconds since it opened (the renderer plays the lid's sheet from it). */
    public float openT;
    /** Which pocket of Vitala holds it, -1 for none. */
    public final int pocket;

    public Chest(float x, float y, int loka, int pocket) {
        this.x = x;
        this.y = y;
        this.loka = loka;
        this.pocket = pocket;
    }

    public boolean touches(float px, float py) {
        float dx = px - x, dy = py - y;
        return dx * dx + dy * dy <= TOUCH_R * TOUCH_R;
    }

    /** The coin purse of a chest: its share of the loka's budget (Balance), spread over the chests the loka holds. */
    public static int coins(int loka, java.util.Random rng) {
        return Balance.chestCoins(loka, rng);
    }

    /** The one useful thing, from the loka's own table: Atala mends and stocks Akasha shards; Vitala pays more of everything and its rare find is
     *  a SURGE (full boost and shield). Weights are out of 100. */
    public static int item(int loka, java.util.Random rng) {
        int r = rng.nextInt(100);
        if (loka == 0) return r < 45 ? MEND : r < 85 ? SHARDS : LIFE;
        return r < 35 ? MEND : r < 70 ? SHARDS : r < 88 ? SURGE : LIFE;
    }

    /** How many shards a chest of this loka gives. */
    public static int shards(int loka) { return loka == 0 ? 3 : 5; }
}
