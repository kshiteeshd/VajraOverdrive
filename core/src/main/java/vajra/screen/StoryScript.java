package vajra.screen;

/** The pre-game story: nineteen panels in three pages, each with the narrator's line. The art is
 *  assets/story/pN-K.png (cut from the three story pages by tools/story/split_panels.py, in reading
 *  order). Lines are set in capitals and kept short, like every other spoken line in the game. */
public final class StoryScript {
    private StoryScript() {}

    /** One panel: its page, its image under assets/story, and the narrator's words. */
    public static final class Panel {
        public final int page;
        public final String file, text;

        Panel(int page, String file, String text) {
            this.page = page;
            this.file = "story/" + file;
            this.text = text;
        }
    }

    public static final Panel[] PANELS = {
        // I. THE THRONE
        new Panel(1, "p1-1.png", "IN SWARGA, INDRA UPHELD THE THREE WORLDS WITH ONE WEAPON: THE VAJRAYUDHA."),
        new Panel(1, "p1-2.png", "HE HAD NEVER KNOWN LOSS. HE DID NOT YET KNOW ITS WEIGHT."),
        new Panel(1, "p1-3.png", "A GUEST CAME BEFORE THE THRONE. HE BOWED TOO LOW, AND CARRIED A PARASOL INTO THE HALL."),
        new Panel(1, "p1-4.png", "TRIVIKRAMA. HE SMILED AS TIME SMILES: WITHOUT WARMTH, AND WITHOUT HASTE."),
        new Panel(1, "p1-5.png", "HIS FEET DID NOT TOUCH THE FLOOR OF HEAVEN."),
        new Panel(1, "p1-6.png", "IN HIS HAND WAS A VESSEL OF WATER. HE POURED IT OUT, AS ONE SEALS A PLEDGE."),
        // II. THE ASKING
        new Panel(2, "p2-1.png", "INDRA WATCHED HIM, AND COULD NOT NAME WHAT HE SAW."),
        new Panel(2, "p2-2.png", "\"LORD OF HEAVEN,\" HE SAID, \"LEND ME YOUR BOLT.\""),
        new Panel(2, "p2-3.png", "AND INDRA, WHO HAD NEVER RELEASED IT, GAVE IT."),
        new Panel(2, "p2-4.png", "TRIVIKRAMA WALKED OUT INTO THE CLOUDS, THE BOLT IN HIS HAND. INDRA REACHED AFTER HIM, TOO LATE."),
        new Panel(2, "p2-5.png", "THE SAGE DADHICHI HAD GIVEN HIS BONES FOR THAT BOLT. INDRA HAD LET IT GO."),
        new Panel(2, "p2-6.png", "TRIVIKRAMA BROKE IT INTO SEVEN, AND SENT ONE PIECE DOWN INTO EACH LOKA."),
        // III. THE JOURNEY
        new Panel(3, "p3-1.png", "THE GUEST WALKED INTO THE LIGHT. HIS SHADOW REMAINED A MOMENT LONGER."),
        new Panel(3, "p3-2.png", "INDRA UNDERSTOOD THAT THIS WAS NO THEFT. IT WAS A TRIAL."),
        new Panel(3, "p3-3.png", "WITHOUT THE BOLT, SWARGA BEGAN TO FAIL."),
        new Panel(3, "p3-4.png", "SEVEN LOKAS BELOW. SEVEN PIECES. SEVEN RULERS, EACH BELIEVING WHAT THE METAL TELLS HIM."),
        new Panel(3, "p3-5.png", "A KING WITHOUT HIS WEAPON IS ONLY A MAN SEATED ON A THRONE. INDRA LEFT THE THRONE."),
        new Panel(3, "p3-6.png", "\"I AM INDRA. WHAT WAS GIVEN IN SACRIFICE WILL BE RETURNED.\""),
        new Panel(3, "p3-7.png", "HE TOOK THE ONE THING SWARGA STILL HELD: A SHIP. THE FIRST GATE IS ATALA."),
    };

    /** How many pages the story has. */
    public static int pages() { return PANELS[PANELS.length - 1].page; }
}
