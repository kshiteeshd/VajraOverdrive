package campaign;

import enemy.data.EnemyRegistry;
import enemy.data.EnemyType;
import fleet.EnemyGroup;
import fleet.FleetDefinition;

import java.util.List;

public class FleetFactory {

    private static final int DEFAULT_SPACING = 40;
    private static final int TANK_SPACING    = 52;
    private static final int FAST_SPACING    = 32;

    // ── Tier-1 type lookups ───────────────────────────────────────────
    private static EnemyType basic()    { return EnemyRegistry.getByName("BasicEnemy");   }
    private static EnemyType fast()     { return EnemyRegistry.getByName("FastEnemy");    }
    private static EnemyType tank()     { return EnemyRegistry.getByName("TankEnemy");    }
    private static EnemyType sniper()   { return EnemyRegistry.getByName("SniperEnemy");  }

    // ── Tier-2 type lookups ───────────────────────────────────────────
    private static EnemyType basicT2()  { return EnemyRegistry.getByName("BasicEnemy_T2");  }
    private static EnemyType fastT2()   { return EnemyRegistry.getByName("FastEnemy_T2");   }
    private static EnemyType tankT2()   { return EnemyRegistry.getByName("TankEnemy_T2");   }
    private static EnemyType sniperT2() { return EnemyRegistry.getByName("SniperEnemy_T2"); }

    // ── BasicEnemy T1 ─────────────────────────────────────────────────
    public static FleetDefinition line(int count) {
        return new FleetDefinition(basic(),
                FleetDefinition.Formation.LINE, count, DEFAULT_SPACING, 80);
    }

    public static FleetDefinition v(int count) {
        if (count % 2 == 0) count++;
        return new FleetDefinition(basic(),
                FleetDefinition.Formation.V, count, DEFAULT_SPACING, 100);
    }

    public static FleetDefinition lineScaled(int base, int level) {
        return new FleetDefinition(basic(),
                FleetDefinition.Formation.LINE,
                base + (level / 5), DEFAULT_SPACING, 80);
    }

    public static FleetDefinition vScaled(int base, int level) {
        int count = base + (level / 5);
        if (count % 2 == 0) count++;
        return new FleetDefinition(basic(),
                FleetDefinition.Formation.V, count, DEFAULT_SPACING, 100);
    }

    // ── FastEnemy T1 ──────────────────────────────────────────────────
    public static FleetDefinition fastLine(int count) {
        return new FleetDefinition(fast(),
                FleetDefinition.Formation.LINE, count, FAST_SPACING, 70);
    }

    public static FleetDefinition fastLineScaled(int base, int level) {
        return new FleetDefinition(fast(),
                FleetDefinition.Formation.LINE,
                base + (level / 6), FAST_SPACING, 70);
    }

    public static FleetDefinition fastV(int base, int level) {
        int count = base + (level / 6);
        if (count % 2 == 0) count++;
        return new FleetDefinition(fast(),
                FleetDefinition.Formation.V, count, FAST_SPACING, 90);
    }

    // ── TankEnemy T1 ──────────────────────────────────────────────────
    public static FleetDefinition tankLine(int count) {
        return new FleetDefinition(tank(),
                FleetDefinition.Formation.LINE, count, TANK_SPACING, 90);
    }

    public static FleetDefinition tankLineScaled(int base, int level) {
        return new FleetDefinition(tank(),
                FleetDefinition.Formation.LINE,
                base + (level / 8), TANK_SPACING, 90);
    }

    public static FleetDefinition tankV(int base, int level) {
        int count = base + (level / 8);
        if (count % 2 == 0) count++;
        return new FleetDefinition(tank(),
                FleetDefinition.Formation.V, count, TANK_SPACING, 110);
    }

    // ── SniperEnemy T1 ────────────────────────────────────────────────
    public static FleetDefinition sniperLine(int count) {
        return new FleetDefinition(sniper(),
                FleetDefinition.Formation.LINE, count, DEFAULT_SPACING, 85);
    }

    public static FleetDefinition sniperLineScaled(int base, int level) {
        return new FleetDefinition(sniper(),
                FleetDefinition.Formation.LINE,
                base + (level / 7), DEFAULT_SPACING, 85);
    }

    // ── BasicEnemy T2 ─────────────────────────────────────────────────
    public static FleetDefinition basicT2Line(int base, int level) {
        return new FleetDefinition(basicT2(),
                FleetDefinition.Formation.LINE,
                base + (level / 5), DEFAULT_SPACING, 80);
    }

    public static FleetDefinition basicT2V(int base, int level) {
        int count = base + (level / 5);
        if (count % 2 == 0) count++;
        return new FleetDefinition(basicT2(),
                FleetDefinition.Formation.V, count, DEFAULT_SPACING, 100);
    }

    // ── FastEnemy T2 ──────────────────────────────────────────────────
    public static FleetDefinition fastT2Line(int base, int level) {
        return new FleetDefinition(fastT2(),
                FleetDefinition.Formation.LINE,
                base + (level / 6), FAST_SPACING, 70);
    }

    public static FleetDefinition fastT2V(int base, int level) {
        int count = base + (level / 6);
        if (count % 2 == 0) count++;
        return new FleetDefinition(fastT2(),
                FleetDefinition.Formation.V, count, FAST_SPACING, 90);
    }

    // ── TankEnemy T2 ──────────────────────────────────────────────────
    public static FleetDefinition tankT2Line(int base, int level) {
        return new FleetDefinition(tankT2(),
                FleetDefinition.Formation.LINE,
                base + (level / 8), TANK_SPACING, 90);
    }

    // ── SniperEnemy T2 ────────────────────────────────────────────────
    public static FleetDefinition sniperT2Line(int base, int level) {
        return new FleetDefinition(sniperT2(),
                FleetDefinition.Formation.LINE,
                base + (level / 7), DEFAULT_SPACING, 85);
    }

    // ── Mixed formations ──────────────────────────────────────────────
    public static FleetDefinition mixed(List<EnemyGroup> groups,
                                        FleetDefinition.Formation formation,
                                        int spacing, int formationY) {
        return new FleetDefinition(groups, formation, spacing, formationY);
    }

    public static EnemyGroup group(EnemyType type, int count) {
        return new EnemyGroup(type, count);
    }
}