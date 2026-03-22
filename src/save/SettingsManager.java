package save;

import java.io.*;

/**
 * Loads and saves the single SettingsProfile to saves/settings.cfg.
 * Call SettingsManager.get() anywhere to read current settings.
 * Call SettingsManager.save() after any change.
 */
public class SettingsManager {

    private static final String PATH = "saves/settings.cfg";

    private static SettingsProfile instance;

    /** Returns the loaded (or default) settings. Never null. */
    public static SettingsProfile get() {
        if (instance == null) load();
        return instance;
    }

    /** Persist current settings to disk. */
    public static void save() {
        // Ensure saves/ folder exists
        File dir = new File("saves");
        if (!dir.exists()) dir.mkdirs();

        try (ObjectOutputStream out =
                     new ObjectOutputStream(new FileOutputStream(PATH))) {
            out.writeObject(instance);
        } catch (Exception e) {
            System.err.println("SettingsManager: save failed — " + e.getMessage());
        }
    }

    // ── Internal ─────────────────────────────────────────────────────

    private static void load() {
        File f = new File(PATH);
        if (f.exists()) {
            try (ObjectInputStream in =
                         new ObjectInputStream(new FileInputStream(f))) {
                instance = (SettingsProfile) in.readObject();
                System.out.println("SettingsManager: settings loaded.");
                return;
            } catch (Exception e) {
                System.err.println("SettingsManager: corrupt settings, resetting — "
                        + e.getMessage());
                f.delete();
            }
        }
        // Default settings
        instance = new SettingsProfile();
        System.out.println("SettingsManager: using default settings.");
    }
}