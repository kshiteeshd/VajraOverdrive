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

    // ── Enemy type lookups ────────────────────────────────────────────
    private static EnemyType basic()  {
        return EnemyRegistry.getByName("BasicEnemy");
    }
    private static EnemyType fast()   {
        return EnemyRegistry.getByName("FastEnemy");
    }
    private static EnemyType tank()   {
        return EnemyRegistry.getByName("TankEnemy");
    }
    private static EnemyType sniper() {
        return EnemyRegistry.getByName("SniperEnemy");
    }

    // ── BasicEnemy formations ─────────────────────────────────────────
    public static FleetDefinition line(int count) {
        return new FleetDefinition(basic(),
                FleetDefinition.Formation.LINE, count, DEFAULT_SPACING, 80);
    }

    public static FleetDefinition v(int count) {
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

    // ── FastEnemy formations ──────────────────────────────────────────
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

    // ── TankEnemy formations ──────────────────────────────────────────
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

    // ── SniperEnemy formations ────────────────────────────────────────
    public static FleetDefinition sniperLine(int count) {
        return new FleetDefinition(sniper(),
                FleetDefinition.Formation.LINE, count, DEFAULT_SPACING, 85);
    }

    public static FleetDefinition sniperLineScaled(int base, int level) {
        return new FleetDefinition(sniper(),
                FleetDefinition.Formation.LINE,
                base + (level / 7), DEFAULT_SPACING, 85);
    }

    // ── Mixed fleets ──────────────────────────────────────────────────
    public static FleetDefinition mixed(List<EnemyGroup> groups,
                                        FleetDefinition.Formation formation,
                                        int spacing,
                                        int formationY) {
        return new FleetDefinition(groups, formation, spacing, formationY);
    }

    // ── Group helper ──────────────────────────────────────────────────
    public static EnemyGroup group(EnemyType type, int count) {
        return new EnemyGroup(type, count);
    }
}