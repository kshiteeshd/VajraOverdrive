package save;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import save.GameMode;

/**
 * One save slot.
 *
 * CHANGED from previous version:
 *  - serialVersionUID bumped to 2L  (new fields added — old .sav files
 *    will be detected as stale and cleanly deleted by SaveManager).
 *  - Added: gameMode, lastPlayedMs, shipTier.
 *  - Added: helper lastPlayedString() for the Load Game UI.
 */
public class PlayerProfile implements Serializable {

    // Bump whenever fields change so stale saves are caught cleanly.
    private static final long serialVersionUID = 2L;

    // ── Core ──────────────────────────────────────────────────────────
    public String   name;
    public int      level;
    public int      score;
    public String   region;
    public int      lives;

    // ── New fields ────────────────────────────────────────────────────
    /** CAMPAIGN or ENDLESS */
    public GameMode gameMode    = GameMode.CAMPAIGN;

    /** System.currentTimeMillis() of the last save — shown in Load Game. */
    public long     lastPlayedMs = 0;

    /** Player ship upgrade tier 1-5. */
    public int      shipTier    = 1;

    // ── Constructors ──────────────────────────────────────────────────
    public PlayerProfile() {}

    public PlayerProfile(String name, int level, GameMode mode) {
        this.name        = name;
        this.level       = level;
        this.score       = 0;
        this.region      = "RED";
        this.lives       = 3;
        this.gameMode    = mode;
        this.lastPlayedMs = System.currentTimeMillis();
        this.shipTier    = 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /**
     * Human-readable last-played string for the Load Game screen.
     * Returns "Never" if the profile was never saved after creation.
     */
    public String lastPlayedString() {
        if (lastPlayedMs <= 0) return "Never";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy  HH:mm");
        return sdf.format(new Date(lastPlayedMs));
    }

    /**
     * One-line summary shown on the save slot card:
     *   LVL 07  |  BLUE  |  00034200  |  CAMPAIGN
     */
    public String slotSummary() {
        String modeTag = (gameMode == GameMode.ENDLESS) ? "ENDLESS" : "CAMPAIGN";
        return String.format("LVL %02d  |  %s  |  %08d  |  %s",
                level,
                region != null ? region : "RED",
                score,
                modeTag);
    }
}