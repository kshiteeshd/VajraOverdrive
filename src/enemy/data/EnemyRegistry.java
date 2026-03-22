package enemy.data;

import enemy.types.BasicEnemy;
import enemy.types.FastEnemy;
import enemy.types.SniperEnemy;
import enemy.types.TankEnemy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EnemyRegistry {

    private static final List<EnemyType>        enemies   = new ArrayList<>();
    // FIX (Batch 1 analysis): O(1) lookup instead of linear scan
    private static final Map<String, EnemyType> byName    = new HashMap<>();

    static {

        // ══════════════════════════════════════════════════════════════
        // TIER 1 ENEMIES — used in levels 1-15 (RED / YELLOW / BLUE)
        // ══════════════════════════════════════════════════════════════

        // ── BASIC T1 ──────────────────────────────────────────────────
        // Standard formation enemy. Fires straight down. Static hold.
        EnemyStats basicT1 = new EnemyStats(
                20, 1.0, 1.5,
                2, 1, 0,
                false, false, false,
                100, 28, 28,
                EnemyStats.MovePattern.STATIC,
                1
        );
        register(new EnemyType(
                "BasicEnemy",
                (x, y, em) -> new BasicEnemy(x, y, basicT1, em),
                1, 1, 10, basicT1
        ));

        // ── FAST T1 ───────────────────────────────────────────────────
        // Low HP, zigzag movement, rapid small shots.
        EnemyStats fastT1 = new EnemyStats(
                10, 2.2, 2.5,
                1, 1, 0,
                false, false, false,
                150, 20, 20,
                EnemyStats.MovePattern.ZIGZAG,
                1
        );
        register(new EnemyType(
                "FastEnemy",
                (x, y, em) -> new FastEnemy(x, y, fastT1, em),
                2, 3, 8, fastT1
        ));

        // ── TANK T1 ───────────────────────────────────────────────────
        // High HP, slow, heavy single shot, horizontal strafe.
        EnemyStats tankT1 = new EnemyStats(
                60, 0.6, 0.8,
                5, 1, 0,
                false, false, false,
                300, 40, 40,
                EnemyStats.MovePattern.STRAFE,
                1
        );
        register(new EnemyType(
                "TankEnemy",
                (x, y, em) -> new TankEnemy(x, y, tankT1, em),
                3, 5, 5, tankT1
        ));

        // ── SNIPER T1 ─────────────────────────────────────────────────
        // Medium HP, static, fires aimed shots at player position.
        EnemyStats sniperT1 = new EnemyStats(
                25, 0.8, 1.0,
                8, 1, 0,
                false, false, false,
                250, 28, 28,
                EnemyStats.MovePattern.STATIC,
                1
        );
        register(new EnemyType(
                "SniperEnemy",
                (x, y, em) -> new SniperEnemy(x, y, sniperT1, em),
                3, 8, 4, sniperT1
        ));

        // ══════════════════════════════════════════════════════════════
        // TIER 2 ENEMIES — used in levels 16-25 (GREEN / WHITE)
        // Same classes, distinct stats + move patterns + score values.
        // Visual distinction handled in each subclass render() via
        // stats.tier check.
        // ══════════════════════════════════════════════════════════════

        // ── BASIC T2 ─────────────────────────────────────────────────
        // More HP, faster, strafe movement, fires a 2-shot burst.
        // Score doubled vs T1.
        EnemyStats basicT2 = new EnemyStats(
                45, 1.3, 2.2,
                3, 2, 14,
                false, true, false,
                220, 28, 28,
                EnemyStats.MovePattern.STRAFE,
                2
        );
        register(new EnemyType(
                "BasicEnemy_T2",
                (x, y, em) -> new BasicEnemy(x, y, basicT2, em),
                2, 1, 8, basicT2
        ));

        // ── FAST T2 ──────────────────────────────────────────────────
        // More HP, DIVE movement (dives at player then retreats),
        // fires a 3-shot scatter. Harder to dodge than T1 zigzag.
        EnemyStats fastT2 = new EnemyStats(
                22, 2.8, 3.2,
                2, 3, 18,
                false, true, false,
                300, 20, 20,
                EnemyStats.MovePattern.DIVE,
                2
        );
        register(new EnemyType(
                "FastEnemy_T2",
                (x, y, em) -> new FastEnemy(x, y, fastT2, em),
                3, 3, 6, fastT2
        ));

        // ── TANK T2 ──────────────────────────────────────────────────
        // Double HP vs T1, strafe + fires 3-shot scatter spread.
        // Slow but the scatter pattern covers wide horizontal area.
        EnemyStats tankT2 = new EnemyStats(
                120, 0.7, 1.1,
                6, 3, 20,
                false, true, false,
                600, 40, 40,
                EnemyStats.MovePattern.STRAFE,
                2
        );
        register(new EnemyType(
                "TankEnemy_T2",
                (x, y, em) -> new TankEnemy(x, y, tankT2, em),
                4, 5, 3, tankT2
        ));

        // ── SNIPER T2 ────────────────────────────────────────────────
        // More HP, fires 3-shot aimed burst in quick succession.
        // Still static but shoots much more frequently.
        EnemyStats sniperT2 = new EnemyStats(
                50, 0.9, 1.6,
                9, 3, 10,
                false, true, false,
                500, 28, 28,
                EnemyStats.MovePattern.STATIC,
                2
        );
        register(new EnemyType(
                "SniperEnemy_T2",
                (x, y, em) -> new SniperEnemy(x, y, sniperT2, em),
                4, 8, 3, sniperT2
        ));
    }

    // ── Registration ─────────────────────────────────────────────────
    private static void register(EnemyType type) {
        enemies.add(type);
        byName.put(type.name, type);
    }

    // ── Queries ───────────────────────────────────────────────────────
    public static List<EnemyType> getEnemiesForWave(int wave) {
        return enemies.stream()
                .filter(e -> e.unlockWave <= wave)
                .collect(Collectors.toList());
    }

    // FIX: O(1) HashMap lookup — was O(n) linear scan on every formation build
    public static EnemyType getByName(String name) {
        return byName.get(name);
    }
}