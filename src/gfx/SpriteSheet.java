package gfx;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Loads a horizontal sprite strip PNG and slices it into frames.
 *
 * Place PNGs in resources/sprites/<name>.png
 * All frames must be the same width and height.
 *
 * If the file is missing, the sheet is marked invalid and returns
 * null frames — callers fall back to placeholder rendering.
 */
public class SpriteSheet {

    private final BufferedImage[] frames;
    private final int frameWidth;
    private final int frameHeight;
    private final boolean valid;

    /**
     * @param path       path relative to working dir, e.g. "resources/sprites/player.png"
     * @param frameW     width of each frame in pixels
     * @param frameH     height of each frame in pixels
     */
    public SpriteSheet(String path, int frameW, int frameH) {
        this.frameWidth  = frameW;
        this.frameHeight = frameH;

        BufferedImage sheet = null;
        try {
            File f = new File(path);
            if (f.exists()) {
                sheet = ImageIO.read(f);
            } else {
                // Try classpath
                var stream = SpriteSheet.class.getClassLoader()
                        .getResourceAsStream(path.replace("resources/", ""));
                if (stream != null) sheet = ImageIO.read(stream);
            }
        } catch (IOException e) {
            System.err.println("SpriteSheet: failed to load " + path + " — " + e.getMessage());
        }

        if (sheet != null && frameW > 0 && frameH > 0) {
            int count = sheet.getWidth() / frameW;
            frames    = new BufferedImage[count];
            for (int i = 0; i < count; i++) {
                frames[i] = sheet.getSubimage(i * frameW, 0, frameW, frameH);
            }
            valid = true;
            System.out.println("SpriteSheet loaded: " + path
                    + " (" + count + " frames, " + frameW + "x" + frameH + ")");
        } else {
            frames = new BufferedImage[0];
            valid  = false;
            System.out.println("SpriteSheet: using placeholder for " + path);
        }
    }

    public BufferedImage getFrame(int index) {
        if (!valid || frames.length == 0) return null;
        return frames[Math.max(0, Math.min(index, frames.length - 1))];
    }

    public int  getFrameCount() { return frames.length; }
    public int  getFrameWidth() { return frameWidth;    }
    public int  getFrameHeight(){ return frameHeight;   }
    public boolean isValid()    { return valid;         }
}