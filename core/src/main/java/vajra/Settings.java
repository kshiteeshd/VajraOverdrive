package vajra;

import com.badlogic.gdx.utils.Json;

/** The pilot's settings as plain data. Ownership lives with the {@link vajra.profile.Profile} (each pilot has their own), which serializes them inside profiles.json; Settings itself is pure:
 *  serialize/deserialize are string in / string out so the roundtrip tests run headless, and out-of-range values clamp so a hand-edited file can't break feel.
 *
 *  <p>Five groups: DISPLAY (vsync, the frame cap, the window's size, the frame readout), AUDIO (four volumes, kept for the sound that is coming), GAMEPLAY (shake, popup size, dialogue speed, the
 *  tutorial hints), CONTROLS (the keys: see {@link Keymap}) and ACCESSIBILITY (reduced flashing, gentler camera). */
public final class Settings {
    public float shake = 1f;      // camera-shake multiplier, 0..1
    public float popupScale = 1f; // floating-text size multiplier, 0..2
    public float master = 0.8f, music = 0.6f, sfx = 0.9f, voice = 1f;   // volumes, 0..1
    public float textSpeed = 1f;  // how fast dialogue types, 0.5..2
    public boolean hints = true;  // the tutorial: the tour of the instruments, the first sight of a lamp or a bay, the lessons that come with the flight
    public boolean reduceFlash = false;   // no white flashes and no flaring light bands (the shattering glass, the door, a death's burst)
    public boolean gentleCamera = false;  // the camera does not hold on a boss's astra or sweep to a gate: it stays with the hull
    public boolean vsync = true;
    public int fpsCap = 60;       // 0 = unlimited
    public int windowW = 1280, windowH = 720;
    public boolean showFps = false;
    public int[] keys = new int[Keymap.COUNT];

    /** The game's own switch for the tutorial (the tests turn it off with -Dvajra.tutorial=false): a pilot's hints setting can only ask for less than this allows. */
    public static boolean tutorialAllowed = !"false".equals(System.getProperty("vajra.tutorial"));

    public static final int[] FPS_CAPS = {30, 60, 90, 120, 144, 0};
    public static final int[][] WINDOWS = {{1280, 720}, {1600, 900}, {1920, 1080}};

    public static class Data {
        public float shake = 1, popupScale = 1;
        public float master = 0.8f, music = 0.6f, sfx = 0.9f, voice = 1f, textSpeed = 1f;
        public boolean hints = true, reduceFlash, gentleCamera, vsync = true, showFps;
        public int fpsCap = 60, windowW = 1280, windowH = 720;
        public int[] keys;
        public int hudStyle; // retired (the one HUD is final); still read so an older profiles.json loads
    }

    public static String serialize(Settings s) {
        Data d = new Data();
        d.shake = s.shake;
        d.popupScale = s.popupScale;
        d.master = s.master;
        d.music = s.music;
        d.sfx = s.sfx;
        d.voice = s.voice;
        d.textSpeed = s.textSpeed;
        d.hints = s.hints;
        d.reduceFlash = s.reduceFlash;
        d.gentleCamera = s.gentleCamera;
        d.vsync = s.vsync;
        d.fpsCap = s.fpsCap;
        d.windowW = s.windowW;
        d.windowH = s.windowH;
        d.showFps = s.showFps;
        d.keys = s.keys.clone();
        return new Json().toJson(d);
    }

    /** Read a save back; out-of-range values clamp so a hand-edited file can't break feel. */
    public static void deserialize(Settings s, String json) {
        Data d;
        try {
            d = new Json().fromJson(Data.class, json);
        } catch (RuntimeException e) {
            return; // a corrupt file stays at defaults
        }
        if (d == null) return;
        s.shake = clamp(d.shake, 0, 1);
        s.popupScale = clamp(d.popupScale, 0, 2);
        s.master = clamp(d.master, 0, 1);
        s.music = clamp(d.music, 0, 1);
        s.sfx = clamp(d.sfx, 0, 1);
        s.voice = clamp(d.voice, 0, 1);
        s.textSpeed = clamp(d.textSpeed, 0.5f, 2f);
        s.hints = d.hints;
        s.reduceFlash = d.reduceFlash;
        s.gentleCamera = d.gentleCamera;
        s.vsync = d.vsync;
        s.fpsCap = validCap(d.fpsCap);
        s.windowW = Math.max(960, Math.min(3840, d.windowW));
        s.windowH = Math.max(540, Math.min(2160, d.windowH));
        s.showFps = d.showFps;
        s.keys = new int[Keymap.COUNT];
        if (d.keys != null) for (int i = 0; i < Keymap.COUNT && i < d.keys.length; i++) s.keys[i] = Math.max(0, d.keys[i]);
    }

    private static int validCap(int c) {
        for (int v : FPS_CAPS) if (v == c) return c;
        return 60;
    }

    /** Every setting back to how it ships (the keys too); the pilot's name and book are not settings. */
    public static void reset(Settings s) {
        deserialize(s, serialize(new Settings()));
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
