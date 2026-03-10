package entity;

import java.awt.*;

public abstract class Entity {
    protected double x;
    protected double y;

    protected double velocityX;
    protected double velocityY;

    protected int width;
    protected int height;

    protected int health;

    public Entity(double x, double y, int width, int height){
        this.x = x;
        this.y = y;

        this.width = width;
        this.height = height;
    }

    public abstract void update();

    public abstract void render(Graphics g);

    public Rectangle getBounds(){
        return new Rectangle((int)x,(int)y,width,height);
    }

}
