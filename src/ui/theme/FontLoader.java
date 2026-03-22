package ui.theme;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Loads custom fonts.
 *
 * Search order:
 *   1. resources/fonts/<name>  (folder parallel to src — your setup)
 *   2. Classpath  fonts/<name>  (fallback for JAR packaging)
 *   3. System monospace bold   (always works, still looks retro)
 *
 * Put your font at:   resources/fonts/press_start.ttf
 * That folder must be on the compiler / run classpath.
 * In IntelliJ: File → Project Structure → Modules → Sources tab
 *              → mark the  resources/  folder as "Resources".
 * In a Maven project: it belongs in  src/main/resources/fonts/.
 */
public class FontLoader {

    public static Font loadFont(String resourcePath, float size) {

        // --- 1. Try file-system path relative to working directory ----------
        // "fonts/press_start.ttf" → looks for  ./resources/fonts/press_start.ttf
        // and also  ./fonts/press_start.ttf
        String[] fsPaths = {
                "resources/" + resourcePath,
                resourcePath
        };

        for (String fsPath : fsPaths) {
            File f = new File(fsPath);
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    Font font = Font.createFont(Font.TRUETYPE_FONT, fis);
                    registerFont(font);
                    System.out.println("Font loaded from file: " + fsPath);
                    return font.deriveFont(size);
                } catch (Exception e) {
                    System.out.println("Font file found but failed to load: " + fsPath + " — " + e.getMessage());
                }
            }
        }

        // --- 2. Try classpath (works when packaged as JAR) ------------------
        try {
            InputStream is = FontLoader.class
                    .getClassLoader()
                    .getResourceAsStream(resourcePath);

            if (is != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is);
                registerFont(font);
                System.out.println("Font loaded from classpath: " + resourcePath);
                return font.deriveFont(size);
            }
        } catch (Exception e) {
            System.out.println("Classpath font load failed: " + resourcePath + " — " + e.getMessage());
        }

        // --- 3. Intentional fallback — monospace bold looks retro -----------
        System.out.println("Using fallback font for: " + resourcePath);
        return fallback(size);
    }

    private static void registerFont(Font font) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(font);
    }

    /** Returns the best-looking monospace fallback available on the system. */
    private static Font fallback(float size) {
        String[] candidates = { "Press Start 2P", "Courier New", "Courier", "Monospaced" };
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        java.util.Set<String> available = new java.util.HashSet<>(
                java.util.Arrays.asList(ge.getAvailableFontFamilyNames()));
        for (String name : candidates) {
            if (available.contains(name)) {
                return new Font(name, Font.BOLD, (int) size);
            }
        }
        return new Font(Font.MONOSPACED, Font.BOLD, (int) size);
    }
}
