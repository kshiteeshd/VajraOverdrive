package vajra.coach;

import java.util.ArrayList;
import java.util.List;

/** THE PRIMER: every first time the game asks of a pilot, in the order the world meets it.
 *  Ids are stable and index the profile's learned-book - append, never renumber.
 *
 *  <p>THRUST and FIRE are taught in the run itself (see Coach); the rest are read here, and
 *  each carries `when` - the moment a contextual hint should fire for it. */
public final class Lessons {
    public static final int THRUST = 0, NOSE = 1, FIRE = 2, BOOST = 3, DASH = 4, SHIELD = 5,
            WEAPONS = 6, ASTRA = 7, HULL = 8, CHECKPOINT = 9, PICKUPS = 10, HANGAR = 11,
            HULLS = 12, GATES = 13, VOICES = 14, WARDEN = 15, CODEX = 16, PAUSE = 17;
    public static final int COUNT = 18;

    public static final String[] GROUPS = {"FLIGHT", "COMBAT", "SURVIVAL", "WORLD", "SYSTEMS"};

    public static final Lesson[] ALL = new Lesson[COUNT];

    static {
        add(THRUST, "FLIGHT", "THRUST", keys("W", "A", "S", "D"),
                lines("W IS ALWAYS UP AND D ALWAYS RIGHT, WHEREVER THE NOSE POINTS.",
                        "THRUST NEVER TURNS THE SHIP. IT SLIDES."),
                "THE FIRST BREATH OF EVERY NEW PILOT");
        add(NOSE, "FLIGHT", "THE NOSE", keys("Q", "/", "E"),
                lines("Q TURNS THE NOSE COUNTER-CLOCKWISE, E CLOCKWISE.",
                        "HOLD THE LEFT MOUSE BUTTON AND THE NOSE FOLLOWS THE CURSOR.",
                        "LET GO AND IT KEEPS ITS HEADING."),
                "AFTER THE FIRST THRUST");
        add(BOOST, "FLIGHT", "BOOST", keys("SHIFT"),
                lines("HOLD WHILE THRUSTING FOR SPEED. THE ENERGY METER PAYS FOR IT.",
                        "EMPTY, THE HULL FLIES AT ITS OWN PACE AGAIN."),
                "THE FIRST OPEN STRETCH OF SKY");
        add(DASH, "FLIGHT", "DASH", keys("C"),
                lines("A SHORT LEAP IN THE DIRECTION YOU PUSH - ALONG THE NOSE IF YOU HOLD NONE.",
                        "0.4 S OF GHOST: SHOTS PASS THROUGH THE HULL.",
                        "THE COOLDOWN STARTS WHEN THE SMEAR ENDS."),
                "THE FIRST ENEMY ROUND IN FLIGHT");

        add(FIRE, "COMBAT", "FIRE", keys("SPACE", "/", "LMB"),
                lines("SPACE FIRES ALONG THE NOSE.",
                        "THE LEFT MOUSE BUTTON ALSO SWINGS THE NOSE ONTO THE CURSOR."),
                "RIGHT AFTER THE FIRST THRUST");
        add(WEAPONS, "COMBAT", "WEAPONS", keys("V", "/", "B"),
                lines("V CYCLES TO THE NEXT WEAPON YOU OWN, B TO THE PREVIOUS.",
                        "NEW PATTERNS ARE BOUGHT IN THE HANGAR."),
                "THE FIRST PATTERN BOUGHT");
        add(SHIELD, "COMBAT", "SHIELD", keys("G", "/", "RMB"),
                lines("DRAINS WHILE HELD. WHAT ACTUALLY LANDS ON IT SETS THE COST.",
                        "A SLOW RING IS CHEAP TO EAT. A SHELL IS NOT."),
                "THE FIRST HEAVY SHELL");
        add(ASTRA, "COMBAT", "THE AKASHA ASTRA", keys("F"),
                lines("FIVE SHARDS FORGE IT. F CHARGES FOR 1.75 S, THEN RELEASES.",
                        "TWENTY CUBES LEAVE THE HULL, EACH HUNTING ITS OWN ENEMY THROUGH ROCK.",
                        "EVERY LOKA FORGES ITS OWN, AND YOU CARRY THEM ALL. X CHOOSES WHICH ONE F SPENDS."),
                "THE FIRST FORGED CHARGE");

        add(HULL, "SURVIVAL", "HULL AND LIVES", keys(),
                lines("THE HULL BAR IS THE SHIP. WHEN IT EMPTIES, A LIFE GOES.",
                        "THREE LIVES AT THE START, FIVE AT MOST. NO LIVES, THE RUN ENDS."),
                "THE FIRST HIT TAKEN");
        add(CHECKPOINT, "SURVIVAL", "CHECKPOINTS", keys(),
                lines("EACH BAND HOLDS ONE. FLY THROUGH IT TO BIND IT.",
                        "A LOST LIFE WAKES YOU AT THE LAST ONE BOUND."),
                "THE FIRST ONE IN SIGHT");
        add(PICKUPS, "SURVIVAL", "COINS, SHARDS, REPAIR", keys(),
                lines("EVERYTHING THAT FALLS FROM A KILL IS YOURS IF YOU FLY INTO IT.",
                        "COINS BUY. SHARDS FORGE THE ASTRA. A HEALTH DROP MENDS THE HULL."),
                "THE FIRST KILL");

        add(HANGAR, "WORLD", "THE HANGAR", keys("TAB", "/", "ENTER"),
                lines("FLY INTO A SHOP BAY AND HOLD STILL. THE DOCK TAKES THE HULL.",
                        "TAB CHANGES RAIL: HULLS, ARMS (THE WHEEL), ASTRA. A/D MOVE, ENTER BUYS OR FITS.",
                        "ENTER ON THE PATTERN IN YOUR GUNS TEMPERS IT: RANK II HITS HARDER, RANK III ADDS AN ABILITY.",
                        "AN UNBOUGHT HULL OR TIER IS A LOCK - ITS LOOK IS YOURS ONCE YOU PAY. ESC LEAVES."),
                "THE FIRST SHOP BAY");
        add(HULLS, "WORLD", "HULLS", keys("1", "-", "7"),
                lines("A NUMBER FITS THAT HULL - IF YOU OWN IT THIS RUN.",
                        "Z CYCLES TO THE NEXT HULL YOU OWN."),
                "THE FIRST HULL BOUGHT");
        add(GATES, "WORLD", "GATES AND BANDS", keys(),
                lines("EACH BAND IS A CAVERN, EACH GATE HAS ONE DOORWAY ON THE CENTRE LINE.",
                        "THE FIRST CROSSING OF A BAND PAYS ONCE. THE GRAND GATE IS GUARDED BY FOUR WHO ASK FIRST."),
                "THE GRAND GATE");
        add(VOICES, "WORLD", "VOICES", keys(),
                lines("SOME HULLS ARE PEOPLE. THEY NEVER SHOOT AND CANNOT BE SHOT.",
                        "FLY NEAR ONE AND IT SPEAKS ONCE. THEY ASK FOR WORK, AND PAY."),
                "THE FIRST FRIENDLY HULL");
        add(WARDEN, "WORLD", "THE RULER", keys(),
                lines("A PIECE OF THE BOLT TELLS ITS HOLDER THE WORLD IS HIS. BALA FEELS HIS PEOPLE FALL.",
                        "THE GAZE BAR UNDER THE SCORE SHOWS HOW CLOSELY HE WATCHES. FULL, HE WAITS IN THE LAST BAND.",
                        "HE LEARNS WHAT YOU DODGE."),
                "ENTERING THE BOSS BAND");

        add(CODEX, "SYSTEMS", "THE CODEX", keys("ESC"),
                lines("PAUSE, THEN CODEX. PAGES LIFT AS YOU MEET THE WORLD.",
                        "A PAGE SEEN ONCE IS YOURS FOR GOOD."),
                "THE FIRST PAGE LIFTED");
        add(PAUSE, "SYSTEMS", "PAUSE AND FULLSCREEN", keys("ESC", "/", "F11"),
                lines("ESC FREEZES THE RUN. F11 TOGGLES FULLSCREEN, F12 SWAPS THE WINDOW SIZE.",
                        "R STARTS A NEW RUN AFTER THE LAST ONE ENDS."),
                "THE FIRST PAUSE");
    }

    private Lessons() {}

    /** The lessons of one group, in teaching order. */
    public static List<Lesson> inGroup(String group) {
        List<Lesson> out = new ArrayList<>();
        for (Lesson l : ALL) if (l.group.equals(group)) out.add(l);
        return out;
    }

    private static void add(int id, String group, String title, String[] keys, String[] body,
                            String when) {
        ALL[id] = new Lesson(id, group, title, keys, body, when);
    }

    private static String[] keys(String... k) { return k; }
    private static String[] lines(String... l) { return l; }
}
