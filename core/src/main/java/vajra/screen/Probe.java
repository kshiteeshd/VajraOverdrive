package vajra.screen;

import com.badlogic.gdx.Gdx;

/** A frame profiler for the renderer: off in the game (a boolean test, nothing more), on in the perf test, where each {@link #lap} waits
 *  for the GPU (glFinish) and bills the time since the last lap to a named slot, so the cost of every pass is its own number. */
public final class Probe {
    private Probe() {}

    public static boolean enabled;
    /** F3: show the frame-time readout (see GameScreen). */
    public static boolean showFrame;
    public static final String[] NAMES = {"floor (raycast)", "stack (walls x layers)", "mist+backdrop+shadows", "play layer sprites", "sparks+bars",
            "lights", "ship+effects", "foreground+bullets", "above-hull pass", "motes"};
    public static final long[] NS = new long[NAMES.length];
    private static long last;

    public static void start() {
        if (!enabled) return;
        Gdx.gl.glFinish();
        last = System.nanoTime();
    }

    public static void lap(int slot) {
        if (!enabled) return;
        Gdx.gl.glFinish();
        long now = System.nanoTime();
        NS[slot] += now - last;
        last = now;
    }

    public static void reset() { java.util.Arrays.fill(NS, 0L); }
}
