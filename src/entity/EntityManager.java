package entity;

import enemy.EnemyEntity;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Central entity list.
 *
 * FIXED:
 *  - Removal is now two-pass: update loop marks removable, then a
 *    single removeIf() clears them. No more index-shift corruption
 *    when multiple entities die in the same frame.
 *  - pending list still used to avoid mid-update-iteration adds.
 *  - render() still snapshots the list to avoid ConcurrentModification
 *    between the game loop thread and the AWT paint thread.
 */
public class EntityManager {

    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> pending  = new ArrayList<>();

    // ── Add ───────────────────────────────────────────────────────────
    public void add(Entity e) {
        pending.add(e);
    }

    // ── Access ────────────────────────────────────────────────────────
    public List<Entity> getEntities() {
        return entities;
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {
        // Merge entities queued during last frame first
        if (!pending.isEmpty()) {
            entities.addAll(pending);
            pending.clear();
        }

        // Update all entities
        for (Entity e : entities) {
            e.update();
        }

        // Two-pass removal — avoids index-shift corruption when multiple
        // entities die in the same frame (e.g. area explosion)
        entities.removeIf(Entity::isRemovable);
    }

    // ── Render ────────────────────────────────────────────────────────
    public void render(Graphics g) {
        // Snapshot so the AWT render thread never sees a structural
        // modification made by the game loop update thread.
        List<Entity> snapshot = new ArrayList<>(entities);
        for (Entity e : snapshot) {
            e.render(g);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────
    public int countEnemies() {
        int count = 0;
        for (Entity e : entities) {
            if (e instanceof EnemyEntity) count++;
        }
        return count;
    }

    /**
     * Removes all entities immediately — used when rebuilding a session
     * so stale projectiles and particles from the previous run don't
     * carry over.
     */
    public void clear() {
        entities.clear();
        pending.clear();
    }
}