package gfx;

/**
 * Legacy sprite registry.
 * All entity sprites now use ImageSequenceAnimation via ShipRegistry.
 * This class is kept to avoid breaking any remaining import references.
 * load() is a safe no-op.
 */
public class SpriteRegistry {

    private static boolean loaded = false;

    public static void load() {
        if (loaded) return;
        loaded = true;
        System.out.println("SpriteRegistry: legacy loader — no-op. " +
                "All sprites now use ImageSequenceAnimation.");
    }

    public static gfx.AnimationSet getAnimSet(String name) {
        return new gfx.AnimationSet();   // empty — returns null frames
    }

    public static gfx.SpriteSheet getSheet(String name) {
        return null;
    }
}