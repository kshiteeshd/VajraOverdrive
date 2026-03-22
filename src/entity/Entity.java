
package entity;

import java.awt.*;

/**
 * Base class for all game objects.
 */
public abstract class Entity {

    public double x;
    public double y;

    protected double velocityX;
    protected double velocityY;

    public int width;
    protected int height;

    public boolean removable = false;
    public int health;

    public Entity(double x, double y, int width, int height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
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
