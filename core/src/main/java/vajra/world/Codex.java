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
    public static final int COUNT = 17;

    // page ids: stable, never renumbered
    public static final int ATALA = 0, VAJRA = 1, DEEPENING = 2,
            SCOUT = 3, MAYA = 4, KALIYA = 5,
            BEACON = 6, ROGUE = 7, RETEMPER = 8,
            BALA = 9, LETTER = 10, FIFTH = 11,
            VITALA = 12, SELLER = 13, MAID = 14, BHAIRAVA = 15, HATAKI = 16;

    public static final String[] TITLE = {
            "ATALA", "THE VAJRAYUDHA", "THE DEEPENING",
            "UPARICHARA'S SCOUT", "THE NINTH MAYA", "KALIYA THE TIDE-PILOT",
            "THE BEACON", "DAMBHA THE ROGUE", "THE RETEMPERING",
            "BALA, RULER OF ATALA", "MAYA'S LETTER", "THE FIFTH STEP",
            "VITALA", "THE GOLD-SELLER", "GAURI'S MAID", "BHAIRAVA, HERALD OF CINDERS", "THE HATAKI"
    };

    public static final String[] BODY = {
            "THE FIRST OF THE SEVEN LOKAS BELOW THE EARTH. NO SUN: THE JEWELS OF ITS PEOPLE GIVE THE LIGHT.\n"
            + "BALA, SON OF MAYA, HOLDS IT, AND HOLDS A PIECE OF THE BOLT.\n"
            + "INDRA CAME DOWN FROM SWARGA FOR IT.",
            "INDRA'S BOLT, FORGED FROM THE BONES OF THE SAGE DADHICHI.\n"
            + "TRIVIKRAMA'S THIRD STEP BROKE IT IN SEVEN. ONE PIECE LIES IN EACH LOKA.\n"
            + "ALL SEVEN, FORGED AGAIN, MAKE THE WEAPON WHOLE.",
            "EVERY BAND CROSSED IS ANOTHER FLOOR OF THE SAME HOUSE.\n"
            + "A PIECE OF THE BOLT TELLS ITS HOLDER THAT THE WORLD AROUND HIM IS HIS.\n"
            + "THE DEEPER THE FLOOR, THE LOUDER THE PIECE.",
            "A WATCH-PILOT OF KING UPARICHARA, WHOSE CHARIOT WAS INDRA'S GIFT.\n"
            + "HE FELL ABOVE THE THIRD BAND, AND HIS BEACON STILL BURNS.\n"
            + "QUOTAS COUNT SOLDIERS. NOBODY COUNTED HIM.",
            "THE NINTH OF THE MAKER'S LINE. SHE PUT THE HAMMER DOWN.\n"
            + "HER SON HOLDS THE DOOR OF ATALA.\n"
            + "HER WOODS STILL BEND THE ROCK FOR HER.",
            "A NAGA OF THE WATER ROAD, DRIVEN UP BY GARUDA'S SHADOW.\n"
            + "HE CARRIES A NAME FOR TAKSHAKA, FAR BELOW: HIS OWN.\n"
            + "HIS CARAVAN WAITS WHERE THE ROAD STILL RUNS.",
            "THE WATCH-CRAFT'S LAMP, RAISED FROM ITS ROOST.\n"
            + "IT SPEAKS TO THE BROKERS' BOARDS, NOT TO SOLDIERS.\n"
            + "A ROGUE'S CARD WENT UP THE SAME HOUR.",
            "A ROGUE OF THE ILLUSION WOOD, GOLD-VENTED AND IMPATIENT.\n"
            + "HE SPOILS THE SEAM OF MAYA VANA.\n"
            + "THE PURSE IS THE CARD. NOBODY COLLECTS IT TWICE.",
            "SIX WRECKS OF MAYA'S WOODS, BROUGHT HOME TO HER HAMMER.\n"
            + "SHE PAYS IN THE HEAVIEST METAL THIS BAND EVER RAN,\n"
            + "AND THE FIRST SHARD HER FORGE STILL MINTS.",
            "SON OF MAYA, MASTER OF NINETY-SIX ILLUSIONS.\n"
            + "HE BELIEVES ATALA IS HIS BECAUSE THE PIECE SAYS SO.\n"
            + "HE LEARNS WHAT YOU DODGE, AND DOES NOT FORGET.",
            "HE HELD THE DOOR TOO LONG.\n"
            + "THE LIGHT IN HIS HAND TOOK THE DOOR FOR A LAMP.\n"
            + "GO DEEPER, INDRA. THE HAMMER WAITS. - M., SOMETIME OF MAYA VANA",
            "TRIVIKRAMA TOOK THREE STEPS. A FOURTH IS WRITTEN, NEVER TAKEN.\n"
            + "IT STANDS BELOW THE SEVENTH LOKA, AND HE STANDS ON IT.\n"
            + "ATALA IS ONLY THE STAIRS.",
            "THE SECOND LOKA: BLACK STONE, AND FIRE. HARA AND GAURI KEEP IT.\n"
            + "ITS SOIL IS BLACK, ITS RIVER IS GOLD, AND ITS WALLS ARE FORGES.\n"
            + "NOBODY IN IT IS A SOLDIER BY TRADE. EVERYONE IN IT IS LIT FROM INSIDE.",
            "A BHUTA OF HARA'S COURT, PALE AND SMILING, WITH A BALANCE ON HIS BACK.\n"
            + "HE WEIGHS HATAKA GOLD AND KNOWS WHERE EVERY VAULT IS CUT.\n"
            + "ASK HIM, AND THE COMPASS POINTS AT TREASURE.",
            "A LAMP-BEARER OF GAURI'S, SMALL, WITH LOTUS PETALS FOR WINGS.\n"
            + "LIGHT ONE OF HER LAMPS AND THE BAND HOLDS ITS BREATH FOR SIX SECONDS.\n"
            + "HER WELLS MEND WHAT THE FIRE BREAKS. DRINK WHEN YOU ARE BROKEN.",
            "HARA AT THE CREMATION GROUND, LORD OF BURNING GOLD, HOLDER OF THE SECOND PIECE.\n"
            + "HE LEAVES ASH THAT BURNS AND A FLOOR THAT SHRINKS.\n"
            + "HIS THIRD EYE OPENS A LITTLE MORE EVERY TIME HIS FIRE IS TESTED.",
            "THE RIVER OF LIQUID GOLD THAT RUNS THE LENGTH OF VITALA, BORN WHERE HARA AND GAURI MEET.\n"
            + "FIRE DRINKS IT AND SPITS HATAKA GOLD. IT CARRIES EVERYTHING ON IT, UP.\n"
            + "IN THE RAPIDS IT CARRIES YOU WHETHER YOU WILL OR NO."
    };

    private Codex() {}

    public static void unlock(World w, int id) {
        unlock(w, id, false);
    }

    /** A page lifts in the run, silently: the book never interrupts the flight. The pause screen
     *  marks CODEX with a gold diamond while there are pages not yet read.  (`quiet` is kept so
     *  the older call sites read the same; it no longer changes anything.) */
    public static void unlock(World w, int id, boolean quiet) {
        if (id < 0 || id >= COUNT || w.codex[id]) return;
        w.codex[id] = true;
    }

    /** How many pages this pilot has lifted that the book has not shown them yet: this run's
     *  lifts, minus what the profile already holds. */
    public static int unread(World w, boolean[] book) {
        int n = 0;
        for (int i = 0; i < COUNT; i++)
            if (w.codex[i] && !(book != null && i < book.length && book[i])) n++;
        return n;
    }
}
