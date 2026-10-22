package vajra.world;

import vajra.enemy.Enemy;

/** THE VOICE RAIL — what the people on the map say, keyed by event, once per run.
 *  The copy standard is Bala's ("STAND DOWN. THIS ONE IS MINE."): short, unflattered, no
 *  thanks. Speech fires through the Popups pool like every other word in the game; the
 *  once-per-run flags live on the body itself (`voiceMet`), so a run owns nothing on exit.
 *
 *  <p>Chatter never holds the gate: the premise chain (quota seal, warden, band) runs exactly
 *  as before, and a voice adds only the WHY. Long line-wraps stay under the boss's longest. */
public final class Chatter {
    static final float SIGHT_R = 260; // how close a hull reads a face, px

    private Chatter() {}

    /** Run every frame from World.update: the first approach to any voice is its speech. */
    public static void update(World w) {
        for (int i = 0; i < w.voices.size(); i++) {
            Enemy e = w.voices.get(i);
            if (e.voiceMet) continue;
            vajra.ship.Ship hull = w.nearestHull(e.x, e.y);
            float dx = hull.x - e.x, dy = hull.y - e.y;
            if (dx * dx + dy * dy > SIGHT_R * SIGHT_R) continue;
            e.voiceMet = true;
            speak(w, e, "I FELL ABOVE THE THIRD BAND - NO RESCUE RUNS THIS DEEP.");
            speak(w, e, "MY BEACON LIES JUST INSIDE THE GATE ABOVE. RAISE IT.");
        }
    }

    /** THE SCOUT'S BEACON: the band-2 cache answers. Spoken only if he was met first —
     *  every other cache pays without a word, exactly the no-announcement cache law. */
    public static void beaconRaised(World w) {
        for (int i = 0; i < w.voices.size(); i++) {
            Enemy e = w.voices.get(i);
            if (e.voice && e.voiceMet) {
                speak(w, e, "THE BEACON IS UP - TAKE WHAT IT PAYS, AND MY REPORT RIDES ON.");
            }
        }
    }

    /** One line, said as a person says it: from their hull, calm cyan, a long life so it
     *  can be read mid-fight. */
    static void speak(World w, Enemy from, String line) {
        w.popups.push(new Popups.Spec().text(line)
                .at(from.x, from.y + 46).life(3.2f).rise(6)
                .color(0.55f, 0.95f, 1f).scale(1.15f));
    }
}
