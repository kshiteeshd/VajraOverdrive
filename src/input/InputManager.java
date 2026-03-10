package input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputManager implements KeyListener {

    private static final boolean[] keys = new boolean[256];



    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if(key < keys.length){
            keys[key] = true;
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if(key < keys.length){
            keys[key] = false;
        }


    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    public static boolean isKeyPresent(int keyCode){
        if(keyCode <keys.length){
            return keys[keyCode];
        }

        return false;
    }
}
