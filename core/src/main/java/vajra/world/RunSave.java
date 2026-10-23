package vajra.world;

import com.badlogic.gdx.utils.Json;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import vajra.enemy.Enemy;
import vajra.ship.Hangar;

/** THE SAVED RUN: what CONTINUE wakes. A run is rebuilt the way a new one is (the world is
 *  generated, deterministic), and then this record is poured over it: the hull and where it hangs,
 *  the purse and everything bought, the score and the kill book, the foes that have already fallen,
 *  the checkpoints, caches and wrecks taken, the people already met and the jobs they gave.
 *
 *  <p>What is deliberately NOT kept: bullets, particles, damage on foes still standing (they wake at
 *  full armour), and the hangar's docked pose (the hull wakes in open air at its saved spot). The
 *  story beats that already played do not replay: the opening and every first meeting are settled,
 *  not staged.
 *
 *  <p>Pure and string-shaped, like SaveGame, so a headless test round-trips it. A save file is
 *  untrusted input: unknown keys are skipped and every index is clamped. */
public final class RunSave {
    public static final int VERSION = 1;
    private static final Json JSON = new Json();
    static { JSON.setIgnoreUnknownFields(true); }

    private RunSave() {}

    public static class Data {
        public int version = VERSION;
        // the hull
        public float x, y, facing, hp, boost, shield;
        // the world's ledger
        public int lives, loka, gaze, bounty, salvage, salvageTaken;
        public long points;
        public float clock;
        public boolean bossDead;
        public int[] kills;
        public boolean[] codex, regionScored, wreckTaken, cpCollected, voicesMet;
        public int[] cache;
        public int respawnCp = -1;
        public int[] deadGarrison;
        // the hangar
        public int coins, shipIndex, weaponIndex, hangarLoka;
        public int[] shards, forgeProgress, forgeCharges, weaponRank;
        public boolean[] pieces, forged, owned, weaponOwned;
    }

    // --- capture ---------------------------------------------------------------------------

    public static String capture(World w) {
        Data d = new Data();
        d.x = w.ship.x;
        d.y = w.ship.y;
        d.facing = w.ship.facing;
        d.hp = w.ship.hp;
        d.boost = w.ship.boostEnergy;
        d.shield = w.ship.shieldEnergy;
        d.lives = w.lives;
        d.loka = w.lokaIndex;
        d.gaze = w.gazeStage;
        d.points = w.score.points;
        d.clock = w.clock;
        d.bossDead = w.bossDead;
        d.kills = w.kills.clone();
        d.codex = w.codex.clone();
        d.regionScored = w.regionScored().clone();
        d.wreckTaken = w.wreckTaken.clone();
        d.cache = w.cacheState[w.slot].clone();
        d.cpCollected = new boolean[w.checkpoints.length];
        for (int i = 0; i < w.checkpoints.length; i++) {
            if (w.checkpoints[i] == null) continue;
            d.cpCollected[i] = w.checkpoints[i].collected;
            if (w.checkpoints[i] == w.respawnCp) d.respawnCp = i;
        }
        d.voicesMet = new boolean[w.voices.size()];
        for (int i = 0; i < w.voices.size(); i++) d.voicesMet[i] = w.voices.get(i).voiceMet;
        d.bounty = w.missions.bounty;
        d.salvage = w.missions.salvage;
        d.salvageTaken = w.missions.salvageTaken;

        // the garrison that has fallen: every static id that is no longer a live body
        Set<Integer> alive = new HashSet<>();
        for (Enemy e : w.enemies) if (!e.dead && !e.dismissed && e.netId < w.garrison) alive.add(e.netId);
        ArrayList<Integer> dead = new ArrayList<>();
        for (int id = 0; id < w.garrison; id++) if (!alive.contains(id)) dead.add(id);
        d.deadGarrison = new int[dead.size()];
        for (int i = 0; i < d.deadGarrison.length; i++) d.deadGarrison[i] = dead.get(i);

        Hangar h = w.hangar;
        d.coins = h.coins;
        d.shipIndex = h.shipIndex;
        d.weaponIndex = h.weaponIndex;
        d.hangarLoka = h.loka;
        d.shards = h.shards.clone();
        d.forgeProgress = h.forgeProgress.clone();
        d.forgeCharges = h.forgeCharges.clone();
        d.weaponRank = h.weaponRank.clone();
        d.pieces = h.pieces.clone();
        d.forged = h.forged.clone();
        d.weaponOwned = h.weaponOwned.clone();
        d.owned = new boolean[h.owned.length * 3];
        for (int i = 0; i < h.owned.length; i++)
            for (int t = 0; t < 3; t++) d.owned[i * 3 + t] = h.owned[i][t];
        return JSON.toJson(d);
    }

    // --- restore ---------------------------------------------------------------------------

    /** Pour a saved run over a freshly built World. False (and the world untouched enough to fly a new
     *  run) when the text is not a run this build can read. */
    public static boolean restore(World w, String json) {
        Data d;
        try {
            d = JSON.fromJson(Data.class, json);
        } catch (RuntimeException e) {
            return false;
        }
        if (d == null || d.version != VERSION) return false;

        // the hangar first: the hull's def and the weapon follow its indices
        Hangar h = w.hangar;
        h.coins = Math.max(0, d.coins);
        h.loka = clamp(d.hangarLoka, 0, LokaTable.COUNT - 1);
        copy(d.shards, h.shards);
        copy(d.forgeProgress, h.forgeProgress);
        copy(d.forgeCharges, h.forgeCharges);
        copy(d.weaponRank, h.weaponRank);
        copy(d.pieces, h.pieces);
        copy(d.forged, h.forged);
        copy(d.weaponOwned, h.weaponOwned);
        if (d.owned != null)
            for (int i = 0; i < h.owned.length; i++)
                for (int t = 0; t < 3 && i * 3 + t < d.owned.length; t++) h.owned[i][t] = d.owned[i * 3 + t];
        h.owned[0][0] = true;
        h.weaponOwned[0] = true;
        w.selectShip(clamp(d.shipIndex, 0, h.owned.length - 1));
        w.selectWeapon(clamp(d.weaponIndex, 0, h.weaponOwned.length - 1));

        w.lokaIndex = clamp(d.loka, 0, LokaTable.COUNT - 1);
        w.lives = clamp(d.lives, 1, World.MAX_LIVES);
        w.score.points = Math.max(0, Math.min(Score.MAX, d.points));
        w.clock = Math.max(0, d.clock);
        w.bossDead = d.bossDead;
        w.gazeStage = clamp(d.gaze, 0, 3);
        copy(d.kills, w.kills);
        copy(d.codex, w.codex);
        copy(d.regionScored, w.regionScored());
        copy(d.wreckTaken, w.wreckTaken);
        if (d.cache != null)
            for (int i = 0; i < w.cacheState[w.slot].length && i < d.cache.length; i++)
                w.cacheState[w.slot][i] = d.cache[i] == World.CACHE_DONE ? World.CACHE_DONE : World.CACHE_SEALED;
        for (int i = 0; i < w.checkpoints.length; i++) {
            Checkpoint c = w.checkpoints[i];
            if (c == null) continue;
            c.collected = d.cpCollected != null && i < d.cpCollected.length && d.cpCollected[i];
            if (i == d.respawnCp) w.respawnCp = c;
        }
        if (d.voicesMet != null)
            for (int i = 0; i < w.voices.size() && i < d.voicesMet.length; i++)
                w.voices.get(i).voiceMet = d.voicesMet[i];

        // jobs: the bounty's rogue is a runtime body, so an open card is raised again
        w.missions.salvage = clamp(d.salvage, 0, 2);
        w.missions.salvageTaken = clamp(d.salvageTaken, 0, Missions.SALVAGE_NEED);
        w.missions.bounty = Missions.NONE;
        if (d.bounty == Missions.DONE) w.missions.bounty = Missions.DONE;
        else if (d.bounty == Missions.OPEN && !w.voices.isEmpty()) w.missions.openBounty();

        // the fallen stay fallen
        if (d.deadGarrison != null) {
            Set<Integer> gone = new HashSet<>();
            for (int id : d.deadGarrison) gone.add(id);
            w.enemies.removeIf(e -> e.netId < w.garrison && gone.contains(e.netId));
        }

        // the hull, last: wherever it hangs in open air, at the hp it had
        w.ship.x = Math.max(0, Math.min(w.width, d.x));
        w.ship.y = Math.max(0, Math.min(w.height, d.y));
        w.ship.vx = w.ship.vy = 0;
        w.ship.facing = d.facing;
        w.ship.hp = Math.max(1, Math.min(w.ship.maxHp(), d.hp));
        w.ship.boostEnergy = Math.max(0, d.boost);
        w.ship.shieldEnergy = Math.max(0, Math.min(w.ship.shieldCap(), d.shield));
        w.settleRegion();
        w.cues.clear(); // the opening already happened; first meetings are settled, not staged
        return true;
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private static void copy(int[] from, int[] to) {
        if (from != null) System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }

    private static void copy(boolean[] from, boolean[] to) {
        if (from != null) System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
    }
}
