package vajra.world;

import vajra.enemy.Brain;
import vajra.enemy.Enemy;
import vajra.ship.Hangar;
import vajra.ship.Ship;
import vajra.ship.ShipControls;
import vajra.ship.ShipDef;
import vajra.weapon.Bullet;
import vajra.weapon.WeaponDef;

import com.badlogic.gdx.utils.Pool;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class World {
    public float width = 5200;
    public float height = 20600;

    public Ship ship;
    public final Hangar hangar = new Hangar();
    public TileMap tileMap;
    public TileMap tileMapR2;
    public TileMap tileMapR3;
    public TileMap tileMapR4;
    public TileMap tileMapBoss;
    public Orb[] orbs = new Orb[6];
    public Checkpoint[] checkpoints = new Checkpoint[5]; // Atala bands 1-5: Bhu Loka has none
    public Checkpoint respawnCp;    // most recent collected checkpoint; null = region entrance
    public int lives = 3;
    /** SIDE STORY: the people of Patala (the Scout, Maya, Kaliya, the Gold-Seller, Gauri's Maid) and the jobs they give (Maya's wrecks, the bounty).
     *  OFF in the game for now - nobody is placed, so no voice scene, job or wreck exists; the code, scripts and saves are untouched. The tests
     *  run with it on (-Dvajra.sidestory=true) so the dormant feature stays covered; flip it here when the side characters return. */
    public static boolean sideStory = Boolean.getBoolean("vajra.sidestory");
    public static final int MAX_LIVES = 5; // a forged release can lift a run back to this, never past
    /** This run was flown with other pilots: it folds into the multiplayer book, not the
     *  campaign one. */
    public boolean multiplayer;
    /** This run has already been written into the pilot's book — the run screen and the
     *  pause screen both end a run, and only the first of them counts. */
    public boolean recorded;
    public Enemy boss;              // Bala: null until the kill quota lifts the seal, then in the pit
    public boolean bossDead;
    public boolean won;         // Bala fell: the run ends in victory (after the banner lap)
    public static final float VICTORY_LAP = 4f; // seconds the world still runs after the kill, banner up

    /** THE WORLD AXIS: every gate, the court, shops and the boss arena pour out of x = 2600 —
     *  the single spine the map is mirrored about. Brains, HUD compass and gen all read this. */
    public static final float WORLD_CX = 2600f;
    /** The boss arena's heart: the pit Bala holds, and the kill ring below the banner. */
    public static final float PIT_X = 2600f, PIT_Y = 19104f;

    /** The run is over: the blade whole, or Bala fallen (in the first-loka world) and the banner lap done. There is no game over: the last life spent,
     *  the hull still wakes at the last lit lamp, for as long as the pilot likes. A
     *  guest's seat also ends when the host's run does — the swarm carries that bit. */
    public boolean runEnded() {
        return vajraDone || (won && victoryT >= VICTORY_LAP)
                || (clientSeat && (hostFlags & 1) != 0);
    }

    public boolean vajraDone;       // all 7 Vajrayudha bands recovered: the run is over
    public float victoryT = -1;     // seconds since the kill; -1 = no victory yet
    public static final float BOSS_WAKE_Y = 18000; // cross this line and the commander calls his ring
    public final int[] kills = new int[Enemy.Kind.values().length]; // player kills per kind
    public final int[] quota = new int[Enemy.Kind.values().length]; // kills Bala demands per kind
    public final Score score = new Score(); // points + combo: the run's ledger
    public final Popups popups = new Popups(); // floating text: score payments, combo tiers
    // A cache per Atala band: touch it and the local squad converges; kill them and it opens.
    // Never drawn — the player is not meant to learn where one is (see README: production must
    // keep caches invisible).
    // CACHE STATE IS PER PLAYER, not per world: two ships in one band each get their own
    // stash, so co-op players cannot open each other's. Solo is slot 0.
    public static final int CACHE_SEALED = 0, CACHE_CONTESTED = 1, CACHE_DONE = 2;
    public static final int SLOTS = 4; // co-op seats; only slot 0 is populated today
    public float[][] caches = new float[5][2];
    public int[][] cacheState = new int[SLOTS][5];
    public int slot; // which seat this run's ship occupies
    // LIVE CO-OP (docs/MULTIPLAYER.md, phase 3): the other pilot is a ghost kept by packets;
    // my hull is authoritative on my machine. Foes and loot are the host's truth, streamed.
    public Ship partner;
    public int partnerShipIdx = -1; // their hull art, so a SHIP packet rebuilds it on change
    public int partnerSeat = 1; // the partner's roster id; their SHIP packets stamp it

    /** A remote hull beyond pilot 1 — same mirror law, one seat each. */
    public static class SeatHull {
        public final int seat;    // the roster id whose word this hull is
        public Ship ship;
        public int art = -1;
        public SeatHull(int seat) { this.seat = seat; }
    }

    /** Seats above pilot 1 land here; they fly the same beams, pains and bullets the partner does. */
    public final ArrayList<SeatHull> extras = new ArrayList<>();

    /** The hull mirror of that seat, or null until its first SHIP packet lands. */
    public SeatHull extra(int seat) {
        for (SeatHull x : extras) if (x.seat == seat) return x;
        return null;
    }
    public boolean clientSeat;  // guests run only their own hull; the host runs everything else
    public byte hostFlags;      // the swarm's run bits: bit0 host run over, bit1 host won

    /** This machine flies a GUEST seat: its own hull only, the fight as ghosts. The local
     *  garrison is dropped (the world is built with the whole roster standing in it, and a
     *  guest must see the fight ONCE, as the host's ghosts — not twice). The loot id space is
     *  pushed to the high half so a guest's own drop can never collide with the host's: a
     *  TAKE carries an id and the hub retires what it finds under it. */
    public void seatAsClient() {
        clientSeat = true;
        dropSeq = 0x8000;
        enemies.clear();
        boss = null;
    }
    public final java.util.ArrayList<Integer> takes = new java.util.ArrayList<>(); // my sweeps to retire
    public ShotSink shotWire;   // the spawn chokes fire rounds through it; the net pump sets it
    public PainSink painWire;   // a partner hull touched, shoved or a kill paying their book
    /** The Astra's wire: a guest seat announces the foes it aimed at and the hub arms the
     *  same volley there, so the signature weapon is a real weapon on every seat. */
    public interface AstraSink { void astra(int kind, float power, int[] targetIds, int count); }
    public AstraSink astraWire;
    public float shipBeat, swarmBeat; // this seat's send-cadence debt
    public int shooter;         // whose round/contact is about to hurt someone: a roster id
    int enemySeq, dropSeq;      // the host's id ledgers for foes and ground loot
    /** How many foes the world was BUILT with (ids 0..garrison-1). RunSave records which of them have
     *  fallen; everything that spawns later (the warden, a bounty rogue, summons) has a higher id. */
    public int garrison;
    public java.util.ArrayList<Enemy> enemies = new java.util.ArrayList<>();
    /** THE VOICES: the people of Patala live off the roster — a run's order of battle counts
     *  nothing of them, threatening none of them, and no behaviour law ever mistakes them
     *  for a combatant. They tick in their own pass and render in their own loop. */
    public final java.util.ArrayList<Enemy> voices = new java.util.ArrayList<>();
    /** People talking mid-flight (bubbles over their hulls) and jobs' words on the HUD (toasts). */
    public final java.util.ArrayList<Speech> speech = new java.util.ArrayList<>();
    public final java.util.ArrayList<Toast> toasts = new java.util.ArrayList<>();

    /** A person says a few short lines over their hull while the world keeps moving. */
    public void say(Enemy who, String... lines) {
        speech.add(new Speech(who, lines));
    }

    /** A title and a line on the HUD, for a few seconds. */
    public void toast(String title, String line) {
        toasts.add(new Toast(title, line));
    }

    /** The speeches that are up: for each person only the OLDEST one that is not done - a second
     *  line for someone already talking waits its turn instead of opening a second box over the first. */
    public java.util.List<Speech> heads() {
        java.util.ArrayList<Speech> out = new java.util.ArrayList<>(2);
        outer:
        for (Speech s : speech) {
            if (s.done()) continue;
            for (Speech o : out) if (o.who == s.who) continue outer;
            out.add(s);
        }
        return out;
    }

    /** True while this person has something to say right now. */
    public boolean talking(Enemy who) {
        for (Speech s : speech) if (s.who == who && !s.done()) return true;
        return false;
    }

    private void tickWords(float dt) {
        for (Speech s : heads()) s.update(dt); // only the one being said ticks; the rest wait
        for (int i = speech.size() - 1; i >= 0; i--) if (speech.get(i).done()) speech.remove(i);
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast t = toasts.get(i);
            t.age += dt;
            if (t.done()) toasts.remove(i);
        }
    }
    public final List<Bullet> bullets = new ArrayList<>();
    public final List<Effect> effects = new ArrayList<>();
    public final List<Pickup> pickups = new ArrayList<>(); // coins + health pops, swept by the hull
    public static final float KNOCK = 260;  // contact impulse, px/s, applied to both bodies
    public static final float RAM = 10;     // damage the enemy eats per contact touch
    public static final float RAM_CD = 0.5f;// seconds between rams on the same enemy
    // LOOT tables live in LootSystem.
    // Pancha Tatva in pancha order (LOKA-LAWS §2). WHICH tatva each loka pays — and the
    // difficulty curve a run is graded against — live in LokaTable.
    public static final String[] TATVA = { "Akasha", "Vayu", "Agni", "Jala", "Prithvi" };
    public static final float[][] TATVA_RGB = {
            { 0.72f, 0.64f, 1.00f }, { 0.86f, 0.94f, 1.00f }, { 1.00f, 0.45f, 0.10f },
            { 0.25f, 0.82f, 0.90f }, { 0.82f, 0.66f, 0.28f } };
    public int lokaIndex = 0; // which loka this run is in (LokaTable index; 0 = Atala)
    /** THE CODEX: this run's page lifts, folded into the profile at endRun. */
    public final boolean[] codex = new boolean[Codex.COUNT];
    /** MAYA'S GROVE: the six wrecks of Maya Vana, wound through the band at gen (wreckTaken
     *  is the host's truth — taken wrecks stay taken for everyone). */
    public final float[][] wrecks = new float[6][2];
    public final boolean[] wreckTaken = new boolean[6];
    public static final float PULL_R = 90;   // magnetism reach, px
    public static final float PULL_G = 1500; // accel at 100% magnet: terminal = PULL_G / DRAG = 500 px/s

    private final Pool<Bullet> bulletPool = new Pool<Bullet>(64, 512) {
        @Override
        protected Bullet newObject() {
            return new Bullet();
        }
    };

    final Random orbRng;
    public final Random fxRng = new Random(2107); // particles only (public: Ship smoke rolls); orb/blink rolls stay pure
    Random lootRng;

    /** A run's chaos, seeded. Seed 0 keeps the legacy constants — every existing test and
     *  solo flight builds the world it always did. A multiplayer session seeds its shadow
     *  runs with one number so every pilot races the same orb spots, loot tables and boss
     *  blinks (terrain is fixed hand-tuned content; it does not vary per seed). */
    /** Where a fresh run wakes: the camera clamps at the world's floor, so this rides the view's lower edge. */
    public static final float SPAWN_Y = 90;

    /** THE WALLS. The old rock tiles are the play layer again (docs/ATALA_PLAN.md): the walls stop hulls, bodies and rounds, and the
     *  renderer stacks them behind it for depth. -Dvajra.walls=false sweeps them away after generation (the open-land experiment). */
    public static boolean walls = Boolean.parseBoolean(System.getProperty("vajra.walls", "true"));
    /** This world's own copy of the switch, fixed when it was built. */
    public final boolean walled = walls;
    /** Bumped whenever a wall tile changes after the world is built: the renderer's baked chunks of wall are redone. */
    public int wallVersion;
    /** The belt of boulders between Bhu Loka and Atala: {x, y, radius, seed}. Solid to hulls, bodies and rounds. */
    public final float[][] boulders = Boundary.belt(width, 1600f);

    /** THE TWO WORLDS: {@code ATALA_ONLY} is the first loka alone (the unit tests' world, 20,600 px tall); {@code CAMPAIGN} is the game: Atala, and
     *  VITALA ON ITS ROOF - one continuous world 47,600 px tall, Bala's gold door at the top of Atala opening straight into the Plunge. The pilot can
     *  fly up into Vitala, and back down into Atala. */
    public static final int ATALA_ONLY = 0, CAMPAIGN = 2;
    public final boolean campaign;
    /** Which ruler is current: 0 Bala (Atala), 1 Bhairava (Vitala). Killing the first lifts his seal and the second becomes the quota's business;
     *  killing the last one wins the run. */
    public int stage;
    /** The fire vents of Vitala (none in Atala) and the seconds a lit shrine has left on its hush: while it runs the current and the vents rest. */
    public final Vents vents = new Vents();
    public float quietT;
    private float ventHurtT;
    /** Burning ground and hairlines of fire (Vitala's enemies and boss lay them). */
    public final Hazards hazards = new Hazards();
    /** The treasure chests of every loka (docs/TREASURE_CHESTS.md). */
    public final java.util.ArrayList<Chest> chests = new java.util.ArrayList<>();
    /** Vitala's side caverns {cx, cy, rx, ry, side, type} and whether each one's ember well has been drunk. */
    public float[][] pockets = new float[0][];
    public boolean[] wellUsed = new boolean[0];

    /** The boss's post and the tile row that seals his arena's north door (the kill lifts it), for the CURRENT stage. */
    public float pitX = PIT_X, pitY = PIT_Y;
    /** The line that wakes the boss's ring (Bala's is 18000, Bhairava's is a little way into the Altar). */
    public float bossWakeY = BOSS_WAKE_Y;
    TileMap sealMap;
    int sealRow, sealC0, sealC1;
    /** Every stage's own seal and kill quota, filled at generation; {@link #beginStage} makes one current. */
    final TileMap[] sealMaps = new TileMap[2];
    final int[] sealRows = new int[2], sealC0s = new int[2], sealC1s = new int[2];
    final int[][] stageQuota = new int[2][Enemy.Kind.values().length];
    /** The world axis (the spine the gates and the mirrored garrison hang on): x = 2600 in both lokas. */
    public float axisX = WORLD_CX;
    /** The region of each cache (a cache lives in the band whose enemies hear it). */
    public int[] cacheRegion = { 1, 2, 3, 4, 5 };

    public World(long seed) {
        this(seed, ATALA_ONLY);
    }

    public World(long seed, int mode) {
        this.campaign = mode == CAMPAIGN;
        orbRng = seed == 0 ? new Random(21) : new Random(seed ^ 0x004F21L);
        lootRng = seed == 0 ? new Random(90210) : new Random(seed ^ 0x90210L);
        ship = new Ship(width / 2, 800, ShipDef.ALL[0], hangar);
        new WorldGen(this).build();
        if (!walled) stripWalls();
        else markMirages();
        garrison = enemySeq; // the static garrison is ids [0, garrison): what a saved run strikes off
        docking = new DockingSystem(this);
        cacheDirector = new CacheDirector(this);
        enemyDirector = new EnemyDirector(this);
        missions = new Missions(this);
        Codex.unlock(this, Codex.ATALA, true);  // the run's opening record: where the pilot is
        Codex.unlock(this, Codex.VAJRA, true);  // and what he carries (no popup over the first frame)
        // THE WAKING: low in the open sky, a hair above the bottom of the view, nose straight up
        ship.x = width / 2f;
        ship.y = SPAWN_Y;
        ship.vx = ship.vy = 0;
        ship.facing = (float) (Math.PI / 2);
        cue(Cue.OPENING);
    }

    /** A campaign world with the pilot already in Vitala (the menu's VITALA row, and the tests): Bala's door stands open, the opening and the
     *  Atala beats are settled, and the second ruler is the one the quota is for. */
    public static World vitala(long seed) {
        World w = new World(seed, CAMPAIGN);
        w.startInVitala();
        return w;
    }

    private void startInVitala() {
        for (int c = sealC0s[0]; c <= sealC1s[0]; c++) sealMaps[0].set(c, sealRows[0], TileMap.EMPTY);
        wallVersion++;
        bossDead = true;
        beginStage(1);
        cues.clear();                                  // no opening scene: the pilot is already here
        ship.x = axisX;
        ship.y = Hataki.Y0 + SPAWN_Y;
        ship.vx = ship.vy = 0;
        lokaIndex = 1;
        hangar.loka = 1;
        boolean[] seen = regionScored();
        for (int i = 0; i < seen.length && i <= 6; i++) seen[i] = true;
        settleRegion();
    }

    /** Make stage s the current one: its ruler, its arena's door, its seal, its kill quota, a fresh gaze. */
    void beginStage(int s) {
        stage = s;
        boss = null;
        bossDead = false;
        won = false;
        victoryT = -1;
        gazeStage = 0;
        java.util.Arrays.fill(kills, 0);
        System.arraycopy(stageQuota[s], 0, quota, 0, quota.length);
        sealMap = sealMaps[s];
        sealRow = sealRows[s];
        sealC0 = sealC0s[s];
        sealC1 = sealC1s[s];
        if (s == 1) {
            pitX = Hataki.ALTAR_X;
            pitY = Hataki.ALTAR_Y;
            bossWakeY = Hataki.START[5] + 250f;
        }
    }

    /** The last stage of this world: Bhairava in the campaign, Bala alone in the first-loka world. */
    int lastStage() { return campaign ? 1 : 0; }

    /** The region index of the current boss's arena. */
    public int arenaRegion() { return stage == 0 ? 5 : regionStart.length - 1; }

    /** HOLD YOUR POST (docs/ENEMY_BIBLE.md): an awake unit whose hull is farther than {@link Enemy#LOSE_FACTOR} wake ranges from the unit's own
     *  spawn stands down - flies home and sleeps there. Checked six times a second; hunters, the routed, the dismissed, the talking, the ruler and the
     *  people are exempt (they are on a job of their own). */
    private void holdPosts() {
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            if (e.dead || e.boss || e.voice || e.hunt || e.routed || e.dismissed || e.parley || e.diveState != 0 || !e.active) { if (!e.active) e.standDown = false; continue; }
            Ship h = nearestHull(e.spawnX, e.spawnY);
            float fx = h.x - e.spawnX, fy = h.y - e.spawnY, lose = Math.max(e.wakeRange, 600f) * Enemy.LOSE_FACTOR;
            e.standDown = fx * fx + fy * fy > lose * lose;
        }
    }

    /** The region a world height lies in. */
    public int regionIndexAt(float y) {
        int r = 0;
        for (int i = 0; i < regionStart.length; i++) if (y >= regionStart[i]) r = i;
        return r;
    }

    /** Which loka a world height is in (0 Atala/Bhu Loka, 1 Vitala). */
    public int lokaAt(float y) { return campaign && y >= Hataki.Y0 ? 1 : 0; }

    /** Grow this world's arrays for Vitala on its roof (the generator calls it once, after Atala is whole). */
    void expandForVitala() {
        height = Hataki.TOP;
        int n = regionStart.length;
        regionStart = concatF(regionStart, Hataki.START);
        regionEnd = concatF(regionEnd, Hataki.END);
        regionEnd[regionEnd.length - 1] = Hataki.TOP;                       // the last band runs on into Sutala's hall
        String[] names = java.util.Arrays.copyOf(regionNames, n + 6);
        for (int i = 0; i < 6; i++) names[n + i] = i == 5 ? "Boss: Bhairava" : Hataki.NAMES[i];
        regionNames = names;
        lokaOfRegion = new int[] { 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1 };
        checkpoints = java.util.Arrays.copyOf(checkpoints, 10);
        shops = java.util.Arrays.copyOf(shops, 8);
        for (int i = 4; i < 8; i++) shops[i] = new float[2];
        caches = java.util.Arrays.copyOf(caches, 10);
        for (int i = 5; i < 10; i++) caches[i] = new float[2];
        cacheRegion = new int[] { 1, 2, 3, 4, 5, 7, 8, 9, 10, 11 };
        cacheState = new int[SLOTS][10];
        shopState = new int[8];
        shopTimer = new float[8];
        regionScored = null;
    }

    private static float[] concatF(float[] a, float[] b) {
        float[] out = java.util.Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** THE MIRAGE WOOD: about three blocks of wall in ten, in whole 8x8-tile blocks, are illusions - drawn like rock and nothing to a
     *  hull, a body or a round. Not near the gates, the shops or the checkpoints, whose walls stay real. */
    private void markMirages() {
        float y0 = regionStart[2] + 300f, y1 = regionEnd[2] - 300f;
        java.util.HashSet<Long> rooted = new java.util.HashSet<>(); // blocks that hold a plant or a torch keep their rock: they grow from it
        for (float[] pl : plants) { rooted.add(block(pl[0], pl[1])); rooted.add(block(pl[6], pl[7])); }
        for (float[] t : torches) { rooted.add(block(t[0], t[1])); rooted.add(block(t[3], t[4])); }
        for (TileMap m : maps)
            for (int ty = 0; ty < m.rows; ty++) {
                float wy = m.oy + ty * TileMap.TILE;
                if (wy < y0 || wy > y1) continue;
                for (int tx = 0; tx < m.cols; tx++) {
                    if (m.raw(tx, ty) == TileMap.EMPTY) continue;
                    float wx = m.ox + tx * TileMap.TILE;
                    if (GroundMap.rnd((int) (wx / 256f), (int) (wy / 256f), 0x31AA) >= 0.30f || nearLandmark(wx, wy) || rootedNear(rooted, wx, wy)) continue;
                    m.markGhost(tx, ty);
                }
            }
    }

    /** True if a block holding a plant or a torch is this block or one of its eight neighbours. */
    private static boolean rootedNear(java.util.Set<Long> rooted, float x, float y) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++) if (rooted.contains(block(x + dx * 256f, y + dy * 256f))) return true;
        return false;
    }

    private static long block(float x, float y) {
        return ((long) (int) Math.floor(x / 256f) << 32) ^ ((int) Math.floor(y / 256f) & 0xFFFFFFFFL);
    }

    private boolean nearLandmark(float x, float y) {
        for (float[] s : shops) if (Math.abs(s[0] - x) < 380 && Math.abs(s[1] - y) < 380) return true;
        for (Checkpoint c : checkpoints) if (Math.abs(c.x - x) < 380 && Math.abs(c.y - y) < 380) return true;
        return false;
    }

    /** Lift the boulder belt out of the world (the test area wants only the wall tiles). */
    public void clearBoulders() {
        for (int i = 0; i < boulders.length; i++) boulders[i] = new float[] {-9999f, -9999f, 0f, 0f};
    }

    /** Sweep the rock away: every tile map emptied, and the plants and torches that were rooted in it dropped. */
    private void stripWalls() {
        for (TileMap m : maps) java.util.Arrays.fill(m.cells, TileMap.EMPTY);
        plants = new float[0][];
        torches = new float[0][];
    }

    /** STORY CUES: moments the screen may stage as a cutscene. The world only names them; the
     *  screen drains the queue (or throws it away, in a shared race). */
    public final java.util.ArrayDeque<Cue> cues = new java.util.ArrayDeque<>();

    public void cue(Cue c) {
        cues.add(c);
    }

    public World() {
        this(0);
    }

    private final DockingSystem docking;
    private final CacheDirector cacheDirector;
    public final Missions missions; // THE MISSIONS: run-scoped side work, the bounty first
    private final LootSystem loot = new LootSystem();
    private final EnemyDirector enemyDirector;

    public String[] regionNames = { "Avatarana", "Svarna Dvara", "Maya Vana", "Svapna Mandala", "Bala Mukha", "Boss: Bala" };
    public static final float ATALA_Y = 1600; // the grand gate: below = Bhu Loka, at/above = Atala
    public float[] regionStart = { 0, ATALA_Y, 4100, 7600, 12000, 17600 };
    public float[] regionEnd = { 1600, 4100, 7600, 12000, 17600, 20600 };
    public float[][] shops = new float[4][2]; // Atala bands 1-4: none in Bhu, none in the boss band
    public int[] shopState = new int[shops.length];
    public float[] shopTimer = new float[shops.length];
    public static final int UNDOCKED = 0, DOCKING = 1, DOCKED = 2, SITTING = 3;
    public static final float DOCK_TIME = 0.6f; // the doc1..doc6 sequence: 6 frames × 0.1s

    public float[][] torches; // {anchorX, anchorY, angle, sampleX, sampleY}: wall face it sits on
    public float[][] plants;  // {x, y, variant, phase, angle, size, sampleX, sampleY}: base anchored on rock

    /** The world clock: drives enemy orbits, dust and every animation in one place. */
    public float clock; // sim seconds since the run began; the wire stamps SHIP poses with it

    // SCORE: band crossings paid once per run (dying back into an earlier band never re-pays).
    private boolean[] regionScored;
    private int lastRegion;

    /** The bands whose first crossing has paid, sized on first use (RunSave reads and writes it). */
    boolean[] regionScored() {
        if (regionScored == null) {
            regionScored = new boolean[regionStart.length];
            regionScored[0] = true;
        }
        return regionScored;
    }

    /** A restored run is standing in the band it saved in: no "first crossing" fires on the first frame. */
    void settleRegion() { lastRegion = currentRegionIndex(); }

    /** True when the WHOLE hull sits inside the marked bay under shop i. */
    public boolean inDockBox(int i, float x, float y) {
        return docking.inDockBox(i, x, y);
    }

    /** A docking just finished — the screen opens the hangar on this. */
    public boolean shopReady() {
        return docking.shopReady();
    }

    /** Closing the hangar parks that bay as SITTING, so it cannot reopen until you fly out. */
    public void closeDocked() {
        docking.closeDocked();
    }

    public void update(float dt, ShipControls input) {
        if (clientSeat) { // a guest's seat: my hull, my shots, the swarm as ghosts (see docs)
            updateSeat(dt, input);
            return;
        }
        if (victoryT >= 0) victoryT += dt;
        shooter = 0; // this frame's hurts are the host's own unless a round claims otherwise
        int chain = score.combo;
        score.update(dt);
        if (chain >= 5 && score.combo == 0) // a chain worth missing just dropped
            popups.push(new Popups.Spec().text("COMBO LOST").img("shanti").at(ship.x, ship.y + 30)
                    .color(1f, 0.35f, 0.3f).scale(1.2f).life(1f).intro(0));
        popups.update(dt);
        if (boss == null) enemyDirector.trySpawnBoss(); // the frame the quota fills, he is simply there
        gazeCheck();
        ship.update(dt, input, this);
        if (campaign) tickVitala(dt);
        hazards.update(this, dt);
        tickChests(dt);
        if (((int) (clock * 60f)) % 10 == 0) holdPosts();
        regionEntry();
        for (Orb o : orbs) o.update(dt, this, orbRng);
        clock += dt;
        if (partner != null) { // the partner glide between packets, like everything else
            partner.x += partner.vx * dt;
            partner.y += partner.vy * dt;
        }
        for (SeatHull x : extras) if (x.ship != null) { // extras glide too
            x.ship.x += x.ship.vx * dt;
            x.ship.y += x.ship.vy * dt;
        }
        // index loop: a summon (boss blink) may append to the roster mid-iteration
        for (int i = 0; i < enemies.size(); i++) enemies.get(i).update(dt, this, clock);
        if (!clientSeat) enemyDirector.tickBackup(dt);                  // the leaders' calls: reserves rise from the floor
        separateEnemies(); // bodies never sit on each other
        updateAstra(dt); // the forged weapon's cubes, after the roster has moved
        // Contact: each body shoves the other away along the contact line, opposite directions.
        // Impulses are velocity kicks — ship friction and Brain steering bleed them off naturally.
        for (Enemy e : enemies) contact(e);
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(this, dt);
            if (!b.dead) collide(b);
            if (b.dead) {
                bullets.remove(i);
                bulletPool.free(b);
            }
        }
        for (int i = enemies.size() - 1; i >= 0; i--) if (enemies.get(i).dead) enemies.remove(i);
        for (int i = effects.size() - 1; i >= 0; i--) {
            Effect e = effects.get(i);
            e.update(dt);
            if (e.done()) effects.remove(i);
        }
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            // LIFETIME: uncollected loot used to sit in the world forever, so a full clear
            // buried the band in coins and a health pop at a full hull floated until the end
            // of time. Everything fades out on its own now.
            p.life -= dt;
            if (p.life <= 0) { pickups.remove(i); continue; }
            // MAGNETISM: a faint tug toward the hull inside PULL_R, force scaled by the hangar's
            // magnet power. Coins, health, shards, pieces — everything gets it.
            float mx = ship.x - p.x, my = ship.y - p.y;
            float m2 = mx * mx + my * my;
            if (m2 < PULL_R * PULL_R && m2 > 4) {
                float inv = 1 / (float) Math.sqrt(m2);
                p.vx += mx * inv * hangar.magnet * PULL_G * dt;
                p.vy += my * inv * hangar.magnet * PULL_G * dt;
            }
            p.update(dt);
            float dx = ship.x - p.x, dy = ship.y - p.y;
            if (dx * dx + dy * dy > Pickup.COLLECT_R * Pickup.COLLECT_R) continue;
            if (p.type == Pickup.HEALTH && ship.hp >= ship.maxHp()) continue; // full hull: it floats on
            loot.collect(this, p);
            pickups.remove(i);
        }
        for (Checkpoint c : checkpoints) collectCheckpoint(c);
        cacheDirector.update();
        cacheDirector.resolve();
        for (Enemy v : voices) v.update(dt, this, clock); // THE VOICES hover off the roster
        Chatter.update(this); // the voices meet the pilot on their own events, once per run
        tickWords(dt);
        missions.update(); // the bounty pays the frame the rogue is spent; wrecks pay as they're carried
        docking.update(dt); // after ship.update, so the bay test sees this frame's position
    }

    /** A checkpoint under the hull: taken, scored, and the new respawn anchor. The guest seat
     *  runs the same test — the checkpoints are the same geometry on every machine, so a guest
     *  anchors on the same stone the host did. */
    /** VITALA'S TWO MECHANICS, once a step: the Hataki's CURRENT carries every hull and body on the gold (a drift in the pools, a hard pull in the
     *  Hearth Deep's rapids), and the FIRE VENTS burn whatever stands in a jet while it flames. A lit shrine hushes both for six seconds. */
    private void tickVitala(float dt) {
        if (quietT > 0f) quietT = Math.max(0f, quietT - dt);
        boolean quiet = quietT > 0f;
        vents.arenaOn = stage == 1 && boss != null && !bossDead && boss.bossPhase == 2;   // the Altar's rim vents open in the second phase (Kindling)
        flood = stage == 1 && boss != null && !bossDead && boss.bossPhase >= 3;           // and in the third the cataract is opened: the Flood
        ship.heated = !(quietT > 0f) && ship.y > Hataki.START[1];                      // the forge-lands' heat, past the Plunge, unless a shrine holds its breath
        if (Hataki.on(ship.x, ship.y)) Codex.unlock(this, Codex.HATAKI);
        if (!quiet) {
            carry(ship, dt);
            for (int i = 0; i < enemies.size(); i++) {
                Enemy e = enemies.get(i);
                if (e.dead || e.boss || e.submerged() || !e.active) continue;      // a sleeper keeps its place: the river does not float it away
                float[] f = flowBuf;
                Hataki.flow(e.x, e.y, f);
                if (f[0] == 0f && f[1] == 0f) continue;
                float k = Hataki.forced(e.y) ? 0.3f : 0.15f;                  // a post holds against the river: it only leans on a unit
                float[] c = collide(e.x + f[0] * k * dt - e.w / 2, e.y + f[1] * k * dt - e.h / 2, e.w, e.h);
                e.x = c[0] + e.w / 2;
                e.y = c[1] + e.h / 2;
            }
        }
        ventHurtT = Math.max(0f, ventHurtT - dt);
        for (int i = 0; i < vents.count(); i++) {
            if (vents.state(i, clock, quiet) != Vents.FLAMING) continue;
            if (ventHurtT <= 0f && vents.burns(i, ship.x, ship.y, 10f, clock, quiet)) {
                ship.damage(VENT_DAMAGE, this);
                ventHurtT = 0.35f;
            }
            // (the forge's own are at home in its fire: a vent burns the pilot, never the garrison - and never wakes a court)
        }
    }

    // ---- TREASURE CHESTS and EMBER WELLS ---------------------------------------------------------------------------------------

    /** Fly into a chest and it opens (a guarded one stays shut until its ambush has fallen); an open one plays its lid; a well in a side cavern of
     *  Vitala mends a hurt hull once. */
    private void tickChests(float dt) {
        for (int i = 0; i < chests.size(); i++) {
            Chest c = chests.get(i);
            if (c.open) { c.openT += dt; continue; }
            if (c.guards > 0) {                                               // a den's chest: shut while its guards live
                int alive = 0;
                for (int j = 0; j < enemies.size(); j++) {
                    Enemy e = enemies.get(j);
                    if (!e.dead && e.siteGuard && Math.abs(e.homeX - c.x) < 420f && Math.abs(e.homeY - c.y) < 300f) alive++;
                }
                if (alive == 0) c.guards = 0;
            }
            if (c.guards == 0 && c.touches(ship.x, ship.y)) openChest(c);
        }
        for (int i = 0; i < wellUsed.length; i++) {
            if (wellUsed[i] || pockets[i][5] != MapGen.WELL) continue;
            float dx = ship.x - pockets[i][0], dy = ship.y - pockets[i][1];
            if (dx * dx + dy * dy > 46f * 46f) continue;
            if (ship.hp >= ship.maxHp() * 0.98f && ship.boostEnergy >= 99f) continue;   // nothing to mend: keep it for later
            wellUsed[i] = true;
            ship.hp = ship.maxHp();
            ship.shieldEnergy = ship.shieldCap();
            ship.boostEnergy = 100f;
            addEffect(Effect.ring(pockets[i][0], pockets[i][1], 120, 1f, 0.6f, 0.2f));
            addEffect(Effect.ring(pockets[i][0], pockets[i][1], 60, 1f, 0.95f, 0.7f));
            popups.push(new Popups.Spec().text("THE WELL MENDS").at(ship.x, ship.y + 34).color(1f, 0.7f, 0.3f).scale(1.2f).life(1.6f));
        }
    }

    /** Pay a chest: coins that fly out, and the one useful thing of its loka's table. Deterministic from where it stands, so a restored run
     *  and a shared race roll the same treasure. */
    public void openChest(Chest c) {
        if (c.open) return;
        c.open = true;
        c.openT = 0f;
        Random rng = new Random(Float.floatToIntBits(c.x) * 31L + Float.floatToIntBits(c.y));
        int coins = Chest.coins(c.loka, rng), item = Chest.item(c.loka, rng);
        for (int i = 0; i < Math.min(coins, 14); i++) {                       // the purse bursts out, the rest goes straight in
            float a = (float) (i * 2.3999632 + rng.nextFloat() * 0.3f);
            Pickup p = new Pickup();
            p.netId = dropSeq++;
            p.type = Pickup.COIN;
            p.variant = rng.nextInt(Pickup.VARIANTS);
            p.x = c.x;
            p.y = c.y;
            float d = 30f + 50f * rng.nextFloat();
            p.vx = (float) Math.cos(a) * d * Pickup.DRAG;
            p.vy = (float) Math.sin(a) * d * Pickup.DRAG;
            p.phase = rng.nextFloat() * 6.2831853f;
            pickups.add(p);
        }
        hangar.coins += Math.max(0, coins - 14);
        String label = Chest.ITEM[item];
        switch (item) {
            case Chest.MEND:
                ship.hp = ship.maxHp();
                ship.shieldEnergy = ship.shieldCap();
                break;
            case Chest.SHARDS: {
                int[] pay = LokaTable.at(c.loka).shards;
                int n = Chest.shards(c.loka);
                for (int k = 0; k < n; k++) hangar.addShard(pay[0]);
                label = n + " " + (pay[0] == 2 ? "AGNI" : "AKASHA") + " SHARDS";
                break;
            }
            case Chest.LIFE:
                lives = Math.min(MAX_LIVES, lives + 1);
                break;
            default:                                                          // SURGE: boost and shield brimming
                ship.boostEnergy = 100f;
                ship.shieldEnergy = ship.shieldCap();
                break;
        }
        float[] col = c.loka == 0 ? new float[] {1f, 0.92f, 0.6f} : new float[] {1f, 0.62f, 0.2f};
        addEffect(Effect.ring(c.x, c.y, 110, col[0], col[1], col[2]));
        addEffect(Effect.ring(c.x, c.y, 54, 1f, 1f, 0.9f));
        for (int i = 0; i < 18; i++) {
            float a = (float) (i * Math.PI * 2 / 18), v = 90f + rng.nextFloat() * 150f;
            addEffect(Effect.spark(c.x, c.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v + 40f, 2.6f, 0.6f + rng.nextFloat() * 0.5f,
                    3f, col[0], col[1], col[2]));
        }
        popups.push(new Popups.Spec().text("TREASURE  +" + coins + "  " + label).at(c.x, c.y + 42).color(col[0], col[1], col[2]).scale(1.3f).life(2.2f));
        score.add(Score.CHECKPOINT);
    }

    /** THE FLOOD (the boss's third phase): the Hataki pours into the Altar and carries every hull one way - east, across the hall. */
    public boolean flood;
    public static final float FLOOD_PUSH = 85f;
    public static final float VENT_DAMAGE = 8f, VENT_DAMAGE_FOE = 28f;
    private final float[] flowBuf = new float[2];

    /** The Hataki's pull on the hull: along the river's own direction, hard where the current is forced, light in the pools. */
    private void carry(Ship s, float dt) {
        Hataki.flow(s.x, s.y, flowBuf);
        if (flood && s.y > Hataki.START[5]) flowBuf[0] += FLOOD_PUSH;
        if (flowBuf[0] == 0f && flowBuf[1] == 0f) return;
        float k = Hataki.forced(s.y) ? 1f : 0.6f;
        float[] c = collide(s.x + flowBuf[0] * k * dt - Ship.W / 2, s.y + flowBuf[1] * k * dt - Ship.H / 2, Ship.W, Ship.H);
        s.x = c[0] + Ship.W / 2;
        s.y = c[1] + Ship.H / 2;
    }

    /** The current's push on a body at a point, px/s (what the HUD and the tests read); zero off the gold, or while the shrine hushes it. */
    public float[] currentAt(float x, float y) {
        float[] out = new float[2];
        if (campaign && quietT <= 0f) Hataki.flow(x, y, out);
        return out;
    }

    private void collectCheckpoint(Checkpoint c) {
        if (c.collected || !c.touches(ship.x, ship.y)) return;
        c.collected = true;
        if (c.y >= Hataki.Y0) quietT = 6f;  // a shrine of Vitala hushes the band: no current, no flame, for six seconds
        respawnCp = c;
        score.add(Score.CHECKPOINT);
        popups.push(new Popups.Spec().text("ANCHORED").img("pratishtha")
                .at(c.x, c.y + 30).color(0.5f, 0.9f, 1f).scale(1.1f));
        addEffect(Effect.ring(c.x, c.y, 54, 1f, 0.8f, 0.35f));                      // the lamp catches
        addEffect(Effect.ring(c.x, c.y, 28, 1f, 0.97f, 0.8f));
        for (int i = 0; i < 16; i++) {                                              // and sheds embers
            float a = (float) (i * Math.PI * 2 / 16 + fxRng.nextFloat() * 0.3f), v = 70f + fxRng.nextFloat() * 130f;
            addEffect(Effect.spark(c.x, c.y + 6f, (float) Math.cos(a) * v, (float) Math.sin(a) * v + 50f, 2.5f,
                    0.5f + fxRng.nextFloat() * 0.4f, 3f, 1f, 0.72f + 0.2f * fxRng.nextFloat(), 0.3f));
        }
    }

    // ---- the guest seat ----------------------------------------------------------------------

    /** The client machine's run in full: my hull, my shots, my use of the land, and the
     *  swarm's foes and loot as gliding ghosts. Damage and kills travel as PAINs instead
     *  of impulses — my machine owns everything about my hull; the host owns the fight. */
    private void updateSeat(float dt, ShipControls input) {
        int chain = score.combo;
        score.update(dt);
        if (chain >= 5 && score.combo == 0) // a chain worth missing just dropped
            popups.push(new Popups.Spec().text("COMBO LOST").img("shanti").at(ship.x, ship.y + 30)
                    .color(1f, 0.35f, 0.3f).scale(1.2f).life(1f).intro(0));
        popups.update(dt);
        ship.update(dt, input, this);
        regionEntry();
        guestCheckpoints();
        for (Orb o : orbs) o.update(dt, this, orbRng); // the same drift the host's orbs take
        if (partner != null) {
            partner.x += partner.vx * dt;
            partner.y += partner.vy * dt;
        }
        for (SeatHull x : extras) if (x.ship != null) { // extras glide here too
            x.ship.x += x.ship.vx * dt;
            x.ship.y += x.ship.vy * dt;
        }
        clock += dt;
        updateAstra(dt);
        ghostGlide(dt);
        for (int i = bullets.size() - 1; i >= 0; i--) { // flight and expiry only; combat waits for the swarm
            Bullet b = bullets.get(i);
            b.update(this, dt);
            if (b.dead) {
                bullets.remove(i);
                bulletPool.free(b);
            }
        }
        for (int i = pickups.size() - 1; i >= 0; i--) { // my magnet, my sweep; the host is told
            Pickup p = pickups.get(i);
            float mx = ship.x - p.x, my = ship.y - p.y;
            float m2 = mx * mx + my * my;
            if (m2 < PULL_R * PULL_R && m2 > 4) {
                float inv = 1 / (float) Math.sqrt(m2);
                p.vx += mx * inv * hangar.magnet * PULL_G * dt;
                p.vy += my * inv * hangar.magnet * PULL_G * dt;
            }
            p.update(dt);
            float dx = ship.x - p.x, dy = ship.y - p.y;
            if (dx * dx + dy * dy > Pickup.COLLECT_R * Pickup.COLLECT_R) continue;
            if (p.type == Pickup.HEALTH && ship.hp >= ship.maxHp()) continue; // full hull: it floats on
            loot.collect(this, p);
            takes.add(p.netId); // the hub retires that drop; the next swarm carries the absence
            pickups.remove(i);
        }
        docking.update(dt);
    }

    /** The guest seat's checkpoints: the same stones the host anchors on (the world is the
     *  same geometry on every machine), so a guest respawns where the crew respawns. */
    private void guestCheckpoints() {
        for (Checkpoint c : checkpoints) collectCheckpoint(c);
    }

    /** Ghost render objects glide toward their last snapshot truth (10 Hz set, 60 Hz flow). */
    private void ghostGlide(float dt) {
        float k = 1f - (float) Math.exp(-12f * dt);
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy g = enemies.get(i);
            if (!g.ghost) continue;
            if (g.gone) {
                enemies.remove(i);
            } else {
                g.x += (g.gx - g.x) * k;
                g.y += (g.gy - g.y) * k;
            }
        }
        for (Pickup p : pickups) {
            p.x += (p.gx - p.x) * k;
            p.y += (p.gy - p.y) * k;
        }
    }

    /** The swarm's foe rows: keyed on the host's ids, each ghost gliding to the newest truth.
     *  The flavour's low 5 bits name the Kind, the top three the boss / elite / rank tags;
     *  hpFrac is the host's read of the foe's armour, so the guest's bar is honest. */
    public void ghostAt(int id, float x, float y, int flavour, int hpFrac) {
        for (Enemy e : enemies) {
            if (e.ghost && e.netId == id) {
                e.gone = false;
                e.gx = x;
                e.gy = y;
                e.seen = true;
                e.hp = e.maxHp * (hpFrac / 255f);
                e.dispHp = e.hp;
                return;
            }
        }
        Enemy e = new Enemy(Enemy.Kind.values()[flavour & 0x1F], x, y);
        e.ghost = true;
        e.netId = id;
        e.boss = (flavour & 0x20) != 0;
        e.elite = (flavour & 0x40) != 0;
        if ((flavour & 0x80) != 0) e.rank = 1;
        e.active = true; // ghosts never wake; they start at the swarm's truth
        e.hp = e.maxHp * (hpFrac / 255f);
        e.dispHp = e.hp;
        e.gx = x;
        e.gy = y;
        e.seen = true;
        enemies.add(e);
        if (e.boss && boss == null) boss = e; // the warden shows on the guest's bar too
    }

    /** Loot rows: keyed the same way, riding the ghosts' glide. */
    public void lootGhost(int id, int type, int variant, float x, float y) {
        for (Pickup p : pickups) {
            if (p.netId == id) {
                p.gx = x;
                p.gy = y;
                p.seen = true;
                return;
            }
        }
        Pickup p = new Pickup();
        p.netId = id;
        p.type = type;
        p.variant = variant;
        p.x = x;
        p.y = y;
        p.gx = x;
        p.gy = y;
        p.seen = true;
        pickups.add(p);
    }

    /** Everything the last swarm failed to mention is gone on the host too. */
    public void expireMissingGhosts() {
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (!e.ghost) continue;
            if (!e.seen) enemies.remove(i);
            else e.seen = false;
        }
        for (int i = pickups.size() - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            if (!p.seen) pickups.remove(i);
            else p.seen = false;
        }
    }

    /** The hub has spoken about my seat: the hull or the book feels it here, immediately. */
    public void pain(long bits, int kind, float b, float c) {
        switch (kind) {
            case PAIN_TOUCH: // a partner's round touched me; my shields and armour answer locally
                ship.damage(b, this);
                break;
            case PAIN_SHOVE: // contact: the line's kick is what the push gives me
                ship.vx += b;
                ship.vy += c;
                break;
            case PAIN_KILL: // my round landed: my chain, my rate; the land's popup is news anyway
                Enemy.Kind k = Enemy.Kind.values()[(int) (bits & 0xFF)];
                boolean boss = (bits & 0x100) != 0, elite = (bits & 0x200) != 0, lead = (bits & 0x400) != 0;
                int tier = boss ? 3 : elite ? 2 : lead ? 1 : 0;
                score.combo++;
                score.comboT = Score.WINDOW;
                long pay = (long) (Score.TIER[tier] + Math.round(k.hp));
                score.add(pay);
                popups.push(new Popups.Spec().text("+" + pay).at(b, c + 10)
                        .color(1f, 215 / 255f, 0).scale(boss || elite ? 1.4f : 1f));
                break;
            default:
                break;
        }
    }

    // ---- co-op plumbing both seats read ------------------------------------------------------

    /** Is the partner hull flying? Its hp is its pilot's word, arriving on the SHIP packets. */
    public boolean partnerAlive() {
        return partner != null && partner.hp > 0;
    }

    /** The brains' targeting eye: whichever hull is nearest their mischief. */
    public Ship nearestHull(float x, float y) {
        Ship best = ship;
        float bestD2 = (x - ship.x) * (x - ship.x) + (y - ship.y) * (y - ship.y);
        if (partnerAlive()) {
            float d2 = (x - partner.x) * (x - partner.x) + (y - partner.y) * (y - partner.y);
            if (d2 < bestD2) { best = partner; bestD2 = d2; }
        }
        for (SeatHull gh : extras) { // the extra pilots' hulls fly into their fire too
            if (gh.ship == null || gh.ship.hp <= 0) continue;
            float d2 = (x - gh.ship.x) * (x - gh.ship.x) + (y - gh.ship.y) * (y - gh.ship.y);
            if (d2 < bestD2) { best = gh.ship; bestD2 = d2; }
        }
        return best;
    }

    /** The deepest hull crossing of the wake line — a boss wakes on any hull, not one. */
    public float deepestHullY() {
        float top = ship.y;
        if (partnerAlive() && partner.y > top) top = partner.y;
        for (SeatHull gh : extras)
            if (gh.ship != null && gh.ship.hp > 0 && gh.ship.y > top) top = gh.ship.y;
        return top;
    }

    /** Something the host did to my hull or my kill book, born in {@code countKill}/collide. */
    public static class Pain {
        public int target, kind;
        public long a;
        public float b, c;
    }

    public interface PainSink {
        void pain(Pain p);
    }

    /** A round the sim birthed: the wire relays it, def-less, and both skies match. */
    public static class Shot {
        public float x, y, vx, vy, damage, radius, r, g, b;
        public boolean hostile;
    }

    public interface ShotSink {
        void shot(Shot s);
    }

    public static final int PAIN_TOUCH = 0, PAIN_SHOVE = 1, PAIN_KILL = 2;

    /** Which loka each band belongs to (index into LokaTable). Atala holds regions 0-5, so
     *  every band of the current build is loka 0; the next loka claims regions 6 onwards.
     *  This is the mapping the HUD's region names and the economy's "which element pays"
     *  both read — bands are NOT lokas, and treating them as one is how Atala would end up
     *  paying Vayu in its own boss arena. */
    public int[] lokaOfRegion = { 0, 0, 0, 0, 0, 0 };

    /** SCORE: the first crossing into a band pays once per run. The spawn band is pre-scored
     *  (you start there), and however many times a death throws you back, already-seen bands
     *  stay silent. Sized from regionStart, so a future loka's bands cost nothing. */
    private void regionEntry() {
        if (regionScored == null) {
            regionScored = new boolean[regionStart.length];
            regionScored[0] = true;
        }
        int r = currentRegionIndex();
        // the deepest loka reached IS the loka: it decides which tatva the drops pay, which
        // hulls the hangar will sell, and how mean the roster is. Best-run-so-far, so a death
        // back down a band never un-sells anything or re-lights a tatva you already fed.
        int loka = lokaOfRegion[Math.min(r, lokaOfRegion.length - 1)];
        if (loka > lokaIndex) {
            lokaIndex = loka;
            hangar.loka = lokaIndex;
        }
        if (r == lastRegion) return;
        lastRegion = r;
        if (!regionScored[r]) {
            regionScored[r] = true;
            score.add(Score.REGION);
            Codex.unlock(this, Codex.DEEPENING); // the descent teaches, the book takes the page
            if (r == 1) cue(Cue.ATALA_GATE);
            if (r == 6) {                                                           // Bala's door: Vitala
                cue(Cue.VITALA_ENTER);
                Codex.unlock(this, Codex.VITALA);
            }
            popups.push(new Popups.Spec().text("THE DEEPENING").img("pravesh")
                    .at(ship.x, ship.y + 40).color(1f, 1f, 1f).scale(1.2f).life(1.8f));
        }
    }

    /** What a death pays: the whole purse leaves as ONE fanned burst. Called from Enemy.hurt —
     *  retires and stand-downs never pay. */
    public void dropLoot(Enemy e) {
        loot.drop(this, lootRng, e);
    }

    /** The cache fights back: the local squad within reach converges on the ship — site guards
     *  stay put, and so does everything outside the radius. */
    public void touchCache(int i) {
        cacheDirector.touch(i);
    }

    /** A contested cache stays shut while any of its squad still lives. */
    public void resolveCaches() {
        cacheDirector.resolve();
    }

    /** Death drops you at the most recent collected checkpoint, else the region entrance. */
    public float[] respawnAnchor() {
        if (respawnCp != null) return new float[] { respawnCp.x, respawnCp.y };
        ANCHOR[0] = width / 2;
        ANCHOR[1] = regionStart[currentRegionIndex()] + 120;
        return ANCHOR;
    }

    private static final float[] ANCHOR = new float[2];

    /** True when a body of half-size half centred here touches no rock and no shop wall. */
    public boolean openFor(float x, float y, float half) {
        return !blockedPx(x, y) && !blockedPx(x - half, y - half) && !blockedPx(x + half, y - half)
                && !blockedPx(x - half, y + half) && !blockedPx(x + half, y + half);
    }

    /** Every pair of live bodies keeps its distance: two enemies that overlap are eased apart a few
     *  pixels a frame, each into open air (the push goes through collide, so it never drives a body
     *  into rock). The ruler and anyone held for a scene do not move; the others yield to them. */
    private void separateEnemies() {
        int n = enemies.size();
        for (int i = 0; i < n; i++) {
            Enemy a = enemies.get(i);
            if (a.dead || a.ghost) continue;
            for (int j = i + 1; j < n; j++) {
                Enemy b = enemies.get(j);
                if (b.dead || b.ghost) continue;
                float need = (a.w + b.w) * 0.5f * 0.92f;
                float dx = b.x - a.x, dy = b.y - a.y;
                if (Math.abs(dx) >= need || Math.abs(dy) >= need) continue;
                float d2 = dx * dx + dy * dy;
                if (d2 >= need * need) continue;
                float d = (float) Math.sqrt(d2);
                if (d < 0.01f) { dx = 1; dy = 0; d = 1; } // an exact stack splits sideways
                float push = Math.min(3f, (need - d) * 0.5f);
                boolean fa = a.boss || a.parley, fb = b.boss || b.parley;
                float sa = fa ? 0f : (fb ? 2f : 1f), sb = fb ? 0f : (fa ? 2f : 1f);
                nudge(a, -dx / d * push * sa, -dy / d * push * sa);
                nudge(b, dx / d * push * sb, dy / d * push * sb);
            }
        }
    }

    private void nudge(Enemy e, float dx, float dy) {
        if (dx == 0 && dy == 0) return;
        float[] c = collide(e.x + dx - e.w / 2, e.y + dy - e.h / 2, e.w, e.h);
        e.x = c[0] + e.w / 2;
        e.y = c[1] + e.h / 2;
    }

    boolean spotClear(float x, float y) {
        return !blockedPx(x, y) && !blockedPx(x + 40, y) && !blockedPx(x - 40, y)
                && !blockedPx(x, y + 40) && !blockedPx(x, y - 40);
    }

    /** Resolves one live shot against whatever it is meant to hit. */
    private void collide(Bullet b) {
        if (b.hostile) {
            hullHit(b); // my hull, and the partner's in co-op
            return;
        }
        // BULLET DUEL: a player round trades with hostile fire it can beat. Equal damage
        // cancels both (same power, same cancellation); a round that beats the shot kills it;
        // a round that can't beat it is spent for nothing and the shot flies on. The trade
        // always costs the round, so clearing fire is never free — and light weapons (Light
        // MG, a weak charge) can't touch heavy shots at all. A splash round goes off where
        // it died: an intercepted bomb still explodes, and eats the shot that stopped it.
        for (Bullet o : bullets) {
            if (!o.hostile || o.dead) continue;
            if (Math.abs(b.x - o.x) > b.radius + o.radius) continue;
            if (Math.abs(b.y - o.y) > b.radius + o.radius) continue;
            if (b.damage >= o.damage) o.dead = true;
            if (b.aoe > 0) b.detonate(this);
            else b.dead = true;
            break;
        }
        if (b.dead) return;
        shooter = b.owner < 0 ? 0 : b.owner; // an unstamped round is this seat's own; relays carry ids
        for (Enemy e : enemies) {
            if (e.dead || e.submerged()) continue;                      // a round does not reach the floor
            if (Math.abs(b.x - e.x) > e.w / 2 + b.radius) continue;
            if (Math.abs(b.y - e.y) > e.h / 2 + b.radius) continue;
            if (b.aoe > 0) { // splash: bursts on contact, never a plain hit
                b.detonate(this);
                return;
            }
            e.hitVx = b.vx;
            e.hitVy = b.vy;
            boolean killed = e.hurt(b.damage, this);
            e.hitVx = e.hitVy = 0f;
            spray(b.x, b.y, b.vx, b.vy, b.r, b.g, b.b); // sparks thrown along the shot's path
            if (!b.pierceEnemies || killed) {
                b.dead = true;
                break;
            }
        }
    }

    /** Hostile fire against a hull: mine answers locally, an owned hull's pilot answers. */
    private void hullHit(Bullet b) {
        int hulls = 2 + extras.size();
        for (int i = 0; i < hulls; i++) {
            SeatHull gh = i >= 2 ? extras.get(i - 2) : null;
            Ship h = i == 0 ? ship : i == 1 ? partner : gh.ship;
            if (h == null || h.iFrames > 0) continue; // respawn grace: the ship ghosts, shots pass through
            if (Math.abs(b.x - h.x) < Ship.W / 2f + b.radius
                    && Math.abs(b.y - h.y) < Ship.H / 2f + b.radius) {
                if (h == ship) h.damage(b.damage, this);
                else { // the hull is their truth: their damage code will have felt it exactly
                    h.hp = Math.max(0, h.hp - b.damage); // the mirror's bar moves until the next word
                    if (painWire != null) {
                        Pain p = new Pain();
                        p.target = gh != null ? gh.seat : partnerSeat;
                        p.kind = PAIN_TOUCH;
                        p.b = b.damage;
                        painWire.pain(p);
                    }
                }
                b.dead = true;
                break;
            }
        }
    }

    /** Contact against one live enemy for every hull that could ram it. */
    private void contact(Enemy e) {
        body(e, ship, 0);
        if (partnerAlive()) body(e, partner, partnerSeat);
        for (SeatHull gh : extras) { // the extras ram too, and their shoves go home
            if (gh.ship == null || gh.ship.hp <= 0) continue;
            body(e, gh.ship, gh.seat);
        }
    }

    private void body(Enemy e, Ship h, int seat) {
        if (e.dead || e.bossHidden || e.submerged()) return; // mid-blink or down at the floor: no body to shove or ram
        float dx = h.x - e.x, dy = h.y - e.y;
        if (Math.abs(dx) >= Ship.W / 2 + e.w / 2 || Math.abs(dy) >= Ship.H / 2 + e.h / 2) return;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) { dx = 0; dy = 1; len = 1; } // dead centre: shove straight up
        if (h == ship) { // MY shove is mine; a remote hull's is a message home
            h.vx += dx / len * KNOCK;
            h.vy += dy / len * KNOCK;
        } else if (painWire != null) {
            Pain p = new Pain();
            p.target = seat;
            p.kind = PAIN_SHOVE;
            p.b = dx / len * KNOCK;
            p.c = dy / len * KNOCK;
            painWire.pain(p);
        }
        e.vx -= dx / len * KNOCK; // both bodies shove back: the enemy eats the same answer
        e.vy -= dy / len * KNOCK;
        e.recover = Enemy.RECOVER; // straight home for 2s, kind flair paused
        if (e.contactCd <= 0) {
            e.contactCd = RAM_CD;
            shooter = seat;
            e.hurt(RAM, this);
        }
    }

    /** Splash: full damage inside the radius, half in the overlap fringe. The caller has
     *  already stamped {@code shooter} (the detonating round's owner) so kills pay right. */
    public void areaDamage(float x, float y, float radius, float damage) {
        for (Enemy e : enemies) {
            if (e.dead) continue;
            float dx = e.x - x, dy = e.y - y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > radius + e.w / 2) continue;
            e.hurt(d <= radius ? damage : damage * 0.5f, this);
        }
    }

    /** Region-graded shop/checkpoint guards: a rank-1 leader on the post with two sentries
     *  flanking below it. Region 0 stays open — that is where the player spawns. */
    static final Enemy.Kind[][] GUARD = {
            { null, null },
            { Enemy.Kind.VAKRA, Enemy.Kind.AURIS },
            { Enemy.Kind.AURIS, Enemy.Kind.VAKRA },
            { Enemy.Kind.BHAVI, Enemy.Kind.VAKRA },
            { Enemy.Kind.BHAVI, Enemy.Kind.VAKRA },
            { Enemy.Kind.MAYAVI, Enemy.Kind.BHAVI },
            // Vitala's six regions: the keeper's two sentries (the Plunge has no post)
            { null, null },
            { Enemy.Kind.LANCE, Enemy.Kind.AURIS },
            { Enemy.Kind.VAKRA, Enemy.Kind.LANCE },
            { Enemy.Kind.HATAKA, Enemy.Kind.AURIS },
            { Enemy.Kind.BHAVI, Enemy.Kind.VAKRA },
            { Enemy.Kind.MAYAVI, Enemy.Kind.BHAVI },
    };

    void add(Enemy e) {
        if (clientSeat) return; // a guest's world holds no local roster; foes arrive as ghosts
        e.netId = enemySeq++; // the host's ledger: every foe a swarm can speak about
        if (!e.boss && !e.voice) {                                                // the band it is placed in is its wall
            int r = regionIndexAt(e.spawnY);
            e.bandMin = regionStart[r] + (r <= 1 ? 48f : 24f);
            e.bandMax = regionEnd[r] - 24f;
        }
        if (campaign && e.homeY >= Hataki.Y0 && !e.voice) {                       // Vitala's own: its own brain, and its own gifts
            e.vitala = true;
            if (e.kind == Enemy.Kind.HATAKA) e.phasing = true;                    // Rekha the wraith walks through rock
            if (e.kind == Enemy.Kind.BHAVI) e.armoured = true;                    // Tamra is copper-plated
        }
        // DIFFICULTY CURVE: the loka grades every unit it places, hull and fire alike, so a
        // deeper band's roster is meaner without any new behaviour code. An elite's doubling
        // rides on top of this, at placement.
        float rs = e.rankScale(); // a leader and an elite are bigger bodies than their court
        e.w *= rs;
        e.h *= rs;
        int own = lokaAt(e.spawnY);                                               // the loka it STANDS in, not the deepest one the pilot has reached
        float ramp = e.boss ? 1f : Balance.ramp(own, regionIndexAt(e.spawnY));   // and the band grades it again: a loka teaches before it tests
        float hp = LokaTable.at(own).hpScale * ramp;
        e.hp = e.maxHp = e.maxHp * hp;
        e.dispHp = e.maxHp;
        e.dmgScale = LokaTable.at(own).dmgScale * Balance.rampFire(own, regionIndexAt(e.spawnY));
        for (TileMap m : tileMaps()) {
            int x0 = (int) Math.floor((e.homeX - e.w / 2 - 16 - m.ox) / TileMap.TILE);
            int x1 = (int) Math.floor((e.homeX + e.w / 2 + 16 - m.ox) / TileMap.TILE);
            int y0 = (int) Math.floor((e.homeY - e.h / 2 - 16 - m.oy) / TileMap.TILE);
            int y1 = (int) Math.floor((e.homeY + e.h / 2 + 16 - m.oy) / TileMap.TILE);
            for (int ty = y0; ty <= y1; ty++) {
                for (int tx = x0; tx <= x1; tx++) {
                    if (tx >= 0 && ty >= 0 && tx < m.cols && ty < m.rows) m.set(tx, ty, TileMap.EMPTY);
                }
            }
        }
        enemies.add(e);
    }

    public float time() {
        return clock;
    }

    // --- THE ASTRA RITE ----------------------------------------------------------------------
    // See vajra.world.Astra for the rules in one place. HOLD F invokes (a cage of cubes gathers, the hull goes heavy,
    // foes in front of the nose are locked), LET GO looses it, and the price (five shards of the astra's element) is
    // paid at the loosing, never at the start. Hold longer and the volley is bigger and hits harder; let go too soon
    // and the mantra falters for free. Five elemental astras, each its own character, and the Brahmastra of all five.

    /** The shell's radius at the start of the invocation and at full strength. */
    private static final float WIND_R_OUT = 150f, WIND_R_IN = 58f;
    /** Frames held still at the moment of a full release. Impact is mostly the absence of motion. */
    public static final float RELEASE_FREEZE = 0.14f;
    /** The release is drawn in the astra's own tatva, lifted toward white for the crest: a hue that is nobody's
     *  element (it used to be water blue mixed in) reads as a mistake. */
    private static final float CREST_LIFT = 0.55f;
    private static final float[] BRAHMA_RGB = {1f, 0.86f, 0.45f}, PASHUPATA_RGB = {0.92f, 0.84f, 0.66f};

    /** Which astra X has chosen: an {@link Astra} kind. */
    public int astraSel;
    /** HOLD F is down: the cage is gathering. */
    public boolean astraInvoking;
    /** Seconds the invocation has been held. */
    public float astraCharge;
    /** Seconds until another can be invoked. */
    public float astraCd;
    /** The astra being invoked, or last loosed (colours the cage, the release and every impact). */
    public int astraKind;
    /** The foes locked by the invocation, nearest the nose first (valid up to {@link #astraLockCount}). */
    public final Enemy[] astraLocks = new Enemy[32];
    public int astraLockCount;
    /** Frames to freeze the world on release. Drained by the screen, not here. */
    public float hitStop;
    /** 1 at the instant of a full release, decaying. The screen turns this into a pull BACK, so the astra opens the
     *  view up rather than closing it in - the only way this camera moves. */
    public float astraPunch;

    /** Cubes in flight or in the shell, oldest first. The renderer draws them; nothing else touches them. */
    public final List<AstraCube> astra = new ArrayList<>();
    /** The Agni and Vayu releases: fire flowing out of the hull, and the tornado (see AstraFlow). */
    public final AstraFlow flow = new AstraFlow();

    /** THE THIRD EYE: while > 0 the Pashupatastra's beam burns from {@link #gazeShip} along its nose (seconds left), its damage graded by {@link #gazeDm}. */
    public float gazeT, gazeDm = 1f;
    public Ship gazeShip;
    public int gazeOwner;

    private void startGaze(Ship from, int seat, float dm) {
        gazeShip = from;
        gazeOwner = seat;
        gazeDm = dm;
        gazeT = Astra.GAZE_TIME;
    }

    /** The beam's tick: every living foe on the line takes its share, plate and rock be damned. */
    private void gazeBurn(float dt) {
        Ship g = gazeShip;
        if (g == null || g.hp <= 0) { gazeT = 0f; return; }
        float ca = (float) Math.cos(g.facing), sa = (float) Math.sin(g.facing);
        float half = Astra.GAZE_HALF * (1f + 0.1f * Astra.TIER[Astra.PASHUPATA]);
        for (Enemy e : enemies) {
            if (e.dead || e.dismissed || e.voice || e.ghost || e.bossHidden) continue;
            float dx = e.x - g.x, dy = e.y - g.y;
            float along = dx * ca + dy * sa, across = Math.abs(-dx * sa + dy * ca);
            if (along < 0f || along > Astra.GAZE_LEN || across > half + Math.max(e.w, e.h) * 0.5f) continue;
            e.hurtPiercing(Astra.GAZE_DPS * gazeDm * dt, this, gazeOwner);
        }
        if (fxRng.nextFloat() < 0.5f) {                                  // ash and sparks thrown off the beam's length
            float d = fxRng.nextFloat() * Astra.GAZE_LEN * 0.8f;
            debris(g.x + ca * d, g.y + sa * d, 160, crestR(), crestG(), crestB());
        }
    }

    public float astraCd() { return astraCd; }
    public boolean winding() { return astraInvoking; }

    /** How far the camera should pull back for what the astras are doing (>= 1; GameScreen eases toward it, quickly out and slowly back): a little while the mantra builds,
     *  a screen's worth for Agni's bloom (650 px reach), the tornado's whole radius for Vayu, the Pashupatastra's long beam, a volley in flight. 1 when nothing is going on. */
    /** The pull-back an astra being formed is heading for (for the renderer to bake the walls of that wider view ahead of time): 1 when none is. */
    public float astraWarm() { return astraInvoking ? 2.0f : 1f; }

    public float astraView() {
        float v = 1f;
        if (astraInvoking) v = Math.max(v, 1f + 0.3f * Astra.k(astraCharge));
        if (flow.kind == AstraFlow.AGNI && flow.t < 2.9f) v = Math.max(v, 1.9f);
        if (flow.kind == AstraFlow.LIGHTNING) v = Math.max(v, 1.6f);
        if (flow.kind == AstraFlow.BOULDER) v = Math.max(v, 1.7f);
        if (flow.kind == AstraFlow.LOTUS) v = Math.max(v, 2.0f);
        if (flow.kind == AstraFlow.VAYU && flow.t < AstraFlow.T_END) v = Math.max(v, Math.max(1.5f, Math.min(2.0f, flow.radius / 330f)));
        if (gazeT > 0f) v = Math.max(v, 1.9f);
        for (AstraCube c : astra) if (c.armed) { v = Math.max(v, 1.35f); break; }
        return v;
    }
    /** 0..1 through the invocation. The renderer and the HUD both read this, so the cage, the shake and the dial all
     *  build off one number. */
    /** The view creeps out (it does not leap) while Brahma is forged and while the lotus works. */
    public boolean astraSlowView() { return (astraInvoking && astraKind == Astra.BRAHMA) || flow.kind == AstraFlow.LOTUS; }

    public float windK() { return astraInvoking ? Astra.k(astraCharge) : 1f; }

    /** The astra F would invoke: the one X chose if it can be paid, else the first that can, else -1. */
    public int loadedAstra() {
        if (Astra.canLoose(hangar, astraSel)) return astraSel;
        for (int k = 0; k < Astra.KINDS; k++) if (Astra.canLoose(hangar, k)) return k;
        return -1;
    }

    /** Ready to START an invocation: something to loose, none running, no cooldown, the run alive. */
    public boolean astraReady() {
        return astraCd <= 0 && !astraInvoking && !runEnded() && loadedAstra() >= 0;
    }

    /** X: step the choice to the next astra that can be paid (wrapping). False when none can. */
    public boolean cycleAstra() {
        for (int step = 1; step <= Astra.KINDS; step++) {
            int k = (astraSel + step) % Astra.KINDS;
            if (Astra.canLoose(hangar, k)) {
                astraSel = k;
                float[] c = astraRgb(k);
                popups.push(new Popups.Spec().text(Astra.NAME[k] + " x" + Astra.releases(hangar, k))
                        .at(ship.x, ship.y + 60).color(c[0], c[1], c[2]).scale(1.2f).life(1.4f));
                return true;
            }
        }
        return false;
    }

    /** The colour of an astra: its element's tatva, or the Brahmastra's own gold. */
    public static float[] astraRgb(int kind) { return kind >= 0 && kind < Astra.ELEMENTS ? TATVA_RGB[kind] : kind == Astra.PASHUPATA ? PASHUPATA_RGB : BRAHMA_RGB; }

    /** The colour of the astra in the shell or in flight. */
    public float[] astraRgb() { return astraRgb(astraKind); }

    public float crestR() { float[] e = astraRgb(); return e[0] + (1f - e[0]) * CREST_LIFT; }
    public float crestG() { float[] e = astraRgb(); return e[1] + (1f - e[1]) * CREST_LIFT; }
    public float crestB() { float[] e = astraRgb(); return e[2] + (1f - e[2]) * CREST_LIFT; }

    /** The loka's own element: its colour code for the HUD accent. */
    public float[] elementRgb() { return TATVA_RGB[LokaTable.at(lokaIndex).shards[0]]; }

    /** BEGIN the invocation (F goes down). The cage starts with a few cubes and fills as you hold. False when there is
     *  nothing to invoke or it is too soon. Nothing is paid yet. */
    public boolean astraBegin() {
        if (!astraReady()) return false;
        astraKind = loadedAstra();
        astraInvoking = true;
        astraCharge = 0;
        astraLockCount = 0;
        ship.winding = true;
        ship.trauma = Math.max(ship.trauma, 0.25f); // the hull shudders as it loads
        float[] el = astraRgb();
        syncCage();
        popups.push(new Popups.Spec().text("THE " + Astra.EPITHET[astraKind]).img("taptam")
                .at(ship.x, ship.y + 70).color(crestR(), crestG(), crestB()).scale(1.4f).life(Astra.WIND_FULL + 0.4f));
        return true;
    }

    /** Give up the invocation without loosing it: nothing is spent. A death, a pause, a hangar, a scene. */
    public void astraCancel() {
        if (!astraInvoking) return;
        astraInvoking = false;
        astraCharge = 0;
        astraLockCount = 0;
        ship.winding = false;
        dropHeldCubes();
    }

    /** LET GO (F goes up): loose it, or let the mantra falter if it was not held long enough. True when it flew. */
    public boolean astraLoose() {
        if (!astraInvoking) return false;
        astraInvoking = false;
        ship.winding = false;
        float charge = astraCharge;
        if (charge < Astra.WIND_MIN) { // too soon: the mantra falters, and costs nothing
            astraCharge = 0;
            dropHeldCubes();
            astraLockCount = 0;
            astraCd = Math.max(astraCd, Astra.FALTER_LOCKOUT);
            return false;
        }
        int kind = astraKind;
        refreshLocks(); // while the charge is still held: the locks are as many as the cubes it earned (there may be none: it flies on along the nose)
        if (!Astra.pay(hangar, kind)) { // the shards went (sold) while you held it
            astraCharge = 0;
            dropHeldCubes();
            astraLockCount = 0;
            return false;
        }
        astraCharge = 0;
        float power = Astra.power(charge), dm = Astra.damageMult(power);
        astraCd = Astra.COOLDOWN;
        hitStop = RELEASE_FREEZE * (0.35f + 0.65f * power);
        astraPunch = 0.4f + 0.6f * power;
        ship.dipT = 0.35f;
        ship.trauma = Math.max(ship.trauma, 0.4f + 0.4f * power);
        float grand = 1f + 0.15f * Astra.TIER[kind];                                   // the deeper the astra, the greater its opening
        if (kind == Astra.PRITHVI) {                                                   // boulders out of the mandala (which stays)
            dropHeldCubes();
            flow.startBoulders(ship, 0, dm, power, (long) (clock * 1000f) * 31L + 5L);
            boons(kind);
            if (astraWire != null) astraWire.astra(kind, power, new int[0], 0);
            astraLockCount = 0;
            return true;
        }
        if (kind == Astra.BRAHMA) {                                                    // the lotus: five petals spread through the world
            dropHeldCubes();
            flow.startLotus(ship, 0, dm, power, (long) (clock * 1000f) * 31L + 6L);
            boons(kind);
            if (astraWire != null) astraWire.astra(kind, power, new int[0], 0);
            float[] lo = astraRgb();
            addEffect(Effect.ring(ship.x, ship.y, (180f + 90f * power) * grand, lo[0], lo[1], lo[2]));
            astraLockCount = 0;
            return true;
        }
        if (kind == Astra.AKASHA) {                                                    // the circular lightning
            dropHeldCubes();
            flow.startLightning(ship, 0, dm, power, (long) (clock * 1000f) * 31L + 3L);
            boons(kind);
            if (astraWire != null) astraWire.astra(kind, power, new int[0], 0);
            float[] lt = astraRgb();
            addEffect(Effect.ring(ship.x, ship.y, (160f + 80f * power) * grand, lt[0], lt[1], lt[2]));
            astraLockCount = 0;
            return true;
        }
        if (kind == Astra.AGNI || kind == Astra.VAYU) {                                // a flow, not cubes: fire pours out of the hull / the wind turns into a tornado
            long seed = (long) (clock * 1000f) * 31L + 7L;
            if (kind == Astra.AGNI) {
                dropHeldCubes();
                flow.startAgni(ship, 0, dm, power, seed);
            } else {
                flow.startVayu(ship, 0, dm, power, seed);                              // the held shards keep circling, then stream out with the wind
                boons(kind);
            }
            if (astraWire != null) astraWire.astra(kind, power, new int[0], 0);
            float[] fl = astraRgb();
            addEffect(Effect.ring(ship.x, ship.y, (140f + 70f * power) * grand, fl[0], fl[1], fl[2]));
            astraLockCount = 0;
            return true;
        }
        if (kind == Astra.PASHUPATA) {                                                 // the third eye opens: a beam, not cubes
            dropHeldCubes();
            startGaze(ship, 0, dm);
            if (astraWire != null) astraWire.astra(kind, power, new int[0], 0);
            float[] eye = astraRgb();
            addEffect(Effect.ring(ship.x, ship.y, (160f + 80f * power) * grand, eye[0], eye[1], eye[2]));
            astraLockCount = 0;
            return true;
        }

        // every held cube goes to a locked foe, round and round the locks
        int n = 0, held = 0;
        for (AstraCube c : astra) if (!c.armed) held++;
        int[] ids = new int[held];
        int fired = 0;
        for (AstraCube c : astra) {
            if (c.armed) continue;
            Enemy t = astraLockCount > 0 ? astraLocks[fired % astraLockCount] : null;   // no lock: it flies straight and finds what it meets
            float a = ship.facing + (fired - (held - 1) * 0.5f) * 0.18f; // a fan off the nose, a volley leaving as one
            c.dress(c.element, dm);
            c.arm(a, Astra.SPEED[c.element], t);
            if (t != null && fired < ids.length) ids[n++] = t.netId;
            fired++;
        }
        boons(kind);
        // THE WIRE: a guest seat names the foes it locked and the hub looses the same volley there, so the astra lands
        // for real and the kills pay the pilot who fired it.
        if (astraWire != null) astraWire.astra(kind, power, ids, n);
        float[] el = astraRgb();
        addEffect(Effect.ring(ship.x, ship.y, (120f + 60f * power) * grand, el[0], el[1], el[2]));
        addEffect(Effect.ring(ship.x, ship.y, (70f + 40f * power) * grand, crestR(), crestG(), crestB()));
        for (int i = 0; i < held; i++) debris(ship.x, ship.y, 340, crestR(), crestG(), crestB());
        astraLockCount = 0;
        return true;
    }

    /** What the astra gives its caster at the moment of release: a boon of its element, never a free full repair. */
    private void boons(int kind) {
        boolean all = kind == Astra.BRAHMA;
        if (kind == Astra.AKASHA || all) ship.shieldEnergy = ship.shieldCap();
        if (kind == Astra.VAYU || all) ship.boostEnergy = 100;
        if (kind == Astra.PRITHVI) ship.iFrames = Math.max(ship.iFrames, Astra.STONE_SKIN);
        if (all) ship.iFrames = Math.max(ship.iFrames, Astra.STONE_SKIN * 0.5f);
        // Jala's boon is its blows: each hit mends the hull (see onAstraHit)
    }

    private void dropHeldCubes() {
        for (int i = astra.size() - 1; i >= 0; i--) {
            AstraCube c = astra.get(i);
            if (c.armed) continue;
            debris(c.x, c.y, 90, crestR(), crestG(), crestB());
            astra.remove(i);
        }
    }

    /** THE MANDALA IS A SHIELD: while an astra is being formed every enemy round that touches its circle is destroyed where it touches, so nothing reaches the hull. */
    private void mandalaGuard(float k) {
        float R = Astra.mandalaRadius(astraKind, k), r2 = R * R;
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = bullets.get(i);
            if (!b.hostile || b.dead) continue;
            float dx = b.x - ship.x, dy = b.y - ship.y;
            if (dx * dx + dy * dy > r2) continue;
            b.dead = true;
            float[] e = astraRgb();
            addEffect(Effect.ring(b.x, b.y, 16f, crestR(), crestG(), crestB()));
            addEffect(Effect.spark(b.x, b.y, -dx * 0.5f, -dy * 0.5f, 3f, 0.3f, 3f, e[0], e[1], e[2]));
        }
    }

    /** Grow the cage to the cubes the invocation has earned so far, spaced evenly round the hull. */
    private void syncCage() {
        int want = Astra.cubesAt(astraKind, astraCharge);
        int held = 0;
        float orbit = 0;
        for (AstraCube c : astra) if (!c.armed) { held++; orbit = c.orbit; }
        for (int i = held; i < want; i++) {
            AstraCube c = new AstraCube(ship.x, ship.y, 0f, 0f, null, 0f);
            c.armed = false;
            c.element = Astra.cubeElement(astraKind, i);
            c.orbit = orbit;
            c.spin = fxRng.nextFloat() * 6.2831853f; // so no two start in phase
            astra.add(c);
            held++;
        }
        int j = 0;
        for (AstraCube c : astra) if (!c.armed) c.slot = (float) (j++ * 2 * Math.PI / Math.max(1, held));
    }

    /** Re-pick the locks: living foes inside the view and range, those nearest the line of the nose first. */
    private void refreshLocks() {
        int cap = Math.min(astraLocks.length, Astra.locksAt(astraKind, astraCharge));
        astraLockCount = 0;
        float[] score = new float[astraLocks.length];
        for (Enemy e : enemies) {
            if (e.dead || e.dismissed || e.voice || e.ghost || e.bossHidden || e.submerged()) continue;
            float dx = e.x - ship.x, dy = e.y - ship.y;
            if (Math.abs(dx) > Astra.VIEW_HALF_W || Math.abs(dy) > Astra.VIEW_HALF_H) continue;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > Astra.LOCK_RANGE) continue;
            float off = Math.abs(wrapAngle((float) Math.atan2(dy, dx) - ship.facing));
            float sc = off * 420f + d;   // a foe off the nose costs like 420 px per radian
            int at = astraLockCount;
            while (at > 0 && score[at - 1] > sc) {
                if (at < cap) { astraLocks[at] = astraLocks[at - 1]; score[at] = score[at - 1]; }
                at--;
            }
            if (at < cap) { astraLocks[at] = e; score[at] = sc; }
            if (astraLockCount < cap) astraLockCount++;
        }
    }

    /** A volley a guest seat looses, resolved on the host: the same cubes, from that seat's hull, at the same foes,
     *  and the kills pay the pilot who fired them. (The guest paid its own shards and keeps its own boons.) */
    public void astraFor(int seat, int kind, float power, int[] ids) {
        if (kind < 0 || kind >= Astra.KINDS) return;
        Ship from = ship;
        if (seat != 0) {
            if (seat == partnerSeat && partner != null) {
                from = partner;
            } else {
                for (SeatHull gh : extras)
                    if (gh.seat == seat && gh.ship != null) { from = gh.ship; break; }
            }
        }
        float dm = Astra.damageMult(Math.max(0f, Math.min(1f, power)));
        if (kind == Astra.PASHUPATA) { startGaze(from, seat, dm); return; }   // a guest's third eye burns from its own hull
        if (kind == Astra.AGNI || kind == Astra.VAYU) {                       // and its fire or its storm from its own hull
            long seed = (long) (clock * 1000f) * 31L + seat;
            if (kind == Astra.AGNI) flow.startAgni(from, seat, dm, power, seed); else flow.startVayu(from, seat, dm, power, seed);
            return;
        }
        if (kind == Astra.AKASHA) { flow.startLightning(from, seat, dm, power, (long) (clock * 1000f) * 31L + seat); return; }
        if (kind == Astra.PRITHVI) { flow.startBoulders(from, seat, dm, power, (long) (clock * 1000f) * 31L + seat); return; }
        if (kind == Astra.BRAHMA) { flow.startLotus(from, seat, dm, power, (long) (clock * 1000f) * 31L + seat); return; }
        if (ids.length == 0) { // the guest loosed it with nothing locked: the same straight fan, from that hull
            int count = Astra.cubesAt(kind, Astra.WIND_MIN + Math.max(0f, Math.min(1f, power)) * (Astra.WIND_FULL - Astra.WIND_MIN));
            for (int i = 0; i < count; i++) {
                AstraCube c = new AstraCube(from.x, from.y, 0f, 0f, null, 0f);
                c.element = Astra.cubeElement(kind, i);
                c.dress(c.element, dm);
                c.arm(from.facing + (i - (count - 1) * 0.5f) * 0.18f, Astra.SPEED[c.element], null);
                c.owner = seat;
                astra.add(c);
            }
            return;
        }
        for (int i = 0; i < ids.length; i++) {
            Enemy t = enemyByNetId(ids[i]);
            if (t == null) continue;
            AstraCube c = new AstraCube(from.x, from.y, 0f, 0f, t, 0f);
            c.element = Astra.cubeElement(kind, i);
            c.dress(c.element, dm);
            c.arm(from.facing + (i - (ids.length - 1) * 0.5f) * 0.18f, Astra.SPEED[c.element], t);
            c.owner = seat; // the kill is theirs
            astra.add(c);
        }
    }

    /** The foe the host's ledger stamped with this id — the guest's astra names its targets in the host's own
     *  numbering, so the hub can loose the same volley. */
    public Enemy enemyByNetId(int id) {
        for (Enemy e : enemies) if (e.netId == id && !e.dead && !e.ghost) return e;
        return null;
    }

    /** What a cube does the instant it lands: splash the crowd round the foe, mend the pilot, or carry on to the next
     *  foe (the Gale chains). True when the cube is spent. */
    private boolean onAstraHit(AstraCube c) {
        Enemy t = c.target;
        float[] e = TATVA_RGB[c.element];
        float cr = e[0] + (1f - e[0]) * CREST_LIFT, cg = e[1] + (1f - e[1]) * CREST_LIFT, cb = e[2] + (1f - e[2]) * CREST_LIFT;
        addEffect(Effect.ring(c.x, c.y, 26, cr, cg, cb));
        addEffect(Effect.ring(c.x, c.y, 46, e[0], e[1], e[2]));
        for (int i = 0; i < 12; i++) { // a burst of its own element
            float a = (float) (i * Math.PI * 2 / 12 + fxRng.nextFloat() * 0.4f), v = 120f + fxRng.nextFloat() * 220f;
            addEffect(Effect.spark(c.x, c.y, (float) Math.cos(a) * v, (float) Math.sin(a) * v, 4f, 0.35f + fxRng.nextFloat() * 0.25f,
                    c.element == 4 ? 4f : 3f, i % 3 == 0 ? 1f : cr, i % 3 == 0 ? 1f : cg, i % 3 == 0 ? 1f : cb));
        }
        if (c.splash > 0 && t != null) {
            addEffect(Effect.ring(c.x, c.y, c.splash, e[0], e[1], e[2]));
            for (Enemy o : enemies) {
                if (o == t || o.dead || o.dismissed || o.voice || o.ghost) continue;
                float dx = o.x - t.x, dy = o.y - t.y;
                if (dx * dx + dy * dy <= c.splash * c.splash) o.hurt(c.damage * Astra.SPLASH_SHARE, this, c.owner);
            }
        }
        if (c.heal > 0 && c.owner == 0 && !clientSeat) ship.hp = Math.min(ship.maxHp(), ship.hp + c.heal);
        if (c.chain > 0 && t != null) {
            Enemy next = nextChainTarget(t, c.last);
            if (next != null) {
                c.chain--;
                c.last = t;
                c.target = next;
                c.life = Math.max(c.life, 1.5f);
                return false; // it flies on
            }
        }
        return true;
    }

    /** A cube with nothing to chase looks ahead for a foe: the nearest living one within 650 px that is not behind it. Null when the way is clear. */
    public Enemy astraSeek(float x, float y, float vx, float vy) {
        Enemy best = null;
        float bd = 650f * 650f, vl = (float) Math.sqrt(vx * vx + vy * vy);
        for (Enemy o : enemies) {
            if (o.dead || o.dismissed || o.voice || o.ghost || o.bossHidden || o.submerged()) continue;
            float dx = o.x - x, dy = o.y - y, d = dx * dx + dy * dy;
            if (d >= bd) continue;
            if (vl > 0.001f && (dx * vx + dy * vy) < 0f) continue;      // not behind the flight
            bd = d;
            best = o;
        }
        return best;
    }

    /** The nearest living foe to `from` within chain reach, other than the one just struck. */
    private Enemy nextChainTarget(Enemy from, Enemy skip) {
        Enemy best = null;
        float bd = 360f * 360f;
        for (Enemy o : enemies) {
            if (o == from || o == skip || o.dead || o.dismissed || o.voice || o.ghost) continue;
            float dx = o.x - from.x, dy = o.y - from.y, d = dx * dx + dy * dy;
            if (d < bd) { bd = d; best = o; }
        }
        return best;
    }

    /** The invocation builds, cubes in flight fly and retire. Rock is never consulted - that is the whole point of them. */
    public void updateAstra(float dt) {
        if (astraCd > 0) astraCd -= dt;
        if (astraInvoking) {
            if (ship.hp <= 0 || runEnded()) {
                astraCancel(); // a hull that is gone invokes nothing, and loses nothing
            } else {
                astraCharge = Math.min(astraCharge + dt, Astra.WIND_FULL + 3f);
                float k = windK();
                syncCage();
                for (AstraCube c : astra) if (!c.armed) c.hold(ship, dt, k, WIND_R_OUT, WIND_R_IN);
                mandalaGuard(k);
                refreshLocks();
                ship.trauma = Math.max(ship.trauma, 0.1f + 0.2f * k);
                if (k > 0.5f && astraKind != Astra.BRAHMA && fxRng.nextFloat() < 0.6f * k && !astra.isEmpty()) { // sparks off the cage come with the light, in the second half of the charge
                    AstraCube c = astra.get(fxRng.nextInt(astra.size()));
                    debris(c.x, c.y, 120, crestR(), crestG(), crestB());
                }
            }
        }
        astraPunch *= (float) Math.exp(-4.5f * dt);
        if (!astraInvoking && flow.holdsShards()) for (AstraCube c : astra) if (!c.armed) c.hold(ship, dt, 1f, WIND_R_OUT, WIND_R_IN);   // the storm's shards keep circling
        if (!astraInvoking && flow.kind == AstraFlow.BOULDER && flow.t < AstraFlow.T_ROCK + 0.3f) { astraKind = Astra.PRITHVI; mandalaGuard(1f); }   // and the mandala that throws the rocks is still a shield
        flow.step(this, dt);
        if (gazeT > 0f) {
            gazeT = Math.max(0f, gazeT - dt);
            gazeBurn(dt);
        }

        for (int i = astra.size() - 1; i >= 0; i--) {
            AstraCube c = astra.get(i);
            if (!c.armed) continue; // held cubes ride the cage, they do not hunt yet
            boolean hit = c.think(this, dt, c.turn);
            if (hit && !onAstraHit(c)) hit = false; // a chaining cube lives on
            if (hit || c.life <= 0 || c.x < -200 || c.x > width + 200
                    || c.y < -200 || c.y > height + 200) {
                astra.remove(i);
            }
        }
    }

    /** Random point in the built regions that is not inside a wall. Falls back to Bhu Loka. */
    public float[] randomOpenSpot(Random rng) {
        for (int i = 0; i < 8; i++) {
            float x = 100 + rng.nextFloat() * (width - 200);
            float y = 100 + rng.nextFloat() * (height - 200);
            if (!blockedPx(x - 16, y - 16) && !blockedPx(x + 16, y - 16)
                    && !blockedPx(x - 16, y + 16) && !blockedPx(x + 16, y + 16)) {
                return new float[] { x, y };
            }
        }
        return new float[] { width / 2, 800 };
    }

    public boolean blockedPx(float x, float y) {
        if (Boundary.hit(boulders, x, y, 0f, 1600f)) return true;
        for (TileMap m : maps) if (m.solidPx(x, y)) return true;
        return false;
    }

    /** Pushes the rect out of every map and every shop wall. Returns corrected x,y.
     *  Writes into the instance scratch buffer; callers read it immediately. */
    private final float[] collideBuf = new float[2];

    public float[] collide(float x, float y, float w, float h) {
        for (TileMap m : maps) {
            float[] c = m.pushOut(x, y, w, h);
            x = c[0];
            y = c[1];
        }
        collideBuf[0] = x;
        collideBuf[1] = y;
        if (Math.abs(y + h / 2 - 1600f) < Boundary.DEPTH + 160f) pushOutBoulders(collideBuf, w, h);
        for (float[] shop : shops) // a shop's board and legs are solid: no flying through them
            for (float[] wall : DOCK_WALLS)
                pushOutRect(collideBuf, w, h, shop[0] + wall[0], shop[1] + wall[1], wall[2], wall[3]);
        return collideBuf;
    }

    /** A rect (corner xy, size w x h) pushed out of any boulder it overlaps: along the line from the rock's centre to the
     *  nearest point of the rect, by the overlap. */
    private void pushOutBoulders(float[] xy, float w, float h) {
        for (int pass = 0; pass < 2; pass++)
            for (float[] b : boulders) {
                float r = b[2] * 0.85f;
                float nx = Math.max(xy[0], Math.min(b[0], xy[0] + w)), ny = Math.max(xy[1], Math.min(b[1], xy[1] + h));
                float dx = nx - b[0], dy = ny - b[1], d2 = dx * dx + dy * dy;
                if (d2 >= r * r) continue;
                float d = (float) Math.sqrt(d2);
                if (d < 1e-3f) { dx = 0f; dy = 1f; d = 0f; }
                else { dx /= d; dy /= d; }
                float push = r - d;
                xy[0] += dx * push;
                xy[1] += dy * push;
            }
    }

    // Solid parts of a shop: x, y, w, h as world offsets from the sign centre. The posts' inner
    // faces ARE the square's sides; the floor stays open between the feet — you rise up into it.
    private static final float[][] DOCK_WALLS = {
            { -56, -16, 112, 72 }, // the sign board
            { -52, -56, 16, 40 },  // left leg
            { 40, -56, 16, 40 },   // right leg
            { -26, -52, 10, 32 },  // left dock post (art cols 10-11)
            { 20, -52, 8, 32 },    // right dock post (art cols 21-22)
    };

    /** AABB push-out along the smallest penetration — what TileMap.pushOut does for tiles.
     *  xy is the rect's bottom-left corner in, corrected corner out; no allocation. */
    private static void pushOutRect(float[] xy, float w, float h, float rx, float ry, float rw, float rh) {
        float l = xy[0] + w - rx, r = rx + rw - xy[0], b = xy[1] + h - ry, t = ry + rh - xy[1];
        if (l <= 0 || r <= 0 || b <= 0 || t <= 0) return; // no overlap
        float m = Math.min(Math.min(l, r), Math.min(b, t));
        if (m == l) xy[0] -= l;
        else if (m == r) xy[0] += r;
        else if (m == b) xy[1] -= b;
        else xy[1] += t;
    }

    /** Cached: this is called per entity per frame, it must not allocate. */
    public TileMap[] tileMaps() {
        return maps;
    }

    TileMap[] maps;

    public void spawnBullet(float x, float y, float angle, WeaponDef def, float power, float dmgMult) {
        if (bullets.size() >= 400) return;
        Bullet b = bulletPool.obtain();
        b.init(x, y, angle, def, power, dmgMult, false);
        bullets.add(b);
        addEffect(Effect.flash(x, y, x + b.vx * 0.02f, y + b.vy * 0.02f, b.r, b.g, b.b)); // muzzle flash
        if (shotWire != null) shotWire.shot(shotOf(b)); // my round, announced so both skies match
    }

    /** Enemy fire. Held under 300 so the player always keeps slots for their own gun. */
    public void spawnEnemyBullet(float x, float y, float angle) {
        spawnEnemyBullet(x, y, angle, Bullet.HOSTILE, 1f);
    }

    /** Ungraded hostile fire (Bala runs his own rows at the loka's scale). */
    public void spawnEnemyBullet(float x, float y, float angle, WeaponDef def) {
        spawnEnemyBullet(x, y, angle, def, 1f);
    }

    /** Same, with the boss's own shot rows (speed/damage/colour differ per attack).
     *  dmgScale is the loka's fire multiplier — a deeper band bites harder. */
    public void spawnEnemyBullet(float x, float y, float angle, WeaponDef def, float dmgScale) {
        if (bullets.size() >= 300) return;
        Bullet b = bulletPool.obtain();
        b.init(x, y, angle, def, 1f, dmgScale, true);
        bullets.add(b);
        addEffect(Effect.flash(x, y, x + b.vx * 0.04f, y + b.vy * 0.04f, b.r, b.g, b.b)); // muzzle flash
        if (shotWire != null) shotWire.shot(shotOf(b)); // hostile fire streams to the guest too
    }

    /** The wire's read of a round: pose, bite, tint — no def key, the fallback draws it. */
    private Shot shotOf(Bullet b) {
        Shot s = new Shot();
        s.x = b.x;
        s.y = b.y;
        s.vx = b.vx;
        s.vy = b.vy;
        s.damage = b.damage;
        s.radius = b.radius;
        s.r = b.r;
        s.g = b.g;
        s.b = b.b;
        s.hostile = b.hostile;
        return s;
    }

    /** A pooled bullet for the wire's inbound rounds (the pool's house rule: construct via World). */
    public Bullet obtainBullet() {
        return bulletPool.obtain();
    }

    // THE RULER'S GAZE. The ruler of a loka holds a piece of the bolt, and the piece tells him the
    // loka is his. He feels his people fall through it: a few and he does not mind, more and the
    // piece warms, enough and he looks up, all of the quota and he walks into the last band and
    // waits. The player reads it on the HUD bar and in three short scenes.
    public static final float GAZE_NOTICED = 0.25f, GAZE_WATCHING = 0.6f;
    public int gazeStage; // 0 unaware, 1 noticed, 2 watching, 3 awake and waiting

    /** 0..1: how much of the ruler's quota of fallen the pilot has already paid. */
    public float gaze() {
        if (boss != null) return 1f;
        int need = 0, have = 0;
        for (int i = 0; i < quota.length; i++) {
            need += quota[i];
            have += Math.min(kills[i], quota[i]);
        }
        return need == 0 ? 0f : have / (float) need;
    }

    private void gazeCheck() {
        float g = gaze();
        int stage = boss != null ? 3 : g >= GAZE_WATCHING ? 2 : g >= GAZE_NOTICED ? 1 : 0;
        if (stage <= gazeStage) return;
        gazeStage = stage; // a burst of kills that jumps a stage plays only the stage it landed on
        cue(stage == 1 ? Cue.GAZE_NOTICED : stage == 2 ? Cue.GAZE_WATCHING : Cue.GAZE_ROUSED); // each ruler's own words: Scenes reads this.stage
    }

    /** The seal: every kind's kill quota filled — only then does Bala notice the ship. */
    public boolean quotaMet() {
        for (int i = 0; i < quota.length; i++) if (kills[i] < quota[i]) return false;
        return true;
    }

    /** The seal lifts: Bala appears in his arena the moment the run's kill quota is full.
     *  Nothing on screen counts down — the loka simply changes. */
    public void trySpawnBoss() {
        enemyDirector.trySpawnBoss();
    }

    public void countKill(Enemy e) {
        kills[e.kind.ordinal()]++;
        if (shooter != 0) { // a partner's kill: the seals stay shared, the pay flies home
            if (painWire != null) {
                Pain p = new Pain();
                p.target = shooter;
                p.kind = PAIN_KILL;
                p.a = e.kind.ordinal() | (e.boss ? 0x100 : 0) | (e.elite ? 0x200 : 0)
                        | (e.rank == 1 ? 0x400 : 0);
                p.b = e.x;
                p.c = e.y;
                painWire.pain(p);
            }
            return;
        }
        long before = score.points;
        int mult = score.mult();
        score.kill(this, e);
        long paid = score.points - before; // kill value + any leader/wipe flat, multiplier in
        if (paid > 0)
            popups.push(new Popups.Spec().text("+" + paid).at(e.x, e.y + 10)
                    .color(1f, 215 / 255f, 0).scale(e.boss || e.elite ? 1.4f : 1f));
        if (score.mult() > mult)
            popups.push(new Popups.Spec().text(tierWord(score.mult())).img(tierArt(score.mult()))
                    .at(ship.x, ship.y + 28).color(1f, 215 / 255f, 0).scale(1.5f).life(1.6f));
    }

    /** The scripture pair for a multiplier tier: the pre-rendered Devanagari word and its
     *  English line. Package-private: the headless test pins the mapping. */
    static String tierArt(int mult) {
        switch (mult) {
            case 2: return "dvitiya";
            case 3: return "tritiya";
            case 4: return "chaturth";
            default: return "pancham"; // the ladder caps at five
        }
    }

    static String tierWord(int mult) {
        switch (mult) {
            case 2: return "TWOFOLD";
            case 3: return "THREEFOLD";
            case 4: return "FOURFOLD";
            default: return "FIVEFOLD";
        }
    }

    /** The call: the whole arena retinue leaves its post and forms the commander's ring. */
    public void orderRetinue(Enemy commander) {
        enemyDirector.orderRetinue(commander);
    }

    /** Bala falls: the retinue stands down, the ring effect goes up, the purse changes hands,
     *  and the gold gate across the north corridor opens to the next loka. */
    public void victory(Enemy b) {
        bossDead = true;
        boolean last = stage >= lastStage();
        boolean bala = stage == 0;
        if (last) { // the first-loka world ends with its ruler, after the banner lap; the campaign goes on, up into Sutala's coming-soon hall
            if (!campaign) won = true;
            victoryT = 0;
        }
        cue(bala ? Cue.BALA_FALLS : Cue.BHAIRAVA_FALLS);
        Codex.unlock(this, bala ? Codex.BALA : Codex.BHAIRAVA);
        score.add(Score.BOSS);
        Popups.Spec slain = new Popups.Spec().text(LokaTable.at(lokaIndex).boss.toUpperCase() + " SLAIN");
        if (bala) slain.img("bala");
        popups.push(slain.at(b.x, b.y + 40).color(1f, 215 / 255f, 0).scale(1.8f).life(2.2f));
        for (Enemy e : enemies)
            if (!e.dead && e.commander == b) {
                e.dismissed = true;
                e.commander = null;
            }
        hangar.coins += Balance.purse(stage);                                  // the ruler's purse: its share of the loka's budget
        hazards.ring = false;                                                  // the ash ring goes out with its herald
        if (bala) Chatter.wardenFalls(this); // the voices pay what a fall promised: Maya's letter
        for (int c = sealC0; c <= sealC1; c++) sealMap.set(c, sealRow, TileMap.EMPTY);
        wallVersion++;                                                          // the baked wall chunks must be redone
        addEffect(Effect.ring(axisX, sealMap.oy + (sealRow + 1) * TileMap.TILE, 260, 1f, 0.84f, 0.3f));
        addEffect(Effect.ring(b.x, b.y, 500, 1f, 0.84f, 0.3f));
        addEffect(Effect.ring(b.x, b.y, 260, 1f, 1f, 1f));
        ship.trauma = Math.min(1f, ship.trauma + 0.5f);
        if (!last) beginStage(stage + 1);                                      // Bala's door is open: Vitala's ruler is next
    }

    /** Mid-berserk deployment: a gold elite pair at his old post, straight into his ring (cap 6). */
    public void summonElites(float x, float y, Enemy commander) {
        enemyDirector.summonElites(x, y, commander);
    }

    /** A random open berth in the arena for the boss's blink: 350-850 px from the ship. */
    public float[] bossBlinkSpot() {
        return enemyDirector.bossBlinkSpot();
    }

    /** Beam: instant line from (x,y) along angle, stops at the first wall or at range, and scalds what it crosses. */
    public void laser(float x, float y, float angle, float range, float r, float g, float b, float damage) {
        float dx = (float) Math.cos(angle), dy = (float) Math.sin(angle);
        float d = wallDist(x, y, dx, dy, range);
        float ex = x + dx * d, ey = y + dy * d;
        for (Enemy e : enemies) {
            if (!e.dead && segDist(e.x, e.y, x, y, ex, ey) <= e.w / 2 + 6) e.hurt(damage, this);
        }
        addEffect(Effect.flash(x, y, ex, ey, r, g, b));
        beamEcho(x, y, angle, damage, r, g, b); // co-op: a flyer stands in for the line elsewhere
    }

    /** A beam's wire echo: a fast flyer is the beam on every OTHER screen — the real hit
     *  stays the firing seat's word, so the flyer is visuals, never a second verdict.
     *  No local round is created: the firing machine announces, nobody resolves twice. */
    private void beamEcho(float x, float y, float angle, float damage, float r, float g, float b) {
        if (shotWire == null) return;
        Shot s = new Shot();
        s.x = x;
        s.y = y;
        s.vx = (float) Math.cos(angle) * 3000f;
        s.vy = (float) Math.sin(angle) * 3000f;
        s.damage = damage;
        s.radius = 7;
        s.r = r;
        s.g = g;
        s.b = b;
        s.hostile = false;
        shotWire.shot(s);
    }

    /** Distance along a unit ray until rock, sampled every 16px; range when nothing is in the way. */
    public float wallDist(float x, float y, float ux, float uy, float range) {
        for (float d = 16; d <= range; d += 16) {
            if (blockedPx(x + ux * d, y + uy * d)) return d;
        }
        return range;
    }

    /** True when no rock sits between two points. Enemies ask before they open fire. */
    public boolean clearLine(float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return true;
        return wallDist(ax, ay, dx / len, dy / len, len) >= len;
    }

    private static float segDist(float px, float py, float ax, float ay, float bx, float by) {
        float vx = bx - ax, vy = by - ay;
        float len2 = vx * vx + vy * vy;
        float t = len2 < 0.01f ? 0 : ((px - ax) * vx + (py - ay) * vy) / len2;
        t = Math.max(0, Math.min(1, t));
        float cx = ax + vx * t - px, cy = ay + vy * t - py;
        return (float) Math.sqrt(cx * cx + cy * cy);
    }

    /** Snaps aim to the nearest enemy inside the cone. Returns aim unchanged when cone <= 0. */
    public float assistAim(float aim, float coneDeg) {
        if (coneDeg <= 0) return aim;
        float best = aim, bestDiff = (float) Math.toRadians(coneDeg);
        for (Enemy e : enemies) {
            if (e.dead) continue;
            float dx = e.x - ship.x, dy = e.y - ship.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 900) continue;
            float to = (float) Math.atan2(dy, dx);
            float diff = Math.abs(wrapAngle(to - aim));
            if (diff < bestDiff) {
                bestDiff = diff;
                best = to;
            }
        }
        return best;
    }

    /** Shortest signed delta between two angles, in (-pi, pi]. */
    public static float wrapAngle(float a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    public void addEffect(Effect e) {
        effects.add(e);
    }

    /** One debris spark flying outward from (x, y). */
    public void debris(float x, float y, float speed, float r, float g, float b) {
        float a = fxRng.nextFloat() * 6.2831853f;
        float sp = speed * (0.4f + fxRng.nextFloat());
        addEffect(Effect.spark(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                2.5f, 0.5f, 3, r, g, b));
    }

    /** Three sparks thrown along a shot's path from where it landed. */
    public void spray(float x, float y, float ux, float uy, float r, float g, float b) {
        float ang = (float) Math.atan2(uy, ux);
        for (int k = -1; k <= 1; k++) {
            float a = ang + k * 0.45f;
            float sp = 70 + (k + 1) * 35;
            addEffect(Effect.spark(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp,
                    3, 0.35f, 3, r, g, b));
        }
    }

    public void selectShip(int i) {
        if (i < 0 || i >= ShipDef.ALL.length || !hangar.ownsShip(i)) return;
        hangar.shipIndex = i;
        ship.setDef(ShipDef.ALL[i]);
    }

    public void selectWeapon(int i) {
        if (i < 0 || i >= WeaponDef.ALL.length) return;
        hangar.weaponIndex = i;
        ship.weapon.def = hangar.weapon(i); // at the rank the pilot has tempered it to
    }

    public int currentRegionIndex() {
        float cy = ship.y;
        for (int i = 0; i < regionNames.length; i++) {
            if (cy >= regionStart[i] && cy < regionEnd[i]) return i;
        }
        return regionNames.length - 1;
    }

    public String currentRegion() {
        return regionNames[currentRegionIndex()];
    }

    /** Which loka the ship is in: Bhu before the gate, Atala beyond it. */
    public String lokaName() {
        return currentRegionIndex() == 0 ? "Bhu" : "Atala";
    }

}