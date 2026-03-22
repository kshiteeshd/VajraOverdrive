// LevelRegistry.java
package campaign;

import java.util.HashMap;
import java.util.Map;

public class LevelRegistry {

    private static final Map<Integer, LevelDefinition> levels = new HashMap<>();

    static {

        // ── RED REGION (1-5) — T1 basic only ─────────────────────────
        levels.put(1, new LevelBuilder(1, "RED")
                .wave(WavePattern.BASIC)
                .wave(WavePattern.V_ATTACK)
                .build());

        levels.put(2, new LevelBuilder(2, "RED")
                .wave(WavePattern.BASIC)
                .wave(WavePattern.DOUBLE_LINE)
                .build());

        levels.put(3, new LevelBuilder(3, "RED")
                .wave(WavePattern.BASIC)
                .wave(WavePattern.V_ATTACK)
                .wave(WavePattern.DOUBLE_LINE)
                .build());

        levels.put(4, new LevelBuilder(4, "RED")
                .wave(WavePattern.SWARM)
                .wave(WavePattern.V_ATTACK)
                .build());

        levels.put(5, new LevelBuilder(5, "RED")
                .wave(WavePattern.SWARM)
                .wave(WavePattern.DOUBLE_LINE)
                .wave(WavePattern.BOSS_RED)        // Vanguard
                .build());

        // ── YELLOW REGION (6-10) — fast enemies introduced ────────────
        levels.put(6, new LevelBuilder(6, "YELLOW")
                .wave(WavePattern.BASIC)
                .wave(WavePattern.FAST_WAVE)
                .build());

        levels.put(7, new LevelBuilder(7, "YELLOW")
                .wave(WavePattern.FAST_WAVE)
                .wave(WavePattern.V_ATTACK)
                .build());

        levels.put(8, new LevelBuilder(8, "YELLOW")
                .wave(WavePattern.DOUBLE_LINE)
                .wave(WavePattern.FAST_WAVE)
                .wave(WavePattern.V_ATTACK)
                .build());

        levels.put(9, new LevelBuilder(9, "YELLOW")
                .wave(WavePattern.FAST_SWARM)
                .wave(WavePattern.DOUBLE_LINE)
                .build());

        levels.put(10, new LevelBuilder(10, "YELLOW")
                .wave(WavePattern.SWARM)
                .wave(WavePattern.FAST_WAVE)
                .wave(WavePattern.BOSS_YELLOW)     // Swarmmaster
                .build());

        // ── BLUE REGION (11-15) — tanks + snipers introduced ─────────
        levels.put(11, new LevelBuilder(11, "BLUE")
                .wave(WavePattern.FAST_WAVE)
                .wave(WavePattern.TANK_LINE)
                .build());

        levels.put(12, new LevelBuilder(12, "BLUE")
                .wave(WavePattern.TANK_LINE)
                .wave(WavePattern.FAST_SWARM)
                .build());

        levels.put(13, new LevelBuilder(13, "BLUE")
                .wave(WavePattern.ASSAULT)
                .wave(WavePattern.SNIPER_LINE)
                .build());

        levels.put(14, new LevelBuilder(14, "BLUE")
                .wave(WavePattern.TANK_V)
                .wave(WavePattern.SNIPER_LINE)
                .wave(WavePattern.FAST_WAVE)
                .build());

        levels.put(15, new LevelBuilder(15, "BLUE")
                .wave(WavePattern.SIEGE)
                .wave(WavePattern.FAST_SWARM)
                .wave(WavePattern.BOSS_BLUE)       // Ironclad
                .build());

        // ── GREEN REGION (16-20) — T2 enemies introduced ─────────────
        // Level 16: first T2 exposure — just basic T2 so player can learn
        levels.put(16, new LevelBuilder(16, "GREEN")
                .wave(WavePattern.BASIC_T2_LINE)
                .wave(WavePattern.SNIPER_LINE)
                .build());

        // Level 17: T2 fast introduced alongside T1 sniper
        levels.put(17, new LevelBuilder(17, "GREEN")
                .wave(WavePattern.FAST_T2_WAVE)
                .wave(WavePattern.BASIC_T2_V)
                .build());

        // Level 18: T2 tank arrives — heavyweight wave
        levels.put(18, new LevelBuilder(18, "GREEN")
                .wave(WavePattern.TANK_T2_LINE)
                .wave(WavePattern.FAST_T2_WAVE)
                .build());

        // Level 19: T2 sniper introduced — siege gets dangerous
        levels.put(19, new LevelBuilder(19, "GREEN")
                .wave(WavePattern.SNIPER_T2_LINE)
                .wave(WavePattern.ASSAULT_ELITE)
                .build());

        // Level 20: full T2 mixed — hardest GREEN level
        levels.put(20, new LevelBuilder(20, "GREEN")
                .wave(WavePattern.SIEGE_ELITE)
                .wave(WavePattern.BLITZ_ELITE)
                .wave(WavePattern.BOSS_GREEN)      // Phantom
                .build());

        // ── WHITE REGION (21-25) — all T2, elite combinations ────────
        levels.put(21, new LevelBuilder(21, "WHITE")
                .wave(WavePattern.SIEGE_ELITE)
                .wave(WavePattern.FAST_T2_V)
                .build());

        levels.put(22, new LevelBuilder(22, "WHITE")
                .wave(WavePattern.BLITZ_ELITE)
                .wave(WavePattern.SIEGE_ELITE)
                .build());

        levels.put(23, new LevelBuilder(23, "WHITE")
                .wave(WavePattern.ASSAULT_ELITE)
                .wave(WavePattern.TANK_T2_LINE)
                .wave(WavePattern.SNIPER_T2_LINE)
                .build());

        levels.put(24, new LevelBuilder(24, "WHITE")
                .wave(WavePattern.SIEGE_ELITE)
                .wave(WavePattern.BLITZ_ELITE)
                .wave(WavePattern.ASSAULT_ELITE)
                .build());

        // Level 25: final level — three waves, all T2, maximum pressure
        levels.put(25, new LevelBuilder(25, "WHITE")
                .wave(WavePattern.BLITZ_ELITE)
                .wave(WavePattern.SIEGE_ELITE)
                .wave(WavePattern.BOSS_WHITE)      // Apex
                .build());
    }

    public static LevelDefinition getLevel(int level) {
        return levels.get(level);
    }
}