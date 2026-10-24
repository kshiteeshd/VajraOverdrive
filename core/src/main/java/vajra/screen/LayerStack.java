package vajra.screen;

/** THE LAYERED WALLS of the test area: the same wall tiles, no background, stacked one behind another like cards. Layer 0 is the
 *  play layer (scale 1); every layer behind it is the SAME walls again, smaller by `shrink` about the screen centre (so when the
 *  camera moves it slides by only `shrink^k` of the distance: that is the parallax), darker (it is further off and in the haze)
 *  and peeking out down and to the left of the one in front, as in the sketch of a stack of cards. Pure numbers, so the stack
 *  can be pinned without a window. */
final class LayerStack {
    private LayerStack() {}

    static final float MIN_SHRINK = 0.85f, MAX_SHRINK = 0.99f;
    static final int MIN_LAYERS = 0, MAX_LAYERS = 30;

    /** How big layer k is against the play layer: shrink^k. */
    static float scale(int k, float shrink) { return (float) Math.pow(shrink, k); }

    /** The camera's lean: the vanishing point rides toward where the hull is going, by at most this many screen px. */
    static final float MAX_LEAN = 22f;

    /** How far (screen px) a layer of scale s drifts when the vanishing point is `lean` px off centre: the far layers move most. */
    static float leanShift(float lean, float s) { return lean * (1f - s); }

    static final float MAX_PEEK = 40f;

    /** The zoom multiplier that makes the far floor slide across the screen at `target` times the camera's move: the shader already
     *  scales the ground by Altitude.ground(floorFly), so the camera must make up the rest. (The floor and the last wall layer used to
     *  move at different speeds; they now share one number.) */
    static float floorZoomMul(float floorFly, float target) { return Altitude.ground(floorFly) / target; }

    /** The speed the floor should have, as a fraction of the camera's move: a half layer in front of the last wall layer, eased in from
     *  the open meadow's own ground speed as the shaft opens (`open` 0..1). */
    static float floorSpeed(float layers, float shrink, float openGround, float open) {
        float target = (float) Math.pow(shrink, layers + 0.5f);
        return openGround + (target - openGround) * open;
    }

    /** How far layer k sits down and to the left of the play layer on the screen, in screen px: `peek` for each layer back. */
    static float shiftX(int k, float peek) { return -peek * k; }

    static float shiftY(int k, float peek) { return -peek * 0.85f * k; }

    /** Layer k's tint out of `layers` behind the play layer: white at the front, a deep blue-black at the back. */
    static float[] tint(int k, int layers, float[] out) {
        return tint(k, layers, DEFAULT_DEEP, out);
    }

    static final float[] DEFAULT_DEEP = {0.12f, 0.16f, 0.34f};

    /** The same, toward a given deepest colour (each band has its own). */
    static float[] tint(int k, float layers, float[] deep, float[] out) {
        float t = layers <= 0 ? 0f : Math.min(1f, k / layers);
        t = t * (2f - t);                                    // the haze thickens quickly, then settles
        for (int i = 0; i < 3; i++) out[i] = 1f - (1f - deep[i]) * t;
        return out;
    }
}
