package vajra.world;

/** BALA'S MAYA-ASTRA, the clock of it: Bala, son of Maya, forms a mandala and the whole screen is a MIRROR that cracks outward from the mandala's heart, into great pieces;
 *  every piece then shows the whole frame turned to its own angle, as the pieces of a broken mirror do. Pure illusion until the third casting. (screen/ShatterFx draws it.)
 *
 *  <p>He casts it three times, once for each of his phases, and it is never the same:
 *  <ul><li>{@link #ONE_BY_ONE} (first): the pieces turn one at a time, a snap each, and in the end the glass mends;
 *  <li>{@link #ALL_AT_ONCE} (second): every piece turns in the same instant, and the glass mends;
 *  <li>{@link #DOORS} (third): the pieces turn one at a time and are then DOORS: each opens on another part of Atala loka, and the hull is carried through the door whose piece it is
 *      over when the doors are counted. Bala stays where he is; the way back to him is the fight.</ul>
 *
 *  <pre>
 *  0 ........ FORM .. 2.6   the mandala forms round Bala (he stands still, silent, throughout)
 *  2.8 ..... CRACK .. 6.2   the heart of the glass is his
 *  4.8 ..... TILT          one by one over 6.4 s, or all at once at 4.8
 *  ONE_BY_ONE: heals 13.0-14.4, ends 14.6        ALL_AT_ONCE: heals 9.5-10.9, ends 11.1
 *  DOORS: 11.0 windows are drawn   13.8 the doors open (1.2 s)   19.0 the hull's door is chosen   20.0 through it   20.8 ends
 *  </pre> */
public final class Maya {
    public static final int ONE_BY_ONE = 0, ALL_AT_ONCE = 1, DOORS = 2;
    public static final float FORM = 2.6f, CRACK_AT = 2.8f, CRACK_LEN = 3.4f, TILT_AT = 4.8f, TILT_SPAN = 6.4f, HEAL_LEN = 1.4f;
    public static final float PREP_AT = 11.0f, OPEN_AT = 13.8f, OPEN_LEN = 1.2f, PICK_AT = 19.0f, ENTER_LEN = 1.0f, TELE_AT = 20.0f, DOORS_END = 20.8f;
    public static final int DOOR_COUNT = 24;

    public boolean active;
    public float t, x, y;
    public long seed;
    public int mode;
    /** The one whose astra it is: the mandala and the cracks' heart stay on him, and he holds still until it ends. */
    public vajra.enemy.Enemy boss;
    /** DOORS: where each piece opens (world px), the piece the hull was over at the count (-1 until then), and whether the hull has been carried. */
    public final float[] doorX = new float[DOOR_COUNT], doorY = new float[DOOR_COUNT];
    public int doors, chosen = -1;
    public boolean carried;
    /** Where the hull was when the windows began: every window follows the hull's own movement from here, as if it were flying there, and the door that takes it keeps that offset. */
    public float originX, originY;
    public boolean originSet;

    public void start(vajra.enemy.Enemy by, long seed, int mode) {
        boss = by;
        start(by.x, by.y, seed, mode);
    }

    public void start(float x, float y, long seed, int mode) {
        this.x = x;
        this.y = y;
        this.seed = seed;
        this.mode = mode;
        t = 0f;
        active = true;
        doors = 0;
        chosen = -1;
        carried = false;
        originSet = false;
    }

    public void step(float dt) {
        if (!active) return;
        t += dt;
        if (boss != null && !boss.dead) { x = boss.x; y = boss.y; }
        if (t > end()) { active = false; boss = null; }
    }

    public float healAt() { return mode == ONE_BY_ONE ? TILT_AT + TILT_SPAN + 1.8f : mode == ALL_AT_ONCE ? 9.5f : 1e9f; }

    public float end() { return mode == DOORS ? DOORS_END : healAt() + HEAL_LEN + 0.2f; }

    /** The mandala's charge, 0..1 (it is drawn only while the glass is cracking). */
    public float form() { return Math.min(1f, t / FORM); }

    /** How much the camera is held on the one who forges it, 0..1: it goes to him in half a second, stays while the mandala forms, and comes back to the hull over the next second. */
    public float focus() {
        if (!active || boss == null) return 0f;
        float in = clamp(t / 0.5f), out = 1f - clamp((t - (FORM + 0.3f)) / 1.0f);
        float k = in * out;
        return k * k * (3f - 2f * k);
    }

    /** How far the cracks have run, 0..1. */
    public float crack() { return clamp((t - CRACK_AT) / CRACK_LEN); }

    /** 1 until the glass mends, then falling to 0: every piece's turn is multiplied by it. */
    public float fade() { return 1f - clamp((t - healAt()) / HEAL_LEN); }

    /** Seconds since the tilt began (may be negative). */
    public float tiltTime() { return t - TILT_AT; }

    /** DOORS: how open the doors are, 0..1. */
    public float open() { return mode == DOORS ? clamp((t - OPEN_AT) / OPEN_LEN) : 0f; }

    /** DOORS: how far the chosen door has come to fill the screen, 0..1. */
    public float enter() { return mode == DOORS ? clamp((t - PICK_AT) / ENTER_LEN) : 0f; }

    /** DOORS: 1 while the door fills the screen, then falling to 0 over 0.6 s after the hull has been carried through it. */
    public float doorVeil() { return mode != DOORS ? 0f : t < TELE_AT ? enter() : clamp(1f - (t - TELE_AT) / 0.6f); }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
