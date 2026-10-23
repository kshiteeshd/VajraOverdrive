package vajra.screen;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.ArrayList;
import java.util.List;
import vajra.scene.Palette;
import vajra.world.Codex;
import vajra.world.Missions;
import vajra.world.Toast;
import vajra.world.World;

/** THE JOBS THE PEOPLE HAVE GIVEN, kept on the HUD instead of floating over the hull: a small temple
 *  panel on the left lists each open job with its progress as a row of diamonds - the Scout's beacon,
 *  Maya's six wrecks, the brokers' bounty. A job's WORDS come once, as a toast at the top of the
 *  screen when it opens and when it pays. Pure state is read from the world, so the list is testable. */
final class HudJobs {
    static final float X = 14, TOP = 520, W = 244, ROW = 46;

    /** One open job. */
    static final class Job {
        final String who, title;
        final int have, need;
        Job(String who, String title, int have, int need) {
            this.who = who;
            this.title = title;
            this.have = have;
            this.need = need;
        }
    }

    /** The jobs open right now, in the order the pilot met their givers. Pure. */
    static List<Job> open(World w) {
        List<Job> out = new ArrayList<>();
        vajra.enemy.Enemy scout = vajra.world.Chatter.person(w, "SCOUT");
        if (scout != null && scout.voiceMet && !w.codex[Codex.BEACON])
            out.add(new Job("THE SCOUT", "RAISE THE BEACON", 0, 1));
        if (w.missions.salvage == Missions.OPEN)
            out.add(new Job("THE NINTH MAYA", "SALVAGE THE WRECKS", w.missions.salvageTaken, Missions.SALVAGE_NEED));
        if (w.missions.bounty == Missions.OPEN)
            out.add(new Job("THE BROKERS", "BOUNTY: DAMBHA", 0, 1));
        return out;
    }

    private static float panelH(int n) { return n * ROW + 14; }

    /** The tracker's rectangle, or null when no job is open. */
    static float[] rect(World w) {
        int n = open(w).size();
        return n == 0 ? null : new float[] {X, TOP - panelH(n), X + W, TOP};
    }

    void shapes(Ui ui, World w) {
        List<Job> jobs = open(w);
        if (jobs.isEmpty()) return;
        float h = panelH(jobs.size()), y0 = TOP - h;
        float[] c = Palette.PERSON;
        Ornament.panel(ui, X, y0, W, h, 6, c[0], c[1], c[2], 0.55f, false);
        for (int i = 0; i < jobs.size(); i++) {
            Job j = jobs.get(i);
            float ry = TOP - 14 - i * ROW;
            for (int k = 0; k < j.need; k++) {
                boolean done = k < j.have;
                ui.shapes.setColor(UiTheme.GOLD_R, UiTheme.GOLD_G, UiTheme.GOLD_B, done ? 1f : 0.3f);
                Ornament.diamond(ui, X + 22 + k * 16, ry - 30, done ? 5f : 3.5f);
            }
        }
    }

    void text(Ui ui, SpriteBatch b, World w) {
        List<Job> jobs = open(w);
        if (jobs.isEmpty()) return;
        float[] c = Palette.PERSON;
        for (int i = 0; i < jobs.size(); i++) {
            Job j = jobs.get(i);
            float ry = TOP - 14 - i * ROW;
            ui.caption.setColor(c[0], c[1], c[2], 0.9f);
            ui.caption.draw(b, j.who, X + 16, ry - 2);
            ui.hud.setColor(1, 1, 1, 0.95f);
            ui.hud.draw(b, j.title, X + 16, ry - 14);
            if (j.need > 1) {
                ui.caption.setColor(1, 1, 1, 0.6f);
                ui.right(b, ui.caption, j.have + " / " + j.need, X + W - 14, ry - 2);
            }
        }
        ui.caption.setColor(1, 1, 1, 1);
        ui.hud.setColor(1, 1, 1, 1);
    }
}
