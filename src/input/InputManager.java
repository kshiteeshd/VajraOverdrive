package input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Keyboard state tracker with press detection.
 *
 * FIXED:
 *  - Array size increased from 256 to 512 so extended key codes
 *    (VK_KP_*, some function keys) are handled instead of silently
 *    returning false.
 */
public class InputManager implements KeyListener {

    private static final int KEY_COUNT = 512;

    private static final boolean[] keys    = new boolean[KEY_COUNT];
    private static final boolean[] pressed = new boolean[KEY_COUNT];

    public static boolean isKeyPresent(int keyCode) {
        if (keyCode >= 0 && keyCode < KEY_COUNT) return keys[keyCode];
        return false;
    }

    public static boolean isKeyPressed(int keyCode) {
        if (keyCode >= 0 && keyCode < KEY_COUNT && pressed[keyCode]) {
            pressed[keyCode] = false;
            return true;
        }
        return false;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < KEY_COUNT) {
            if (!keys[code]) pressed[code] = true;
            keys[code] = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code >= 0 && code < KEY_COUNT) keys[code] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}