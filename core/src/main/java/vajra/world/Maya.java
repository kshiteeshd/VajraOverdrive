package vajra.world;

/** BALA'S MAYA-ASTRA, the clock of it: Bala, son of Maya, forms a mandala and the whole screen is a MIRROR that cracks outward from the mandala's heart, slowly, into great pieces;
 *  every piece then shows the whole frame turned to its own angle, as the pieces of a broken mirror do; at the end the glass mends. Pure illusion: nothing here hurts.
 *  (screen/ShatterFx draws it; the boss starts it at the opening of his second and third phases.)
 *
 *  <pre>
 *  0 ........ FORM .. 2.6   the mandala forms round Bala
 *  2.8 ..... CRACK .. 6.2   cracks run out from its heart, ray by ray, to the edges of the screen
 *  4.8 ..... TILT ... 8.3   piece by piece, from the heart outward, each piece turns to its own angle
 *  12.5 .... HEAL ... 13.9  the pieces turn back and the cracks close
 *  </pre> */
public final class Maya {
    public static final float FORM = 2.6f, CRACK_AT = 2.8f, CRACK_LEN = 3.4f, TILT_AT = 4.8f, TILT_LEN = 3.5f, HEAL_AT = 12.5f, HEAL_LEN = 1.4f, END = 14.0f;

    public boolean active;
    public float t, x, y;
    public long seed;

    public void start(float x, float y, long seed) {
        this.x = x;
        this.y = y;
        this.seed = seed;
        t = 0f;
        active = true;
    }

    public void step(float dt) {
        if (!active) return;
        t += dt;
        if (t > END) active = false;
    }

    /** The mandala's charge, 0..1 (it is drawn only while the glass is cracking). */
    public float form() { return Math.min(1f, t / FORM); }

    /** How far the cracks have run, 0..1. */
    public float crack() { return clamp((t - CRACK_AT) / CRACK_LEN); }

    /** 1 until the glass mends, then falling to 0: the cracks' own brightness and every piece's turn are multiplied by it. */
    public float fade() { return 1f - clamp((t - HEAL_AT) / HEAL_LEN); }

    /** Seconds since the tilt began (may be negative). */
    public float tiltTime() { return t - TILT_AT; }

    private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }
}
