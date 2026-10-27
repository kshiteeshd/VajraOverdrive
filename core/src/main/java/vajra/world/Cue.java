package vajra.world;

/** A story moment the world has just reached. World only NAMES it (World.cue); the screen
 *  decides whether to stage it, so logic never imports rendering and a headless test can assert
 *  "this event fires exactly once". */
public enum Cue {
    OPENING,      // the run begins
    ATALA_GATE,   // first crossing through the grand gate into Atala
    BALA_WAKES,   // the commander wakes on the crossing into his arena
    VOICE_SCOUT,  // first meeting: Uparichara's Scout
    VOICE_MAYA,   // first meeting: the Ninth Maya
    VOICE_KALIYA, // first meeting: Kaliya the Tide-Pilot
    GAZE_NOTICED, // the ruler's piece warms: the first of his people have fallen
    GAZE_WATCHING,// he looks up and speaks to the empty hall
    GAZE_ROUSED,  // the quota is full: he takes his place in the last band
    BALA_FALLS,   // the ruler's last breath
    VITALA_ENTER, // first crossing through Bala's door into the Plunge
    VOICE_SELLER, // first meeting: the Gold-Seller, a bhuta of Hara's court
    VOICE_MAID,   // first meeting: Gauri's Maid, keeper of the shrine lamps
    BHAIRAVA_WAKES,  // Vitala's ruler wakes as the hull comes into the Altar
    BHAIRAVA_FALLS,  // and his last breath
    FIRST_LAMP,      // a lamp (a checkpoint) comes into sight for the first time: the camera goes to it and says what it is
    FIRST_BAY        // so does a maker's bay (a shop)
}
