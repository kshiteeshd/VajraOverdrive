package entity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class EntityManager {

    private List<Entity> entities;

    public EntityManager(){
        entities = new ArrayList<>();
    }

    public void addEntities(Entity entity){
        entities.add(entity);
    }

    public void removeEntity(Entity entity){
        entities.remove(entity);
    }

    public void update(){
        for(Entity entity: entities){
            entity.update();
        }
    }

    public void render(Graphics g){
        for(Entity entity: entities){
            entity.render(g);
        }
    }
}
