package save;

import java.io.*;

/**
 * Handles saving and loading player profiles (save slots 1-N).
 *
 * CHANGED:
 *  - loadProfile() now catches InvalidClassException separately and
 *    auto-deletes the stale file so the slot appears empty rather than
 *    crashing. This fires when PlayerProfile.serialVersionUID changes.
 *  - nextEmptySlot() added — used by New Game flow to auto-assign a slot.
 *  - MAX_SLOTS constant centralised here.
 */
public class SaveManager {

    public  static final int    MAX_SLOTS   = 5;
    private static final String SAVE_FOLDER = "saves";

    static {
        File dir = new File(SAVE_FOLDER);
        if (!dir.exists()) dir.mkdirs();
    }

    // ── File path ────────────────────────────────────────────────────
    private static File getFile(int slot) {
        return new File(SAVE_FOLDER + "/slot" + slot + ".sav");
    }

    // ── Save ─────────────────────────────────────────────────────────
    public static void saveProfile(int slot, PlayerProfile profile) {
        profile.lastPlayedMs = System.currentTimeMillis();
        try (ObjectOutputStream out =
                     new ObjectOutputStream(new FileOutputStream(getFile(slot)))) {
            out.writeObject(profile);
        } catch (Exception e) {
            System.err.println("SaveManager: save failed slot " + slot
                    + " — " + e.getMessage());
        }
    }

    // ── Load ─────────────────────────────────────────────────────────
    public static PlayerProfile loadProfile(int slot) {
        File file = getFile(slot);
        if (!file.exists()) return null;

        try (ObjectInputStream in =
                     new ObjectInputStream(new FileInputStream(file))) {
            return (PlayerProfile) in.readObject();

        } catch (InvalidClassException ice) {
            // serialVersionUID mismatch — profile schema changed
            System.out.println("SaveManager: stale save in slot " + slot
                    + " — deleting and treating as empty.");
            file.delete();
            return null;

        } catch (Exception e) {
            System.err.println("SaveManager: load failed slot " + slot
                    + " — " + e.getMessage());
            return null;
        }
    }

    // ── Delete ───────────────────────────────────────────────────────
    public static void deleteProfile(int slot) {
        File file = getFile(slot);
        if (file.exists()) file.delete();
    }

    // ── Exists ───────────────────────────────────────────────────────
    public static boolean slotExists(int slot) {
        return getFile(slot).exists();
    }

    // ── Next empty slot (1-based) ─────────────────────────────────────
    /**
     * Returns the first slot number (1..MAX_SLOTS) that has no save file.
     * Returns -1 if all slots are full.
     */
    public static int nextEmptySlot() {
        for (int i = 1; i <= MAX_SLOTS; i++) {
            if (!slotExists(i)) return i;
        }
        return -1;
    }
}