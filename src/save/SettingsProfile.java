package save;

import java.io.Serializable;

/**
 * Persisted player settings.
 * Saved to saves/settings.cfg (separate from profile slots).
 *
 * Sound / music are wired but no-ops until SoundManager is implemented.
 * Resolution and brightness are applied in GameCanvas each frame.
 */
public class SettingsProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Audio ─────────────────────────────────────────────────────────
    /** Master sound effects on/off. */
    public boolean soundEnabled  = true;

    /** Background music on/off. */
    public boolean musicEnabled  = true;

    /** SFX volume 0–100. */
    public int     soundVolume   = 80;

    /** Music volume 0–100. */
    public int     musicVolume   = 60;

    // ── Display ───────────────────────────────────────────────────────
    /**
     * Brightness multiplier 0.5–1.5.
     * Applied as a translucent black (darken) or white (lighten)
     * overlay in GameCanvas.paintComponent().
     * 1.0 = normal, no overlay.
     */
    public float   brightness    = 1.0f;

    /**
     * Contrast scale 0.5–1.5.
     * Placeholder — wired to UI but no visual effect until
     * a post-process pass is added.
     */
    public float   contrast      = 1.0f;

    /**
     * Target resolution index into RESOLUTIONS array.
     *   0 = 1000×600  (default windowed)
     *   1 = 1280×768
     *   2 = 1920×1080
     *   3 = Fullscreen (native)
     */
    public int     resolutionIdx = 0;

    // ── Resolution table ─────────────────────────────────────────────
    public static final String[] RESOLUTION_LABELS = {
            "1000 x 600  (default)",
            "1280 x 768",
            "1920 x 1080",
            "FULLSCREEN"
    };
}