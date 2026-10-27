package vajra.world;

import vajra.ship.Hangar;

/** THE ASTRAS: what a pilot can loose, what it costs, and what each one is.
 *
 *  <p>An astra is a divine weapon called down by an invocation, aimed, and loosed. In this game the rite has
 *  three beats and one price:
 *  <ol>
 *   <li><b>Invoke</b> - HOLD F. A cage of cubes gathers round the hull and the hull goes heavy. The longer you hold,
 *       the more cubes, the harder they hit, and the more foes in front of you are marked (locked).</li>
 *   <li><b>Aim</b> - the locks are the foes in view nearest the line of the nose, so the nose is the aim.</li>
 *   <li><b>Loose</b> - LET GO of F. The cubes fly, each one at a locked foe. Let go too early and the mantra
 *       falters: nothing flies and nothing is spent.</li>
 *  </ol>
 *  <b>The price</b> is {@link #COST} shards of the astra's own element, paid at the moment of release (not when you
 *  start, so a hull that dies mid-invocation loses nothing). There is no forge to buy and no stored charge: the shard
 *  pile IS the fuel, which makes selling shards a real choice against keeping them.
 *
 *  <p>There are five astras, one per tatva, each with its own character, and a sixth, the BRAHMASTRA, that needs five
 *  shards of every element and looses all five at once. This class is only data and pure rules; the world runs them
 *  ({@link World#astraBegin}, {@link World#astraLoose}). */
public final class Astra {
    private Astra() {}

    public static final int AKASHA = 0, VAYU = 1, AGNI = 2, JALA = 3, PRITHVI = 4, BRAHMA = 5, PASHUPATA = 6;
    /** The five elemental astras, the Brahmastra, and the Pashupatastra (Rasatala's: fire and stone). */
    public static final int KINDS = 7, ELEMENTS = 5;
    /** How far down the lokas each astra lives, 0 (Atala) to 6 (Patala): the higher it is, the bigger and richer its mandala, bolts and release are drawn. */
    public static final int[] TIER = {0, 4, 1, 2, 3, 6, 5};
    /** Shards of the astra's element a release costs (the Brahmastra: five of each). */
    public static final int COST = 5;

    /** The shortest invocation that counts: let go before this and the mantra falters (nothing is spent). */
    public static final float WIND_MIN = 0.9f;
    /** The invocation at full strength. Holding on past it keeps the astra full and waiting. */
    public static final float WIND_FULL = 3.4f;
    /** Seconds before another astra can be invoked after a release. */
    public static final float COOLDOWN = 6f;
    /** Seconds before another can be invoked after a faltered mantra, so a stuttering key cannot flicker the cage. */
    public static final float FALTER_LOCKOUT = 0.5f;
    /** How far from the hull a foe can be locked, and the half-extents of the view they must be inside. */
    public static final float LOCK_RANGE = 900f, VIEW_HALF_W = 700f, VIEW_HALF_H = 420f;

    public static final String[] NAME = {"AKASHA ASTRA", "VAYU ASTRA", "AGNI ASTRA", "JALA ASTRA", "PRITHVI ASTRA", "BRAHMASTRA", "PASHUPATASTRA"};
    public static final String[] EPITHET = {"THE HUNTER", "THE GALE", "THE PYRE", "THE FLOOD", "THE MOUNTAIN", "THE LAST ARROW", "THE THIRD EYE"};
    public static final String[] EFFECT = {
            "SEEKS ITS PREY THROUGH ROCK. THE SHIELD IS REFILLED.",
            "FLIES FAST AND CHAINS FROM FOE TO FOE. THE BOOST IS REFILLED.",
            "BURSTS ON IMPACT AND BURNS THE CROWD ROUND IT.",
            "EVERY HIT MENDS THE HULL.",
            "FEW CUBES, HEAVY BLOWS. STONE SKIN FOR TWO SECONDS.",
            "ALL FIVE AT ONCE. FIVE SHARDS OF EVERY ELEMENT.",
            "THE THIRD EYE OPENS: A BEAM STEERED BY THE NOSE. ARMOUR AND ROCK DO NOT STOP IT. FIVE SHARDS OF FIRE AND STONE."};

    // per elemental kind, at FULL strength: cubes, damage per cube, speed, steering, chain hits, splash radius,
    // hull mended per hit. (The Brahmastra is built from these, half the cubes of each.)
    public static final int[] CUBES = {16, 10, 8, 12, 6};
    public static final float[] DAMAGE = {70f, 55f, 85f, 50f, 150f};
    public static final float[] SPEED = {900f, 1500f, 850f, 900f, 800f};
    public static final float[] TURN = {5.5f, 7f, 5f, 5.5f, 4.5f};
    public static final int[] CHAIN = {0, 3, 0, 0, 0};
    public static final float[] SPLASH = {0f, 0f, 120f, 0f, 0f};
    public static final float[] HEAL_PER_HIT = {0f, 0f, 0f, 3f, 0f};
    /** Splash damage as a share of the hit's. */
    public static final float SPLASH_SHARE = 0.6f;
    /** Seconds of stone skin (full invulnerability) the Prithvi astra gives at release; the Brahmastra half. */
    public static final float STONE_SKIN = 2f;

    /** The tatva whose shards pay for this kind, or -1 for the Brahmastra (every one). */
    public static int element(int kind) { return kind >= 0 && kind < ELEMENTS ? kind : -1; }

    /** The shards a kind has to pay with: its own element's; the Pashupatastra's fire and stone together; the Brahmastra's smallest element. */
    public static int stock(Hangar h, int kind) {
        if (kind == PASHUPATA) return h.shards[AGNI] + h.shards[PRITHVI];
        if (kind >= ELEMENTS) { int m = Integer.MAX_VALUE; for (int t = 0; t < ELEMENTS; t++) m = Math.min(m, h.shards[t]); return m; }
        return h.shards[kind];
    }

    /** How many times this astra could be loosed right now from the shard pile. */
    public static int releases(Hangar h, int kind) {
        if (kind < 0 || kind >= KINDS || !h.canForge()) return 0;
        if (kind == PASHUPATA) return (h.shards[AGNI] + h.shards[PRITHVI]) / COST;
        if (kind < ELEMENTS) return h.shards[kind] / COST;
        int n = Integer.MAX_VALUE;
        for (int t = 0; t < ELEMENTS; t++) n = Math.min(n, h.shards[t] / COST);
        return n;
    }

    public static boolean canLoose(Hangar h, int kind) { return releases(h, kind) > 0; }

    /** Take the price out of the pile. False (and nothing taken) when it cannot be paid. */
    public static boolean pay(Hangar h, int kind) {
        if (!canLoose(h, kind)) return false;
        if (kind == PASHUPATA) { // five shards of fire and stone, from the deeper pile first
            for (int i = 0; i < COST; i++) h.shards[h.shards[AGNI] >= h.shards[PRITHVI] ? AGNI : PRITHVI]--;
        } else if (kind < ELEMENTS) h.shards[kind] -= COST;
        else for (int t = 0; t < ELEMENTS; t++) h.shards[t] -= COST;
        return true;
    }

    /** Cubes in an invocation of this kind at full strength. The Brahmastra is half of each element's, at least two. */
    public static int fullCubes(int kind) {
        if (kind == PASHUPATA) return 3; // three small tridents ride the mandala; the release is a beam, not cubes
        if (kind < ELEMENTS) return CUBES[kind];
        int n = 0;
        for (int t = 0; t < ELEMENTS; t++) n += brahmaShare(t);
        return n;
    }

    /** The cubes one element contributes to the Brahmastra. */
    public static int brahmaShare(int element) { return Math.max(2, Math.round(CUBES[element] * 0.5f)); }

    /** The element of the i-th cube of an invocation: the kind itself, or, for the Brahmastra, the elements in turn
     *  in blocks, so every element is present at any charge. */
    public static int cubeElement(int kind, int i) {
        if (kind == PASHUPATA) return i % 2 == 0 ? AGNI : PRITHVI;
        if (kind < ELEMENTS) return kind;
        return i % ELEMENTS; // interleaved: a half-charged Brahmastra still carries all five
    }

    /** How strongly the invocation has built, 0 at the first instant to 1 at {@link #WIND_FULL}. */
    public static float k(float charge) { return Math.max(0f, Math.min(1f, charge / WIND_FULL)); }

    /** Cubes gathered after `charge` seconds: a third of them at the start, all at full strength. */
    public static int cubesAt(int kind, float charge) {
        int full = fullCubes(kind);
        return Math.max(1, Math.round(full * (0.35f + 0.65f * k(charge))));
    }

    /** How hard a release lands, 0..1 across the span from the shortest invocation that counts to a full one. */
    public static float power(float charge) {
        return Math.max(0f, Math.min(1f, (charge - WIND_MIN) / (WIND_FULL - WIND_MIN)));
    }

    /** Damage multiplier for a release of `power`: 70% of the listed damage at the shortest, 130% at full. */
    public static float damageMult(float power) { return 0.7f + 0.6f * power; }

    /** Foes locked after `charge` seconds: one for every cube gathered, at most. */
    public static int locksAt(int kind, float charge) { return cubesAt(kind, charge); }

    /** The radius of an astra's mandala at charge k (0..1): it tightens as the charge builds and is bigger the deeper the astra's loka. The mandala is drawn at exactly this size
     *  and the same circle is the pilot's shield while it forms (every enemy round that touches it is destroyed). */
    public static float mandalaRadius(int kind, float k) {
        return (128f - 62f * Math.max(0f, Math.min(1f, k))) * (1f + 0.12f * TIER[Math.max(0, Math.min(KINDS - 1, kind))]);
    }

    public static String key(int kind) { return kind >= 0 && kind < KINDS ? NAME[kind] : NAME[BRAHMA]; }

    /** The Pashupatastra's beam: seconds it burns, damage a second (it ignores armour and rock), half-width and length in px. */
    public static final float GAZE_TIME = 2.2f, GAZE_DPS = 420f, GAZE_HALF = 26f, GAZE_LEN = 1250f;
}
