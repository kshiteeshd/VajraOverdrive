package vajra.world;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import vajra.enemy.Enemy;
import vajra.ship.ShipDef;
import vajra.weapon.WeaponDef;

/** THE TEST ARENA: every scenario the game can put a pilot in, one click away (Settings > TEST). A scenario is a category, a name, a line of what it is for, and a setup that
 *  arranges a fresh campaign world: where the ship is, what it carries, who is nearby. The arena never saves (see GameScreen) and never touches the pilot's book.
 *
 *  <p>It lives in the world package because setting a scene up needs the world's own hands (spotClear, the quota, the stage). */
public final class Arena {
    /** One scenario. */
    public static final class Scenario {
        public final String category, name, blurb;
        final boolean vitala, fresh;
        public boolean tour;                 // plays the tutorial of a new game as soon as the flight starts
        final Consumer<World> setup;

        Scenario(String category, String name, String blurb, boolean vitala, boolean fresh, Consumer<World> setup) {
            this.category = category;
            this.name = name;
            this.blurb = blurb;
            this.vitala = vitala;
            this.fresh = fresh;
            this.setup = setup;
        }
    }

    public static final String SANDBOX = "SANDBOX", STORY = "STORY AND SCENES", FOES = "FOES", WEAPONS = "WEAPONS", ASTRAS = "ASTRAS", BOSSES = "RULERS AND BOSSES",
            WORLD = "WORLD AND LOKAS", LOOT = "LOOT AND SHOP", HUD = "HUD AND UI", SHIPS = "HULLS", HAZARDS = "HAZARDS AND STATUS", TUTORIAL = "TUTORIALS", STRESS = "STRESS";

    public static final String[] CATEGORIES = {SANDBOX, STORY, FOES, WEAPONS, ASTRAS, BOSSES, WORLD, LOOT, HUD, SHIPS, HAZARDS, TUTORIAL, STRESS};

    private static final String[] BANDS = {"Avatarana", "Svarna Dvara", "Maya Vana", "Svapna Mandala", "Bala Mukha", "Boss: Bala"};
    private static final List<Scenario> ALL = new ArrayList<>();

    public static List<Scenario> in(String category) {
        List<Scenario> out = new ArrayList<>();
        for (Scenario s : ALL) if (s.category.equals(category)) out.add(s);
        return out;
    }

    public static int count() { return ALL.size(); }

    /** A fresh world arranged for `s`. */
    public static World build(Scenario s) {
        World w = s.vitala ? World.vitala(0) : new World(0, World.CAMPAIGN);
        w.cues.clear();
        if (!s.fresh) armoury(w);
        s.setup.accept(w);
        return w;
    }

    // ---- the pilot's kit ------------------------------------------------------------------------------------------------------------

    /** Everything owned and paid for: all hulls at all variants, all 36 patterns at rank III, shards of every element, coins, five lives, five stored repairs. */
    static void armoury(World w) {
        w.hangar.unlockAll();
        w.hangar.loka = 6;
        w.lives = World.MAX_LIVES;
        w.medkits = World.MEDKIT_MAX;
        w.selectShip(0);
        w.selectWeapon(0);
        w.ship.hp = w.ship.maxHp();
    }

    // ---- helpers --------------------------------------------------------------------------------------------------------------------

    /** The nearest open spot to (x, y): open ground all round, so nothing is placed inside rock. */
    static float[] clearNear(World w, float x, float y) {
        for (float r = 0; r <= 1400f; r += 40f)
            for (int k = 0; k < 16; k++) {
                float a = k * (float) Math.PI / 8f, px = x + (float) Math.cos(a) * r, py = y + (float) Math.sin(a) * r;
                if (px > 100 && px < w.width - 100 && py > 100 && py < w.height - 100 && w.spotClear(px, py)) return new float[] {px, py};
            }
        return new float[] {x, y};
    }

    static void tp(World w, float x, float y) {
        float[] c = clearNear(w, x, y);
        w.ship.x = c[0];
        w.ship.y = c[1];
        w.ship.vx = w.ship.vy = 0;
        w.ship.facing = (float) Math.PI / 2f;
    }

    static Enemy foe(World w, Enemy.Kind k, float dx, float dy) {
        float[] c = clearNear(w, w.ship.x + dx, w.ship.y + dy);
        Enemy e = new Enemy(k, c[0], c[1]);
        e.active = true;
        e.wakeRange = 2200f;
        e.vitala = w.stage == 1 && w.campaign;
        w.add(e);
        return e;
    }

    /** A target that cannot die, move or fire: it shows every hit, burn, slow and freeze, and heals to full. */
    static Enemy dummy(World w, float dx, float dy) {
        Enemy e = foe(w, Enemy.Kind.BHAVI, dx, dy);
        e.hp = e.maxHp = 1_000_000f;
        e.dummy = true;
        return e;
    }

    static void dummyField(World w) {
        for (int row = 0; row < 3; row++)
            for (int i = -3; i <= 3; i++) dummy(w, i * 150f, 320f + row * 280f);
    }

    static void ring(World w, Enemy.Kind k, int n, float radius) {
        for (int i = 0; i < n; i++) {
            float a = i * 6.2832f / n;
            foe(w, k, (float) Math.cos(a) * radius, (float) Math.sin(a) * radius + 120f);
        }
    }

    static void clearFoes(World w) {
        for (Enemy e : w.enemies) if (!e.boss) e.dead = true;
    }

    /** The region's middle: region i of the Atala bands. */
    static void atalaRegion(World w, int i) {
        w.settleBeats();
        float end = i + 1 < w.regionStart.length ? w.regionStart[i + 1] : w.regionStart[i] + 3000f;
        tp(w, w.axisX, (w.regionStart[i] + end) / 2f);
    }

    static void vitalaRegion(World w, int i) {
        w.settleBeats();
        tp(w, vajra.world.Hataki.CX, (vajra.world.Hataki.START[i] + vajra.world.Hataki.END[i]) / 2f);
    }

    /** The ruler of the current stage stands in his arena, awake the moment the hull comes in. */
    static Enemy ruler(World w) {
        w.gazeStage = 99;                                   // (the ruler's three gaze scenes are for a real run's climb to him)
        System.arraycopy(w.quota, 0, w.kills, 0, w.quota.length);
        w.trySpawnBoss();
        w.cues.removeIf(c -> c == Cue.GAZE_NOTICED || c == Cue.GAZE_WATCHING || c == Cue.GAZE_ROUSED);
        Enemy b = w.boss;
        if (b == null) return null;
        b.vitala = w.stage == 1;
        tp(w, w.pitX, w.pitY - 280f);                       // inside his hall, and he is already awake (the wake scenes are under STORY)
        b.active = true;
        w.settleBeats();
        w.cues.removeIf(c -> c == Cue.FIRST_LAMP || c == Cue.FIRST_BAY || c == Cue.BALA_WAKES || c == Cue.BHAIRAVA_WAKES);
        return b;
    }

    // ---- what the in-flight keys call ----
    static final Enemy.Kind[] PACK_KINDS = java.util.Arrays.stream(Enemy.Kind.values()).filter(k -> k != Enemy.Kind.BALA).toArray(Enemy.Kind[]::new);

    public static int packKinds() { return PACK_KINDS.length; }

    public static String packName(int i) { return PACK_KINDS[Math.floorMod(i, PACK_KINDS.length)].name(); }

    /** F8: three of a kind, awake, in front of the hull. */
    public static void pack(World w, int kindIndex) {
        Enemy.Kind k = PACK_KINDS[Math.floorMod(kindIndex, PACK_KINDS.length)];
        for (int i = -1; i <= 1; i++) foe(w, k, i * 130f, 380f);
    }

    /** F9: every foe but a ruler is gone. */
    public static void clear(World w) { clearFoes(w); }

    /** F10: the hull whole, the lives and repairs full, shards and coins plenty. */
    public static void refill(World w) {
        w.ship.hp = w.ship.maxHp();
        w.ship.shieldEnergy = w.ship.shieldCap();
        w.lives = World.MAX_LIVES;
        w.medkits = World.MEDKIT_MAX;
        java.util.Arrays.fill(w.hangar.shards, 99);
        w.hangar.coins = Math.max(w.hangar.coins, 99999);
    }

    static void ownedOnly(World w, int... weapons) {
        java.util.Arrays.fill(w.hangar.weaponOwned, false);
        for (int i : weapons) w.hangar.weaponOwned[i] = true;
    }

    private static void add(String cat, String name, String blurb, Consumer<World> setup) {
        ALL.add(new Scenario(cat, name, blurb, false, false, setup));
    }

    private static void addV(String cat, String name, String blurb, Consumer<World> setup) {
        ALL.add(new Scenario(cat, name, blurb, true, false, setup));
    }

    private static void addFresh(String cat, String name, String blurb, Consumer<World> setup) {
        ALL.add(new Scenario(cat, name, blurb, false, true, setup));
    }

    // ---- the catalogue --------------------------------------------------------------------------------------------------------------

    static {
        // SANDBOX
        add(SANDBOX, "FREE FLIGHT, FULL ARMOURY", "Atala, no one near. All seven hulls at every variant, all 36 patterns at rank III, astras, coins, five lives. Number keys change hull, V and B change pattern.", w -> clearFoes(w));
        addFresh(SANDBOX, "AS A NEW PILOT", "The base hull and the Single Bolt, three lives, nothing owned: what a first-time pilot has.", w -> { });
        add(SANDBOX, "EMPTY SKY", "No foe within the band: fly, boost, shield and turn in peace.", w -> clearFoes(w));
        add(SANDBOX, "TARGET RANGE", "Twenty-one target hulls that cannot die or move, in three rows. See every hit, number and status.", w -> { clearFoes(w); dummyField(w); });
        addV(SANDBOX, "VITALA: FREE FLIGHT", "The pilot is already in Vitala with the full armoury.", w -> { clearFoes(w); });
        addV(SANDBOX, "VITALA: TARGET RANGE", "The target range, over Vitala's ground.", w -> { clearFoes(w); dummyField(w); });

        // STORY AND SCENES
        add(STORY, "OPENING (INDRA'S COURT)", "The opening scene of a new game, from the first frame.", w -> w.cue(Cue.OPENING));
        add(STORY, "THE GRAND GATE", "The first crossing into Atala.", w -> { tp(w, w.axisX, World.ATALA_Y - 300f); w.cue(Cue.ATALA_GATE); });
        add(STORY, "SCOUT (UPARICHARA)", "First meeting with the Scout.", w -> w.cue(Cue.VOICE_SCOUT));
        add(STORY, "THE NINTH MAYA", "First meeting with the Ninth Maya.", w -> w.cue(Cue.VOICE_MAYA));
        add(STORY, "KALIYA THE TIDE-PILOT", "First meeting with Kaliya.", w -> w.cue(Cue.VOICE_KALIYA));
        add(STORY, "THE RULER NOTICES (GAZE 1)", "The ruler's piece warms: the first of his people have fallen.", w -> w.cue(Cue.GAZE_NOTICED));
        add(STORY, "THE RULER WATCHES (GAZE 2)", "He looks up and speaks to the empty hall.", w -> w.cue(Cue.GAZE_WATCHING));
        add(STORY, "THE RULER ROUSED (GAZE 3)", "The quota is full: he takes his place in the last band.", w -> w.cue(Cue.GAZE_ROUSED));
        add(STORY, "FIRST SIGHT OF A LAMP", "The camera goes to a checkpoint and says what it is.", w -> {
            Checkpoint c = w.checkpoints[0];
            if (c != null) { tp(w, c.x - 300f, c.y - 300f); w.beatX = c.x; w.beatY = c.y; }
            w.cue(Cue.FIRST_LAMP);
        });
        add(STORY, "FIRST SIGHT OF A BAY", "The camera goes to a maker's bay.", w -> {
            tp(w, w.shops[0][0] - 300f, w.shops[0][1] - 300f);
            w.beatX = w.shops[0][0];
            w.beatY = w.shops[0][1];
            w.cue(Cue.FIRST_BAY);
        });
        addV(STORY, "ENTERING VITALA", "The crossing through Bala's door into the Plunge.", w -> w.cue(Cue.VITALA_ENTER));
        addV(STORY, "THE GOLD-SELLER", "First meeting with the Gold-Seller of Hara's court.", w -> w.cue(Cue.VOICE_SELLER));
        addV(STORY, "GAURI'S MAID", "First meeting with the keeper of the shrine lamps.", w -> w.cue(Cue.VOICE_MAID));
        add(STORY, "BALA WAKES", "The ruler of Atala wakes on your crossing into his arena.", w -> { Enemy b = ruler(w); if (b != null) { b.active = false; tp(w, w.pitX, w.bossWakeY + 80f); } });
        addV(STORY, "BHAIRAVA WAKES", "The ruler of Vitala wakes as you come into the Altar.", w -> { Enemy b = ruler(w); if (b != null) { b.active = false; tp(w, w.pitX, w.bossWakeY + 80f); } });
        add(STORY, "YOUR DEATH, WITH LIVES", "The hull is lost: particles, the camera to the lamp, a wake. Then you fly on.", w -> { w.ship.invuln = 0; w.ship.iFrames = 0; w.ship.hp = 1f; w.ship.damage(9999f, w); });
        add(STORY, "YOUR DEATH, NO LIVES LEFT", "There is no game over: with none left you still wake at a lamp.", w -> { w.lives = 0; w.ship.invuln = 0; w.ship.iFrames = 0; w.ship.hp = 1f; w.ship.damage(9999f, w); });

        // FOES: one scenario per kind, then the groups
        for (Enemy.Kind k : Enemy.Kind.values()) {
            if (k == Enemy.Kind.BALA) continue;
            add(FOES, k.name() + (k.keeper() ? " (A KEEPER)" : ""), "One " + k.name() + " in front of you, awake. Watch its fire, its movement and what it drops.", w -> { clearFoes(w); foe(w, k, 0, 420f); });
        }
        add(FOES, "ALL KINDS AT ONCE", "One of every kind in a ring.", w -> { clearFoes(w); Enemy.Kind[] ks = Enemy.Kind.values(); int n = 0; for (Enemy.Kind k : ks) if (k != Enemy.Kind.BALA) { float a = n++ * 6.2832f / (ks.length - 1); foe(w, k, (float) Math.cos(a) * 520f, (float) Math.sin(a) * 520f + 100f); } });
        add(FOES, "ELITE PAIR", "Two gold elites: twice the hull, the ring of a commander.", w -> { clearFoes(w); for (int s = -1; s <= 1; s += 2) { Enemy e = foe(w, Enemy.Kind.VAKRA, s * 120f, 420f); e.elite = true; e.rank = 1; e.hp = e.maxHp = e.maxHp * 2; } });
        add(FOES, "RANKS: ZERO, ONE, TWO", "The same kind at three ranks: how rank changes size, hull and fire.", w -> { clearFoes(w); for (int r = 0; r < 3; r++) { Enemy e = foe(w, Enemy.Kind.VAKRA, (r - 1) * 220f, 420f); e.rank = r; } });
        add(FOES, "HORDE OF THIRTY", "Thirty snappers and fan-firers. Test crowd fire and the minimap.", w -> { clearFoes(w); ring(w, Enemy.Kind.AURIS, 15, 560f); ring(w, Enemy.Kind.VAKRA, 15, 760f); });
        add(FOES, "ARMOURED WALL", "Three gate-wards: plate blunts your shots.", w -> { clearFoes(w); for (int i = -1; i <= 1; i++) foe(w, Enemy.Kind.DVARAPALA, i * 160f, 420f); });
        add(FOES, "THE LAMP-KEEPER MENDS", "A lamp-keeper with three wounded snappers: it mends them as you watch.", w -> { clearFoes(w); foe(w, Enemy.Kind.DIPAKA, 0, 450f); for (int i = -1; i <= 1; i++) { Enemy e = foe(w, Enemy.Kind.AURIS, i * 130f, 330f); e.hp = e.maxHp * 0.3f; } });
        add(FOES, "THE WEAKLING BETRAYS", "A Hataka at half hull: it vents once, and again on its way down.", w -> { clearFoes(w); Enemy e = foe(w, Enemy.Kind.HATAKA, 0, 380f); e.hp = e.maxHp * 0.52f; });
        add(FOES, "SLEEPING FOES", "A ring of foes asleep at their posts: they wake as you come close, or when hit.", w -> { clearFoes(w); for (int i = 0; i < 8; i++) { float a = i * 0.785f; Enemy e = foe(w, Enemy.Kind.VAKRA, (float) Math.cos(a) * 600f, (float) Math.sin(a) * 600f + 200f); e.active = false; e.wakeRange = 330f; } });
        addV(FOES, "VITALA: FLYING RESERVES", "Vitala's leader calls reserves up from below; they fly in, never dive.", w -> { clearFoes(w); Enemy l = foe(w, Enemy.Kind.LANCE, 0, 500f); l.rank = 1; ring(w, Enemy.Kind.VAKRA, 6, 700f); });
        addV(FOES, "VITALA: FOES THAT DODGE", "Snappers and fan-firers with a brain: they sidestep your fire.", w -> { clearFoes(w); ring(w, Enemy.Kind.AURIS, 4, 450f); ring(w, Enemy.Kind.VAKRA, 4, 650f); });

        // WEAPONS: one per pattern, at rank III against a target range; V/B and F6/F7 change them on the fly
        for (WeaponDef d : WeaponDef.ALL) {
            final int idx = d.idx;
            String where = idx < WeaponDef.STANDARD ? "STANDARD" : LokaTable.at(d.minLoka).name.toUpperCase();
            add(WEAPONS, (idx + 1) + ". " + d.name.toUpperCase(), where + " pattern. Twenty-one targets that cannot die; fire and see the pattern at rank III. F6 changes pattern, F7 the rank.", w -> {
                clearFoes(w);
                dummyField(w);
                w.hangar.weaponRank[idx] = WeaponDef.RANKS - 1;
                w.selectWeapon(idx);
            });
        }
        add(WEAPONS, "RANK I, II, III", "The Single Bolt at rank I. F7 steps it up: see what rank adds.", w -> { clearFoes(w); dummyField(w); java.util.Arrays.fill(w.hangar.weaponRank, 0); w.selectWeapon(0); });
        add(WEAPONS, "WEAPON WHEEL, ALL 36", "Hold V for the wheel with every pattern on it (two rings).", w -> { clearFoes(w); dummyField(w); });
        add(WEAPONS, "ONLY THE STARTER OWNED", "Wheel and V/B with one pattern owned.", w -> { clearFoes(w); dummyField(w); ownedOnly(w, 0); w.selectWeapon(0); });
        add(WEAPONS, "HEATED HULL (FORGE-LANDS SCATTER)", "The heat's scatter on every shot.", w -> { clearFoes(w); dummyField(w); w.ship.heated = true; });

        // ASTRAS
        for (int k = 0; k < Astra.KINDS; k++) {
            final int kind = k;
            add(ASTRAS, Astra.NAME[k] + " (" + Astra.EPITHET[k] + ")", "Hold F to invoke, let go to loose it. Ten releases of shards and a ring of foes round you. " + Astra.EFFECT[k], w -> {
                clearFoes(w);
                java.util.Arrays.fill(w.hangar.shards, 50);
                w.astraSel = kind;
                ring(w, Enemy.Kind.VAKRA, 8, 520f);
                ring(w, Enemy.Kind.AURIS, 8, 780f);
            });
        }
        addFresh(ASTRAS, "BASE HULL CANNOT FORGE", "The base variant with shards in hand: F does nothing and says why. Buy variant 2 in a bay.", w -> { java.util.Arrays.fill(w.hangar.shards, 20); w.hangar.coins = 5000; ring(w, Enemy.Kind.VAKRA, 4, 520f); });
        add(ASTRAS, "ASTRA WITH NO FOES", "Loose an astra into empty ground: it flies along the nose.", w -> { clearFoes(w); java.util.Arrays.fill(w.hangar.shards, 50); });
        add(ASTRAS, "SHARD RING (PARTIAL)", "Two, three and four shards of different elements: the gem's ring fills.", w -> { clearFoes(w); for (int t = 0; t < 5; t++) w.hangar.shards[t] = t == 0 ? 0 : t == 4 ? 5 : t; });
        add(ASTRAS, "ONLY BRAHMASTRA READY", "Exactly five of every element: the Last Arrow can be loosed once.", w -> { clearFoes(w); java.util.Arrays.fill(w.hangar.shards, 5); w.astraSel = Astra.BRAHMA; ring(w, Enemy.Kind.AURIS, 10, 600f); });
        add(ASTRAS, "SELL SHARDS AT A BAY", "Twenty of each element and a bay at hand: selling spends the astras.", w -> { java.util.Arrays.fill(w.hangar.shards, 20); tp(w, w.shops[0][0] + 2f, w.shops[0][1] - 46f); });

        // RULERS AND BOSSES
        add(BOSSES, "BALA: THE FULL FIGHT", "From his wake: his retinue, his three phases, his Maya-astras, the yield.", w -> ruler(w));
        add(BOSSES, "BALA: PHASE TWO (66%)", "Bala at two thirds: the shattering astra changes.", w -> { Enemy b = ruler(w); if (b != null) b.hp = b.maxHp * 0.62f; });
        add(BOSSES, "BALA: PHASE THREE (33%)", "Bala at a third: the astra that opens doors into Atala.", w -> { Enemy b = ruler(w); if (b != null) b.hp = b.maxHp * 0.3f; });
        add(BOSSES, "BALA: ABOUT TO YIELD", "One hit and he gives way: the yield scene, his piece of the bolt, the veil that parts.", w -> { Enemy b = ruler(w); if (b != null) b.hp = 3f; });
        add(BOSSES, "MAYA ASTRA: ONE BY ONE", "The mirror shatters and each piece turns in its own time.", w -> { Enemy b = ruler(w); if (b != null) w.startMaya(b, Maya.ONE_BY_ONE); });
        add(BOSSES, "MAYA ASTRA: ALL AT ONCE", "The whole mirror turns together.", w -> { Enemy b = ruler(w); if (b != null) w.startMaya(b, Maya.ALL_AT_ONCE); });
        add(BOSSES, "MAYA ASTRA: THE DOORS", "Doors into Atala, live windows, the crack at his feet; the ship is free to fly into any.", w -> { Enemy b = ruler(w); if (b != null) w.startMaya(b, Maya.DOORS); });
        addV(BOSSES, "BHAIRAVA: THE FULL FIGHT", "Vitala's ruler from his wake, with his ash cloud and his new words.", w -> ruler(w));
        addV(BOSSES, "BHAIRAVA: PHASE TWO (66%)", "Bhairava at two thirds.", w -> { Enemy b = ruler(w); if (b != null) b.hp = b.maxHp * 0.62f; });
        addV(BOSSES, "BHAIRAVA: PHASE THREE (33%)", "Bhairava at a third: the Flood.", w -> { Enemy b = ruler(w); if (b != null) b.hp = b.maxHp * 0.3f; });
        addV(BOSSES, "BHAIRAVA: PHASE FOUR (20%)", "Bhairava in the Last Fire: the immolation.", w -> { Enemy b = ruler(w); if (b != null) b.hp = b.maxHp * 0.2f; });
        addV(BOSSES, "BHAIRAVA: ABOUT TO YIELD", "One hit and he gives way.", w -> { Enemy b = ruler(w); if (b != null) b.hp = 3f; });
        addV(BOSSES, "BHAIRAVA'S ASH CLOUD NOW", "The grey cloud and its mandala over your hull: every hull slowed, foes seen.", w -> { Enemy b = ruler(w); w.ash.start(w.ship.x, w.ship.y + 200f, 520f, 40f); });

        // WORLD AND LOKAS
        for (int i = 0; i < 6; i++) {
            final int r = i;
            add(WORLD, "ATALA, BAND " + (i + 1), "The middle of Atala's band " + (i + 1) + " (" + BANDS[i] + ").", w -> atalaRegion(w, r));
        }
        for (int i = 0; i < 6; i++) {
            final int r = i;
            addV(WORLD, "VITALA, REGION " + (i + 1), "Vitala's region " + (i + 1) + ".", w -> vitalaRegion(w, r));
        }
        add(WORLD, "THE GRAND GATE (BHU LOKA SIDE)", "Just below the gate.", w -> tp(w, w.axisX, World.ATALA_Y - 400f));
        add(WORLD, "BALA'S ARENA, SEALED", "Bala's arena with the seal still down.", w -> tp(w, w.pitX, w.regionStart[5] - 200f));
        add(WORLD, "A SHOP BAY (ATALA)", "Fly into the bay: the dock takes the hull and the shop opens.", w -> tp(w, w.shops[0][0] + 2f, w.shops[0][1] - 200f));
        add(WORLD, "A LAMP (CHECKPOINT)", "Fly through the lamp to bind it; die to wake here.", w -> { Checkpoint c = w.checkpoints[0]; if (c != null) tp(w, c.x, c.y - 300f); });
        addV(WORLD, "VITALA, THE ARRIVAL", "The first screen of Vitala: the Hataki is born out of the dark behind you.", w -> { w.settleBeats(); tp(w, Hataki.CX, Hataki.Y0 + 90f); });
        addV(WORLD, "A SHOP BAY (VITALA)", "Vitala's bay.", w -> tp(w, w.shops[4][0] + 2f, w.shops[4][1] - 200f));
        add(WORLD, "ROCK AND WALLS", "Fly along the rock and test collisions, bullets on stone and mud.", w -> { clearFoes(w); atalaRegion(w, 2); });

        // LOOT AND SHOP
        add(LOOT, "COIN SHOWER", "Thirty coins of every art, floating about you.", w -> { for (int i = 0; i < 30; i++) w.spawnPickup(Pickup.COIN, i % 5, w.ship.x + (float) Math.cos(i) * (80 + i * 6), w.ship.y + 200f + (float) Math.sin(i) * (80 + i * 6)); });
        add(LOOT, "SHARDS OF ALL FIVE", "One shard of each element: see the gems, the ring and the pickup.", w -> { java.util.Arrays.fill(w.hangar.shards, 0); for (int t = 0; t < 5; t++) w.spawnPickup(Pickup.SHARD, t, w.ship.x - 200f + t * 100f, w.ship.y + 220f); });
        add(LOOT, "HEALTH DROPS AND REPAIRS", "Hurt hull, health drops to collect, five stored repairs and key 1.", w -> { w.ship.hp = w.ship.maxHp() * 0.3f; w.medkits = 2; for (int i = 0; i < 4; i++) w.spawnPickup(Pickup.HEALTH, 0, w.ship.x - 150f + i * 100f, w.ship.y + 200f); });
        add(LOOT, "A PIECE OF THE VAJRA", "A piece seeks the hull: the big popup and the 1 of 7 panel.", w -> { Pickup p = w.spawnPickup(Pickup.PIECE, 0, w.ship.x, w.ship.y + 260f); if (p != null) p.seek = true; });
        addFresh(LOOT, "SHOP WITH LITTLE MONEY", "A bay, fifty coins: short, locked and affordable rows.", w -> { w.hangar.coins = 50; tp(w, w.shops[0][0] + 2f, w.shops[0][1] - 200f); });
        add(LOOT, "SHOP WITH EVERYTHING OWNED", "A bay with all hulls, patterns and ranks owned.", w -> tp(w, w.shops[0][0] + 2f, w.shops[0][1] - 200f));
        addFresh(LOOT, "SHOP: A RICH NEW PILOT", "A bay with 20000 coins and nothing owned: buy hulls, tiers, patterns, ranks.", w -> { w.hangar.coins = 20000; w.hangar.loka = 6; tp(w, w.shops[0][0] + 2f, w.shops[0][1] - 200f); });
        add(LOOT, "KILL LOOT", "Weak foes that drop coins, health and shards as they fall.", w -> { clearFoes(w); for (int i = -3; i <= 3; i++) foe(w, Enemy.Kind.HATAKA, i * 90f, 380f); });

        // HUD AND UI
        add(HUD, "LOW HULL (20%)", "The tube amber-red and the smoke.", w -> w.ship.hp = w.ship.maxHp() * 0.2f);
        add(HUD, "CRITICAL HULL (8%)", "The last sliver.", w -> w.ship.hp = w.ship.maxHp() * 0.08f);
        add(HUD, "ONE LIFE LEFT", "A single life on the HUD.", w -> w.lives = 1);
        add(HUD, "COMBO CHAIN", "Weak foes to kill without being hit: raise the combo and read the popups.", w -> { clearFoes(w); for (int i = -4; i <= 4; i++) foe(w, Enemy.Kind.HATAKA, i * 80f, 360f + Math.abs(i) * 30f); });
        add(HUD, "THE DIAL BUSY", "Forty foes at distance: the sweep, loot, lamps, bays.", w -> { clearFoes(w); ring(w, Enemy.Kind.VAKRA, 20, 900f); ring(w, Enemy.Kind.AURIS, 20, 1200f); });
        add(HUD, "ALL SEVEN PIECES", "The Vajra panel full: seven of seven.", w -> java.util.Arrays.fill(w.hangar.pieces, true));
        add(HUD, "SHOP SCREEN, HULLS TAB", "A bay: open the hangar and browse the hulls, tiers and ASTRA locks.", w -> tp(w, w.shops[0][0] + 2f, w.shops[0][1] - 200f));

        // HULLS
        for (int i = 0; i < ShipDef.ALL.length; i++) {
            final int id = i;
            ShipDef d = ShipDef.ALL[i];
            add(SHIPS, (i + 1) + ". " + d.name.toUpperCase(), "Fly this hull, all variants owned: number keys swap hull, the bay changes its colourway.", w -> { clearFoes(w); w.selectShip(id); dummyField(w); });
        }
        addFresh(SHIPS, "ONLY THE BASE HULL", "One hull owned: the number keys and Z cycle only what is owned.", w -> clearFoes(w));
        add(SHIPS, "SHIELD AND BOOST DRILL", "Foes that shoot while you drain the arcs: shield, boost, refill.", w -> { clearFoes(w); ring(w, Enemy.Kind.AURIS, 10, 520f); });

        // HAZARDS AND STATUS
        add(HAZARDS, "ASH CLOUD ON YOU", "Inside a grey cloud: mud slows the hull, sounds and view change.", w -> { clearFoes(w); w.ash.start(w.ship.x, w.ship.y, 520f, 60f); ring(w, Enemy.Kind.VAKRA, 4, 450f); });
        add(HAZARDS, "THE FLOOD (JALA)", "The Jala flood over a ring of foes: it carries, soaks and drowns.", w -> { clearFoes(w); ring(w, Enemy.Kind.VAKRA, 8, 420f); w.flow.startWater(w.ship, 0, 1f, 1f, 7L); });
        add(HAZARDS, "FOES ON FIRE", "Targets alight: the burn ticks.", w -> { clearFoes(w); for (int i = -2; i <= 2; i++) dummy(w, i * 170f, 340f).ignite(40f, 600f, 0); });
        add(HAZARDS, "FOES SLOWED", "Targets with a heavy slow.", w -> { clearFoes(w); for (int i = -2; i <= 2; i++) { Enemy e = foe(w, Enemy.Kind.VAKRA, i * 170f, 340f); e.slowBy(0.7f, 600f); } });
        add(HAZARDS, "FOES FROZEN", "Targets frozen where they stand.", w -> { clearFoes(w); for (int i = -2; i <= 2; i++) { Enemy e = foe(w, Enemy.Kind.AURIS, i * 170f, 340f); e.freeze(600f); } });
        add(HAZARDS, "HEAVY FIRE ON YOU", "Many foes, close: hit flashes, shake, repairs.", w -> { clearFoes(w); ring(w, Enemy.Kind.VAKRA, 12, 380f); });

        // TUTORIALS
        ALL.add(tour(new Scenario(TUTORIAL, "THE TUTORIAL OF A NEW GAME", "The HUD tour and the controls, exactly as every new game plays it.", false, false, w -> clearFoes(w))));
        add(TUTORIAL, "FIRST LESSONS (THRUST AND FIRE)", "Coach prompts only: fly and fire to learn.", w -> clearFoes(w));

        // STRESS
        add(STRESS, "SIXTY FOES", "Sixty awake foes at once.", w -> { clearFoes(w); ring(w, Enemy.Kind.AURIS, 20, 700f); ring(w, Enemy.Kind.VAKRA, 20, 950f); ring(w, Enemy.Kind.LANCE, 20, 1200f); });
        add(STRESS, "BULLET STORM", "Dummy field and the fastest weapons: bullet-count limits.", w -> { clearFoes(w); dummyField(w); w.selectWeapon(6); });
        add(STRESS, "EVERY ASTRA, FIFTY SHARDS", "Spam astras on a crowd.", w -> { clearFoes(w); java.util.Arrays.fill(w.hangar.shards, 80); ring(w, Enemy.Kind.AURIS, 20, 700f); ring(w, Enemy.Kind.VAKRA, 20, 950f); });
    }

    private static Scenario tour(Scenario s) {
        s.tour = true;
        return s;
    }

    private Arena() {}
}
