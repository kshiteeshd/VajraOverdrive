package save;

/**
 * In-memory access to the active player profile.
 *
 * CHANGED:
 *  - createProfile() now takes a GameMode parameter.
 *  - save() stamps lastPlayedMs automatically via SaveManager.
 */
public class ProfileManager {

    private static PlayerProfile currentProfile;
    private static int           currentSlot = -1;

    // ── Load existing slot ────────────────────────────────────────────
    public static void loadSlot(int slot) {
        currentProfile = SaveManager.loadProfile(slot);
        currentSlot    = slot;
    }

    // ── Create new profile in a slot ─────────────────────────────────
    public static void createProfile(int slot, String name, GameMode mode) {
        currentProfile = new PlayerProfile(name, 1, mode);
        currentSlot    = slot;
        SaveManager.saveProfile(slot, currentProfile);
    }

    // ── Delete slot ───────────────────────────────────────────────────
    public static void deleteProfile(int slot) {
        SaveManager.deleteProfile(slot);
        if (currentSlot == slot) {
            currentProfile = null;
            currentSlot    = -1;
        }
    }

    // ── Persist current profile ───────────────────────────────────────
    public static void save() {
        if (currentProfile != null && currentSlot != -1) {
            SaveManager.saveProfile(currentSlot, currentProfile);
        }
    }

    // ── Getters ───────────────────────────────────────────────────────
    public static PlayerProfile getProfile()   { return currentProfile; }
    public static int           getCurrentSlot(){ return currentSlot;   }
}