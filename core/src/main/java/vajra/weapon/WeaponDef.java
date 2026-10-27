package vajra.weapon;

/** Firing-pattern data. Every weapon is a row here — patterns are data, not classes.
 *  "Reload" is the cooldown stat; magazine + reload is a later mechanic if wanted. */
public class WeaponDef {
    public String name;
    public int count = 1;        // projectiles per shot
    public int burst = 1;        // shots per trigger pull
    public float spreadDeg;      // total spread across count
    public float burstGap;       // s between burst shots
    public float cooldown = 0.3f;
    public float speed = 700;
    public float damage = 10;
    public float bulletSize = 5;
    public float life = 2.5f;
    public float chargeTime;     // 0 = instant fire
    public float range;          // beam range
    public float aoeRadius;
    public float homingTurn;     // deg/s
    public float aimAssistDeg;
    public boolean beam;
    public boolean aimAssist;
    public boolean pierceEnemies;
    public boolean pierceWalls;
    public float r = 1f, g = 0.9f, b = 0.3f;
    public int cost; // coins to unlock in the hangar; 0 = comes with the hull
    public int idx;  // its row in ALL, stamped below: the hangar labels rows by index
    public int minLoka; // the deepest loka a pilot must have reached before this pattern is offered at all
    public String ability = ""; // what rank III adds, named for the hangar
    public WeaponDef art = this; // the row this one was tempered from: the sprite is keyed by it

    // --- THE LOKA PATTERNS (indices 15..35): simple, single-minded behaviours; 0 / false = the plain bolt ---
    public boolean rear;         // a second, weaker copy flies out of the tail
    public float rearDamage = 0.6f;
    public boolean boomerang;    // flies out for half its life, then comes home
    public float waveAmp, waveFreq; // sideways sine (px, rad/s)
    public float burnDps, burnTime; // sets the foe alight / poisons it
    public float slow, slowTime; // fraction of speed taken, for this long
    public float stun;           // seconds frozen on hit
    public float knock;          // shove on hit (px/s)
    public float leech;          // hull mended per hit
    public int bounces;          // rock ricochets
    public int chain;            // jumps to other foes
    public float chainRange = 260f;
    public float drag;           // px/s^2 shed: a mine settles where it stops
    public float prox;           // goes off when a foe comes this close (after a short arming)
    public float zoneRadius, zoneTime, zoneDps; // what it leaves behind where it bursts
    public float pull, pullRadius; // drags foes in while it flies
    public float grow;           // radius gained per second
    public float rehit;          // seconds before a piercing round may hurt the same foe again
    public float executeBelow, executeMult = 3f; // damage multiplied on foes under this share of hull
    public int splitCount;       // shards it breaks into
    public float splitAt = -1f;  // seconds in; -1 = when it ends or hits
    public boolean splitOnHit;
    public float splitSpread = 360f, splitSpeed = 320f, splitLife = 0.6f, splitDamage = 5f;

    private WeaponDef() {}

    // --- RANKS: a pattern is bought once and then tempered. Rank I is the pattern as drawn; rank II
    // hits 25% harder; rank III hits 50% harder AND gains one ability - a trait the pattern did not
    // have, taken from the same flags every weapon already uses. Enemy hulls climb to 5.5x over the
    // seven lokas; a pilot's damage climbs the same road through three roads: a better pattern, a
    // higher rank, a better hull tier (docs/ECONOMY.md).
    public static final int RANKS = 3;
    public static final float[] RANK_DAMAGE = {1f, 1.25f, 1.5f};

    /** This pattern at a rank (0 = I, 1 = II, 2 = III): a copy with the rank's damage and, at III, its ability.
     *  Rank I returns the pattern itself. */
    public WeaponDef ranked(int rank) {
        if (rank <= 0) return this;
        WeaponDef d = copy();
        d.damage = damage * RANK_DAMAGE[Math.min(rank, RANKS - 1)];
        if (rank >= RANKS - 1) d.applyAbility();
        return d;
    }

    private WeaponDef copy() {
        WeaponDef d = new WeaponDef();
        d.name = name; d.count = count; d.burst = burst; d.spreadDeg = spreadDeg; d.burstGap = burstGap;
        d.cooldown = cooldown; d.speed = speed; d.damage = damage; d.bulletSize = bulletSize; d.life = life;
        d.chargeTime = chargeTime; d.range = range; d.aoeRadius = aoeRadius; d.homingTurn = homingTurn;
        d.aimAssistDeg = aimAssistDeg; d.beam = beam; d.aimAssist = aimAssist; d.pierceEnemies = pierceEnemies;
        d.pierceWalls = pierceWalls; d.r = r; d.g = g; d.b = b; d.cost = cost; d.idx = idx;
        d.minLoka = minLoka; d.ability = ability; d.art = art;
        d.rear = rear; d.rearDamage = rearDamage; d.boomerang = boomerang; d.waveAmp = waveAmp; d.waveFreq = waveFreq;
        d.burnDps = burnDps; d.burnTime = burnTime; d.slow = slow; d.slowTime = slowTime; d.stun = stun; d.knock = knock;
        d.leech = leech; d.bounces = bounces; d.chain = chain; d.chainRange = chainRange; d.drag = drag; d.prox = prox;
        d.zoneRadius = zoneRadius; d.zoneTime = zoneTime; d.zoneDps = zoneDps; d.pull = pull; d.pullRadius = pullRadius;
        d.grow = grow; d.rehit = rehit; d.executeBelow = executeBelow; d.executeMult = executeMult;
        d.splitCount = splitCount; d.splitAt = splitAt; d.splitOnHit = splitOnHit; d.splitSpread = splitSpread;
        d.splitSpeed = splitSpeed; d.splitLife = splitLife; d.splitDamage = splitDamage;
        return d;
    }

    public WeaponDef shard; // the pieces a splitting round breaks into (made on first use)

    /** What the pieces of this round are: small, quick, short-lived, wearing the same sprite, breaking no further. */
    WeaponDef shardOf() {
        WeaponDef d = copy();
        d.count = 1; d.burst = 1; d.spreadDeg = 0f;
        d.speed = splitSpeed; d.life = splitLife; d.damage = splitDamage;
        d.bulletSize = Math.max(3f, bulletSize * 0.6f);
        d.splitCount = 0; d.splitOnHit = false; d.splitAt = -1f;
        d.rear = false; d.chain = 0; d.zoneRadius = 0f; d.aoeRadius = 0f; d.grow = 0f; d.boomerang = false; d.drag = 0f; d.prox = 0f;
        d.pierceEnemies = pierceEnemies || splitOnHit;
        d.rehit = 0.25f;
        return d;
    }

    /** Rank III: the one thing this pattern learns. */
    private void applyAbility() {
        switch (idx) {
            case 0: aimAssist = true; aimAssistDeg = 12f; break;            // Single Bolt: a true bolt
            case 1: pierceEnemies = true; break;                              // Double Bolt: a lancing pair
            case 2: homingTurn = 70f; break;                                  // Burst: a chasing burst
            case 3: aimAssist = true; aimAssistDeg = 14f; break;              // Twin Burst: reads the gap
            case 4: pierceEnemies = true; break;                              // Buckshot: shreds a crowd
            case 5: aoeRadius = 55f; break;                                   // Heavy Shell: concussive
            case 6: homingTurn = 90f; break;                                  // Light MG: a seeking stream
            case 7: pierceEnemies = true; break;                              // Charge Shot: pierces what it charged for
            case 8: range += 300f; break;                                     // Laser: a long beam
            case 9: aoeRadius += 45f; break;                                  // Bomb: a wider burst
            case 10: homingTurn += 120f; break;                               // Missile: a hunter
            case 11: count += 1; break;                                       // Tracking Missiles: a third head
            case 12: homingTurn = 120f; break;                                // Aim Shot: bends to its mark
            case 13: pierceWalls = true; break;                               // Piercer: through the wall too
            case 14: pierceEnemies = true; break;                             // Wallbore: through everything
            case 15: rearDamage = 1f; break;                                  // Mirror Shot: the reflection bites as hard
            case 16: speed *= 1.3f; life *= 0.85f; break;                     // Wind Scythe: a swift return
            case 17: count += 1; break;                                       // Maya Weave: a third strand
            case 18: burnDps *= 1.7f; burnTime += 1f; break;                  // Ember Dart: a long burn
            case 19: count += 2; spreadDeg += 12f; break;                     // Ash Spray: a wider sweep
            case 20: splitCount += 4; break;                                  // Cinder Bomb: more embers
            case 21: slow = Math.min(0.8f, slow + 0.3f); slowTime += 1.5f; break; // Tide Bolt: deep current
            case 22: count += 6; spreadDeg = 340f; break;                     // Ripple Ring: a full ring
            case 23: leech *= 1.8f; break;                                    // Boon Shot: overflow
            case 24: bounces += 4; break;                                     // Ricochet: many bounces
            case 25: chain += 2; break;                                       // Arc Chain: two more jumps
            case 26: aoeRadius += 50f; break;                                 // Sigil Mine: a wider sigil
            case 27: homingTurn = 200f; break;                                // Serpent Coil: sharper hunting
            case 28: burnDps *= 1.8f; break;                                  // Venom Fang: deep venom
            case 29: zoneRadius += 45f; zoneTime += 1.5f; break;              // Naga Cloud: a greater cloud
            case 30: pullRadius += 100f; pull *= 1.3f; break;                 // Gravity Well: a deeper well
            case 31: stun += 0.6f; break;                                     // Quake Shell: a long stun
            case 32: splitCount += 3; break;                                  // Shatter Shard: more shards
            case 33: splitCount += 2; splitSpread += 20f; break;              // Jewel Prism: a fuller fan
            case 34: executeBelow = 0.55f; break;                             // Executioner: reaches higher
            case 35: grow += 25f; break;                                      // Last Light: swells faster
            default: break;
        }
    }

    /** Coins to buy the NEXT rank of this pattern (0 = already at the top). Rank II 60% of the price,
     *  rank III 130% - never under 60 / 150. */
    public int rankCost(int currentRank) {
        if (currentRank >= RANKS - 1) return 0;
        int base = Math.round(cost * (currentRank == 0 ? 0.6f : 1.3f) / 10f) * 10;
        return Math.max(currentRank == 0 ? 60 : 150, base);
    }

    /** Shards rank III also takes: a tempered pattern is a forged one. */
    public int rankShards(int currentRank) {
        return currentRank == RANKS - 2 ? 2 : 0;
    }

    public static WeaponDef mk(String name) {
        WeaponDef d = new WeaponDef();
        d.name = name;
        return d;
    }

    public WeaponDef rear() { rear = true; return this; }
    public WeaponDef boomerang() { boomerang = true; return this; }
    public WeaponDef wave(float amp, float freq) { waveAmp = amp; waveFreq = freq; return this; }
    public WeaponDef burn(float dps, float time) { burnDps = dps; burnTime = time; return this; }
    public WeaponDef slow(float k, float time) { slow = k; slowTime = time; return this; }
    public WeaponDef stun(float t) { stun = t; return this; }
    public WeaponDef knock(float v) { knock = v; return this; }
    public WeaponDef leech(float v) { leech = v; return this; }
    public WeaponDef bounces(int n) { bounces = n; return this; }
    public WeaponDef chain(int n, float range) { chain = n; chainRange = range; return this; }
    public WeaponDef drag(float v) { drag = v; return this; }
    public WeaponDef prox(float v) { prox = v; return this; }
    public WeaponDef zone(float radius, float time, float dps) { zoneRadius = radius; zoneTime = time; zoneDps = dps; return this; }
    public WeaponDef pull(float radius, float force) { pullRadius = radius; pull = force; return this; }
    public WeaponDef grow(float v) { grow = v; return this; }
    public WeaponDef rehit(float v) { rehit = v; return this; }
    public WeaponDef execute(float below, float mult) { executeBelow = below; executeMult = mult; return this; }
    public WeaponDef split(int n, float at, float spread, float speed, float life, float dmg) { splitCount = n; splitAt = at; splitSpread = spread; splitSpeed = speed; splitLife = life; splitDamage = dmg; return this; }
    public WeaponDef splitOnHit() { splitOnHit = true; return this; }

    public WeaponDef count(int v) { count = v; return this; }
    public WeaponDef burst(int v) { burst = v; return this; }
    public WeaponDef spreadDeg(float v) { spreadDeg = v; return this; }
    public WeaponDef burstGap(float v) { burstGap = v; return this; }
    public WeaponDef cooldown(float v) { cooldown = v; return this; }
    public WeaponDef speed(float v) { speed = v; return this; }
    public WeaponDef damage(float v) { damage = v; return this; }
    public WeaponDef bulletSize(float v) { bulletSize = v; return this; }
    public WeaponDef life(float v) { life = v; return this; }
    public WeaponDef chargeTime(float v) { chargeTime = v; return this; }
    public WeaponDef range(float v) { range = v; return this; }
    public WeaponDef aoeRadius(float v) { aoeRadius = v; return this; }
    public WeaponDef homingTurn(float v) { homingTurn = v; return this; }
    public WeaponDef aimAssistDeg(float v) { aimAssistDeg = v; return this; }
    public WeaponDef beam() { beam = true; return this; }
    public WeaponDef aimAssist() { aimAssist = true; return this; }
    public WeaponDef pierceEnemies() { pierceEnemies = true; return this; }
    public WeaponDef pierceWalls() { pierceWalls = true; return this; }
    public WeaponDef cost(int v) { cost = v; return this; }
    public WeaponDef colour(float r, float g, float b) { this.r = r; this.g = g; this.b = b; return this; }

    // Price ladder: DPS rises with cost. Every weapon above 200c must beat the
    // cheaper options in raw output — no weapon is a raw downgrade. Atala is the
    // easy loka; these numbers are the baseline that LokaTable.dmgScale grades
    // up for deeper lokas.
    public static final WeaponDef SINGLE = mk("Single Bolt"); // free — the starter
    public static final WeaponDef DOUBLE = mk("Double Bolt").count(2).spreadDeg(10f).cooldown(0.3f).damage(10).cost(90);
    public static final WeaponDef BURST1 = mk("Burst").burst(3).burstGap(0.09f).cooldown(0.5f).speed(760).damage(10).bulletSize(4).cost(120);
    public static final WeaponDef BURST2 = mk("Twin Burst").count(2).spreadDeg(8f).burst(2).burstGap(0.1f).cooldown(0.55f).speed(740).damage(10).bulletSize(4).cost(200);
    public static final WeaponDef SPREAD = mk("Buckshot").count(5).spreadDeg(45f).cooldown(0.5f).speed(660).damage(8).bulletSize(4).cost(110).colour(1f, 0.85f, 0.4f);
    public static final WeaponDef HEAVY = mk("Heavy Shell").cooldown(0.5f).speed(520).damage(36).bulletSize(10).life(3f).cost(240).colour(1f, 0.45f, 0.15f);
    public static final WeaponDef LIGHT = mk("Light MG").cooldown(0.07f).speed(820).damage(4).bulletSize(3).cost(80).colour(0.4f, 0.9f, 1f);
    public static final WeaponDef CHARGE = mk("Charge Shot").cooldown(0.18f).speed(900).damage(16).bulletSize(7).chargeTime(0.8f).cost(320).colour(0.9f, 0.7f, 1f);
    public static final WeaponDef LASER = mk("Laser").beam().cooldown(0.6f).damage(42).range(900f).cost(460).colour(0.6f, 1f, 0.8f);
    public static final WeaponDef BOMB = mk("Bomb").cooldown(0.7f).speed(330).damage(34).bulletSize(12).aoeRadius(90f).life(3f).cost(380).colour(1f, 0.5f, 0.2f);
    public static final WeaponDef MISSILE = mk("Missile").cooldown(0.45f).speed(420).damage(24).bulletSize(6).homingTurn(160f).life(4f).cost(340).colour(1f, 0.3f, 0.3f);
    public static final WeaponDef TRACKING = mk("Tracking Missiles").count(2).spreadDeg(14f).cooldown(0.6f).speed(440).damage(18).bulletSize(6).homingTurn(380f).life(4f).cost(560).colour(0.8f, 0.4f, 1f);
    public static final WeaponDef AIM = mk("Aim Shot").cooldown(0.18f).speed(840).damage(14).aimAssist().aimAssistDeg(20f).cost(160).colour(1f, 1f, 1f);
    public static final WeaponDef PIERCE = mk("Piercer").cooldown(0.28f).speed(780).damage(18).pierceEnemies().cost(170).colour(0.5f, 1f, 0.4f);
    public static final WeaponDef THROUGH = mk("Wallbore").cooldown(0.35f).speed(720).damage(14).pierceWalls().cost(260).colour(0.5f, 0.7f, 1f);

    // --- THE SEVEN LOKAS PATTERNS: three each, every one a single clear idea. (Cost climbs with the loka.)
    // ATALA - the lands of illusion and wind
    public static final WeaponDef MIRROR = mk("Mirror Shot").rear().cooldown(0.3f).speed(740).damage(9).cost(150).colour(0.85f, 0.9f, 1f);
    public static final WeaponDef SCYTHE = mk("Wind Scythe").boomerang().pierceEnemies().rehit(0.3f).cooldown(0.55f).speed(620).damage(16).bulletSize(9).life(2.2f).cost(220).colour(0.6f, 1f, 0.9f);
    public static final WeaponDef WEAVE = mk("Maya Weave").count(2).wave(60f, 9f).cooldown(0.22f).speed(640).damage(7).bulletSize(4).life(2f).cost(280).colour(0.9f, 0.6f, 1f);
    // VITALA - gold, ash and fire
    public static final WeaponDef EMBER = mk("Ember Dart").burn(7f, 3f).cooldown(0.2f).speed(800).damage(5).bulletSize(4).cost(250).colour(1f, 0.55f, 0.2f);
    public static final WeaponDef ASH = mk("Ash Spray").count(4).spreadDeg(26f).cooldown(0.14f).speed(520).damage(3).bulletSize(5).life(0.38f).pierceEnemies().cost(330).colour(0.75f, 0.7f, 0.65f);
    public static final WeaponDef CINDER = mk("Cinder Bomb").split(8, -1f, 360f, 300f, 0.6f, 7f).splitOnHit().cooldown(0.6f).speed(380).damage(8).bulletSize(9).life(0.75f).cost(400).colour(1f, 0.4f, 0.15f);
    // SUTALA - water and the gift
    public static final WeaponDef TIDE = mk("Tide Bolt").slow(0.45f, 2f).cooldown(0.26f).speed(700).damage(11).bulletSize(6).cost(300).colour(0.3f, 0.7f, 1f);
    public static final WeaponDef RIPPLE = mk("Ripple Ring").count(12).spreadDeg(330f).cooldown(0.7f).speed(360).damage(6).bulletSize(5).life(0.9f).cost(360).colour(0.5f, 0.85f, 1f);
    public static final WeaponDef BOON = mk("Boon Shot").leech(2f).cooldown(0.16f).speed(760).damage(8).bulletSize(4).cost(420).colour(1f, 0.85f, 0.4f);
    // TALATALA - the sorcerer's craft
    public static final WeaponDef RICOCHET = mk("Ricochet").bounces(3).cooldown(0.3f).speed(700).damage(12).bulletSize(5).life(3f).cost(380).colour(0.6f, 1f, 0.5f);
    public static final WeaponDef ARC = mk("Arc Chain").chain(3, 260f).cooldown(0.35f).speed(900).damage(9).bulletSize(5).cost(460).colour(0.7f, 0.8f, 1f);
    public static final WeaponDef MINE = mk("Sigil Mine").drag(520f).prox(70f).aoeRadius(100f).cooldown(0.8f).speed(330).damage(40).bulletSize(10).life(9f).cost(520).colour(0.9f, 0.5f, 1f);
    // MAHITALA - the serpents
    public static final WeaponDef COIL = mk("Serpent Coil").wave(40f, 6f).homingTurn(90f).cooldown(0.3f).speed(480).damage(12).bulletSize(6).life(3f).cost(480).colour(0.4f, 1f, 0.5f);
    public static final WeaponDef FANG = mk("Venom Fang").burn(5f, 6f).pierceEnemies().rehit(0.2f).cooldown(0.12f).speed(900).damage(6).bulletSize(4).cost(560).colour(0.5f, 1f, 0.2f);
    public static final WeaponDef CLOUD = mk("Naga Cloud").zone(90f, 4f, 18f).aoeRadius(40f).cooldown(0.9f).speed(420).damage(10).bulletSize(8).life(1.2f).cost(640).colour(0.4f, 0.9f, 0.4f);
    // RASATALA - weight and the dark
    public static final WeaponDef WELL = mk("Gravity Well").pull(220f, 260f).aoeRadius(110f).cooldown(1f).speed(260).damage(30).bulletSize(11).life(1.6f).cost(700).colour(0.6f, 0.4f, 1f);
    public static final WeaponDef QUAKE = mk("Quake Shell").stun(0.7f).knock(520f).cooldown(0.5f).speed(560).damage(22).bulletSize(11).cost(640).colour(0.8f, 0.6f, 0.35f);
    public static final WeaponDef SHARD = mk("Shatter Shard").splitOnHit().split(5, -1f, 360f, 520f, 0.5f, 6f).cooldown(0.3f).speed(800).damage(14).bulletSize(6).cost(600).colour(0.8f, 0.9f, 1f);
    // PATALA - gems and the last light
    public static final WeaponDef PRISM = mk("Jewel Prism").split(3, 0.22f, 40f, 800f, 1f, 9f).cooldown(0.24f).speed(760).damage(5).bulletSize(6).life(0.6f).cost(780).colour(1f, 0.6f, 0.9f);
    public static final WeaponDef EXECUTE = mk("Executioner").execute(0.4f, 3f).aimAssist().aimAssistDeg(10f).cooldown(0.3f).speed(860).damage(14).bulletSize(5).cost(850).colour(1f, 0.25f, 0.3f);
    public static final WeaponDef LASTLIGHT = mk("Last Light").grow(30f).pierceEnemies().rehit(0.3f).cooldown(1.1f).speed(220).damage(40).bulletSize(6).life(3.5f).cost(900).colour(1f, 1f, 0.85f);

    public static final WeaponDef[] ALL = {
            SINGLE, DOUBLE, BURST1, BURST2, SPREAD, HEAVY, LIGHT, CHARGE,
            LASER, BOMB, MISSILE, TRACKING, AIM, PIERCE, THROUGH,
            MIRROR, SCYTHE, WEAVE, EMBER, ASH, CINDER, TIDE, RIPPLE, BOON,
            RICOCHET, ARC, MINE, COIL, FANG, CLOUD, WELL, QUAKE, SHARD, PRISM, EXECUTE, LASTLIGHT
    };

    /** The first fifteen are the standard patterns; the rest are the lokas', three to a loka in loka order. */
    public static final int STANDARD = 15;

    static {
        for (int i = 0; i < ALL.length; i++) ALL[i].idx = i;
        // WHICH LOKA OFFERS WHAT: five patterns in Atala, three in Vitala, two each in Sutala, Talatala
        // and Mahitala, one in Rasatala. A pattern from a deeper loka is not even shown in the bay.
        int[] open = {0, 0, 0, 1, 0, 2, 0, 3, 4, 4, 3, 5, 1, 1, 2,
                0, 0, 0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 6};
        String[] ability = {"TRUE BOLT", "LANCING PAIR", "CHASING BURST", "READS THE GAP", "SHREDDER",
                "CONCUSSIVE", "SEEKING STREAM", "PIERCING CHARGE", "LONG BEAM", "WIDE BLAST", "HUNTER",
                "THIRD HEAD", "BENDS TO ITS MARK", "WALL-PIERCER", "TOTAL BORE",
                "BITING REFLECTION", "SWIFT RETURN", "THIRD STRAND", "LONG BURN", "WIDE SWEEP", "MORE EMBERS",
                "DEEP CURRENT", "FULL RING", "OVERFLOW", "MANY BOUNCES", "TWO MORE JUMPS", "WIDE SIGIL",
                "SHARP HUNTER", "DEEP VENOM", "GREATER CLOUD", "DEEPER WELL", "LONG STUN", "MORE SHARDS",
                "FULLER FAN", "REACHES HIGHER", "SWELLING LIGHT"};
        for (int i = 0; i < ALL.length; i++) {
            ALL[i].minLoka = open[i];
            ALL[i].ability = ability[i];
        }
    }
}
