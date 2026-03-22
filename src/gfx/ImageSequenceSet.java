package gfx;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Named bag of ImageSequenceAnimations for one entity.
 * Each entity owns its own instance so playback states never clash.
 *
 * API is identical to AnimationSet so migration is straightforward:
 *   - swap  AnimationSet  →  ImageSequenceSet
 *   - swap  Animation     →  ImageSequenceAnimation
 *   - everything else (play, update, getFrame, has, isDone) stays the same
 *
 * Usage:
 *   ImageSequenceSet anim = ShipRegistry.buildShipSet(tier);
 *
 *   // update()
 *   anim.update();
 *
 *   // render()
 *   BufferedImage frame = anim.getFrame();
 *   if (frame != null) g.drawImage(frame, x, y, w, h, null);
 *   else               { ... draw fallback shape ... }
 *
 *   // switch states
 *   anim.play("thrust", "idle");   // one-shot then return to idle
 *   anim.play("hit",    "idle");
 *   anim.play("idle");             // loop forever
 */
public class ImageSequenceSet {

    private final Map<String, ImageSequenceAnimation> animations = new HashMap<>();
    private String                 currentName = "";
    private String                 returnTo    = "";
    private ImageSequenceAnimation current     = null;

    // ── Register ──────────────────────────────────────────────────────
    /**
     * Register a named animation.
     * The first registered animation becomes the default.
     */
    public void register(String name, ImageSequenceAnimation anim) {
        animations.put(name, anim);
        if (current == null) {
            currentName = name;
            current     = anim;
        }
    }

    // ── Play ──────────────────────────────────────────────────────────
    /**
     * Switch to a named animation.
     * @param name     the animation key to switch to
     * @param returnTo after a one-shot finishes, auto-switch to this key
     */
    public void play(String name, String returnTo) {
        // Don't restart if already playing and not done
        if (name.equals(currentName) && !isDone()) return;
        ImageSequenceAnimation next = animations.get(name);
        if (next == null) return;
        next.reset();
        current        = next;
        currentName    = name;
        this.returnTo  = returnTo;
    }

    /** Play with no explicit return — stays on last frame when done. */
    public void play(String name) {
        play(name, currentName);
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        if (current == null) return;
        current.update();
        // Auto-return after one-shot completes
        if (current.isDone()
                && !returnTo.isEmpty()
                && !returnTo.equals(currentName)) {
            play(returnTo, returnTo);
        }
    }

    // ── Frame access ──────────────────────────────────────────────────
    /** Returns current frame, or null if no frames loaded. */
    public BufferedImage getFrame() {
        return (current == null) ? null : current.getFrame();
    }

    // ── State queries ─────────────────────────────────────────────────
    public boolean isDone()           { return current != null && current.isDone(); }
    public boolean has(String name)   { return animations.containsKey(name);       }
    public String  getCurrentName()   { return currentName;                         }
    public boolean isValid()          { return current != null && current.isValid();}
}