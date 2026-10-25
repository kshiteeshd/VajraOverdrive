package vajra.enemy;

import vajra.weapon.WeaponDef;
import vajra.world.Effect;
import vajra.world.World;

/** Placed enemy: orbits a leash around its home, wakes near the player, fights back.
 *  Position accumulates (Brain writes velocity), so the AI can actually travel. */
public class Enemy {
    public enum Kind {
        MAYAVI(90, 4.2f, 105), HATAKA(45, 3.8f, 82), AURIS(35, 3.0f, 100),
        LANCE(50, 2.4f, 90), VAKRA(65, 3.4f, 118), BHAVI(85, 2.6f, 130),
        BALA(1500, 0f, 110), // fireCd 0 = never auto-fires: BossBrain runs every attack
        // THE KEEPERS: enemies with a job. They stand what the world needs standing - a gate, a
        // checkpoint, a hangar bay - and fight to keep it. Appended after BALA so every wire id
        // and saved ordinal of the older kinds stays put.
        DVARAPALA(150, 3.2f, 60), // gatekeeper: armoured door-ward, slow heavy shell
        DIPAKA(60, 2.4f, 85),     // lamp-keeper: tends a checkpoint, mends its comrades
        VANIJA(110, 2.9f, 55);    // bay-master: runs a hangar bay, never leaves it

        public final float hp;
        public final float fireCd; // 0 = never fires
        public final float speed;  // tier cruise: fodder plods, berserkers run

        /** A keeper holds a post for the world rather than hunting for the ruler's quota. */
        public boolean keeper() { return this == DVARAPALA || this == DIPAKA || this == VANIJA; }

        Kind(float hp, float fireCd, float speed) {
            this.hp = hp;
            this.fireCd = fireCd;
            this.speed = speed;
        }
    }

    /** ENEMY GUNS: one firing signature per kind, indexed by Kind ordinal (BALA stays null —
     *  BossBrain runs his own rows). Same WeaponDef machinery the boss and the player use,
     *  so a loka scales its guns by swapping this table, not by new code.
     *
     *  <p>ATALA FIRES ONE BULLET. Every kind's attack is a single round (`count(1)`), so the
     *  screen fills with fewer, readable threats and the difficulty lives in WHAT one round
     *  costs rather than how many arrive — see {@link #RANK_DAMAGE}. The numbers are gentle
     *  because the signature (speed, silhouette, tell) is the feature; the damage dials come
     *  up in later loka through `LokaTable.dmgScale`. */
    public static final WeaponDef[] GUN = new WeaponDef[Kind.values().length];

    /** A single round, graded by who is firing: regular / rank-1 commander / elite. Rank used
     *  to mean nothing to a bullet's damage — only loot — so a court looked identical to its
     *  fodder. Now the senior's one round simply hits harder. */
    public static final float[] RANK_DAMAGE = { 1f, 1.45f, 1.9f };

    static {
        GUN[Kind.MAYAVI.ordinal()] = WeaponDef.mk("mayavi lob")
                .speed(230).damage(9).bulletSize(8).life(4.5f)
                .colour(0.75f, 0.40f, 1.00f);                 // marked indirect shell
        GUN[Kind.HATAKA.ordinal()] = WeaponDef.mk("hataka ring")
                .count(1).speed(260).damage(8).bulletSize(5).life(2.5f)
                .colour(0.60f, 0.90f, 0.35f);                 // the betray shot, vented evenly
        GUN[Kind.AURIS.ordinal()] = WeaponDef.mk("auris snap")
                .speed(340).damage(7).bulletSize(5).life(3.5f)
                .colour(0.55f, 0.95f, 1.00f);                 // one fast pellet
        GUN[Kind.LANCE.ordinal()] = WeaponDef.mk("lance tap")
                .speed(620).damage(9).bulletSize(4).life(3f)
                .colour(1.00f, 0.95f, 0.75f);                 // the sentry's fast aimed tap
        GUN[Kind.VAKRA.ordinal()] = WeaponDef.mk("vakra fan")
                .speed(300).damage(8).bulletSize(6).life(3.5f)
                .colour(1.00f, 0.45f, 0.15f);                 // one round down the weave
        GUN[Kind.DVARAPALA.ordinal()] = WeaponDef.mk("dvarapala shell")
                .speed(250).damage(13).bulletSize(9).life(3.2f)
                .colour(0.80f, 0.65f, 1.00f);                 // the door-ward's slow violet shell
        GUN[Kind.DIPAKA.ordinal()] = WeaponDef.mk("dipaka spark")
                .speed(380).damage(5).bulletSize(4).life(2.5f)
                .colour(1.00f, 0.88f, 0.55f);                 // a lamp's quick warm spark
        GUN[Kind.VANIJA.ordinal()] = WeaponDef.mk("vanija toll")
                .speed(320).damage(9).bulletSize(6).life(3f)
                .colour(1.00f, 0.80f, 0.30f);                 // the toll: one aimed gold coin
        GUN[Kind.BHAVI.ordinal()] = WeaponDef.mk("bhavi shell")
                .speed(300).damage(12).bulletSize(9).life(3f)
                .colour(1.00f, 0.35f, 0.20f);                 // one heavy shell, close range
    }

    /** HATAKA's death-throes burst is NOT its attack — it is a one-time ring vented on the way
     *  down, and the tell that makes sweeping it carelessly expensive. Kept as a ring because
     *  that is what makes it readable, but cut from five rounds at 5 to three at 4: it was the
     *  single biggest source of bullet spam in the game. */
    private static final int BETRAY_COUNT = 3;
    private static final float BETRAY_DAMAGE = 4f;

    private static final float FIRE_RANGE = 620;
    private static final float BHAVI_RANGE = 450;  // the rusher's gun only speaks up close
    public static final float ARMS = 600; // call to arms: hit wakes sleepers this close, top 3 regions only
    public static final float RECOVER = 2f; // straight-home window after a knock, seconds

    // --- SIZE BY HIERARCHY. The sheets are 64 px of art; a body is drawn smaller than that, by kind
    //  (a scout is a small thing, a door-ward is a big one) and then by rank: a leader is a little
    //  larger than its court and an elite larger again. The hitbox is the body's core, a bit inside
    //  the drawn size, and grows with the same rank factor. ---
    public static final float LEADER_SCALE = 1.12f, ELITE_SCALE = 1.25f;

    /** Drawn size of a plain unit of this kind, px. */
    public static float baseDraw(Kind k) {
        switch (k) {
            case AURIS: return 34;
            case HATAKA: return 42;
            case LANCE: return 38;
            case VAKRA: case BHAVI: case MAYAVI: return 46;
            case DVARAPALA: return 58;
            case DIPAKA: return 42;
            case VANIJA: return 52;
            default: return 96; // Bala keeps his older art, drawn at his own box
        }
    }

    /** The NAME a kind goes by in a loka. The kinds are role slots shared by every loka (so every brain and gun keeps working); Vitala's roster
     *  and its names come from the original Vajra Overdrive bestiary (docs/reboot/BESTIARY.md, docs/LOKA-LAWS.md): Agneya the Ember, Jvala the
     *  Cinder, Tamra the Ore, Kila the Nail, Dhuma the Smoke, Rekha the Line, and the boss Bhairava, Herald of Cinders. */
    public static String nameIn(Kind k, int loka) {
        if (loka == 1) {
            switch (k) {
                case AURIS: return "DHUMA";      // the Smoke: a small ghost that vanishes and reappears
                case HATAKA: return "REKHA";     // the Line: arms wide, it draws a hairline between its hands
                case LANCE: return "KILA";       // the Nail: thin and fast
                case VAKRA: return "AGNEYA";     // the Ember: a burning dart
                case BHAVI: return "TAMRA";      // the Ore: copper-plated, slow, heavy
                case MAYAVI: return "JVALA";     // the Cinder: hovers and drops burning ground
                case BALA: return "BHAIRAVA";    // Herald of Cinders, Lord of Burning Gold
                default: break;
            }
        }
        return k.name();
    }

    /** Hitbox side of a plain unit of this kind, px. */
    public static float hitSize(Kind k) {
        switch (k) {
            case AURIS: return 28;
            case HATAKA: return 32;
            case LANCE: return 28;
            case VAKRA: return 36;
            case BHAVI: case MAYAVI: return 38;
            case DVARAPALA: return 44;
            case DIPAKA: return 32;
            case VANIJA: return 40;
            default: return 96;
        }
    }

    /** 1 for the rank and file, a little more for a leader, more again for an elite. The ruler is his own size. */
    public float rankScale() {
        return boss ? 1f : elite ? ELITE_SCALE : rank >= 1 ? LEADER_SCALE : 1f;
    }

    /** The size this unit is drawn at, px. */
    public float drawSize() {
        if (voice) return 48; // the people have sheets of their own (NpcImages)
        return kind == Kind.BALA ? w : baseDraw(kind) * rankScale();
    }

    public Kind kind;
    public float x, y, vx, vy;
    public float homeX, homeY;
    public float spawnX, spawnY; // immutable spawn post: restored if a rout finds no new senior
    public float w, h;
    public float speed;    // tier cruise from the Kind; rank-1 units run hotter (Brain)
    public float hp, maxHp;
    public float dispHp;   // hp the bar displays: eases onto hp so damage drains smoothly
    public Enemy commander;
    public int rank;        // 1 = can lead a formation (court commander, elite), 0 = regular
    public float contactCd; // ram cooldown, driven by World's contact pass
    public float recover;   // seconds left of flying straight home after a knock, flair paused
    public boolean sentry;
    public boolean guard;   // stationed post: not part of the mirrored order of battle
    public boolean parley;    // held at its post for a cutscene: no wake, no fire, until released
    public boolean siteGuard; // stands on a shop or checkpoint: ignores cache triggers
    public boolean elite;
    public boolean hunt;   // a cache ambush: converge straight on the ship, no ring, no leash
    // --- THE VOICES: a person, never a target. Lives off the roster (World.voices); the flag
    //  is the class law that spans every damage/fire path in this file and Brain's hover ---
    public boolean voice;
    public boolean voiceMet; // Chatter's first sight already spoken, this run
    public String voiceName; // which person this is ("SCOUT"/"MAYA"/"KALIYA") - Chatter keys on it
    // --- BOSS (Bala) only: phase machine state, ignored by every other unit ---
    public boolean boss;        // run by BossBrain: own wake rule, own HUD bar, own attacks
    public boolean dismissed;   // the commander stood this unit down: leave the arena, no fire
    public boolean bossHidden;  // mid-blink: not drawn, not hit, not colliding
    public boolean bossInvuln;  // stagger beat between phases: hits are ignored
    public boolean bossTelegraph;
    public int bossPhase = 1;   // 1 commander / 2 duel / 3 berserk
    public int bossPattern;     // 0 volley / 1 spiral / 2 mortar / 3 shock
    // MEMORY (step 1 of the wave plan): what each pattern costs him and deals to the hull.
    // BossBrain reads these at every pick — the two patterns the hull punishes hardest retire
    // for a spell, and the pattern that has seared the hull most carries the most weight.
    public final float[] bossTook = new float[4];
    public final float[] bossDeal = new float[4];
    public int bossBanA = -1, bossBanB = -1; // the retiring patterns, edited by BossBrain
    public int bossMind;        // study beats counted
    public float bossDealStart; // the hull's hp when the deal window opened
    public float bossTimer;     // telegraph, stagger or hidden countdown
    public float bossStream;    // spiral shots still to whip out
    public float bossBlink;     // seconds to the next blink (phase 3)
    public float bossAngle;     // spiral rotation / shockwave gap axis
    public float bossMx, bossMy;// mortar marker: where he aimed during the tell
    public float dismissAge;    // seconds since the stand-down (dismissed units only)
    public boolean active;
    // CO-OP: swarm ghosts. A ghost is a render object keyed on the host's netId; its body
    // never simulates (the host runs the fight) and glides to its last snapshot truth.
    public int netId;
    public boolean ghost, gone, seen;
    public float gx, gy;
    public boolean routed;
    public float routAge; // seconds since this unit started running
    public boolean betrayed; // HATAKA: the betray ring has already vented at half hull
    public boolean dead;
    public float facing;
    public float phase;
    public float wakeRange = 900;
    public float fireTimer, hitFlash;
    public float patrolAmp = 40;   // orbit radius about home
    public float orbitRate = 0.3f; // rad/s around that orbit
    public float dmgScale = 1f;  // the loka's fire multiplier (LokaTable.dmgScale)

    // THE DEPTH: the world is a shaft, and below the play layer, down at its floor, a second population lives. A body has a DEPTH, 0 on the play
    // layer and 1 at the floor; below 0.4 it is gone from the fight (no body, no fire, nothing can touch it) and is drawn small and dark UNDER the
    // walls. Two things send a body down and bring it up: a scout's FLANKING DIVE (it sinks, runs beneath you and surfaces at your side), and a
    // leader's CALL (the reserves wait below and rise at his word, see EnemyDirector.tickBackup).
    public float depth;
    public int diveState;          // 0 up and about, 1 sinking, 2 running below, 3 rising
    public float diveT, diveCd = 5f, diveFromX, diveFromY, diveToX, diveToY;
    /** A leader's calls for reserves left (-1 = not set yet) and the seconds until the next. */
    public int backups = -1;
    public float callT = 4f;

    /** VITALA'S BHUTA (the wraith) PHASES: it drifts through walls as if they were mist.  Inside rock it has no body - it cannot be hit or rammed
     *  by anything and it does not fire, the same law as a body down at the floor - and it comes out of the far side whole. */
    public boolean phasing, inRock;

    /** VITALA'S OWN (docs/VITALA_ENEMIES.md): a unit placed in Vitala flies {@link VitalaBrain}, not Atala's Brain. It may be SMOKED (Dhuma: translucent
     *  and untargetable for a moment), ARMOURED (Tamra: copper plate that rings off bolts from the front), and it carries the scratch state of its
     *  brain; a leader also carries the formation it is flying and the trail its column follows. */
    /** HOLD YOUR POST: an awake unit whose hull has gone far from its place STANDS DOWN - it flies back to where it was put (spawnX/Y) and goes to
     *  sleep there, formation restored; the band it was placed in is a wall it cannot pass ({@link #bandMin}, {@link #bandMax}). */
    public boolean standDown;
    public float bandMin = -1e9f, bandMax = 1e9f;
    public static final float TETHER = 760f;
    public static final float LOSE_FACTOR = 1.7f;     // the hull must be this many wake ranges from its post before it stands down
    public boolean vitala, smoked, armoured;
    public int form, vState, vN, vN2, formIdx, slot, slotN, cycle;
    public float clockT, trailT, slotT;
    public float formT, vT, vT2, vA, vX, vY, vX2, vY2;
    public float[] trail;
    public int trailAt;
    /** The heading of the round that is hurting it right now (armour reads it): (0, 0) when the hurt is not a round. */
    public float hitVx, hitVy;
    /** THE ASTRA FLOW (world/AstraFlow): seconds a tornado still holds this foe (its brain sleeps and the flow moves it), its throw velocity, and how long it burns
     *  (and whose fire it is) after Agni's flames touched it. */
    public float caught, flowVx, flowVy, burnT;
    public int burnOwner;
    public float burnPuffT;

    /** Down at the floor, in the rock, or in smoke: no body in the fight. */
    public boolean submerged() { return depth > 0.4f || inRock || smoked; }

    /** Scouts and skirmishers may dive: the rank and file of a roaming kind, never a leader, a keeper, the ruler or a person. */
    private boolean canDive() {
        return (kind == Kind.AURIS || kind == Kind.VAKRA) && !vitala && !boss && !elite && rank == 0 && !voice && !ghost && !parley && !dismissed && !routed
                && !standDown;
    }

    private static final float DIVE_LEASH = 460f;

    private void startDive(World world) {
        vajra.ship.Ship hull = world.nearestHull(x, y);
        if (Math.hypot(hull.x - spawnX, hull.y - spawnY) > 900f) { diveCd = 3f; return; }       // a dive is a flank, never a journey
        for (int tries = 0; tries < 24; tries++) {
            float a = world.fxRng.nextFloat() * 6.2832f, r = 320f + world.fxRng.nextFloat() * 120f;
            float nx = hull.x + (float) Math.cos(a) * r, ny = hull.y + (float) Math.sin(a) * r;
            if (Math.hypot(nx - spawnX, ny - spawnY) > DIVE_LEASH || ny < bandMin || ny > bandMax) continue;   // and it lands within reach of its post
            if (!world.openFor(nx, ny, Math.max(12f, w * 0.5f))) continue;
            diveFromX = x;
            diveFromY = y;
            diveToX = nx;
            diveToY = ny;
            diveState = 1;
            diveT = 0;
            return;
        }
        diveCd = 3f;
    }

    private void diveStep(float dt, World world) {
        boolean wasUnder = submerged();
        switch (diveState) {
            case 1:
                depth = Math.min(1f, depth + dt / 0.8f);
                if (depth >= 1f) { diveState = 2; diveT = 0; }
                break;
            case 2: {
                diveT += dt;
                float u = Math.min(1f, diveT / 1.8f);
                x = diveFromX + (diveToX - diveFromX) * u;
                y = diveFromY + (diveToY - diveFromY) * u;
                if (u >= 1f) diveState = 3;
                break;
            }
            case 3:
                depth = Math.max(0f, depth - dt / 1.0f);
                if (depth <= 0f) {
                    diveState = 0;
                    diveCd = 9f + world.fxRng.nextFloat() * 6f;
                    homeX = x;
                    homeY = y;
                }
                break;
            default:
                depth = Math.max(0f, depth - dt);
                break;
        }
        boolean nowUnder = submerged();
        if (wasUnder && !nowUnder) { // it breaks the surface: a ring and a spray of grit
            world.addEffect(Effect.ring(x, y, 46, 0.9f, 0.8f, 0.55f));
            for (int i = 0; i < 8; i++) {
                float a = (float) (i * Math.PI / 4), v = 90f + world.fxRng.nextFloat() * 90f;
                world.addEffect(Effect.spark(x, y, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 3f, 0.4f, 3f, 0.85f, 0.75f, 0.55f));
            }
        } else if (!wasUnder && nowUnder) {
            world.addEffect(Effect.ring(x, y, 30, 0.5f, 0.45f, 0.4f));
        }
    }

    public Enemy(Kind kind, float x, float y) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.homeX = x;
        this.homeY = y;
        this.spawnX = x;
        this.spawnY = y;
        this.w = hitSize(kind);
        this.h = this.w;
        this.hp = this.maxHp = kind.hp;
        this.dispHp = kind.hp;
        this.speed = kind.speed;
        this.phase = (x * 0.37f + y * 0.73f) % 6.28f;
        this.fireTimer = kind.fireCd > 0 ? 1f + phase * 0.15f : 0f;
    }

    public void update(float dt, World world, float t) {
        if (dead || parley) return;
        if (caught > 0f) { caught -= dt; if (hitFlash > 0) hitFlash -= dt; return; }   // the tornado has it: the flow moves it, its brain waits
        if (fireTimer > 0) fireTimer -= dt;
        if (hitFlash > 0) hitFlash -= dt;
        if (contactCd > 0) contactCd -= dt;
        if (recover > 0) recover -= dt;
        if (diveState != 0 || depth > 0f) {            // sinking, running below, or rising: while it is down there is no AI and no fire
            diveStep(dt, world);
            if (submerged()) { vx = 0; vy = 0; return; }
        } else if (active && canDive() && (diveCd -= dt) <= 0f) startDive(world);
        dispHp += (hp - dispHp) * (1f - (float) Math.exp(-12f * dt)); // bar eases onto hp
        if (boss && bossHidden) { // blink: timers only — no body, no fire
            BossBrain.think(this, world, dt, t);
            return;
        }
        if (!active) {
            if (boss) { // the boss wakes on the crossing, not on proximity
                if (world.deepestHullY() < world.bossWakeY) return;
                active = true;
                world.cue(world.stage == 0 ? vajra.world.Cue.BALA_WAKES : vajra.world.Cue.BHAIRAVA_WAKES);
                if (world.stage == 1) vajra.world.Codex.unlock(world, vajra.world.Codex.BHAIRAVA);
                world.orderRetinue(this);
            } else {
                vajra.ship.Ship hull = world.nearestHull(homeX, homeY);
                float dx = hull.x - homeX, dy = hull.y - homeY;
                if (dx * dx + dy * dy >= wakeRange * wakeRange) return;
                active = true;
            }
        }
        if (standDown) { goHome(world, dt); return; }                   // (World.holdPosts says who stands down: a unit driven alone never does)
        if (kind == Kind.DIPAKA && !ghost && !routed) mend(world, dt);
        if (boss) {
            if (vitala) BhairavaBrain.think(this, world, dt, t);
            else BossBrain.think(this, world, dt, t);
        } else if (vitala) VitalaBrain.think(this, world, dt, t);
        else Brain.think(this, world, dt, t);
        float px0 = x, py0 = y;
        x += vx * dt;
        y += vy * dt;
        if (phasing) inRock = world.blockedPx(x, y);                   // a bhuta goes where it likes
        else {
            float[] c = world.collide(x - w / 2, y - h / 2, w, h);
            x = c[0] + w / 2;
            y = c[1] + h / 2;
        }
        if (!boss) {                                                    // the band it was placed in is a wall: no unit leaves it, none enters Bhu Loka
            if (y < bandMin) { y = bandMin; if (vy < 0) vy = 0; }
            if (y > bandMax) { y = bandMax; if (vy > 0) vy = 0; }
            if (!hunt && !phasing) {                                    // and the post is a tether: whatever the brain wants, a unit is never farther from it than this
                float tx = x - spawnX, ty = y - spawnY, td = (float) Math.sqrt(tx * tx + ty * ty);
                if (td > TETHER) { x = spawnX + tx / td * TETHER; y = spawnY + ty / td * TETHER; vx *= 0.5f; vy *= 0.5f; }
            }
        }
        if (!phasing) notice(world, dt, px0, py0);                      // (a bhuta is never "stuck in rock")
        fire(world);
    }

    /** The flight home of a unit that has stood down: toward the place it was put, round the rock, and to sleep there once it arrives (a unit that
     *  cannot get there in time is set down on the post: nobody is lost). */
    private void goHome(World world, float dt) {
        smoked = false;
        vState = 0;
        float gx0 = world.blockedPx(spawnX, spawnY) ? homeX : spawnX, gy0 = world.blockedPx(spawnX, spawnY) ? homeY : spawnY;   // (a post in rock would hold a unit in rock)
        float dx = gx0 - x, dy = gy0 - y, d = (float) Math.sqrt(dx * dx + dy * dy);
        standT += dt;
        if (d < 26f || standT > 14f) {
            x = gx0; y = gy0; vx = vy = 0f;
            homeX = gx0; homeY = gy0;
            active = false;
            standDown = false;
            standT = 0f;
            return;
        }
        float cruise = Math.max(90f, speed * 1.4f);
        float[] g = VitalaBrain.steer(this, world, dx / d * cruise, dy / d * cruise);
        vx += (g[0] - vx) * Math.min(1f, 5f * dt);
        vy += (g[1] - vy) * Math.min(1f, 5f * dt);
        float[] c = world.collide(x + vx * dt - w / 2, y + vy * dt - h / 2, w, h);
        x = c[0] + w / 2;
        y = c[1] + h / 2;
        if (y < bandMin) y = bandMin;
        if (y > bandMax) y = bandMax;
        if (!phasing && world.blockedPx(x, y)) unstick(world);          // never flies home THROUGH the rock
    }

    private float standT;

    /** Each kind fires its own signature (GUN): count + spread from the def, cadence from the
     *  kind, LOS + range gates unchanged. Wounded units fire faster; BHAVI's shell only speaks
     *  up close; MAYAVI marks where its lob lands; LANCE flashes a one-frame laser sight.
     *  Routed and dismissed units never fire: rout means neutral, stand-down means gone. */
    private void fire(World w) {
        if (vitala || voice || routed || dismissed || inRock || fireTimer > 0 || kind.fireCd <= 0) return;   // (Vitala's units fire from VitalaBrain)
        vajra.ship.Ship hull = w.nearestHull(x, y); // aim at whoever's closest to the muzzle
        float dx = hull.x - x, dy = hull.y - y;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 1 || d > (kind == Kind.BHAVI ? BHAVI_RANGE : FIRE_RANGE)) return;
        if (!w.clearLine(x, y, hull.x, hull.y)) return;
        fireTimer = kind.fireCd * (hp < maxHp * 0.3f ? 0.7f : 1f); // wounded: desperation cadence
        WeaponDef gun = GUN[kind.ordinal()];
        float aim = (float) Math.atan2(dy, dx);
        switch (kind) {
            case LANCE: // the tell: a laser sight along the shot line, one frame
                w.addEffect(Effect.flash(x, y, hull.x, hull.y, 1f, 0.95f, 0.75f));
                break;
            case MAYAVI: // the lob's marker shows exactly where the slow shell lands
                w.addEffect(Effect.ring(hull.x, hull.y, 70, gun.r, gun.g, gun.b));
                break;
            default:
                break;
        }
        for (int i = 0; i < gun.count; i++) {
            float off = gun.count == 1 ? 0
                    : (float) (i * gun.spreadDeg / (gun.count - 1) - gun.spreadDeg / 2);
            w.spawnEnemyBullet(x, y, aim + (float) Math.toRadians(off), gun,
                    dmgScale * RANK_DAMAGE[rankGrade()]);
        }
    }

    // AWARE OF ITS SURROUNDINGS: a unit that pushes against rock and goes nowhere for over a second
    // finds the nearest open air and steps into it, and one that ever ends up inside rock is lifted
    // out at once. (Overlap with mates is resolved by World.separateEnemies.)
    private static final float STUCK_AFTER = 1.2f;
    private float stuckT;

    private void notice(World world, float dt, float px0, float py0) {
        float want = (float) Math.sqrt(vx * vx + vy * vy);
        float moved = (float) Math.sqrt((x - px0) * (x - px0) + (y - py0) * (y - py0));
        if (want > 40 && moved < want * dt * 0.12f) stuckT += dt;
        else stuckT = Math.max(0, stuckT - dt * 2);
        if (stuckT > STUCK_AFTER || world.blockedPx(x, y)) unstick(world);
    }

    /** Step into the nearest open air, ring by ring; failing that, go home. */
    private void unstick(World world) {
        stuckT = 0;
        float half = Math.max(12, w * 0.5f);
        for (float r = 20; r <= 140; r += 24)
            for (int k = 0; k < 8; k++) {
                float a = (float) (k * Math.PI / 4);
                float nx = x + (float) Math.cos(a) * r, ny = y + (float) Math.sin(a) * r;
                if (world.openFor(nx, ny, half)) {
                    x = nx;
                    y = ny;
                    vx = 0;
                    vy = 0;
                    return;
                }
            }
        x = homeX;
        y = homeY;
        vx = 0;
        vy = 0;
    }

    // THE LAMP-KEEPER'S WORK: every few seconds the lamp pulses and mends the comrades near it.
    private static final float MEND_EVERY = 5f, MEND_R = 240f, MEND_FRAC = 0.12f;
    private float mendT = MEND_EVERY;

    private void mend(World w, float dt) {
        if ((mendT -= dt) > 0) return;
        mendT = MEND_EVERY;
        boolean any = false;
        for (Enemy o : w.enemies) {
            if (o == this || o.dead || o.voice || o.ghost || o.hp >= o.maxHp) continue;
            float dx = o.x - x, dy = o.y - y;
            if (dx * dx + dy * dy > MEND_R * MEND_R) continue;
            o.hp = Math.min(o.maxHp, o.hp + o.maxHp * MEND_FRAC);
            any = true;
        }
        if (any) w.addEffect(Effect.ring(x, y, MEND_R, 1f, 0.9f, 0.5f));
    }

    /** TAMRA'S COPPER: a round that comes from the front (against the way the plate faces) rings off at 40%, one from the flank bites at 70%, one
     *  from behind takes it whole. A hurt that is not a round (heat, a vent) has no heading and takes full damage. */
    private float armourFactor(World w) {
        float sp = (float) Math.sqrt(hitVx * hitVx + hitVy * hitVy);
        if (sp < 1f) return 1f;
        float fx = (float) Math.cos(facing), fy = (float) Math.sin(facing);        // the plate faces the hull
        float dot = (hitVx * fx + hitVy * fy) / sp;                                // -1: the round is coming straight at the plate
        float f = dot < -0.35f ? 0.4f : dot < 0.35f ? 0.7f : 1f;
        if (f < 1f) {
            w.addEffect(Effect.ring(x, y, 12, 1f, 0.7f, 0.3f));                     // the clink
        }
        hitVx = hitVy = 0f;
        return f;
    }

    /** The unit is past caring: badly hurt, hunting, betrayed, or the ruler in his last phase.
     *  The sprite sheet's rage frames play for exactly this. */
    public boolean raging() {
        return hp < maxHp * 0.3f || hunt || betrayed || (boss && bossPhase >= 3);
    }

    /** Which RANK_DAMAGE row this unit fires with: an elite outranks a commander outranks fodder.
     *  Public: the headless test pins which grade a unit fires with. */
    public int rankGrade() {
        return elite ? 2 : Math.min(2, rank);
    }

    /** Returns true if this killed it. Rout runs outward from the commander's home. */
    public boolean hurt(float amount, World w) {
        return hurt(amount, w, 0);
    }

    /** A hit that nothing turns aside: plate does not blunt it and a body in rock or smoke is not safe from it (the Pashupatastra's gaze). */
    public boolean hurtPiercing(float amount, World w, int owner) {
        boolean plate = armoured, rock = inRock, smoke = smoked;
        armoured = false;
        inRock = false;
        smoked = false;
        try {
            return hurt(amount, w, owner);
        } finally {
            armoured = plate;
            inRock = rock;
            smoked = smoke;
        }
    }

    /** The same hit, with the roster id that fired it (0 = this seat). */
    public boolean hurt(float amount, World w, int owner) {
        if (dead || submerged()) return false;         // (a body down at the floor, in rock or in smoke cannot be touched)
        if (voice) return false; // a person: nothing to reward, nothing to spend
        // A GHOST is the host's word about a foe, not a body. On a guest seat it must not be
        // hurt at all: the host owns every verdict, and a guest killing its own copy used to
        // score phantom kills, drop phantom loot and delete the host's real drops.
        if (ghost && w.clientSeat) return false;
        if (boss && (bossInvuln || bossHidden)) return false; // stagger beats and blinks eat hits
        parley = false; // struck first: the talking is over
        if (armoured) amount *= armourFactor(w);
        hp = Math.max(0, hp - amount);
        if (boss) bossTook[bossPattern] += amount; // MEMORY: what this pattern cost while it played
        hitFlash = 0.12f; // the hit strobe
        if (boss && !active) w.orderRetinue(this); // shot awake: the ring still forms
        active = true;
        // HATAKA's betray: halfway down it vents once — the weakling bites back on the sweep
        if (kind == Kind.HATAKA && hp > 0 && !betrayed && hp < maxHp * 0.5f) {
            betrayed = true;
            betray(w);
        }
        if (y >= w.regionStart[3]) { // CALL TO ARMS: the top three regions hear the fight...
            for (Enemy o : w.enemies) {
                if (o == this || o.dead || o.active || o.voice) continue;
                float ox = o.x - x, oy = o.y - y;
                if (ox * ox + oy * oy < ARMS * ARMS) o.active = true; // ...and wake up
            }
        }
        if (hp > 0) {
            w.addEffect(Effect.spark(x, y, 1f, 0.9f, 0.6f));
            return false;
        }
        dead = true;
        if (kind == Kind.HATAKA) betray(w); // and once more on the way down
        int prev = w.shooter;
        if (owner != 0) w.shooter = owner; // a guest's volley, resolved here: the kill is theirs
        w.countKill(this);
        w.shooter = prev;
        w.addEffect(Effect.ring(x, y, boss ? 500 : 70, 1f, 0.84f, 0.35f));
        if (!boss) w.addEffect(Effect.death(kind.ordinal() + (vitala ? 100 : 0), elite ? 3 : rank >= 1 ? 2 : 0, x, y, drawSize()));
        int bits = boss ? 26 : elite ? 14 : 8; // the debris shower: bigger bodies throw more
        for (int i = 0; i < bits; i++)
            w.debris(x, y, boss ? 260 : 170, 1f, elite ? 0.9f : 0.84f, elite ? 0.2f : 0.35f);
        w.dropLoot(this);
        if (boss) w.victory(this);
        return true;
    }


    /** HATAKA's betray burst: a close ring vented evenly, once at half hull and once at death
     *  — the weakest unit is the one you don't carelessly sweep with splash or a ram. */
    private void betray(World w) {
        WeaponDef gun = GUN[Kind.HATAKA.ordinal()];
        for (int i = 0; i < BETRAY_COUNT; i++) {
            float a = (float) (Math.PI * 2 * i / BETRAY_COUNT) + fxAngle();
            w.spawnEnemyBullet(x, y, a, gun, dmgScale * BETRAY_DAMAGE / gun.damage);
        }
    }

    /** A stable per-unit offset so two HataKAs do not vent their rings along the same spokes. */
    private float fxAngle() {
        return (phase % 1f) * 1.7f;
    }
}