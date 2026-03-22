package boss;

import entity.EntityManager;
import player.PlayerShip;
import boss.types.*;

/**
 * Maps region names to their boss constructors.
 * WaveManager calls createBoss() when a BOSS wave is triggered.
 *
 * FIXED: imports changed from boss.types.* to boss.* — all boss files
 * live in the boss package, not a boss.types sub-package.
 */
public class BossRegistry {

    /**
     * Creates the boss for the given region at the given spawn position.
     * Returns null if the region has no registered boss.
     */
    public static BossEntity createBoss(String region,
                                        double spawnX, double spawnY,
                                        EntityManager em,
                                        PlayerShip player) {
        return switch (region) {
            case "RED"    -> new VanguardBoss(spawnX, spawnY, em, player);
            case "YELLOW" -> new SwarmmasterBoss(spawnX, spawnY, em, player);
            case "BLUE"   -> new IroncladBoss(spawnX, spawnY, em, player);
            case "GREEN"  -> new PhantomBoss(spawnX, spawnY, em, player);
            case "WHITE"  -> new ApexBoss(spawnX, spawnY, em, player);
            default       -> null;
        };
    }
}