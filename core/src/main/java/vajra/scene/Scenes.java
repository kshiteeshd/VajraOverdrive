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
 *  else in a bubble coloured for who they are (see {@link Palette}). The register is that of the
 *  epics: plain, grave, a little formal, with no jest in it. Nobody explains what the player can see;
 *  a ruler speaks of the piece as of a voice he has obeyed too long.
 *
 *  <p>A cutscene is a plain list of Shots built from the world when it plays, so a subject is wherever
 *  it actually stands. Zoom below 1 magnifies (see GameScreen). */
public final class Scenes {
    static final float CLOSE = 0.5f, MID = 0.7f, WIDE = 0.85f, NEAR = 0.42f;

    private Scenes() {}

    /** Camera y that seats a subject at `y` below the middle of the view, so the bubble has sky. */
    static float frame(float y, float zoom) {
        return y + 100 * zoom;
    }

    public static List<Shot> build(Cue cue, World w) {
        List<Shot> s = new ArrayList<>();
        switch (cue) {
            case OPENING:
                s.add(narr(w, CLOSE, "WHEN TRIVIKRAMA'S THIRD STEP LIFTED FROM SWARGA, THE VAJRAYUDHA WAS GONE."));
                s.add(narr(w, CLOSE, "HE HAD BROKEN IT INTO SEVEN, AND LAID ONE PIECE IN EACH LOKA BELOW."));
                s.add(think(w, CLOSE, "THE SAGE DADHICHI GAVE HIS BONES TO MAKE THAT BOLT.", "I AM INDRA. WHAT WAS GIVEN IN SACRIFICE WILL BE RETURNED."));
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
                if (w.boss != null) s.add(ruler(w, w.boss, CLOSE, "LET THEM STAND ASIDE.", "I WILL ANSWER THIS ONE MYSELF."));
                s.add(think(w, CLOSE, "THE PIECE IS HUMMING IN HIS HAND.", "IT KNOWS ME."));
                break;
            case GAZE_NOTICED:
                if (w.stage == 1) {
                    s.add(pit(w, MID, 2.0f, "FAR AHEAD, THE SECOND PIECE GROWS WARM IN BHAIRAVA'S HAND.",
                            "IT TELLS HIM EACH TIME VITALA BURNS."));
                    s.add(think(w, CLOSE, "THE GOLD IS WARM TO MY HAND TOO.", "THE PIECE IS LISTENING.").pan(2.0f));
                    break;
                }
                s.add(pit(w, MID, 2.0f, "FAR AHEAD, THE PIECE GROWS WARM IN BALA'S HAND.",
                        "IT TELLS HIM EACH TIME ATALA LOSES A SOLDIER."));
                s.add(think(w, CLOSE, "IT HAS FELT ME COMING.", "I CAN FEEL IT, TOO.").pan(2.0f));
                break;
            case GAZE_WATCHING:
                if (w.stage == 1) {
                    s.add(pit(w, MID, 2.0f, "THE FORGES FALL SILENT, ONE BY ONE."));
                    s.add(new Shot(w.pitX, frame(w.pitY, MID), MID, "BHAIRAVA", false,
                            "A HAND IS QUENCHING MY FIRES.",
                            "THE PIECE SAYS ALL THAT BURNS IS MINE. LET THE INTRUDER BURN.")
                            .speakerAt(w.pitX, w.pitY).tint(Palette.RULER[0], Palette.RULER[1], Palette.RULER[2]));
                    s.add(think(w, CLOSE, "HE WILL WAIT FOR ME IN THE ASH.").pan(2.0f));
                    break;
                }
                s.add(pit(w, MID, 2.0f, "THE LOWER BANDS GROW QUIET."));
                float[] gold = Palette.RULER;
                s.add(new Shot(World.PIT_X, frame(World.PIT_Y, MID), MID, "BALA", false,
                        "A STRANGER WALKS MY HALLS, AND MY HALLS GROW EMPTY.",
                        "THE PIECE SAYS ATALA IS MINE. LET HIM COME.")
                        .speakerAt(World.PIT_X, World.PIT_Y).tint(gold[0], gold[1], gold[2]));
                s.add(think(w, CLOSE, "HE KNOWS I AM COMING.").pan(2.0f));
                break;
            case GAZE_ROUSED:
                if (w.stage == 1) {
                    if (w.boss != null)
                        s.add(ruler(w, w.boss, MID, "ENOUGH. THE ASH REMEMBERS EACH ONE OF THEM.",
                                "THE LAST GATE STANDS OPEN. COME, AND BE CONSUMED.").pan(2.0f));
                    s.add(think(w, CLOSE, "HARA, FORGIVE ME. I HAVE COME FOR THE PIECE.").pan(2.0f));
                    break;
                }
                if (w.boss != null)
                    s.add(ruler(w, w.boss, MID, "ENOUGH. I FELT EACH OF THEM FALL.",
                            "THE LAST GATE IS OPEN, AND I STAND IN IT.").pan(2.0f));
                s.add(think(w, CLOSE, "SON OF MAYA. I HAVE COME FOR THE PIECE.").pan(2.0f));
                break;
            case VITALA_ENTER:
                s.add(narr(w, CLOSE, "BALA'S GATE OPENS ON VITALA: BLACK STONE, AND A RIVER OF LIQUID GOLD."));
                s.add(narr(w, CLOSE, "THIS IS THE HATAKI. FIRE DRINKS IT AND BECOMES HATAKA GOLD. HARA AND GAURI KEEP THIS LOKA."));
                s.add(think(w, CLOSE, "THE SECOND PIECE IS IN BHAIRAVA'S HAND.", "THE RIVER WILL CARRY ME UP TO HIM."));
                break;
            case VOICE_SELLER:
                voiceSeller(w, s);
                break;
            case VOICE_MAID:
                voiceMaid(w, s);
                break;
            case BHAIRAVA_WAKES:
                if (w.boss != null) {
                    s.add(narrAt(w.boss, MID, "THE ALTAR OF VITALA. THE ASH HERE HAS BEEN FALLING FOR AN AGE."));
                    s.add(ruler(w, w.boss, CLOSE, "A LIVING MAN AT THE CREMATION GROUND.", "THAT IS NEW.").pan(1.2f));
                    s.add(indraSays(w, CLOSE, "I AM INDRA. I HAVE COME FOR THE PIECE OF THE VAJRA YOU HOLD.").pan(1.1f));
                    s.add(ruler(w, w.boss, CLOSE, "THE FIRE TOLD ME NO ONE WOULD COME.", "IT TOLD ME THAT WHOEVER CAME WOULD BURN.").pan(1.1f));
                    s.add(indraSays(w, CLOSE, "THEN THE FIRE HAS LIED TO YOU.").pan(1.1f));
                    s.add(ruler(w, w.boss, NEAR, "THEN LET US SEE WHICH OF US IT HAS LIED TO.").pan(1.1f));
                }
                s.add(think(w, CLOSE, "THE THIRD EYE IS OPENING."));
                break;
            case BHAIRAVA_FALLS:
                yieldScene(w, s, new String[][] {
                        {"THE THIRD EYE CLOSED. BHAIRAVA KNELT IN HIS OWN ASH,", "AND THE SMOKE FELL STILL ABOUT HIM."},
                        {"A THOUSAND PILGRIMS CAME TO THIS GROUND.", "I CANNOT RECALL ONE NAME."},
                        {"THE PIECE TOLD YOU THAT ALL THAT BURNS WAS YOURS."},
                        {"I BELIEVED IT. IT NEVER ONCE WARMED ME."},
                        {"BHAIRAVA: KEEPER OF THE GROUND WHERE THE DEAD ARE LET GO."},
                        {"THEN LET ME LET GO.", "TELL GAURI HER LAMPS MAY BE LIT AGAIN."},
                        {"I WILL TELL HER."},
                }, "TWO PIECES. FIVE REMAIN.",
                        new String[] {"THE SECOND PIECE ANSWERS THE HAND THAT SEEKS IT.", "BEYOND THE GOLD GATE, THE WATERS OF SUTALA WAIT."}, null);
                break;
            case BALA_FALLS:
                yieldScene(w, s, new String[][] {
                        {"THE LAST STROKE FELL. BALA DID NOT FALL WITH IT.", "HE KNELT, AND THE MANDALA OF HIS ILLUSIONS WENT DARK."},
                        {"...IT WAS NEVER MINE."},
                        {"NO. IT WAS NEVER YOURS."},
                        {"FOR AN AGE IT TOLD ME THAT ATALA WAS MY HOUSE.", "I NEVER ASKED WHOSE VOICE IT WAS."},
                        {"IT HAS ONE VOICE ONLY: THE HOLDER'S OWN WISH.", "YOU HAVE BEEN LISTENING TO YOURSELF."},
                        {"THEN I HAVE BEEN A KING OF NOTHING."},
                        {"YOU HAVE BEEN A DOOR.", "A DOOR EXISTS TO BE OPENED."},
                        {"TAKE IT. MAY IT BE LIGHT IN YOUR HAND,", "AS IT NEVER WAS IN MINE."},
                }, "ONE PIECE. SIX REMAIN.",
                        new String[] {"THE FIRST PIECE ANSWERS THE HAND THAT SEEKS IT.", "BEYOND THE GOLD GATE, VITALA BURNS."},
                        mayaMet(w) ? new String[] {"A LETTER COMES UP FROM MAYA VANA:", "\"HE HELD THE DOOR TOO LONG. GO DEEPER, INDRA. THE HAMMER IS WAITING.\""} : null);
                break;
        }
        return s;
    }

    /** What happens when the scene is over, however it ended (read to the end, skipped, or never
     *  staged because a shared race has no pause to give). */
    public static void finish(Cue cue, World w) {
        if (cue == Cue.ATALA_GATE) releaseGuards(w);
        if (cue == Cue.BALA_FALLS || cue == Cue.BHAIRAVA_FALLS) w.bossReleases();   // the scene is over, however it ended: he lets go
    }

    private static boolean mayaMet(World w) {
        Enemy m = Chatter.person(w, "MAYA");
        return m != null && m.voiceMet;
    }

    // --- a ruler yields: the camera goes back and forth between the ruler and the pilot ----------

    /** THE RULER YIELDS. `talk` is the scene in order: the first entry is the narrator over the fallen ruler; after it the lines go ruler, pilot, ruler, pilot... (the pilot's lines are
     *  the odd positions after the narration - entries 2, 4, 6...), the last pair shot nearer. The camera belongs to whoever speaks: it crosses to the ruler where he kneels and back to the hull
     *  with every turn of the talk. Then the pilot's own thought, and the narrator's closing lines (and a letter, if one has come). */
    private static void yieldScene(World w, List<Shot> s, String[][] talk, String thought, String[] closing, String[] letter) {
        Enemy r = w.yielder != null ? w.yielder : w.boss;
        if (r != null) s.add(narrAt(r, MID, talk[0]));
        for (int i = 1; i < talk.length; i++) {
            boolean ruler = i % 2 == 1;                                       // the ruler speaks first, then the pilot, turn and turn about
            float zoom = i >= talk.length - 2 ? NEAR : CLOSE;                 // and the last exchange is closer
            if (ruler && r != null) s.add(ruler(w, r, zoom, talk[i]).pan(1.1f));
            else s.add(indraSays(w, zoom, talk[i]).pan(1.1f));
        }
        s.add(think(w, CLOSE, thought).pan(1.0f));
        if (letter != null) s.add(narr(w, CLOSE, letter));
        s.add(narr(w, CLOSE, closing));
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
                "ATALA, THE FIRST LOKA BELOW THE EARTH. FOUR OF BALA'S GUARD STEP FROM THE ARCH.")
                .walk(g, ringX(w, g.length), ringY(w, g.length)));
        if (g.length < 4) return; // a thinned gate has nobody left to ask
        s.add(soldier(g[0], "SENTRY", cx, cy, "HALT. WHO COMES TO THE GATE OF BALA?"));
        s.add(indra(w, cx, cy, "INDRA, LORD OF SWARGA."));
        s.add(soldier(g[2], "ESCORT", cx, cy, "INDRA. THEN WHERE IS YOUR VAJRA?"));
        s.add(indra(w, cx, cy, "BROKEN.", "ONE PIECE OF IT LIES IN THIS LOKA."));
        s.add(soldier(g[1], "SENTRY", cx, cy, "WHAT BALA HOLDS, BALA KEEPS."));
        s.add(soldier(g[3], "ESCORT", cx, cy, "THE PIECE HAS TOLD HIM THAT ATALA IS HIS."));
        s.add(indra(w, cx, cy, "THE PIECE SPEAKS NO TRUTH.", "IT ONLY ANSWERS THE WISH OF THE ONE WHO HOLDS IT."));
        s.add(soldier(g[0], "SENTRY", cx, cy, "THEN THE ORDER STANDS. DRAW."));
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
        s.add(person(v, "THE SCOUT", cx, cy, "INDRA. I SERVE THE WATCH OF KING UPARICHARA,", "WHOSE CHARIOT WAS YOUR GIFT."));
        s.add(indra(w, cx, cy, "WHAT DOES THE WATCH SEE?"));
        s.add(person(v, "THE SCOUT", cx, cy, "THE KING'S SEAL IS BROKEN. MY BEACON STANDS ABOVE THE THIRD GATE.",
                "KINDLE IT, AND EVERY CAMP OF ATALA SHOWS ON YOUR COMPASS."));
        s.add(indra(w, cx, cy, "I WILL KINDLE IT."));
    }

    private static void voiceMaya(World w, List<Shot> s) {
        Enemy v = Chatter.person(w, "MAYA");
        if (v == null) return;
        float cx = (v.x + w.ship.x) / 2, cy = frame((v.y + w.ship.y) / 2, WIDE);
        s.add(person(v, "THE NINTH MAYA", cx, cy, "I AM THE NINTH OF THE MAKER'S LINE.", "I PUT DOWN THE HAMMER LONG AGO."));
        s.add(indra(w, cx, cy, "THE LINE OF MAYA, ARCHITECT OF THE ASURAS."));
        s.add(person(v, "THE NINTH MAYA", cx, cy, "MY SON HOLDS THE GATE OF ATALA, AND WITH IT A PIECE OF YOUR BOLT.",
                "SIX WRECKS OF MY WOODS LIE SCATTERED. BRING THEM HOME."));
        s.add(person(v, "THE NINTH MAYA", cx, cy, "WHEN THEY ARE HOME, I WILL TEMPER YOU."));
    }

    private static void voiceKaliya(World w, List<Shot> s) {
        Enemy v = Chatter.person(w, "KALIYA");
        if (v == null) return;
        float cx = (v.x + w.ship.x) / 2, cy = frame((v.y + w.ship.y) / 2, WIDE);
        s.add(person(v, "KALIYA", cx, cy, "I AM KALIYA, PILOT OF THE WATER ROAD.", "THE TIDE IS OPEN TO YOU."));
        s.add(indra(w, cx, cy, "A NAGA, SO FAR FROM MAHATALA?"));
        s.add(person(v, "KALIYA", cx, cy, "GARUDA'S SHADOW DROVE MY KIN UP THE ROAD.",
                "CARRY MY NAME TO TAKSHAKA, WHO WAITS BELOW."));
    }

    private static void voiceSeller(World w, List<Shot> s) {
        Enemy v = Chatter.person(w, "SELLER");
        if (v == null) return;
        float cx = (v.x + w.ship.x) / 2, cy = frame((v.y + w.ship.y) / 2, WIDE);
        s.add(person(v, "THE GOLD-SELLER", cx, cy, "I AM A BHUTA OF HARA'S COURT.", "I WEIGH THE GOLD OF HATAKA."));
        s.add(indra(w, cx, cy, "WHAT USE HAS A SPIRIT FOR GOLD?"));
        s.add(person(v, "THE GOLD-SELLER", cx, cy, "NONE. I ONLY KNOW WHERE IT LIES.",
                "EVERY VAULT IN THESE WALLS IS ON MY SCALE."));
        s.add(person(v, "THE GOLD-SELLER", cx, cy, "WATCH YOUR COMPASS. IT WILL POINT THE WAY."));
    }

    private static void voiceMaid(World w, List<Shot> s) {
        Enemy v = Chatter.person(w, "MAID");
        if (v == null) return;
        float cx = (v.x + w.ship.x) / 2, cy = frame((v.y + w.ship.y) / 2, WIDE);
        s.add(person(v, "GAURI'S MAID", cx, cy, "I TEND THE LAMPS OF GAURI.", "LIGHT ONE, AND THE FIRE HOLDS ITS BREATH."));
        s.add(indra(w, cx, cy, "FOR HOW LONG?"));
        s.add(person(v, "GAURI'S MAID", cx, cy, "SIX BREATHS: ENOUGH TO CROSS A VENT.",
                "HER WELLS MEND WHAT THE FIRE BREAKS."));
    }

    // --- the pit --------------------------------------------------------------------------

    /** The camera goes to the empty last band, where the ruler is - or will be. */
    private static Shot pit(World w, float zoom, float panSecs, String... lines) {
        return new Shot(w.pitX, w.pitY, zoom, null, false, lines).pan(panSecs);
    }

    // --- speakers -------------------------------------------------------------------------

    /** The narrator, framed on the pilot. */
    private static Shot narr(World w, float zoom, String... lines) {
        return new Shot(w.ship.x, frame(w.ship.y, zoom), zoom, null, false, lines);
    }

    /** The narrator, framed on someone else (the ruler where he knelt). */
    private static Shot narrAt(Enemy who, float zoom, String... lines) {
        return new Shot(who.x, frame(who.y, zoom), zoom, null, false, lines).follow(who).pan(1.4f);
    }

    /** Indra thinking aloud, alone in the frame. */
    private static Shot think(World w, float zoom, String... lines) {
        float[] c = Palette.INDRA;
        return new Shot(w.ship.x, frame(w.ship.y, zoom), zoom, "INDRA", true, lines)
                .speakerAt(w.ship.x, w.ship.y).tint(c[0], c[1], c[2]);
    }

    /** Indra speaking aloud to someone, the camera on him. */
    private static Shot indraSays(World w, float zoom, String... lines) {
        float[] c = Palette.INDRA;
        return new Shot(w.ship.x, frame(w.ship.y, zoom), zoom, "INDRA", false, lines)
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
    private static Shot ruler(World w, Enemy who, float zoom, String... lines) {
        float[] c = Palette.RULER;
        return new Shot(who.x, frame(who.y, zoom), zoom, w.stage == 0 ? "BALA" : "BHAIRAVA", false, lines).follow(who).tint(c[0], c[1], c[2]);
    }
}
