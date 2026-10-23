package vajra.scene;

/** Who speaks in which colour. A bubble's tint tells you at a glance whose word it is, and the same
 *  colours colour the people's sprites' halos and the HUD's toasts. */
public final class Palette {
    private Palette() {}

    /** Indra's thoughts: Akasha lavender. */
    public static final float[] INDRA = {0.82f, 0.76f, 1f};
    /** The people of the loka (the Scout, Maya, Kaliya): sky blue. */
    public static final float[] PERSON = {0.6f, 0.86f, 1f};
    /** The soldiers: ivory. */
    public static final float[] SOLDIER = {1f, 0.95f, 0.78f};
    /** The ruler: gold. */
    public static final float[] RULER = {1f, 0.84f, 0.3f};
}
