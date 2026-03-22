package gfx;

import entity.Entity;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Fire-and-forget visual effect entity.
 * Plays a one-shot ImageSequenceAnimation then removes itself.
 */
public class FxEntity extends Entity {

    private final ImageSequenceSet anim;
    private final int renderW;
    private final int renderH;

    public FxEntity(double x, double y,
                    String animKey, int w, int h) {
        super(x - w / 2.0, y - h / 2.0, w, h);
        this.renderW = w;
        this.renderH = h;

        // Build a one-shot animation from the sprites folder
        ImageSequenceSet set = new ImageSequenceSet();
        set.register("default", new ImageSequenceAnimation(
                "resources/sprites/fx/", animKey + "_",
                "png", 6, 60, false
        ));
        this.anim = set;
        this.anim.play("default");
    }

    @Override
    public void update() {
        anim.update();
        if (anim.isDone()) removable = true;
    }

    @Override
    public void render(Graphics g) {
        BufferedImage frame = anim.getFrame();
        if (frame != null)
            g.drawImage(frame, (int) x, (int) y, renderW, renderH, null);
        // No fallback — invisible if no sprite loaded
    }
}
