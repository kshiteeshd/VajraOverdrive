package vajra;

import com.badlogic.gdx.Input;

/** THE KEYS THE PILOT MAY CHOOSE: every flight action that has a key of its own, with the key it ships on. A pilot's own choices live in their Settings (an int a slot, 0 = the default); the arrow keys
 *  and the right Shift stay as second keys for movement and boost whatever is chosen. The hangar's own keys (W/S/A/D/ENTER/TAB/ESC) and the push-to-talk V are not here. */
public enum Keymap {
    UP("MOVE UP", Input.Keys.W), DOWN("MOVE DOWN", Input.Keys.S), LEFT("MOVE LEFT", Input.Keys.A), RIGHT("MOVE RIGHT", Input.Keys.D),
    TURN_LEFT("TURN LEFT", Input.Keys.Q), TURN_RIGHT("TURN RIGHT", Input.Keys.E),
    FIRE("FIRE", Input.Keys.SPACE), BOOST("BOOST", Input.Keys.SHIFT_LEFT), SHIELD("SHIELD", Input.Keys.G),
    WEAPON_NEXT("NEXT WEAPON", Input.Keys.V), WEAPON_PREV("PREVIOUS WEAPON", Input.Keys.B),
    ASTRA("INVOKE ASTRA", Input.Keys.F), ASTRA_CYCLE("CHOOSE ASTRA", Input.Keys.X),
    HULL_NEXT("NEXT HULL", Input.Keys.Z), REPAIR("USE REPAIR", Input.Keys.NUM_1);

    public final String label;
    public final int def;

    Keymap(String label, int def) {
        this.label = label;
        this.def = def;
    }

    public static final int COUNT = values().length;

    /** The key now bound to this action in these settings. */
    public int key(Settings s) {
        int k = s.keys != null && s.keys.length > ordinal() ? s.keys[ordinal()] : 0;
        return k == 0 ? def : k;
    }

    /** A printable name for the key bound to this action. */
    public String name(Settings s) {
        return keyName(key(s));
    }

    public static String keyName(int keycode) {
        String n = Input.Keys.toString(keycode);
        if (n == null) return "?";
        switch (n) {
            case "Space": return "SPACE";
            case "Shift-Left": return "L-SHIFT";
            case "Shift-Right": return "R-SHIFT";
            case "Control-Left": return "L-CTRL";
            case "Alt-Left": return "L-ALT";
            default: return n.toUpperCase();
        }
    }

    /** Bind `a` to `key`. A key another action holds changes hands: that action takes the key `a` had (a swap, so no two actions ever share a key and none is left without one). */
    public static void bind(Settings s, Keymap a, int key) {
        if (s.keys == null || s.keys.length < COUNT) s.keys = java.util.Arrays.copyOf(s.keys == null ? new int[0] : s.keys, COUNT);
        int old = a.key(s);
        for (Keymap o : values()) if (o != a && o.key(s) == key) s.keys[o.ordinal()] = old == o.def ? 0 : old;
        s.keys[a.ordinal()] = key == a.def ? 0 : key;
    }

    /** Every action back on the key it ships on. */
    public static void reset(Settings s) {
        s.keys = new int[COUNT];
    }
}
