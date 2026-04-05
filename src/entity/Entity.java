
package entity;

import java.awt.*;

/**
 * Base class for all game objects.
 */
public abstract class Entity {

    public double x;
    public double y;

    public double velocityX;
    public double velocityY;

    public int width;
    public int height;

    public boolean removable = false;
    public int health;

    public Entity(double x, double y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    public void setRemovable(boolean removable) {
        this.removable = removable;
    }

    public Rectangle getBounds(){
        return new Rectangle((int)x,(int)y,width,height);
    }

    public abstract void update();
    public abstract void render(Graphics g);

    public boolean isRemovable(){
        return removable;
    }
}
