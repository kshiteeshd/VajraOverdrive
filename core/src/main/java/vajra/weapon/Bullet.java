package vajra.weapon;

import vajra.enemy.Enemy;
import vajra.world.Effect;
import vajra.world.World;

/** One projectile, player or enemy. Pooled — never construct directly, use World.spawnBullet
 *  or World.spawnEnemyBullet. Hits are resolved in World.update, not here. The lokas' patterns
 *  (see WeaponDef) add their one idea each in {@link #update}, {@link #hits} and {@link #onHit}. */
public class Bullet {
    public float x, y, vx, vy;
    public float damage, radius, life;
    public float homingTurn; // rad/s
    public float aoe;
    public boolean pierceWalls, pierceEnemies, hostile, dead;
    public float r, g, b;
    public WeaponDef def; // the pattern this was fired from — renderer picks the sprite by it
    public int owner = -1; // the roster id of the seat this round belongs to (co-op books)

    // state of the lokas' patterns
    float age, phase, dmgRatio = 1f;
    int bouncesLeft;
    boolean turned, split;
    private final Enemy[] seen = new Enemy[6];
    private final float[] seenAt = new float[6];
    private static boolean flip;

    /** One stat row for enemy fire, so both sides share the projectile code. */
    public static final WeaponDef HOSTILE = WeaponDef.mk("enemy shot")
            .speed(270f).damage(8f).bulletSize(4f).life(3.5f).colour(1f, 0.42f, 0.3f);

    /** power drives speed and damage (charge); dmgMult is the ship's damage tier only — it must
     *  never touch velocity, or buying a damage upgrade silently speeds the bullet up. */
    public void init(float x, float y, float angle, WeaponDef def, float power, float dmgMult, boolean hostile) {
        this.x = x;
        this.y = y;
        float sp = def.speed * (1 + 0.4f * (power - 1));
        this.vx = (float) Math.cos(angle) * sp;
        this.vy = (float) Math.sin(angle) * sp;
        this.damage = def.damage * power * dmgMult;
        this.radius = def.bulletSize;
        this.life = def.life;
        this.homingTurn = (float) Math.toRadians(def.homingTurn);
        this.aoe = def.aoeRadius;
        this.pierceWalls = def.pierceWalls;
        this.pierceEnemies = def.pierceEnemies;
        this.hostile = hostile;
        this.def = def;
        this.r = def.r;
        this.g = def.g;
        this.b = def.b;
        this.dead = false;
        this.owner = -1; // the spawner (or the wire) claims it right after
        this.age = 0f;
        this.turned = false;
        this.split = false;
        this.bouncesLeft = def.bounces;
        this.phase = (flip = !flip) ? 0f : (float) Math.PI;
        this.dmgRatio = def.damage > 0f ? damage / def.damage : 1f;
        java.util.Arrays.fill(seen, null);
    }

    private boolean special() {
        return !hostile && def != null && def.art != null && def.art.idx >= WeaponDef.STANDARD;
    }

    public void update(World w, float dt) {
        age += dt;
        if (special()) {
            if (stepSpecial(w, dt)) return;
        }
        if (!hostile && homingTurn > 0) steer(w, dt);
        x += vx * dt;
        y += vy * dt;
        life -= dt;
        if (life <= 0) {
            expire(w);
            return;
        }
        if (x < 0 || x > w.width || y < 0 || y > w.height) {
            dead = true;
            return;
        }
        if (w.blockedPx(x, y)) {
            if (!hostile && bouncesLeft > 0) {                 // a ricochet: back out of the rock, turn the blocked axis
                x -= vx * dt;
                y -= vy * dt;
                boolean bx = w.blockedPx(x + vx * dt, y), by = w.blockedPx(x, y + vy * dt);
                if (bx || !by) vx = -vx;
                if (by || !bx) vy = -vy;
                bouncesLeft--;
                w.addEffect(Effect.spark(x, y, r, g, b));
                return;
            }
            // enemy fire is stopped by rock too: cover works both ways
            if (hostile || !pierceWalls) expire(w);
        }
    }

    /** The pattern's own flight. True when the round has ended this step. */
    private boolean stepSpecial(World w, float dt) {
        WeaponDef d = def;
        if (d.grow > 0f) radius += d.grow * dt;
        if (d.drag > 0f) {
            float sp = (float) Math.sqrt(vx * vx + vy * vy), ns = Math.max(0f, sp - d.drag * dt);
            if (sp > 0.001f) { vx *= ns / sp; vy *= ns / sp; }
        }
        if (d.waveAmp > 0f) {                                   // a sine across the line of flight
            float sp = (float) Math.sqrt(vx * vx + vy * vy);
            if (sp > 1f) {
                float lat = d.waveAmp * d.waveFreq * (float) Math.cos(d.waveFreq * age + phase) * dt;
                x += -vy / sp * lat;
                y += vx / sp * lat;
            }
        }
        if (d.boomerang) {
            if (!turned && age >= d.life * 0.5f) turned = true;
            if (turned) {                                       // home on the hull that threw it
                float tx = w.ship.x - x, ty = w.ship.y - y, dist = (float) Math.sqrt(tx * tx + ty * ty);
                if (dist < 34f) { dead = true; return true; }
                float sp = d.speed;
                vx = tx / dist * sp;
                vy = ty / dist * sp;
            }
        }
        if (d.pullRadius > 0f) {
            for (Enemy e : w.enemies) {
                if (e.dead || e.boss || e.submerged()) continue;
                float dx = x - e.x, dy = y - e.y, dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > d.pullRadius || dist < 8f) continue;
                float k = d.pull * dt * (1f - 0.5f * dist / d.pullRadius);
                float nx = e.x + dx / dist * k, ny = e.y + dy / dist * k;
                if (!w.blockedPx(nx, ny)) { e.x = nx; e.y = ny; }
            }
        }
        if (d.prox > 0f && age > 0.45f) {                       // a mine, armed
            for (Enemy e : w.enemies) {
                if (e.dead || e.submerged()) continue;
                float dx = e.x - x, dy = e.y - y;
                if (dx * dx + dy * dy < (d.prox + e.w / 2) * (d.prox + e.w / 2)) { detonate(w); return true; }
            }
        }
        if (d.splitCount > 0 && d.splitAt >= 0f && !split && age >= d.splitAt) {
            splitNow(w);
            dead = true;
            return true;
        }
        return false;
    }

    private void steer(World w, float dt) {
        Enemy best = null;
        float bestD = 700f * 700f;
        for (Enemy e : w.enemies) {
            if (e.dead) continue;
            float dx = e.x - x, dy = e.y - y;
            float d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        if (best == null) return;
        float desired = (float) Math.atan2(best.y - y, best.x - x);
        float cur = (float) Math.atan2(vy, vx);
        float max = homingTurn * dt;
        float turn = Math.max(-max, Math.min(max, World.wrapAngle(desired - cur)));
        float sp = (float) Math.sqrt(vx * vx + vy * vy);
        float na = cur + turn;
        vx = (float) Math.cos(na) * sp;
        vy = (float) Math.sin(na) * sp;
    }

    // ----------------------------------------------------------------------------------- hits
    /** May this round hurt that foe now? (A piercing round that lingers hits a foe once per `rehit`.) */
    public boolean hits(Enemy e) {
        if (def == null || def.rehit <= 0f || !pierceEnemies) return true;
        for (int i = 0; i < seen.length; i++) if (seen[i] == e && age - seenAt[i] < def.rehit) return false;
        return true;
    }

    /** The damage this round does to that foe (an executioner's stroke is worth more on a wounded one). */
    public float damageTo(Enemy e) {
        if (special() && def.executeBelow > 0f && e.hp < e.maxHp * def.executeBelow) return damage * def.executeMult;
        return damage;
    }

    /** After the hit has landed: the pattern's rider. Returns true when the round is spent by it. */
    public boolean onHit(World w, Enemy e, boolean killed) {
        if (def == null) return false;
        if (def.rehit > 0f) {
            int k = 0;
            for (int i = 0; i < seen.length; i++) if (seen[i] == null || seenAt[i] < seenAt[k]) { k = i; if (seen[i] == null) break; }
            seen[k] = e;
            seenAt[k] = age;
        }
        if (!special() || w.clientSeat) return false;
        int seat = owner < 0 ? 0 : owner;
        if (!killed) {
            if (def.burnDps > 0f) e.ignite(def.burnDps, def.burnTime, seat);
            if (def.slow > 0f) e.slowBy(def.slow, def.slowTime);
            if (def.stun > 0f) e.freeze(def.stun);
            if (def.knock > 0f && !e.boss) {
                float sp = (float) Math.sqrt(vx * vx + vy * vy);
                if (sp > 1f) { e.vx += vx / sp * def.knock; e.vy += vy / sp * def.knock; }
            }
        }
        if (def.leech > 0f && w.ship != null) w.ship.hp = Math.min(w.ship.maxHp(), w.ship.hp + def.leech);
        if (def.chain > 0 && chainFrom(w, e)) return true;
        if (def.splitCount > 0 && def.splitOnHit && !split) {
            splitNow(w);
            return true;
        }
        return false;
    }

    /** Arc Chain: jump from the foe struck to the nearest others, each a little weaker. */
    private boolean chainFrom(World w, Enemy first) {
        Enemy from = first;
        float dmg = damage;
        Enemy[] done = new Enemy[def.chain + 1];
        done[0] = first;
        int n = 1;
        for (int j = 0; j < def.chain; j++) {
            Enemy best = null;
            float bestD = def.chainRange * def.chainRange;
            for (Enemy o : w.enemies) {
                if (o.dead || o.submerged()) continue;
                boolean used = false;
                for (int k = 0; k < n; k++) if (done[k] == o) used = true;
                if (used) continue;
                float dx = o.x - from.x, dy = o.y - from.y, d = dx * dx + dy * dy;
                if (d < bestD) { bestD = d; best = o; }
            }
            if (best == null) break;
            dmg *= 0.8f;
            w.addEffect(Effect.flash(from.x, from.y, best.x, best.y, r, g, b));
            w.addEffect(Effect.flash(from.x, from.y, best.x, best.y, 1f, 1f, 1f));
            w.shooter = owner < 0 ? 0 : owner;
            best.hurt(dmg, w);
            done[n++] = best;
            from = best;
        }
        return false;
    }

    private void splitNow(World w) {
        split = true;
        WeaponDef d = def;
        int n = d.splitCount;
        float heading = (float) Math.atan2(vy, vx);
        boolean radial = d.splitSpread >= 359f;
        WeaponDef kid = shard(d);
        for (int i = 0; i < n; i++) {
            float a;
            if (radial) a = heading + i * (float) (Math.PI * 2) / n;
            else a = heading + (n == 1 ? 0f : (i / (float) (n - 1) - 0.5f) * (float) Math.toRadians(d.splitSpread));
            int before = w.bullets.size();
            w.spawnBullet(x, y, a, kid, 1f, dmgRatio);
            if (w.bullets.size() > before) w.bullets.get(w.bullets.size() - 1).owner = owner;
        }
        w.addEffect(Effect.ring(x, y, 36f, r, g, b));
    }

    private static WeaponDef shard(WeaponDef parent) {
        if (parent.shard == null) parent.shard = parent.shardOf();
        return parent.shard;
    }

    /** Boom. Only the player's shots leave a mark; splash goes through World so hits live in one place. */
    public void detonate(World w) {
        dead = true;
        if (hostile) return;
        w.shooter = owner < 0 ? 0 : owner; // an unstamped blast is this seat's own
        if (aoe > 0) {
            w.areaDamage(x, y, aoe, damage);
            w.addEffect(Effect.ring(x, y, aoe, r, g, b));
        } else {
            w.spray(x, y, vx, vy, r, g, b);
        }
        if (special() && def.zoneRadius > 0f) w.addZone(x, y, def.zoneRadius, def.zoneTime, def.zoneDps * dmgRatio, owner < 0 ? 0 : owner, r, g, b);
        if (special() && def.splitCount > 0 && !split && (def.splitAt < 0f)) splitNow(w);
    }

    private void expire(World w) {
        detonate(w);
    }
}
