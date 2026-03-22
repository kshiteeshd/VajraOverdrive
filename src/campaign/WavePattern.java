package campaign;

public enum WavePattern {

    // ── Basic patterns ─────────────────────────────────────────────────
    BASIC,           // single line of BasicEnemy
    V_ATTACK,        // V formation of BasicEnemy
    DOUBLE_LINE,     // two lines of BasicEnemy
    SWARM,           // large line of BasicEnemy

    // ── Mixed patterns ─────────────────────────────────────────────────
    MIXED,           // line + V of BasicEnemy

    // ── Fast enemy patterns ────────────────────────────────────────────
    FAST_WAVE,       // line of FastEnemy
    FAST_SWARM,      // large line of FastEnemy

    // ── Tank patterns ──────────────────────────────────────────────────
    TANK_LINE,       // small line of TankEnemy
    TANK_V,          // V of TankEnemy

    // ── Sniper patterns ────────────────────────────────────────────────
    SNIPER_LINE,     // line of SniperEnemy

    // ── Mixed multi-type ───────────────────────────────────────────────
    ASSAULT,         // BasicEnemy line + FastEnemy V
    SIEGE,           // TankEnemy line + SniperEnemy line
    BLITZ,           // FastEnemy swarm + BasicEnemy V
    ELITE,           // TankEnemy + SniperEnemy + BasicEnemy
}