package campaign;

import fleet.FleetDefinition;
import fleet.EnemyGroup;
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

            // ── Basic ─────────────────────────────────────────────────
            case BASIC -> {
                fleets.add(lineScaled(6, level));
            }
            case V_ATTACK -> {
                fleets.add(vScaled(7, level));
            }
            case DOUBLE_LINE -> {
                fleets.add(lineScaled(6, level));
                fleets.add(lineScaled(6, level));
            }
            case SWARM -> {
                fleets.add(lineScaled(10, level));
            }
            case MIXED -> {
                fleets.add(lineScaled(6, level));
                fleets.add(vScaled(7, level));
            }

            // ── Fast ──────────────────────────────────────────────────
            case FAST_WAVE -> {
                fleets.add(fastLineScaled(6, level));
            }
            case FAST_SWARM -> {
                fleets.add(fastLineScaled(10, level));
            }

            // ── Tank ──────────────────────────────────────────────────
            case TANK_LINE -> {
                fleets.add(tankLineScaled(4, level));
            }
            case TANK_V -> {
                fleets.add(tankV(5, level));
            }

            // ── Sniper ────────────────────────────────────────────────
            case SNIPER_LINE -> {
                fleets.add(sniperLineScaled(4, level));
            }

            // ── Mixed multi-type ──────────────────────────────────────
            case ASSAULT -> {
                // Basic line + Fast V
                fleets.add(lineScaled(6, level));
                fleets.add(fastV(5, level));
            }
            case SIEGE -> {
                // Tank line + Sniper line
                fleets.add(tankLineScaled(3, level));
                fleets.add(sniperLineScaled(4, level));
            }
            case BLITZ -> {
                // Fast swarm + Basic V
                fleets.add(fastLineScaled(8, level));
                fleets.add(vScaled(7, level));
            }
            case ELITE -> {
                // Tank + Sniper + Basic — three consecutive fleets
                fleets.add(tankLineScaled(3, level));
                fleets.add(sniperLineScaled(3, level));
                fleets.add(lineScaled(6, level));
            }
        }

        return new WaveDefinition(waveNumber, fleets);
    }
}