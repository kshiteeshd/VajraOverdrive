package player;

import entity.Entity;
import input.InputManager;
import config.CombatArea;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class PlayerShip extends Entity {

    private double speed = 5;

    private Image sprite;
    private int scale;

    public PlayerShip(double x, double y, int scale) {
        super(x, y, 16*scale, 16*scale);

        this.scale = scale;
        this.health = 100;

        loadSprite();
    }

    private void loadSprite(){
        try{
            sprite = ImageIO.read(new File("assets/sprites/player_ship.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update() {

        velocityX = 0;
        velocityY = 0;

        if(InputManager.isKeyPresent(KeyEvent.VK_LEFT)){
            velocityX = -speed;
        }

        if(InputManager.isKeyPresent(KeyEvent.VK_RIGHT)){
            velocityX = speed;
        }

        if(InputManager.isKeyPresent(KeyEvent.VK_UP)){
            velocityY = -speed;
        }

        if(InputManager.isKeyPresent(KeyEvent.VK_DOWN)){
            velocityY = speed;
        }

        x+=velocityX;
        y+=velocityY;

        // LEFT
        if (x < CombatArea.LEFT_BOUND) {
            x = CombatArea.LEFT_BOUND;
        }

        // TOP
        if (y < CombatArea.TOP_BOUND) {
            y = CombatArea.TOP_BOUND;
        }

        // RIGHT
        if (x > CombatArea.RIGHT_BOUND - width) {
            x = CombatArea.RIGHT_BOUND - width;
        }

        // BOTTOM
        if (y > CombatArea.BOTTOM_BOUND - height) {
            y = CombatArea.BOTTOM_BOUND - height;
        }

    }

    @Override
    public void render(Graphics g) {
        g.drawImage(sprite,(int)x,(int)y,width,height,null);

    }
}
