package gfx;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Named bag of animations for a single entity type.
 *
 * Typical usage in an entity:
 *
 *   private final AnimationSet anim = SpriteRegistry.getAnimSet("player");
 *
 *   // in update():
 *   anim.update();
 *
 *   // in render():
 *   BufferedImage frame = anim.getFrame();
 *   if (frame != null) g.drawImage(frame, (int)x, (int)y, null);
 *   else               { ... placeholder ... }
 *
 * Switch animations by name:
 *   anim.play("hit");      // one-shot, returns to "idle" when done
 *   anim.play("idle");     // looping
 *   anim.play("death");    // one-shot — entity listens for isDone()
 */
public class AnimationSet {

    private final Map<String, Animation> animations = new HashMap<>();
    private String   currentName  = "";
    private String   returnTo     = "";   // after one-shot, return to this
    private Animation current     = null;

    /** Register a named animation. */
    public void register(String name, Animation anim) {
        animations.put(name, anim);
        if (current == null) {
            currentName = name;
            current     = anim;
        }
    }

    /**
     * Switch to a named animation.
     * @param name     animation key
     * @param returnTo after a one-shot completes, switch to this key
     *                 (use "" or the same key for looping animations)
     */
    public void play(String name, String returnTo) {
        if (name.equals(currentName) && !isDone()) return;
        Animation next = animations.get(name);
        if (next == null) return;
        next.reset();
        current        = next;
        currentName    = name;
        this.returnTo  = returnTo;
    }

    /** Convenience — play with no return (stays on last frame). */
    public void play(String name) {
        play(name, currentName);
    }

    public void update() {
        if (current == null) return;
        current.update();
        // Auto-return after one-shot
        if (current.isDone() && !returnTo.isEmpty() && !returnTo.equals(currentName)) {
            play(returnTo, returnTo);
        }
    }

    public BufferedImage getFrame() {
        if (current == null) return null;
        return current.getCurrentFrame();
    }

    public boolean isDone()           { return current != null && current.isDone(); }
    public String  getCurrentName()   { return currentName; }
    public boolean has(String name)   { return animations.containsKey(name); }
}