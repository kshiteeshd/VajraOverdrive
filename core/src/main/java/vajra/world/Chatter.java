package vajra.world;

import vajra.enemy.Enemy;

/** THE VOICE RAIL - what the people of Atala do when the pilot reaches them, and when a job turns.
 *  There are three, and they are people, not targets: Uparichara's Scout, the Ninth Maya, Kaliya the
 *  Tide-Pilot (docs/NPC_BIBLE.md). The pilot is Indra, come down from Swarga for the Vajrayudha.
 *
 *  <p>The FIRST meeting of each is a short cutscene (Scenes, via a cue) - that is where a person says
 *  who they are and what they want. Everything after it is a bubble over their hull while the world
 *  keeps moving ({@link World#say}) and a toast on the HUD ({@link World#toast}). Nothing here ever
 *  holds the gate: a voice adds only the WHY.
 *
 *  <p>The once-per-run flags live on the body itself (voiceMet), so a run owns nothing on exit. */
public final class Chatter {
    static final float SIGHT_R = 260; // how close a hull reads a face, px

    private Chatter() {}

    /** Run every frame from World.update: the first approach to any voice is its meeting. */
    public static void update(World w) {
        for (int i = 0; i < w.voices.size(); i++) {
            Enemy e = w.voices.get(i);
            if (e.voiceMet) continue;
            vajra.ship.Ship hull = w.nearestHull(e.x, e.y);
            float dx = hull.x - e.x, dy = hull.y - e.y;
            if (dx * dx + dy * dy > SIGHT_R * SIGHT_R) continue;
            e.voiceMet = true;
            switch (e.voiceName == null ? "SCOUT" : e.voiceName) {
                case "MAYA":
                    w.cue(Cue.VOICE_MAYA);
                    w.missions.openSalvage();
                    Codex.unlock(w, Codex.MAYA);
                    break;
                case "KALIYA":
                    w.cue(Cue.VOICE_KALIYA);
                    Codex.unlock(w, Codex.KALIYA);
                    break;
                case "SELLER":
                    w.cue(Cue.VOICE_SELLER);
                    Codex.unlock(w, Codex.SELLER);
                    break;
                case "MAID":
                    w.cue(Cue.VOICE_MAID);
                    Codex.unlock(w, Codex.MAID);
                    break;
                default: // THE SCOUT
                    w.cue(Cue.VOICE_SCOUT);
                    Codex.unlock(w, Codex.SCOUT);
            }
        }
    }

    /** The person called `name`, or null. */
    public static Enemy person(World w, String name) {
        for (Enemy e : w.voices) if (name.equals(e.voiceName)) return e;
        return null;
    }

    /** THE SCOUT'S BEACON: the band-2 cache answers. Said only if he was met first - every other cache
     *  pays without a word, exactly the no-announcement cache law. His report is also the gold brokers'
     *  cue: the rogue's card goes up on the board. */
    public static void beaconRaised(World w) {
        Codex.unlock(w, Codex.BEACON);
        Enemy scout = person(w, "SCOUT");
        if (scout == null || !scout.voiceMet) return;
        w.say(scout, "THE BEACON BURNS.", "EVERY CAMP IS ON YOUR COMPASS.");
        w.toast("THE BEACON BURNS", "THE WATCH SEES EVERY CAMP. +100c AND A LIFE.");
        w.missions.openBounty();
    }

    /** THE RETEMPERING: Maya's six wrecks are home. The codex page is Missions' doing. */
    public static void salvageDone(World w) {
        Enemy maya = person(w, "MAYA");
        if (maya != null && maya.voiceMet) w.say(maya, "THE HAMMER IS WARM.", "TAKE THE PURSE AND THE SHARD.");
        w.toast("THE HAMMER IS WARM", "SIX WRECKS HOME. +200c AND A SHARD.");
    }

    /** THE RULER FALLS: the book takes the ruler, the letter and the road below. Maya's letter is
     *  read aloud by the scene (Scenes, BALA_FALLS) when she was met; an unmet voice pays nothing. */
    public static void wardenFalls(World w) {
        Codex.unlock(w, Codex.BALA);
        Codex.unlock(w, Codex.FIFTH);
        Enemy maya = person(w, "MAYA");
        if (maya != null && maya.voiceMet) Codex.unlock(w, Codex.LETTER);
    }
}
