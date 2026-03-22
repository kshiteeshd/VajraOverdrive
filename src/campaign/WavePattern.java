// WavePattern.java
package campaign;

public enum WavePattern {

    // ── Basic T1 ───────────────────────────────────────────────────────
    BASIC,
    V_ATTACK,
    DOUBLE_LINE,
    SWARM,
    MIXED,

    // ── Fast T1 ────────────────────────────────────────────────────────
    FAST_WAVE,
    FAST_SWARM,

    // ── Tank T1 ────────────────────────────────────────────────────────
    TANK_LINE,
    TANK_V,

    // ── Sniper T1 ──────────────────────────────────────────────────────
    SNIPER_LINE,

    // ── Mixed T1 ───────────────────────────────────────────────────────
    ASSAULT,     // basic line + fast V
    SIEGE,       // tank line + sniper line
    BLITZ,       // fast swarm + basic V
    ELITE,       // tank + sniper + basic

    // ── Tier-2 patterns (GREEN / WHITE regions) ─────────────────────
    BASIC_T2_LINE,      // elite basic line
    BASIC_T2_V,         // elite basic V
    FAST_T2_WAVE,       // dive-mode fast swarm
    FAST_T2_V,          // dive-mode fast V
    TANK_T2_LINE,       // heavy scatter tanks
    SNIPER_T2_LINE,     // triple-burst snipers

    // ── Mixed T2 ───────────────────────────────────────────────────────
    ASSAULT_ELITE,      // basic T2 line + fast T2 V
    SIEGE_ELITE,        // tank T2 + sniper T2
    BLITZ_ELITE,        // fast T2 swarm + basic T2 V
    FINAL_PUSH,         // tank T2 + sniper T2 + fast T2 — hardest non-boss wave


    // ── Boss waves — one per region ─────────────────────────────────────
    // These are handled specially in WaveManager:
    // no FleetDefinition is created — a BossEntity is spawned directly.
    BOSS_RED,
    BOSS_YELLOW,
    BOSS_BLUE,
    BOSS_GREEN,
    BOSS_WHITE,
}