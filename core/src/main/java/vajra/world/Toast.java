package vajra.world;

/** A job's word to the pilot: a title and one line, held on the HUD for a few seconds. It replaces
 *  the sentences that used to float up over the hull - a job beginning, a job paid. */
public final class Toast {
    public static final float LIFE = 4.5f, SLIDE = 0.35f;

    public final String title, line;
    public float age;

    public Toast(String title, String line) {
        this.title = title;
        this.line = line;
    }

    public boolean done() { return age >= LIFE; }

    /** 0..1 slide in and out. */
    public float k() {
        return Math.min(1f, Math.min(age / SLIDE, (LIFE - age) / SLIDE));
    }
}
