package gfx;

import java.awt.image.BufferedImage;

/**
 * Drives frame-by-frame playback of a SpriteSheet.
 *
 * Usage:
 *   animation.update();
 *   BufferedImage frame = animation.getCurrentFrame();
 *   if (frame != null) g.drawImage(frame, x, y, null);
 *   else               g.fillRect(x, y, w, h);  // fallback
 *
 * One-shot animations report isDone() = true after the last frame.
 * Looping animations never report done.
 */
public class Animation {

    private final SpriteSheet sheet;
    private final int[]       frameIndices;  // which frames to play, in order
    private final long        msPerFrame;
    private final boolean     loops;

    private int  currentIndex = 0;
    private long lastFrameTime;
    private boolean done      = false;

    /**
     * @param sheet        the sprite sheet to pull frames from
     * @param frameIndices ordered list of frame indices to play
     * @param msPerFrame   milliseconds each frame is shown
     * @param loops        true = loop forever, false = play once then stop
     */
    public Animation(SpriteSheet sheet, int[] frameIndices,
                     long msPerFrame, boolean loops) {
        this.sheet        = sheet;
        this.frameIndices = frameIndices;
        this.msPerFrame   = msPerFrame;
        this.loops        = loops;
        this.lastFrameTime = System.currentTimeMillis();
    }

    /** Convenience constructor — plays all frames in order. */
    public Animation(SpriteSheet sheet, long msPerFrame, boolean loops) {
        this(sheet, makeRange(sheet.getFrameCount()), msPerFrame, loops);
    }

    private static int[] makeRange(int count) {
        int[] r = new int[count];
        for (int i = 0; i < count; i++) r[i] = i;
        return r;
    }

    public void reset() {
        currentIndex   = 0;
        lastFrameTime  = System.currentTimeMillis();
        done           = false;
    }

    public void update() {
        if (done) return;
        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= msPerFrame) {
            lastFrameTime = now;
            currentIndex++;
            if (currentIndex >= frameIndices.length) {
                if (loops) {
                    currentIndex = 0;
                } else {
                    currentIndex = frameIndices.length - 1;
                    done = true;
                }
            }
        }
    }

    public BufferedImage getCurrentFrame() {
        if (frameIndices.length == 0) return null;
        return sheet.getFrame(frameIndices[currentIndex]);
    }

    public boolean isDone()  { return done;               }
    public boolean isValid() { return sheet.isValid();    }
}