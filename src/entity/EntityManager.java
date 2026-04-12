package entity;

import enemy.EnemyEntity;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Central list that stores and updates all entities.
 *
 * FIX: ReadWriteLock prevents ConcurrentModificationException between
 *      the game loop (update thread) and Swing AWT-EventQueue (render).
 *
 * FIX: Removable ProjectileEntity instances are released back to
 *      ProjectilePool BEFORE being removed from the list. This keeps
 *      the object pool populated so subsequent ProjectilePool.get()
 *      calls recycle existing objects instead of always allocating new.
 *      Without this, the pool was permanently empty and get() always
 *      called new ProjectileEntity() — defeating the entire pool.
 */
public class EntityManager {

    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> pending  = new ArrayList<>();

    // ReadWriteLock prevents AWT threading issues without allocating memory
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void add(Entity e) {
        lock.writeLock().lock();
        try {
            pending.add(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void update() {
        lock.writeLock().lock();
        try {
            // Merge pending entities queued during last frame
            if (!pending.isEmpty()) {
                entities.addAll(pending);
                pending.clear();
            }

            for (int i = 0; i < entities.size(); i++) {
                entities.get(i).update();
            }

            // Salvage removable projectiles back into the pool BEFORE
            // removing them from the list. This is what actually populates
            // the pool — without it, ProjectilePool.get() always creates
            // new objects and no recycling ever happens.
            for (int i = 0; i < entities.size(); i++) {
                Entity e = entities.get(i);
                if (e.isRemovable() && e instanceof ProjectileEntity) {
                    ProjectilePool.release((ProjectileEntity) e);
                }
            }

            entities.removeIf(Entity::isRemovable);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void render(Graphics g) {
        // Lock for reading so AWT doesn't crash if the game loop updates it
        lock.readLock().lock();
        try {
            for (int i = 0; i < entities.size(); i++) {
                entities.get(i).render(g);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public int countEnemies() {
        lock.readLock().lock();
        try {
            int count = 0;
            for (int i = 0; i < entities.size(); i++) {
                if (entities.get(i) instanceof EnemyEntity) count++;
            }
            return count;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            entities.clear();
            pending.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}