package vajra.world;

/** THE CODEX — where the world speaks in peace. A pause-screen book of one-page entries the
 *  pilot earns by walking, meeting and finishing. A page lifts inside the run exactly once
 *  (says so with a popup note); the run's lifts fold into the active profile at endRun, so a
 *  page unlocked is a page unlocked for good.
 *
 *  <p>Wave 1's pages carry this plan's own copy: the world, its three voices, the first two
 *  missions, the warden, his fall, and what waits below. Wave 2+ pages add themselves as
 *  lokas open; COUNT grows, older page ids never move. */
public final class Codex {
    public static final int COUNT = 12;

    // page ids: stable, never renumbered
    public static final int ATALA = 0, VAJRA = 1, DEEPENING = 2,
            SCOUT = 3, MAYA = 4, KALIYA = 5,
            BEACON = 6, ROGUE = 7, RETEMPER = 8,
            BALA = 9, LETTER = 10, FIFTH = 11;

    public static final String[] TITLE = {
            "ATALA", "THE VAJRAYUDHA", "THE DEEPENING",
            "UPARICHARA'S SCOUT", "THE NINTH MAYA", "KALIYA THE TIDE-PILOT",
            "THE BEACON", "DAMBHA THE ROGUE", "THE RETEMPERING",
            "BALA, WARDEN OF THE GATE", "MAYA'S LETTER", "THE FIFTH STEP"
    };

    public static final String[] BODY = {
            "THE FIRST LOKA OF PATALA. SUNLESS, JEWEL-WALLED, FLUSH WITH NAGA WATER.\n"
            + "BALA HOLDS IT: SON OF THE MAYA, WARDEN OF THE FIRST BAND OF THE BOLT.\n"
            + "WHAT LIVES HERE ANSWERS TO NO SUN AND NO GOD, ONLY TO DEPTH.",
            "TRIVIKRAMA BROKE THE BOLT THAT HUMBLES MESSENGERS OF STORM.\n"
            + "ONE BAND LIES IN EACH OF THE SEVEN LOKAS: SEVEN PIECES, SEVEN WARDENS,\n"
            + "ONE PILOT COMING DOWN FOR ALL OF THEM.",
            "EVERY BAND CROSSED DEEPENS COLOR AND NIGHT.\n"
            + "THE SCRIPTURE CALLS THE SEVEN THE FLOORS OF ONE HOUSE -\n"
            + "WHOEVER READS THE FLOOR HAS READ THE STORY.",
            "A WATCH-SHIP THAT FELL ON HIS WAY BACK UP. HE SEES FIRST, REPORTS SECOND,\n"
            + "AND THE QUOTA NEVER COUNTED HIM, BECAUSE QUOTAS COUNT SOLDIERS ONLY.\n"
            + "HIS BEACON LIES JUST INSIDE THE GATE ABOVE HIS POST.",
            "MAYA'S LAST WRIGHT GREW OLD OVER HER FORGE IN MAYA VANA.\n"
            + "SHE PUT THE HAMMER DOWN, BUT THE WOOD STILL BENDS ITS ROCK FOR HER.\n"
            + "HER LETTER DID NOT SURVIVE HER SON: IT SURVIVED FOR THE SEEKER.",
            "TIDE-PILOT OF THE WATER ROAD. HIS CARAVAN FLOATS BELOW BAND THREE,\n"
            + "LOADING SPARKS FOR WAYS THE ROCK FORGETS TO CLOSE. THE JOB HE NAMES\n"
            + "DOES NOT EXIST YET - WHICH NEVER STOPPED A COURIER'S OATH.",
            "THE WATCHER'S BEACON, RAISED FROM ITS ROOST 400 PX INSIDE THE GATE.\n"
            + "ITS REPORT DOES NOT SPEAK TO SOLDIERS: IT SPEAKS TO BOARDS,\n"
            + "AND A ROGUE'S CARD WENT UP THE SAME HOUR.",
            "A NAMED ROGUE OF THE ILLUSION WOOD. ELITE, GOLD-VENTED, CLOSE-ARMS:\n"
            + "HE WANTS THE FIGHT SOON. THE PURSE IS THE CARD, THE CARD IS THE LAW,\n"
            + "AND NOBODY COLLECTS TWICE.",
            "SIX FORGED WALL PIECES, HOME UNDER MAYA'S HAMMER.\n"
            + "THE RETEMPERING PAYS IN THE HEAVIEST METAL THAT RAN THIS BAND,\n"
            + "AND THE FIRST SHARD HER FORGE STILL MINTS.",
            "THE COMMANDER HOLDS THE PIT; THE DUEL HOLDS HONOR; BERSERK HOLDS BLINKS.\n"
            + "EVERY FOURTH PATTERN HE STUDIES HIS FRESH SCARS: WHAT YOU DODGE BEST,\n"
            + "HE RETIRES. WHAT HAS SEEN YOUR HULL LEAN, HE KEEPS.",
            "SON OF MINE HELD THE DOOR WITH A LIGHT IN HIS HAND THAT TOOK IT FOR A LAMP.\n"
            + "HE STOOD YOU DOWN WITH HONOR, AND PUT HIS BACK IN FRONT OF IT\n"
            + "TILL THE WORLD CAME THROUGH HIM ANYWAY. - M., SOMETIME OF MAYA VANA",
            "TRIVIKRAMA'S FIFTH STEP NEVER FELL, THE SCRIPTURE SAYS:\n"
            + "IT STANDS BELOW THE SEVENTH LOKA'S FLOOR, AND THE FLOOR CAN BEND.\n"
            + "THOSE THAT SEEK IT ARE SENT. ATALA IS ONLY THE STAIRS."
    };

    private Codex() {}

    /** A page lifts in the run, once: the popup note is where the pilot learns the book exists. */
    public static void unlock(World w, int id) {
        if (id < 0 || id >= COUNT || w.codex[id]) return;
        w.codex[id] = true;
        w.popups.push(new Popups.Spec().text("THE CODEX TAKES A PAGE: " + TITLE[id])
                .at(w.ship.x, w.ship.y - 60).life(2.2f).rise(10)
                .color(0.8f, 0.85f, 1f).scale(1.15f));
    }
}
