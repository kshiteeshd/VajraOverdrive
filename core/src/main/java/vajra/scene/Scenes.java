package vajra.scene;

import java.util.ArrayList;
import java.util.List;
import vajra.enemy.Enemy;
import vajra.world.Chatter;
import vajra.world.Cue;
import vajra.world.World;

/** THE SCRIPT. Every scene of the game, in the story's own words. The pilot is INDRA, king of the
 *  gods, come down from Swarga because TRIVIKRAMA's third step broke the VAJRAYUDHA - the bolt made
 *  of the sage DADHICHI's bones - into seven pieces and hid one in each loka below. The lokas' rulers
 *  hold the pieces, and a piece tells whoever holds it that the world around them is theirs.
 *
 *  <p>The rules of the page: lines are SHORT (a bubble holds one breath, the band holds two); every
 *  scene turns on something the audience is waiting for; the narrator speaks in the band, everyone
 *  else in a bubble coloured for who they are (see {@link Palette}).
 *
 *  <p>A cutscene is a plain list of Shots built from the world when it plays, so a subject is wherever
 *  it actually stands. Zoom below 1 magnifies (see GameScreen). */
public final class Scenes {
    static final float CLOSE = 0.5f, MID = 0.7f, WIDE = 0.85f;

    private Scenes() {}

    /** Camera y that seats a subject at `y` below the middle of the view, so the bubble has sky. */
    static float frame(float y, float zoom) {
        return y + 100 * zoom;
    }

    public static List<Shot> build(Cue cue, World w) {
        List<Shot> s = new ArrayList<>();
        switch (cue) {
            case OPENING:
                s.add(narr(w, CLOSE, "THE THIRD STEP FELL ON SWARGA. WHEN IT LIFTED, THE VAJRAYUDHA WAS GONE."));
                s.add(narr(w, CLOSE, "TRIVIKRAMA BROKE IT IN SEVEN, AND HID ONE PIECE IN EACH LOKA BELOW."));
                s.add(think(w, CLOSE, "DADHICHI GAVE HIS BONES FOR THAT BOLT.", "I AM INDRA. I AM TAKING IT BACK."));
                break;
            case ATALA_GATE:
                gateParley(w, s);
                break;
            case VOICE_SCOUT:
                voiceScout(w, s);
                break;
            case VOICE_MAYA:
                voiceMaya(w, s);
                break;
            case VOICE_KALIYA:
                voiceKaliya(w, s);
                break;
            case BALA_WAKES:
                if (w.boss != null) s.add(ruler(w.boss, CLOSE, "STAND DOWN. THIS ONE IS MINE."));
                s.add(think(w, CLOSE, "I CAN HEAR IT HUM IN HIS HAND."));
                break;
            case GAZE_NOTICED:
                s.add(pit(MID, 2.0f, "FAR ABOVE, IN BALA'S HAND, A PIECE OF THE VAJRA GROWS WARM.",
                        "IT TELLS HIM EVERY TIME ATALA BLEEDS."));
                s.add(think(w, CLOSE, "I FEEL IT. IT FEELS ME.").pan(2.0f));
                break;
            case GAZE_WATCHING:
                s.add(pit(MID, 2.0f, "THE LOWER BANDS GO QUIET."));
                float[] gold = Palette.RULER;
                s.add(new Shot(World.PIT_X, frame(World.PIT_Y, MID), MID, "BALA", false,
                        "SOMEONE IS EMPTYING MY HOUSE.",
                        "THE PIECE SAYS IT IS ALL MINE. LET THEM COME.")
                        .speakerAt(World.PIT_X, World.PIT_Y).tint(gold[0], gold[1], gold[2]));
                s.add(think(w, CLOSE, "HE KNOWS I AM COMING.").pan(2.0f));
                break;
            case GAZE_ROUSED:
                if (w.boss != null)
                    s.add(ruler(w.boss, MID, "ENOUGH. I FELT EVERY ONE OF THEM GO.",
                            "THE LAST DOOR IS OPEN. I AM STANDING IN IT.").pan(2.0f));
                s.add(think(w, CLOSE, "SON OF MAYA. I AM COMING FOR IT.").pan(2.0f));
                break;
            case BALA_FALLS:
                if (w.boss != null)
                    s.add(ruler(w.boss, CLOSE, "TAKE IT. IT NEVER MADE ME A KING.", "IT ONLY MADE ME A DOOR."));
                s.add(think(w, CLOSE, "ONE PIECE. SIX TO GO."));
                if (mayaMet(w))
                    s.add(narr(w, CLOSE, "A LETTER, CARRIED UP FROM MAYA VANA: HE HELD THE DOOR TOO LONG.",
                            "GO DEEPER, INDRA. THE HAMMER WAITS IN MY FORGE."));
                s.add(narr(w, CLOSE, "THE FIRST PIECE ANSWERS. BELOW, VITALA BURNS GOLD."));
                break;
        }
        return s;
    }

    /** What happens when the scene is over, however it ended (read to the end, skipped, or never
     *  staged because a shared race has no pause to give). */
    public static void finish(Cue cue, World w) {
        if (cue == Cue.ATALA_GATE) releaseGuards(w);
    }

    private static boolean mayaMet(World w) {
        Enemy m = Chatter.person(w, "MAYA");
        return m != null && m.voiceMet;
    }

    // --- the grand gate: the first entry to a new loka ------------------------------------

    /** The four honor guards of the grand gate: two LANCE, two HATAKA, in spawn order. */
    static Enemy[] gateGuards(World w) {
        List<Enemy> g = new ArrayList<>();
        for (Enemy e : w.enemies) if (e.parley && !e.dead) g.add(e);
        return g.toArray(new Enemy[0]);
    }

    /** Where guard i stands once it has closed on the pilot: a loose arc above the hull. */
    private static float[] ring(World w, int i) {
        float[] dx = {-150, -55, 55, 150}, dy = {80, 140, 140, 80};
        return new float[] {w.ship.x + dx[i % 4], w.ship.y + dy[i % 4]};
    }

    private static void gateParley(World w, List<Shot> s) {
        Enemy[] g = gateGuards(w);
        float cx = w.ship.x, cy = frame(w.ship.y + 110, WIDE); // one framing for the whole talk
        s.add(new Shot(cx, cy, WIDE, null, false,
                "ATALA. THE FIRST DOOR BELOW THE EARTH. FOUR OF BALA'S GUARD DROP FROM THE ARCH.")
                .walk(g, ringX(w, g.length), ringY(w, g.length)));
        if (g.length < 4) return; // a thinned gate has nobody left to ask
        s.add(soldier(g[0], "SENTRY", cx, cy, "HOLD. WHO COMES TO BALA'S DOOR?"));
        s.add(indra(w, cx, cy, "INDRA, OF SWARGA."));
        s.add(soldier(g[2], "ESCORT", cx, cy, "INDRA. THEN WHERE IS YOUR BOLT?"));
        s.add(indra(w, cx, cy, "BROKEN. PART OF IT LIES BELOW YOU."));
        s.add(soldier(g[1], "SENTRY", cx, cy, "WHAT BALA HOLDS, BALA KEEPS."));
        s.add(soldier(g[3], "ESCORT", cx, cy, "THE PIECE SAYS ATALA IS HIS."));
        s.add(indra(w, cx, cy, "THEN THE PIECE LIES."));
        s.add(soldier(g[0], "SENTRY", cx, cy, "WEAPONS FREE."));
    }

    private static float[] ringX(World w, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) r[i] = ring(w, i)[0];
        return r;
    }

    private static float[] ringY(World w, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n; i++) r[i] = ring(w, i)[1];
        return r;
    }

    /** The talking is over: the guards stand where the scene left them and come awake. */
    private static void releaseGuards(World w) {
        Enemy[] g = gateGuards(w);
        for (int i = 0; i < g.length; i++) {
            float[] p = ring(w, i);
            g[i].x = g[i].homeX = p[0];
            g[i].y = g[i].homeY = p[1];
            g[i].parley = false;
            g[i].active = true;
        }
    }

    // --- the people: their first meetings -------------------------------------------------

    private static void voiceScout(World w, List<Shot> s) {
        Enemy v = Chatter.person(w, "SCOUT");
        if (v == null) return;
        float cx = (v.x + w.ship.x) / 2, cy = frame((v.y + w.ship.y) / 2, WIDE);
        s.add(person(v, "THE SCOUT", cx, cy, "INDRA?", "UPARICHARA'S WATCH. YOU GAVE OUR KING HIS WINGS."));
        s.add(indra(w, cx, cy, "WHAT DOES THE WATCH SEE?"));
        s.add(person(v, "THE SCOUT", cx, cy, "THE KING'S SEAL IS BROKEN. MY BEACON IS UP THE GATE.",
                "RAISE IT. YOU WILL SEE EVERY CAMP."));
        s.add(indra(w, cx, cy, "I WILL RAISE IT."));
    }

    private static void voiceMaya(World w, List<Shot> s) {
        Enemy v = Chatter.person(w, "MAYA");
        if (v == null) return;
        float cx = (v.x + w.ship.x) / 2, cy = frame((v.y + w.ship.y) / 2, WIDE);
        s.add(person(v, "THE NINTH MAYA", cx, cy, "I AM THE NINTH MAYA. I PUT THE HAMMER DOWN."));
        s.add(indra(w, cx, cy, "THE MAKER'S LINE."));
        s.add(person(v, "THE NINTH MAYA", cx, cy, "MY SON HOLDS THE DOOR. HE HOLDS YOUR BOLT.",
                "SIX WRECKS OF MY WOODS. BRING THEM HOME."));
        s.add(person(v, "THE NINTH MAYA", cx, cy, "I WILL TEMPER YOU."));
    }

    private static void voiceKaliya(World w, List<Shot> s) {
        Enemy v = Chatter.person(w, "KALIYA");
        if (v == null) return;
        float cx = (v.x + w.ship.x) / 2, cy = frame((v.y + w.ship.y) / 2, WIDE);
        s.add(person(v, "KALIYA", cx, cy, "KALIYA. TIDE-PILOT. THE WATER ROAD IS OPEN."));
        s.add(indra(w, cx, cy, "A NAGA, THIS FAR FROM MAHATALA?"));
        s.add(person(v, "KALIYA", cx, cy, "GARUDA'S SHADOW DROVE MY KIN UP THE ROAD.",
                "SAY MY NAME TO TAKSHAKA, BELOW."));
    }

    // --- the pit --------------------------------------------------------------------------

    /** The camera goes to the empty last band, where the ruler is - or will be. */
    private static Shot pit(float zoom, float panSecs, String... lines) {
        return new Shot(World.PIT_X, World.PIT_Y, zoom, null, false, lines).pan(panSecs);
    }

    // --- speakers -------------------------------------------------------------------------

    /** The narrator, framed on the pilot. */
    private static Shot narr(World w, float zoom, String... lines) {
        return new Shot(w.ship.x, frame(w.ship.y, zoom), zoom, null, false, lines);
    }

    /** Indra thinking aloud, alone in the frame. */
    private static Shot think(World w, float zoom, String... lines) {
        float[] c = Palette.INDRA;
        return new Shot(w.ship.x, frame(w.ship.y, zoom), zoom, "INDRA", true, lines)
                .speakerAt(w.ship.x, w.ship.y).tint(c[0], c[1], c[2]);
    }

    /** Indra answering inside a fixed framing: the bubble hangs BELOW the hull, the others are above. */
    private static Shot indra(World w, float cx, float cy, String... lines) {
        float[] c = Palette.INDRA;
        return new Shot(cx, cy, WIDE, "INDRA", true, lines).speakerAt(w.ship.x, w.ship.y).below()
                .tint(c[0], c[1], c[2]);
    }

    /** A soldier speaking inside a fixed framing. */
    private static Shot soldier(Enemy who, String name, float cx, float cy, String... lines) {
        float[] c = Palette.SOLDIER;
        return new Shot(cx, cy, WIDE, name, false, lines).follow(who).tint(c[0], c[1], c[2]);
    }

    /** One of the people of the loka, in sky blue. */
    private static Shot person(Enemy who, String name, float cx, float cy, String... lines) {
        float[] c = Palette.PERSON;
        return new Shot(cx, cy, WIDE, name, false, lines).follow(who).tint(c[0], c[1], c[2]);
    }

    /** The ruler, in gold, close. */
    private static Shot ruler(Enemy who, float zoom, String... lines) {
        float[] c = Palette.RULER;
        return new Shot(who.x, frame(who.y, zoom), zoom, "BALA", false, lines).follow(who).tint(c[0], c[1], c[2]);
    }
}
