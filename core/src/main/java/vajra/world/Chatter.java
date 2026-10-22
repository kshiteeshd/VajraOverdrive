package vajra.world;

import vajra.enemy.Enemy;

/** THE VOICE RAIL — what the people on the map say, keyed by event, once per run.
 *  The copy standard is Bala's ("STAND DOWN. THIS ONE IS MINE."): short, unflattered, no
 *  thanks. Speech fires through the Popups pool like every other word in the game; the
 *  once-per-run flags live on the body itself (`voiceMet`), so a run owns nothing on exit.
 *
 *  <p>Chatter never holds the gate: the premise chain (quota seal, warden, band) runs exactly
 *  as before, and a voice adds only the WHY. */
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
            switch (e.voiceName == null ? "SCOUT" : e.voiceName) {
                case "MAYA":
                    speak(w, e, "I AM THE NINTH MAYA, AND I PUT THE HAMMER DOWN YEARS AGO.");
                    speak(w, e, "MY SON HOLDS THE GATE BELOW WITH MY LIGHT IN HIS HAND. WHEN HE FALLS, MY LETTER IS YOURS.");
                    break;
                case "KALIYA":
                    speak(w, e, "KALIYA, TIDE-PILOT. THE WATER ROAD IS OPEN - THE CARAVAN WAITS BELOW.");
                    speak(w, e, "THE SPARK JOB LEAVES THIS BAND SOON. WHEN IT DOES, EVERY PILOT HEARS IT.");
                    break;
                default: // THE SCOUT
                    speak(w, e, "I FELL ABOVE THE THIRD BAND - NO RESCUE RUNS THIS DEEP.");
                    speak(w, e, "MY BEACON LIES JUST INSIDE THE GATE ABOVE. RAISE IT.");
            }
        }
    }

    /** THE SCOUT'S BEACON: the band-2 cache answers. Spoken only if he was met first —
     *  every other cache pays without a word, exactly the no-announcement cache law.
     *  His report is also the gold brokers' cue: the rogue's card goes up on the board. */
    public static void beaconRaised(World w) {
        for (int i = 0; i < w.voices.size(); i++) {
            Enemy e = w.voices.get(i);
            if (!e.voice || !e.voiceMet || !"SCOUT".equals(e.voiceName)) continue;
            speak(w, e, "THE BEACON IS UP - TAKE WHAT IT PAYS, AND MY REPORT RIDES ON.");
            w.missions.openBounty();
        }
    }

    /** THE WARDEN FALLS: whatever a voice promised at the fall is paid here — Maya's letter
     *  (LETTER II), and nothing for a voice not met. The victory card's own law is untouched. */
    public static void wardenFalls(World w) {
        for (int i = 0; i < w.voices.size(); i++) {
            Enemy e = w.voices.get(i);
            if (e.voiceMet && "MAYA".equals(e.voiceName)) {
                speak(w, e, "SON OF MINE HELD THE DOOR WITH A LIGHT IN HIS HAND THAT TOOK IT FOR A LAMP.");
                speak(w, e, "HE STOOD YOU DOWN WITH HONOR, AND PUT HIS BACK IN FRONT OF IT TILL THE WORLD CAME THROUGH HIM ANYWAY.");
                speak(w, e, "GO DEEPER, PILOT. THE HAMMER IS STILL IN MY FORGE - COME BACK FOR IT.");
            }
        }
    }

    /** One line, said as a person says it: from their hull, calm, a long life so it
     *  can be read mid-fight. */
    static void speak(World w, Enemy from, String line) {
        w.popups.push(new Popups.Spec().text(line)
                .at(from.x, from.y + 46).life(3.2f).rise(6)
                .color(0.55f, 0.95f, 1f).scale(1.15f));
    }
}
