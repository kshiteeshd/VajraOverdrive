package gfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Plays a sequence of individual image files as an animation.
 * No sprite strips needed — just drop numbered PNGs in a folder.
 *
 * ── Folder + prefix constructor (auto-numbered files) ────────────────
 *   new ImageSequenceAnimation(
 *       "resources/sprites/ships/tier1/",  // folder
 *       "idle_",                           // prefix
 *       "png",                             // extension
 *       3,                                 // frame count
 *       150,                               // ms per frame
 *       true                               // loops
 *   )
 *   loads: idle_0.png, idle_1.png, idle_2.png
 *
 * ── Explicit file list constructor ───────────────────────────────────
 *   ImageSequenceAnimation.fromFiles(new String[]{
 *       "resources/sprites/ships/tier1/idle_0.png",
 *       "resources/sprites/ships/tier1/idle_1.png",
 *   }, 150, true)
 *
 * If a file is missing it is silently skipped.
 * If zero files load, isValid() = false and getFrame() returns null —
 * callers must render a fallback placeholder.
 */
public class ImageSequenceAnimation {

    private final List<BufferedImage> frames     = new ArrayList<>();
    private final long                msPerFrame;
    private final boolean             loops;

    private int     currentIndex  = 0;
    private long    lastFrameTime;
    private boolean done          = false;
    private boolean valid         = false;

    // ── Folder + prefix constructor ───────────────────────────────────
    public ImageSequenceAnimation(String folder, String prefix,
                                  String ext, int count,
                                  long msPerFrame, boolean loops) {
        this.msPerFrame    = msPerFrame;
        this.loops         = loops;
        this.lastFrameTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            String path = folder + prefix + i + "." + ext;
            BufferedImage img = loadImage(path);
            if (img != null) frames.add(img);
        }

        valid = !frames.isEmpty();
        if (!valid)
            System.out.println("ImageSequenceAnimation: no frames loaded — "
                    + folder + prefix + "*." + ext);
    }

    // ── Explicit file list factory ────────────────────────────────────
    public static ImageSequenceAnimation fromFiles(String[] paths,
                                                   long msPerFrame,
                                                   boolean loops) {
        ImageSequenceAnimation a = new ImageSequenceAnimation(msPerFrame, loops);
        for (String path : paths) {
            BufferedImage img = loadImage(path);
            if (img != null) a.frames.add(img);
        }
        a.valid = !a.frames.isEmpty();
        return a;
    }

    // Private constructor used by fromFiles factory only
    private ImageSequenceAnimation(long msPerFrame, boolean loops) {
        this.msPerFrame    = msPerFrame;
        this.loops         = loops;
        this.lastFrameTime = System.currentTimeMillis();
    }

    // ── Playback ──────────────────────────────────────────────────────
    public void reset() {
        currentIndex   = 0;
        lastFrameTime  = System.currentTimeMillis();
        done           = false;
    }

    public void update() {
        if (done || frames.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= msPerFrame) {
            lastFrameTime = now;
            currentIndex++;
            if (currentIndex >= frames.size()) {
                if (loops) {
                    currentIndex = 0;
                } else {
                    currentIndex = frames.size() - 1;
                    done = true;
                }
            }
        }
    }

    // ── Frame access ──────────────────────────────────────────────────
    /**
     * Returns the current frame, or null if no frames loaded.
     * Always check for null and render a fallback if null.
     */
    public BufferedImage getFrame() {
        if (frames.isEmpty()) return null;
        return frames.get(currentIndex);
    }

    // ── State ─────────────────────────────────────────────────────────
    public boolean isDone()       { return done;             }
    public boolean isValid()      { return valid;            }
    public int     getFrameCount(){ return frames.size();    }
    public long    getMsPerFrame(){ return msPerFrame;       }
    public boolean loops()        { return loops;            }

    // ── Image loader (file system → classpath fallback) ───────────────
    private static BufferedImage loadImage(String path) {
        // 1. File system
        try {
            File f = new File(path);
            if (f.exists()) return ImageIO.read(f);
        } catch (Exception e) {
            System.err.println("ImageSequenceAnimation: fs load failed — " + path);
        }

        // 2. Classpath (for JAR packaging)
        try {
            String cp = path.replace("resources/", "");
            InputStream is = ImageSequenceAnimation.class
                    .getClassLoader()
                    .getResourceAsStream(cp);
            if (is != null) return ImageIO.read(is);
        } catch (Exception e) {
            System.err.println("ImageSequenceAnimation: cp load failed — " + path);
        }

        return null;
    }
}