package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vajra.enemy.Enemy;
import vajra.world.Codex;
import vajra.world.Missions;
import vajra.world.World;

/** THE PEOPLE'S ART: Uparichara's Scout, the Ninth Maya and Kaliya, each on a 64x64 sheet of their
 *  own (assets/npcs/atala/NAME.png, drawn by tools/gen_npc_sheets.py). They are lit from within in sky
 *  blue and wear a thin halo; they never wear a soldier's black slit-eye, so a person cannot be
 *  mistaken for an enemy. Four states, four frames each: idle, talk, work, hail.
 *
 *  <p>A person HAILS (halo widening, a gold mark over their head) until the pilot has met them,
 *  TALKS while a bubble is up, WORKS while their job is open, and otherwise idles. */
public final class NpcImages {
    public static final String[] NAMES = {"scout", "maya", "kaliya"};
    public static final int IDLE = 0, TALK = 1, WORK = 2, HAIL = 3;
    static final int CELL = 64;
    static final float DRAW = 48; // drawn at 3/4 of the cell

    private final Texture[] tex = new Texture[NAMES.length];
    private final TextureRegion[][][] cells = new TextureRegion[NAMES.length][][];

    public NpcImages() {
        for (int i = 0; i < NAMES.length; i++) {
            String path = "npcs/atala/" + NAMES[i] + ".png";
            if (!Gdx.files.internal(path).exists()) continue;
            Texture t = ImageKit.texture(path, false);
            if (t == null) continue;
            tex[i] = t;
            cells[i] = TextureRegion.split(t, CELL, CELL);
        }
    }

    /** Which person this is, by the name Chatter keys on; -1 when there is no art for them. */
    public static int index(Enemy v) {
        String n = v.voiceName == null ? "SCOUT" : v.voiceName.toLowerCase();
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(n)) return i;
        return -1;
    }

    /** What this person is doing now. Pure: pinned by NpcTest. */
    public static int mode(World w, Enemy v) {
        if (w.talking(v)) return TALK;
        if (!v.voiceMet) return HAIL;
        String n = v.voiceName == null ? "SCOUT" : v.voiceName;
        if (n.equals("MAYA") && w.missions.salvage == Missions.OPEN) return WORK;
        if (n.equals("SCOUT") && !w.codex[Codex.BEACON]) return WORK; // his beacon is still down
        return IDLE;
    }

    /** The sheet cell for a mode at a time: row 0 holds idle and talk, row 1 holds work and hail. */
    static int[] cell(int mode, float time, float phase) {
        int row = mode >= WORK ? 1 : 0;
        return new int[] {row, (mode % 2) * 4 + EnemyImages.frame(time, phase, 4)};
    }

    public boolean has(Enemy v) {
        int i = index(v);
        return i >= 0 && cells[i] != null;
    }

    public void draw(SpriteBatch b, World w, Enemy v) {
        int i = index(v);
        if (i < 0 || cells[i] == null) return;
        int[] c = cell(mode(w, v), w.time(), v.phase);
        b.setColor(1, 1, 1, 1);
        b.draw(cells[i][c[0]][c[1]], v.x - DRAW / 2, v.y - DRAW / 2, DRAW, DRAW);
    }

    public void dispose() {
        for (Texture t : tex) if (t != null) t.dispose();
    }
}
