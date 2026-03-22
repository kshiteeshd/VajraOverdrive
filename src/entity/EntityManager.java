package entity;

import enemy.EnemyEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Central list that stores and updates all entities.
 *
 * FIXED:
 *  - ConcurrentModificationException: render() now iterates over a
 *    snapshot copy of the entities list. The game loop (update thread)
 *    and Swing's AWT-EventQueue (render thread) run concurrently.
 *    Without a snapshot, render() iterating while update() removes
 *    entities causes ConcurrentModificationException.
 *  - pending list prevents mid-update-iteration adds (from Batch 3).
 */
public class EntityManager {

    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> pending  = new ArrayList<>();

    public void add(Entity e) {
        pending.add(e);
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void update() {
        // Merge pending entities queued during last frame
        if (!pending.isEmpty()) {
            entities.addAll(pending);
            pending.clear();
        }

        for (int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            e.update();

            if (e.isRemovable()) {
                entities.remove(i);
                i--;
            }
        }
    }

    public void render(Graphics g) {
        // FIXED: iterate a snapshot so the AWT render thread never sees
        // a structural modification made by the game loop update thread.
        List<Entity> snapshot = new ArrayList<>(entities);
        for (Entity e : snapshot) {
            e.render(g);
        }
    }

    public int countEnemies() {
        int count = 0;
        for (Entity e : entities) {
            if (e instanceof EnemyEntity) count++;
        }
        return count;
    }
}