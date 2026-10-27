package vajra.audio;

/** The events the world reports (see {@link SoundSink}). a and b are explained per id. */
public final class Snd {
    public static final int FIRE = 1;          // a = weapon index (a player round leaves the guns)
    public static final int HIT = 2;           // a = weapon index (it landed on a foe)
    public static final int BOOM = 3;          // a = 0 small, 1 big
    public static final int ROCK = 4;          // a round on stone
    public static final int FOE_FIRE = 5;      // a = Enemy.Kind ordinal, or -1
    public static final int FOE_DIE = 6;       // a = kind ordinal
    public static final int FOE_WAKE = 7;      // a = kind ordinal
    public static final int HULL_HIT = 8;      // b = damage
    public static final int SHIELD_HIT = 9;
    public static final int DEATH = 10;        // the pilot's hull breaks up
    public static final int PICKUP = 11;       // a = Pickup type, b = variant
    public static final int ASTRA_INVOKE = 12; // a = astra kind
    public static final int ASTRA_LOOSE = 13;  // a = astra kind
    public static final int ASTRA_CANCEL = 14;
    public static final int CUE = 15;          // a = Cue ordinal
    public static final int LAMP = 16;
    public static final int DOCK = 17;
    public static final int MEDKIT = 18;
    public static final int MAYA = 19;         // a = mode
    public static final int ASH = 20;
    public static final int BETRAY = 21;
    public static final int PIECE_LIFT = 22;   // a ruler lets go of his piece
    public static final int SEAL = 23;
    public static final int BAY_CLOSE = 24;
    public static final int ASTRA_HIT = 25;
    public static final int FOE_HURT_ARMOUR = 26;
    public static final int FOE_MEND = 27;
    public static final int FOE_DODGE = 28;
    public static final int FOE_DIVE = 29;     // b = 1 when it surfaces
    public static final int CHEST = 30;
    public static final int EXECUTE = 31;
    public static final int BOSS_TELEGRAPH = 32;
    public static final int RAM = 33;

    private Snd() {}
}
