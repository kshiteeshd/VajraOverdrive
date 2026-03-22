package gfx;

/**
 * Builds ImageSequenceSets for all player ship tiers.
 *
 * ── Folder structure ─────────────────────────────────────────────────
 *   resources/sprites/ships/tier1/idle_0.png
 *                                idle_1.png
 *                                thrust_0.png
 *                                thrust_1.png
 *                                thrust_2.png
 *                                hit_0.png
 *   resources/sprites/ships/tier2/  ... same layout
 *   ...
 *   resources/sprites/ships/tier5/  ... same layout
 *
 * ── Frame counts per state ───────────────────────────────────────────
 *   idle    : 2 frames  @ 200 ms each  (slow breathe)
 *   thrust  : 3 frames  @  80 ms each  (engine glow pulse)
 *   hit     : 2 frames  @  60 ms each  (one-shot flash)
 *
 * If a PNG is missing the frame is silently skipped.
 * If a whole animation has zero frames it returns null from getFrame()
 * and PlayerShip falls back to its polygon placeholder — no crash.
 *
 * ── Upgrading the ship ───────────────────────────────────────────────
 *   In PlayerShip, call:
 *     this.anim = ShipRegistry.buildShipSet(newTier);
 *   That's all — the new images load automatically.
 */
public class ShipRegistry {

    private static final int MAX_TIER = 5;

    /**
     * Returns a fresh ImageSequenceSet for the given upgrade tier (1–5).
     * Each call creates a new set — each entity must own its own instance.
     */
    public static ImageSequenceSet buildShipSet(int tier) {
        tier = Math.max(1, Math.min(tier, MAX_TIER));   // clamp 1–5
        String base = "resources/sprites/ships/tier" + tier + "/";

        ImageSequenceSet set = new ImageSequenceSet();

        set.register("idle", new ImageSequenceAnimation(
                base, "idle_", "png",
                2,      // frame count
                200,    // ms per frame
                true    // loops
        ));

        set.register("thrust", new ImageSequenceAnimation(
                base, "thrust_", "png",
                3,
                80,
                true
        ));

        set.register("hit", new ImageSequenceAnimation(
                base, "hit_", "png",
                2,
                60,
                false   // one-shot
        ));

        return set;
    }

    /**
     * Builds an enemy ship set from a named folder.
     * Used by EnemyEntity subclasses that want individual image files
     * instead of a sprite strip.
     *
     * Example:
     *   ImageSequenceSet s = ShipRegistry.buildEnemySet("enemy_fast");
     *   // loads resources/sprites/enemies/enemy_fast/idle_0.png  etc.
     */
    public static ImageSequenceSet buildEnemySet(String enemyName) {
        String base = "resources/sprites/enemies/" + enemyName + "/";

        ImageSequenceSet set = new ImageSequenceSet();

        set.register("idle", new ImageSequenceAnimation(
                base, "idle_", "png", 2, 300, true));

        set.register("hit", new ImageSequenceAnimation(
                base, "hit_", "png", 2, 60, false));

        return set;
    }
}