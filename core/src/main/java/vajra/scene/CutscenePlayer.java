package vajra.scene;

import java.util.List;

/** A cutscene's clock, with no Gdx in it so it runs headless. The camera eases onto each
 *  shot's subject, the speaker's line types out, a press completes the line and then moves
 *  on, and after the last line the camera eases back to wherever play had it.
 *
 *  <p>camX / camY / camZoom are what the screen should show this frame. */
public final class CutscenePlayer {
    public static final float MOVE = 0.7f;   // default seconds to ease onto the next subject (Shot.moveSecs)
    public static final float OUTRO = 0.5f;  // seconds to ease back to play
    public static final float CPS = 24f;     // typing speed, characters per second: slow enough to be read as it comes
    /** The pilot's dialogue speed (Settings.textSpeed): a multiplier on CPS. */
    public static float speed = 1f;
    public static final float BANDS_IN = 0.45f; // the letterbox bands slide in over this

    private final List<Shot> shots;
    private int shot, line;
    private float chars;        // characters of the current line typed so far
    private float move;         // seconds into the current camera move
    private float fromX, fromY, fromZoom;
    private boolean outro, done;
    private float age;          // seconds since the scene began

    public float camX, camY, camZoom;

    public CutscenePlayer(List<Shot> shots, float camX, float camY, float camZoom) {
        this.shots = shots;
        this.camX = fromX = camX;
        this.camY = fromY = camY;
        this.camZoom = fromZoom = camZoom;
        if (shots.isEmpty()) done = true;
    }

    public boolean done() { return done; }

    /** How far the black letterbox bands are drawn on, 0..1: they slide in as the scene opens,
     *  stay while it plays, and slide away over the way out. */
    public float bands() {
        if (done) return 0f;
        if (outro) return Math.max(0f, 1f - move / OUTRO);
        return Math.min(1f, age / BANDS_IN);
    }
    public Shot shot() { return outro || done ? null : shots.get(shot); }

    /** The line being said, typed so far; "" while the camera is still travelling. */
    public String text() {
        Shot s = shot();
        if (s == null || move < s.moveSecs) return "";
        String full = s.lines[line];
        return full.substring(0, Math.min(full.length(), (int) chars));
    }

    /** The whole current line, typed or not (so a box can be sized before it fills). */
    public String fullText() {
        Shot s = shot();
        return s == null ? "" : s.lines[line];
    }

    /** True once the whole line is on screen and a press will move on. */
    public boolean lineComplete() {
        Shot s = shot();
        return s != null && move >= s.moveSecs && chars >= s.lines[line].length();
    }

    public void advance(float dt, float liveX, float liveY, float liveZoom) {
        if (done) return;
        move += dt;
        age += dt;
        if (outro) {
            ease(Math.min(1, move / OUTRO), liveX, liveY, liveZoom);
            if (move >= OUTRO) done = true;
            return;
        }
        Shot s = shots.get(shot);
        ease(Math.min(1, move / s.moveSecs), s.x, s.y, s.zoom);
        if (move >= s.moveSecs) chars += CPS * speed * dt;
    }

    /** A key or click: finish the line, else the next line, else the next shot, else leave. */
    public void press() {
        Shot s = shot();
        if (s == null || move < s.moveSecs) return;
        if (chars < s.lines[line].length()) {
            chars = s.lines[line].length();
        } else if (++line >= s.lines.length) {
            line = 0;
            if (++shot >= shots.size()) beginOutro();
            else beginMove();
            chars = 0;
        } else {
            chars = 0;
        }
    }

    /** ESC: straight to the way back out. */
    public void skip() {
        if (!outro && !done) beginOutro();
    }

    private void beginMove() {
        fromX = camX;
        fromY = camY;
        fromZoom = camZoom;
        move = 0;
        if (!outro) { // the same framing as the last shot: no travel, so no waiting on it
            Shot s = shots.get(shot);
            if (Math.abs(s.x - camX) < 1 && Math.abs(s.y - camY) < 1 && Math.abs(s.zoom - camZoom) < 0.005f)
                move = s.moveSecs;
        }
    }

    /** Seconds since this shot began (keeps counting while the line is read). */
    public float shotT() { return move; }

    /** How far the camera is through the current shot's move, 0..1 (1 once it has landed). */
    public float moveT() {
        Shot s = shot();
        return s == null ? 1 : Math.min(1, move / s.moveSecs);
    }

    private void beginOutro() {
        outro = true;
        beginMove();
    }

    private void ease(float t, float tx, float ty, float tz) {
        float k = t * t * (3 - 2 * t); // smoothstep
        camX = fromX + (tx - fromX) * k;
        camY = fromY + (ty - fromY) * k;
        camZoom = fromZoom + (tz - fromZoom) * k;
    }
}
