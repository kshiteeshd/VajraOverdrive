package vajra.coach;

/** THE FIRST FLIGHT: a pilot who has never flown gets the controls one at a time - thrust
 *  first, fire only once the pilot has actually flown - and the coach names the key as it does.
 *  A pilot who has already learned both is never held. Pure; GameScreen feeds it what the pilot
 *  did and gates the controls with it. */
public final class Coach {
    public static final float FLY_FAR = 140f;   // px of real travel that proves the hull can be flown
    public static final float FIRE_HOLD = 0.4f; // seconds of fire pressed that prove the trigger was found

    public enum Stage { THRUST, FIRE, FREE }

    private Stage stage;
    private float startX, startY, fired;
    private boolean placed;

    /** `learnedThrust` / `learnedFire` come from the pilot's book. */
    public Coach(boolean learnedThrust, boolean learnedFire) {
        stage = learnedThrust && learnedFire ? Stage.FREE
                : learnedThrust ? Stage.FIRE : Stage.THRUST;
    }

    public Stage stage() { return stage; }

    /** Fire is a locked control until the pilot has been taught it. */
    public boolean canFire() { return stage == Stage.FREE; }

    /** The lesson being taught now (it names the prompt), or -1 once the pilot is free. */
    public int lesson() {
        return stage == Stage.THRUST ? Lessons.THRUST : stage == Stage.FIRE ? Lessons.FIRE : -1;
    }

    /** Advance by what the pilot did this step. Returns the lesson just completed, else -1. */
    public int update(float dt, float shipX, float shipY, boolean firePressed) {
        if (!placed) {
            placed = true;
            startX = shipX;
            startY = shipY;
        }
        if (stage == Stage.THRUST) {
            float dx = shipX - startX, dy = shipY - startY;
            if (dx * dx + dy * dy >= FLY_FAR * FLY_FAR) {
                stage = Stage.FIRE;
                return Lessons.THRUST;
            }
        } else if (stage == Stage.FIRE) {
            if (firePressed) fired += dt;
            if (fired >= FIRE_HOLD) {
                stage = Stage.FREE;
                return Lessons.FIRE;
            }
        }
        return -1;
    }
}
