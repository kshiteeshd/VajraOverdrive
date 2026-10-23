package vajra.scene;

import vajra.enemy.Enemy;

/** One beat of a cutscene: the camera goes to (x, y) at `zoom`, and someone says `lines` one at
 *  a time. Three kinds of speaker:
 *  - the NARRATOR: plain words at the foot of the screen, no box;
 *  - the PILOT: a thought bubble over the hull;
 *  - anyone or anything else (a guard, a voice, a gate): a speech bubble over them, at (sx, sy)
 *    or, when `who` is set, over that hull wherever it now stands.
 *  A shot can also walk hulls to places (`gather`) while it plays. */
public final class Shot {
    public final float x, y, zoom;          // where the camera goes
    public final String speaker;            // null = the narrator
    public final boolean player;
    public final String[] lines;
    public float sx, sy;                    // the speaker's spot in the world
    public float moveSecs = CutscenePlayer.MOVE; // camera travel time: a long pan takes longer
    public boolean below;                   // bubble hangs under the speaker, not over
    public float walkSecs = 2.2f;           // how long the gathered hulls take to arrive
    public Enemy who;                       // follow this hull instead of (sx, sy)
    public float r = 1, g = 1, b = 1;       // speaker tint
    public Enemy[] gather;                  // hulls that walk during this shot ...
    public float[] gatherX, gatherY;        // ... to these spots

    private float[] fromX, fromY;

    Shot(float x, float y, float zoom, String speaker, boolean player, String... lines) {
        this.x = x;
        this.y = y;
        this.zoom = zoom;
        this.speaker = speaker;
        this.player = player;
        this.lines = lines;
        this.sx = x;
        this.sy = y;
    }

    public boolean narrator() { return speaker == null; }

    public Shot tint(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        return this;
    }

    public Shot speakerAt(float sx, float sy) {
        this.sx = sx;
        this.sy = sy;
        return this;
    }

    public Shot pan(float secs) {
        this.moveSecs = secs;
        return this;
    }

    public Shot below() {
        this.below = true;
        return this;
    }

    public Shot follow(Enemy who) {
        this.who = who;
        return this;
    }

    public Shot walk(Enemy[] hulls, float[] xs, float[] ys) {
        gather = hulls;
        gatherX = xs;
        gatherY = ys;
        return this;
    }

    /** Where the speaker is right now. */
    public float speakerX() { return who != null ? who.x : sx; }
    public float speakerY() { return who != null ? who.y : sy; }

    /** Walk the gathered hulls part-way (t = 0..1 of the camera move). Their start spots are
     *  taken the first time this is called. */
    public void walkTo(float t) {
        if (gather == null) return;
        if (fromX == null) {
            fromX = new float[gather.length];
            fromY = new float[gather.length];
            for (int i = 0; i < gather.length; i++) {
                fromX[i] = gather[i].x;
                fromY[i] = gather[i].y;
            }
        }
        float k = t * t * (3 - 2 * t);
        for (int i = 0; i < gather.length; i++) {
            gather[i].x = fromX[i] + (gatherX[i] - fromX[i]) * k;
            gather[i].y = fromY[i] + (gatherY[i] - fromY[i]) * k;
        }
    }
}
