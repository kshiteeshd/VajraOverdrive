package enemy.data;

import enemy.types.BasicEnemy;
import enemy.types.FastEnemy;
import enemy.types.SniperEnemy;
import enemy.types.TankEnemy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnemyRegistry {

    private static final List<EnemyType> enemies = new ArrayList<>();

    static {

        // ── BASIC ─────────────────────────────────────────────────────
        // Unlocked from wave 1. Standard health, straight shot.
        EnemyStats basicStats = new EnemyStats(
                20, 1.0, 1.5,
                2, 1, 0,
                false, false, false,
                100, 28, 28,
                EnemyStats.MovePattern.STATIC
        );
        enemies.add(new EnemyType(
                "BasicEnemy",
                (x, y, em) -> new BasicEnemy(x, y, basicStats, em),
                1, 1, 10, basicStats
        ));

        // ── FAST ──────────────────────────────────────────────────────
        // Unlocked wave 3. Low HP, zigzag, rapid fire.
        EnemyStats fastStats = new EnemyStats(
                10, 2.2, 2.5,
                1, 1, 0,
                false, false, false,
                150, 20, 20,
                EnemyStats.MovePattern.ZIGZAG
        );
        enemies.add(new EnemyType(
                "FastEnemy",
                (x, y, em) -> new FastEnemy(x, y, fastStats, em),
                2, 3, 8, fastStats
        ));

        // ── TANK ──────────────────────────────────────────────────────
        // Unlocked wave 5. High HP, slow, heavy shot, strafe.
        EnemyStats tankStats = new EnemyStats(
                60, 0.6, 0.8,
                5, 1, 0,
                false, false, false,
                300, 40, 40,
                EnemyStats.MovePattern.STRAFE
        );
        enemies.add(new EnemyType(
                "TankEnemy",
                (x, y, em) -> new TankEnemy(x, y, tankStats, em),
                3, 5, 5, tankStats
        ));

        // ── SNIPER ────────────────────────────────────────────────────
        // Unlocked wave 8. Medium HP, static, fires aimed shot.
        EnemyStats sniperStats = new EnemyStats(
                25, 0.8, 1.0,
                8, 1, 0,
                false, false, false,
                250, 28, 28,
                EnemyStats.MovePattern.STATIC
        );
        enemies.add(new EnemyType(
                "SniperEnemy",
                (x, y, em) -> new SniperEnemy(x, y, sniperStats, em),
                3, 8, 4, sniperStats
        ));
    }

    public static List<EnemyType> getEnemiesForWave(int wave) {
        return enemies.stream()
                .filter(e -> e.unlockWave <= wave)
                .collect(Collectors.toList());
    }

    public static EnemyType getByName(String name) {
        return enemies.stream()
                .filter(e -> e.name.equals(name))
                .findFirst()
                .orElse(null);
    }
}