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
        new Panel(1, "p1-1.png", "IN SWARGA, INDRA KEPT THE THREE WORLDS WITH ONE WEAPON: THE VAJRAYUDHA."),
        new Panel(1, "p1-2.png", "HE HAD NEVER LOST ANYTHING. HE DID NOT KNOW HOW IT FELT."),
        new Panel(1, "p1-3.png", "THEN A GUEST CAME TO THE THRONE. HE BOWED TOO LOW, UNDER A PARASOL, INDOORS."),
        new Panel(1, "p1-4.png", "TRIVIKRAMA. A GOD WHO SMILED THE WAY A CLOCK KEEPS TIME."),
        new Panel(1, "p1-5.png", "HIS FEET DID NOT TOUCH THE FLOOR."),
        new Panel(1, "p1-6.png", "HE CARRIED A CUP OF WATER, AND SPILLED IT FOR NO REASON."),
        // II. THE ASKING
        new Panel(2, "p2-1.png", "INDRA WATCHED HIM, AND COULD NOT SAY WHAT HE WAS LOOKING AT."),
        new Panel(2, "p2-2.png", "\"LORD OF HEAVEN,\" HE SAID. \"LEND ME YOUR BOLT.\""),
        new Panel(2, "p2-3.png", "AND INDRA, WHO HAD NEVER LET IT GO, GAVE IT."),
        new Panel(2, "p2-4.png", "TRIVIKRAMA WALKED OUT INTO THE CLOUDS WITH IT. INDRA REACHED AFTER HIM."),
        new Panel(2, "p2-5.png", "DADHICHI GAVE HIS BONES FOR THAT BOLT. INDRA HAD LET IT GO."),
        new Panel(2, "p2-6.png", "TRIVIKRAMA BROKE IT IN SEVEN, AND SENT ONE PIECE DOWN TO EACH LOKA."),
        // III. THE JOURNEY
        new Panel(3, "p3-1.png", "THE GUEST WALKED INTO THE LIGHT. HIS SHADOW STAYED A MOMENT LONGER."),
        new Panel(3, "p3-2.png", "INDRA UNDERSTOOD THAT THIS WAS NOT A THEFT. IT WAS A QUESTION."),
        new Panel(3, "p3-3.png", "WITHOUT THE BOLT, SWARGA BEGAN TO BLEED."),
        new Panel(3, "p3-4.png", "SEVEN LOKAS BELOW. SEVEN PIECES. SEVEN RULERS WHO BELIEVE WHAT THE METAL TELLS THEM."),
        new Panel(3, "p3-5.png", "A KING WITHOUT HIS WEAPON IS ONLY A MAN WITH A THRONE. HE WALKED AWAY FROM IT."),
        new Panel(3, "p3-6.png", "\"I AM INDRA. I AM TAKING IT BACK.\""),
        new Panel(3, "p3-7.png", "HE TOOK THE ONE THING SWARGA STILL HAD: A SHIP. THE FIRST DOOR IS ATALA."),
    };

    /** How many pages the story has. */
    public static int pages() { return PANELS[PANELS.length - 1].page; }
}
