package vajra.world;

import vajra.enemy.Enemy;

/** Enemy director: the boss seal, the commander's call, mid-berserk deployments and his blink
 *  berths. Extracted from World; the enemy roster itself stays on World for rendering. */
public final class EnemyDirector {
    private final World world;

    public EnemyDirector(World world) {
        this.world = world;
    }

    /** The seal lifts: Bala appears in his arena the moment the run's kill quota is full.
     *  The quota itself stays silent on screen — no banner, no count. */
    public void trySpawnBoss() {
        if (world.boss != null || !world.quotaMet()) return;
        world.boss = new Enemy(Enemy.Kind.BALA, world.pitX, world.pitY);
        world.boss.boss = true;
        world.boss.rank = 1;
        world.add(world.boss);
        world.addEffect(Effect.ring(world.boss.x, world.boss.y, 340, 1f, 0.84f, 0.3f));
    }

    /** The call: the whole arena retinue leaves its post and forms the commander's ring. */
    public void orderRetinue(Enemy commander) {
        for (Enemy e : world.enemies) {
            if (e == commander || e.dead || e.homeY < world.regionStart[5]) continue;
            e.commander = commander;
            e.sentry = false;
            e.active = true;
        }
    }

    /** Mid-berserk deployment: a gold elite pair at his old post, straight into his ring (cap 6). */
    public void summonElites(float x, float y, Enemy commander) {
        int alive = 0;
        for (Enemy o : world.enemies)
            if (!o.dead && o.elite && o.commander == commander) alive++;
        if (alive >= 6) return;
        for (float dx : new float[] { -44, 44 }) {
            Enemy m = new Enemy(Enemy.Kind.VAKRA, x + dx, y - 36);
            m.elite = true;
            m.rank = 1;
            m.hp = m.maxHp = m.maxHp * 2;
            m.wakeRange = 1500;
            m.commander = commander;
            m.active = true;
            world.add(m);
        }
    }

    private static final float CALL_RANGE = 1100f;

    /** THE CALL: a leader (rank 1, not the ruler, not a keeper) in the fight calls RESERVES - two bodies of his own kind that were waiting
     *  down at the floor of the shaft and now RISE beside him (depth 1 to 0 over a second, a grit ring where they break the surface), each
     *  following him. A leader has two calls (an elite three), one every 7 to 11 s while he is awake and the hull is within 1100 px, and no
     *  more than four of his reserves stand at once. */
    public void tickBackup(float dt) {
        java.util.ArrayList<Enemy> calling = null;
        for (int i = 0; i < world.enemies.size(); i++) {
            Enemy e = world.enemies.get(i);
            if (e.dead || e.ghost || e.boss || e.voice || e.kind.keeper() || e.rank < 1 || !e.active || e.submerged() || e.parley || e.dismissed || e.routed) continue;
            if (e.backups < 0) e.backups = e.elite ? 3 : 2;
            if (e.backups == 0) continue;
            if ((e.callT -= dt) > 0f) continue;
            vajra.ship.Ship hull = world.nearestHull(e.x, e.y);
            float dx = hull.x - e.x, dy = hull.y - e.y;
            if (dx * dx + dy * dy > CALL_RANGE * CALL_RANGE) { e.callT = 1f; continue; }
            if (calling == null) calling = new java.util.ArrayList<>();
            calling.add(e);
        }
        if (calling == null) return;
        for (Enemy leader : calling) {
            int mine = 0;
            for (Enemy o : world.enemies) if (!o.dead && o.commander == leader && o.depth > 0f) mine++;
            leader.callT = 7f + world.fxRng.nextFloat() * 4f;
            if (mine >= 4) continue;
            leader.backups--;
            world.addEffect(Effect.ring(leader.x, leader.y, 70, 1f, 0.84f, 0.3f));       // the call goes out
            int placed = 0;
            if (leader.vitala) {                                                       // Vitala has no floor to rise from: the reserves FLY IN, from behind the leader, already in the air
                vajra.ship.Ship hull = world.nearestHull(leader.x, leader.y);
                float bx = leader.x - hull.x, by = leader.y - hull.y, bd = Math.max(1f, (float) Math.hypot(bx, by));
                bx /= bd;
                by /= bd;
                for (float side : new float[] {-1f, 1f}) {
                    float nx = leader.x + bx * 360f - by * side * 110f, ny = leader.y + by * 360f + bx * side * 110f;
                    if (!world.openFor(nx, ny, 14f)) { nx = leader.x - by * side * 130f; ny = leader.y + bx * side * 130f; if (!world.openFor(nx, ny, 14f)) continue; }
                    Enemy r = new Enemy(leader.kind, nx, ny);
                    r.rank = 0;
                    r.active = true;
                    r.wakeRange = 1500f;
                    r.commander = leader;
                    r.dmgScale = leader.dmgScale;
                    r.hp = r.maxHp = leader.maxHp * 0.55f;
                    world.add(r);
                    world.addEffect(Effect.flash(nx, ny, leader.x, leader.y, 1f, 0.7f, 0.2f));        // a streak of ember from where it came in
                    world.addEffect(Effect.ring(nx, ny, 36, 1f, 0.55f, 0.15f));
                    placed++;
                }
                if (placed == 0) leader.backups++;
                continue;
            }
            for (float dx : new float[] {-76f, 76f}) {
                float nx = leader.x + dx, ny = leader.y - 34f;
                if (!world.openFor(nx, ny, 14f)) continue;
                Enemy r = new Enemy(leader.kind, nx, ny);
                r.rank = 0;
                r.depth = 1f;
                r.diveState = 3;                                                     // already below, and rising
                r.active = true;
                r.wakeRange = 1500f;
                r.commander = leader;
                r.dmgScale = leader.dmgScale;
                world.add(r);
                world.addEffect(Effect.ring(nx, ny, 36, 0.4f, 0.35f, 0.3f));
                placed++;
            }
            if (placed == 0) leader.backups++;                                       // nowhere to rise: the call is kept
        }
    }

    /** A random open berth in the arena for the boss's blink: 350-850 px from the ship. */
    public float[] bossBlinkSpot() {
        for (int i = 0; i < 40; i++) {
            float x = 300 + world.orbRng.nextFloat() * (world.width - 600);
            float y = 17900 + world.orbRng.nextFloat() * (world.height - 17900 - 250);
            float dx = x - world.ship.x, dy = y - world.ship.y;
            float d2 = dx * dx + dy * dy;
            if (d2 < 350 * 350 || d2 > 850 * 850) continue;
            if (world.spotClear(x, y)) return new float[] { x, y };
        }
        return new float[] { world.width / 2, 19104 };
    }
}
