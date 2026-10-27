package vajra;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The settings roundtrip, pure string: mutate, serialize, pour into a fresh copy. */
class SettingsTest {
    @Test
    void freshSettingsAreFullIntensity() {
        Settings s = new Settings();
        assertEquals(1f, s.shake, 0.0001f);
        assertEquals(1f, s.popupScale, 0.0001f);
    }

    @Test
    void theSettingsRoundTrip() {
        Settings a = new Settings();
        a.shake = 0.25f;
        a.popupScale = 1.5f;
        Settings b = new Settings();
        Settings.deserialize(b, Settings.serialize(a));
        assertEquals(0.25f, b.shake, 0.0001f);
        assertEquals(1.5f, b.popupScale, 0.0001f);
    }

    @Test
    void outOfRangeValuesClampAndCorruptionStaysDefault() {
        Settings a = new Settings();
        a.shake = 9f;
        a.popupScale = -3f;
        Settings b = new Settings();
        Settings.deserialize(b, Settings.serialize(a));
        assertEquals(1f, b.shake, 0.0001f, "shake clamps to full");
        assertEquals(0f, b.popupScale, 0.0001f, "popup scale clamps to zero");
        Settings c = new Settings();
        Settings.deserialize(c, "{not json at all");
        assertEquals(1f, c.shake, 0.0001f, "a corrupt file stays at defaults");
    }

    @Test
    void everySettingRoundTripsAndClamps() {
        Settings a = new Settings();
        a.master = 0.3f;
        a.music = 0f;
        a.sfx = 2f;
        a.textSpeed = 9f;
        a.hints = false;
        a.reduceFlash = true;
        a.gentleCamera = true;
        a.vsync = false;
        a.fpsCap = 144;
        a.windowW = 1920;
        a.windowH = 1080;
        a.showFps = true;
        Keymap.bind(a, Keymap.FIRE, com.badlogic.gdx.Input.Keys.J);
        Settings b = new Settings();
        Settings.deserialize(b, Settings.serialize(a));
        assertEquals(0.3f, b.master, 1e-4f);
        assertEquals(0f, b.music, 1e-4f);
        assertEquals(1f, b.sfx, 1e-4f, "a volume clamps to full");
        assertEquals(2f, b.textSpeed, 1e-4f, "the dialogue speed clamps to double");
        assertFalse(b.hints);
        assertTrue(b.reduceFlash && b.gentleCamera && !b.vsync && b.showFps);
        assertEquals(144, b.fpsCap);
        assertEquals(1920, b.windowW);
        assertEquals(com.badlogic.gdx.Input.Keys.J, Keymap.FIRE.key(b), "the pilot's own key survives");
        assertEquals(Keymap.UP.def, Keymap.UP.key(b), "the rest stay on their defaults");
        a.fpsCap = 77;
        Settings c = new Settings();
        Settings.deserialize(c, Settings.serialize(a));
        assertEquals(60, c.fpsCap, "an unknown frame limit falls back to 60");
    }

    @Test
    void aKeyAnotherActionHoldsChangesHandsAndResetPutsEverythingBack() {
        Settings s = new Settings();
        Keymap.bind(s, Keymap.FIRE, Keymap.SHIELD.def);                        // FIRE takes G, which SHIELD held
        assertEquals(Keymap.SHIELD.def, Keymap.FIRE.key(s));
        assertEquals(Keymap.FIRE.def, Keymap.SHIELD.key(s), "SHIELD takes the key FIRE had: a swap, nobody left without a key");
        java.util.HashSet<Integer> seen = new java.util.HashSet<>();
        for (Keymap k : Keymap.values()) assertTrue(seen.add(k.key(s)), "no two actions share a key: " + k);
        Keymap.reset(s);
        for (Keymap k : Keymap.values()) assertEquals(k.def, k.key(s));
    }

    @Test
    void resetPutsEverySettingBackHowTheGameShips() {
        Settings s = new Settings();
        s.reduceFlash = true;
        s.shake = 0.1f;
        Keymap.bind(s, Keymap.ASTRA, com.badlogic.gdx.Input.Keys.P);
        Settings.reset(s);
        assertFalse(s.reduceFlash);
        assertEquals(1f, s.shake, 1e-4f);
        assertEquals(Keymap.ASTRA.def, Keymap.ASTRA.key(s));
    }
}
