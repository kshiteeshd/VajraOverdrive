package entity;

import enemy.EnemyEntity;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
            if (!pending.isEmpty()) {
                entities.addAll(pending);
                pending.clear();
            }

            for (int i = 0; i < entities.size(); i++) {
                entities.get(i).update();
            }

            // NEW: Salvage projectiles before they are removed
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
        // We lock the list for reading so AWT doesn't crash if the game loop updates it
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