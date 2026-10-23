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
        d.minLoka = minLoka; d.ability = ability;
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

    public static final WeaponDef[] ALL = {
            SINGLE, DOUBLE, BURST1, BURST2, SPREAD, HEAVY, LIGHT, CHARGE,
            LASER, BOMB, MISSILE, TRACKING, AIM, PIERCE, THROUGH
    };

    static {
        for (int i = 0; i < ALL.length; i++) ALL[i].idx = i;
        // WHICH LOKA OFFERS WHAT: five patterns in Atala, three in Vitala, two each in Sutala, Talatala
        // and Mahitala, one in Rasatala. A pattern from a deeper loka is not even shown in the bay.
        int[] open = {0, 0, 0, 1, 0, 2, 0, 3, 4, 4, 3, 5, 1, 1, 2};
        String[] ability = {"TRUE BOLT", "LANCING PAIR", "CHASING BURST", "READS THE GAP", "SHREDDER",
                "CONCUSSIVE", "SEEKING STREAM", "PIERCING CHARGE", "LONG BEAM", "WIDE BLAST", "HUNTER",
                "THIRD HEAD", "BENDS TO ITS MARK", "WALL-PIERCER", "TOTAL BORE"};
        for (int i = 0; i < ALL.length; i++) {
            ALL[i].minLoka = open[i];
            ALL[i].ability = ability[i];
        }
    }
}
