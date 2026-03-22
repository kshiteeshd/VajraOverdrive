// WaveFactory.java
package campaign;

import fleet.FleetDefinition;
import wave.WaveDefinition;

import java.util.ArrayList;
import java.util.List;

import static campaign.FleetFactory.*;

public class WaveFactory {

    public static WaveDefinition create(int waveNumber,
                                        WavePattern pattern,
                                        int level) {

        List<FleetDefinition> fleets = new ArrayList<>();

        switch (pattern) {

            // ── T1 Basic ─────────────────────────────────────────────
            case BASIC        -> fleets.add(lineScaled(6, level));
            case V_ATTACK     -> fleets.add(vScaled(7, level));
            case DOUBLE_LINE  -> { fleets.add(lineScaled(6, level));
                fleets.add(lineScaled(6, level)); }
            case SWARM        -> fleets.add(lineScaled(10, level));
            case MIXED        -> { fleets.add(lineScaled(6, level));
                fleets.add(vScaled(7, level)); }

            // ── T1 Fast ──────────────────────────────────────────────
            case FAST_WAVE    -> fleets.add(fastLineScaled(6, level));
            case FAST_SWARM   -> fleets.add(fastLineScaled(10, level));

            // ── T1 Tank ──────────────────────────────────────────────
            case TANK_LINE    -> fleets.add(tankLineScaled(4, level));
            case TANK_V       -> fleets.add(tankV(5, level));

            // ── T1 Sniper ────────────────────────────────────────────
            case SNIPER_LINE  -> fleets.add(sniperLineScaled(4, level));

            // ── T1 Mixed ─────────────────────────────────────────────
            case ASSAULT -> { fleets.add(lineScaled(6, level));
                fleets.add(fastV(5, level)); }
            case SIEGE   -> { fleets.add(tankLineScaled(3, level));
                fleets.add(sniperLineScaled(4, level)); }
            case BLITZ   -> { fleets.add(fastLineScaled(8, level));
                fleets.add(vScaled(7, level)); }
            case ELITE   -> { fleets.add(tankLineScaled(3, level));
                fleets.add(sniperLineScaled(3, level));
                fleets.add(lineScaled(6, level)); }

            // ── T2 Single-type ───────────────────────────────────────
            case BASIC_T2_LINE   -> fleets.add(basicT2Line(6, level));
            case BASIC_T2_V      -> fleets.add(basicT2V(7, level));
            case FAST_T2_WAVE    -> fleets.add(fastT2Line(6, level));
            case FAST_T2_V       -> fleets.add(fastT2V(5, level));
            case TANK_T2_LINE    -> fleets.add(tankT2Line(3, level));
            case SNIPER_T2_LINE  -> fleets.add(sniperT2Line(4, level));

            // ── T2 Mixed ─────────────────────────────────────────────
            case ASSAULT_ELITE -> {
                fleets.add(basicT2Line(5, level));
                fleets.add(fastT2V(5, level));
            }
            case SIEGE_ELITE -> {
                fleets.add(tankT2Line(3, level));
                fleets.add(sniperT2Line(4, level));
            }
            case BLITZ_ELITE -> {
                fleets.add(fastT2Line(8, level));
                fleets.add(basicT2V(7, level));
            }
            case FINAL_PUSH -> {
                // All three T2 types — hardest non-boss wave in the game
                fleets.add(tankT2Line(2, level));
                fleets.add(sniperT2Line(3, level));
                fleets.add(fastT2Line(6, level));
            }

            // Add inside the switch(pattern) in WaveFactory.create():

            case BOSS_RED    -> { return new WaveDefinition(waveNumber, "RED");    }
            case BOSS_YELLOW -> { return new WaveDefinition(waveNumber, "YELLOW"); }
            case BOSS_BLUE   -> { return new WaveDefinition(waveNumber, "BLUE");   }
            case BOSS_GREEN  -> { return new WaveDefinition(waveNumber, "GREEN");  }
            case BOSS_WHITE  -> { return new WaveDefinition(waveNumber, "WHITE");  }
        }

        return new WaveDefinition(waveNumber, fleets);
    }
}