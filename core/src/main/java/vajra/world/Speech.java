package vajra.world;

import vajra.enemy.Enemy;

/** A person talking mid-flight: a few short lines said one after another from over their hull,
 *  while the world keeps moving. First meetings are cutscenes; this is for everything after - a
 *  job finished, a word in passing. The screen draws it as the same temple bubble the scenes use.
 *
 *  <p>A line types at {@link #CPS}, holds for {@link #HOLD}, then the next one starts. Pure data and
 *  a clock: no drawing here. */
public final class Speech {
    public static final float CPS = 30f;    // a little quicker than a scene: this is read in passing
    public static final float HOLD = 1.7f;  // seconds a finished line stays up
    public static final float FADE = 0.35f; // and fades over its last moments

    public final Enemy who;
    public final String[] lines;
    private int line;
    private float t;

    public Speech(Enemy who, String... lines) {
        this.who = who;
        this.lines = lines;
    }

    public boolean done() { return line >= lines.length; }

    /** The line being said, whole. */
    public String full() { return done() ? "" : lines[line]; }

    /** The line typed so far. */
    public String typed() {
        if (done()) return "";
        return lines[line].substring(0, Math.min(lines[line].length(), (int) (t * CPS)));
    }

    /** How long the current line takes, typing plus hold. */
    public float lineSeconds() {
        return done() ? 0f : lines[line].length() / CPS + HOLD;
    }

    /** 1 while the line is up, easing to 0 over its last FADE seconds. */
    public float alpha() {
        if (done()) return 0f;
        return Math.min(1f, Math.max(0f, (lineSeconds() - t) / FADE));
    }

    public void update(float dt) {
        if (done()) return;
        t += dt;
        if (t >= lineSeconds()) {
            line++;
            t = 0;
        }
    }
}
