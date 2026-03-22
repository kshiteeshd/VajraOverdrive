package campaign;

import java.util.HashMap;
import java.util.Map;

public class LevelRegistry {

    private static final Map<Integer, LevelDefinition> levels = new HashMap<>();

    static {

        // ── RED REGION (1-5) — basics only ───────────────────────────
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
                .wave(WavePattern.V_ATTACK)
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
                .wave(WavePattern.ASSAULT)
                .build());

        // ── BLUE REGION (11-15) — tanks introduced ────────────────────
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
                .wave(WavePattern.TANK_LINE)
                .build());

        levels.put(14, new LevelBuilder(14, "BLUE")
                .wave(WavePattern.TANK_V)
                .wave(WavePattern.FAST_WAVE)
                .wave(WavePattern.DOUBLE_LINE)
                .build());

        levels.put(15, new LevelBuilder(15, "BLUE")
                .wave(WavePattern.FAST_SWARM)
                .wave(WavePattern.TANK_LINE)
                .wave(WavePattern.ASSAULT)
                .build());

        // ── GREEN REGION (16-20) — snipers introduced ─────────────────
        levels.put(16, new LevelBuilder(16, "GREEN")
                .wave(WavePattern.SNIPER_LINE)
                .wave(WavePattern.FAST_WAVE)
                .build());

        levels.put(17, new LevelBuilder(17, "GREEN")
                .wave(WavePattern.TANK_LINE)
                .wave(WavePattern.SNIPER_LINE)
                .build());

        levels.put(18, new LevelBuilder(18, "GREEN")
                .wave(WavePattern.SIEGE)
                .wave(WavePattern.FAST_SWARM)
                .build());

        levels.put(19, new LevelBuilder(19, "GREEN")
                .wave(WavePattern.BLITZ)
                .wave(WavePattern.SIEGE)
                .build());

        levels.put(20, new LevelBuilder(20, "GREEN")
                .wave(WavePattern.ASSAULT)
                .wave(WavePattern.SIEGE)
                .wave(WavePattern.FAST_SWARM)
                .build());

        // ── WHITE REGION (21-25) — elite combinations ─────────────────
        levels.put(21, new LevelBuilder(21, "WHITE")
                .wave(WavePattern.SIEGE)
                .wave(WavePattern.BLITZ)
                .build());

        levels.put(22, new LevelBuilder(22, "WHITE")
                .wave(WavePattern.BLITZ)
                .wave(WavePattern.SIEGE)
                .wave(WavePattern.FAST_SWARM)
                .build());

        levels.put(23, new LevelBuilder(23, "WHITE")
                .wave(WavePattern.ELITE)
                .wave(WavePattern.ASSAULT)
                .build());

        levels.put(24, new LevelBuilder(24, "WHITE")
                .wave(WavePattern.ELITE)
                .wave(WavePattern.SIEGE)
                .wave(WavePattern.BLITZ)
                .build());

        levels.put(25, new LevelBuilder(25, "WHITE")
                .wave(WavePattern.BLITZ)
                .wave(WavePattern.ELITE)
                .wave(WavePattern.SIEGE)
                .wave(WavePattern.ELITE)
                .build());
    }

    public static LevelDefinition getLevel(int level) {
        return levels.get(level);
    }
}