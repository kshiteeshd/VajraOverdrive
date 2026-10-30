package vajra.screen;

import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import java.util.ArrayDeque;
import vajra.Keymap;
import vajra.ship.Ship;
import vajra.enemy.Enemy;
import vajra.world.World;

/** THE TUTORIAL OF A NEW GAME: the screen starts EMPTY, and each instrument of the HUD comes on the first time the flight gives it a reason. The hull tube when the first shot lands,
 *  the shield when the first bolt comes at you, the boost once you have crossed some distance, the radar when something is near, and so on. Each one arrives with a holographic
 *  callout: corner brackets close on it, a thin line runs to a small panel that unfolds and types a line or two, and the panel fades away on its own. The flight is never held. */
final class HudReveal {
    static final int HULL = 1, REPAIRS = 2, WEAPON = 4, DIAL = 8, SHIELD = 16, BOOST = 32, SCORE = 64, ASTRA = 128, LOKA = 256, VAJRA = 512, ALL = 1023;
    private static final float LIFE = 8f, GAP = 0.7f, CPS = 85f, PW = 360f;

    private static final class Note {
        final int bit;
        final float x, y, w, h;
        final String title, text;

        Note(int bit, float x, float y, float w, float h, String title, String text) {
            this.bit = bit;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.title = title;
            this.text = text;
        }
    }

    private final GlyphLayout gl = new GlyphLayout();
    private final ArrayDeque<Note> queue = new ArrayDeque<>();
    private int mask = ALL, queued;
    private boolean active;
    private Note cur;
    private float t, gap, startX, startY, life = LIFE;
    private boolean placed;

    /** Is this instrument on the screen yet? Always, when no tutorial runs. */
    boolean on(int bit) { return !active || (mask & bit) != 0; }

    boolean active() { return active; }

    /** A callout is on the screen: the flight holds still until it is read (ENTER, SPACE, ESC or a click goes on). */
    boolean holding() { return active && cur != null && t < life - 0.45f; }

    void dismiss() {
        if (cur != null && t > 0.9f) t = Math.max(t, life - 0.45f);
    }

    /** A new game: nothing on the screen until the flight asks for it. */
    void begin() {
        active = true;
        mask = 0;
        queued = 0;
        queue.clear();
        cur = null;
        placed = false;
        t = 0f;
        gap = 1.5f;
        life = LIFE;
    }

    /** The test arena's tutorial: every instrument in turn, one after the other. */
    void demo() {
        begin();
        life = 5.5f;
        for (int bit : new int[] {HULL, REPAIRS, WEAPON, DIAL, SHIELD, BOOST, SCORE, ASTRA, LOKA, VAJRA}) want(bit);
    }

    private void want(int bit) {
        if ((mask & bit) != 0 || (queued & bit) != 0) return;
        queued |= bit;
        queue.add(note(bit));
    }

    /** What the flight has done decides what is taught. `free` is true once the pilot has flown and found the trigger. */
    void update(World w, boolean free, float dt) {
        if (!active) return;
        Ship s = w.ship;
        if (!placed) {
            placed = true;
            startX = s.x;
            startY = s.y;
        }
        float far = (float) Math.hypot(s.x - startX, s.y - startY);
        if (free) want(WEAPON);
        if (s.hp < s.maxHp() - 0.5f) want(HULL);
        if (w.medkits > 0) { want(HULL); want(REPAIRS); }
        int kills = 0;
        for (int k : w.kills) kills += k;
        if (kills > 0) want(SCORE);
        for (int v : w.hangar.shards) if (v > 0) { want(ASTRA); break; }
        if (free && enemyNear(w, 1000f)) want(DIAL);
        if (free && hostileNear(w, 340f)) { want(DIAL); want(SHIELD); }
        if (free && far > 900f) { want(DIAL); want(BOOST); }
        if (far > 1800f || w.currentRegionIndex() >= 1) want(LOKA);
        boolean piece = false;
        for (boolean b : w.hangar.pieces) piece |= b;
        if (piece && w.pieceShowT < 0f) want(VAJRA);

        if (cur != null) {
            t += dt;
            if (t > life) { cur = null; gap = GAP; }
        } else if (gap > 0f) gap -= dt;
        else if (!queue.isEmpty()) {
            cur = queue.poll();
            mask |= cur.bit;
            t = 0f;
            vajra.audio.Audio.get().ui("ui_tour");
        }
    }

    private static boolean enemyNear(World w, float r) {
        for (Enemy e : w.enemies) if (!e.dead && Math.hypot(e.x - w.ship.x, e.y - w.ship.y) < r) return true;
        return false;
    }

    private static boolean hostileNear(World w, float r) {
        for (vajra.weapon.Bullet b : w.bullets) if (b.hostile && !b.dead && Math.hypot(b.x - w.ship.x, b.y - w.ship.y) < r) return true;
        return false;
    }

    // ---- the words ----------------------------------------------------------------------------------------------------------------------

    private static Note note(int bit) {
        vajra.Settings st = vajra.profile.Profiles.active().settings;
        float m = VedicHud.M;
        switch (bit) {
            case HULL: {
                float y0 = Math.min(VedicHud.LIFE_Y - 16, VedicHud.TUBE_CY - 18), y1 = Math.max(VedicHud.LIFE_Y + 16, VedicHud.TUBE_CY + 18);
                return new Note(bit, VedicHud.TUBE_X0 - 18, y0, VedicHud.TUBE_L + 30, y1 - y0, "HULL INTEGRITY",
                        "THE TUBE IS YOUR SHIP. IT DRAINS AS YOU ARE HIT.\nEMPTY, AND A LIFE IS SPENT: THE PIPS ARE YOUR LIVES. YOU WAKE AT THE LAST LAMP.");
            }
            case REPAIRS:
                return new Note(bit, VedicHud.TUBE_X0 + VedicHud.TUBE_L + 4, VedicHud.TUBE_CY - 14, 5 * 15 + 36, 28, "REPAIR CELLS",
                        "A REPAIR FOUND WHILE WHOLE IS STORED, FIVE AT MOST.\nPRESS " + Keymap.REPAIR.name(st) + " TO MEND 40% OF THE HULL.");
            case WEAPON:
                return new Note(bit, VedicHud.MED_CX - 40, VedicHud.MED_CY - 40, 80, 96, "WEAPON PATTERN",
                        "THE PATTERN YOU FIRE. " + Keymap.WEAPON_NEXT.name(st) + " STEPS TO THE NEXT YOU OWN; HOLD IT FOR THE WHEEL.\nNEW PATTERNS ARE BOUGHT IN A BAY.");
            case DIAL:
                return new Note(bit, VedicHud.MAP_CX - VedicHud.ARC_R - 10, VedicHud.MAP_CY - VedicHud.ARC_R - 10, 2 * VedicHud.ARC_R + 20, 2 * VedicHud.ARC_R + 34, "RADAR",
                        "THE SWEEP SHOWS FOES, LOOT, LAMPS, BAYS AND THE RULER. THE LAND UNDER IT IS THE REAL LAND.");
            case SHIELD:
                return new Note(bit, VedicHud.MAP_CX - VedicHud.ARC_R - 12, VedicHud.MAP_CY - VedicHud.ARC_R, 24, 2 * VedicHud.ARC_R, "SHIELD",
                        "HOLD " + Keymap.SHIELD.name(st) + " OR THE RIGHT BUTTON TO RAISE IT. IT TURNS BOLTS ASIDE AND DRAINS WHILE IT IS UP.");
            case BOOST:
                return new Note(bit, VedicHud.MAP_CX + VedicHud.ARC_R - 12, VedicHud.MAP_CY - VedicHud.ARC_R, 24, 2 * VedicHud.ARC_R, "BOOST",
                        "HOLD " + Keymap.BOOST.name(st) + " WHILE YOU FLY FOR A BURST OF SPEED. IT REFILLS WHEN YOU LET GO.");
            case SCORE:
                return new Note(bit, Ui.UI_W / 2f - 150, Ui.UI_H - 52, 300, 42, "SCORE AND CHAIN",
                        "KILL AGAIN BEFORE THE CHAIN RUNS OUT TO RAISE THE COMBO. A HIT BREAKS IT.");
            case ASTRA:
                return new Note(bit, VedicHud.GEM_CX - VedicHud.RING_R - 12, VedicHud.GEM_CY - VedicHud.RING_R - 12, 2 * VedicHud.RING_R + 24, 2 * VedicHud.RING_R + 24, "ASTRA",
                        "FIVE SHARDS OF AN ELEMENT PAY FOR ONE ASTRA. HOLD " + Keymap.ASTRA.name(st) + " TO FORM IT, RELEASE TO LOOSE IT. 2ND AND 3RD HULLS ONLY.");
            case LOKA:
                return new Note(bit, Ui.UI_W - 176, Ui.UI_H / 2f - 110, 170, 150, "LOKA AND DEPTH",
                        "THE WORLD YOU FLY IN, WHERE IT IS, AND HOW FAR DOWN YOU HAVE COME, IN YOJANAS.");
            default:
                return new Note(bit, VajraPanel.PX - 6, VajraPanel.PY - 6, VajraPanel.PW + 12, VajraPanel.PH + 12, "THE VAJRA",
                        "EACH RULER HOLDS A PIECE OF THE BOLT. THE PIECES YOU WIN ARE COUNTED HERE, ONE OF SEVEN.");
        }
    }

    // ---- the callout --------------------------------------------------------------------------------------------------------------------

    void render(Ui ui, SpriteBatch batch) {
        if (!active || cur == null) return;
        Note n = cur;
        float ar = Template.accentR, ag = Template.accentG, ab = Template.accentB;
        float in = MathUtils.clamp(t / 0.45f, 0f, 1f), e = in * in * (3f - 2f * in);
        float out = MathUtils.clamp((life - t) / 0.45f, 0f, 1f), a = Math.min(1f, t / 0.2f) * out;

        gl.setText(ui.hud, n.text, ui.hud.getColor(), PW - 44f, Align.left, true);
        float ph = 70f + gl.height + 16f, bx = n.x, by = n.y, bw = n.w, bh = n.h, cx = bx + bw / 2f, cy = by + bh / 2f;
        float px, py, ax, ay, qx, qy;
        if (cy > Ui.UI_H * 0.75f) {                                             // a box on the top edge: the panel hangs below it
            px = MathUtils.clamp(cx - PW / 2f, 24f, Ui.UI_W - PW - 24f);
            py = by - 56f - ph;
            ax = cx; ay = by; qx = MathUtils.clamp(cx, px + 20f, px + PW - 20f); qy = py + ph;
        } else if (cx < Ui.UI_W / 2f) {                                         // on the left: the panel lies to its right
            px = bx + bw + 70f;
            py = MathUtils.clamp(cy - ph / 2f, 24f, Ui.UI_H - ph - 24f);
            ax = bx + bw; ay = cy; qx = px; qy = MathUtils.clamp(cy, py + 20f, py + ph - 20f);
        } else {                                                                // on the right: to its left
            px = bx - 70f - PW;
            py = MathUtils.clamp(cy - ph / 2f, 24f, Ui.UI_H - ph - 24f);
            ax = bx; ay = cy; qx = px + PW; qy = MathUtils.clamp(cy, py + 20f, py + ph - 20f);
        }

        ui.shapes.setProjectionMatrix(ui.camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        // the brackets close on the instrument
        float pad = 6f + (1f - e) * 40f, L = 14f, th = 2.2f;
        ui.shapes.setColor(ar, ag, ab, 0.95f * a);
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sy = -1; sy <= 1; sy += 2) {
                float x = cx + sx * (bw / 2f + pad), y = cy + sy * (bh / 2f + pad);
                ui.shapes.rect(sx > 0 ? x - L : x, sy > 0 ? y - th : y, L, th);
                ui.shapes.rect(sx > 0 ? x - th : x, sy > 0 ? y - L : y, th, L);
            }
        // the line from the instrument to the panel, drawn out as it arrives, with a pulse running along it
        float lk = MathUtils.clamp((t - 0.15f) / 0.35f, 0f, 1f);
        ui.shapes.setColor(ar, ag, ab, 0.7f * a);
        ui.shapes.rectLine(ax, ay, ax + (qx - ax) * lk, ay + (qy - ay) * lk, 1.4f);
        float pk = (t * 1.6f) % 1f;
        ui.shapes.setColor(1f, 1f, 1f, 0.9f * a * lk);
        ui.shapes.circle(ax + (qx - ax) * lk * pk, ay + (qy - ay) * lk * pk, 2.4f, 10);
        // the panel unfolds from its top edge
        float pk2 = MathUtils.clamp((t - 0.3f) / 0.35f, 0f, 1f), pe = pk2 * pk2 * (3f - 2f * pk2), vh = ph * pe;
        if (vh > 2f) {
            ui.shapes.setColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 0.86f * a);
            ui.shapes.rect(px, py + ph - vh, PW, vh);
            ui.shapes.setColor(ar, ag, ab, 0.9f * a);
            ui.shapes.rect(px, py + ph - vh, 3f, vh);
            if (pe > 0.99f) {
                float life01 = MathUtils.clamp(1f - t / life, 0f, 1f);
                ui.shapes.setColor(ar, ag, ab, 0.25f * a);
                ui.shapes.rect(px + 22f, py + 12f, PW - 44f, 2f);
                ui.shapes.setColor(ar, ag, ab, 0.9f * a);
                ui.shapes.rect(px + 22f, py + 12f, (PW - 44f) * life01, 2f);
            }
            Ornament.panel(ui, px, py + ph - vh, PW, vh, 6, ar, ag, ab, 0.55f * a, false);
        }
        ui.shapes.end();

        if (pe < 0.99f) return;
        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        ui.caption.setColor(ar, ag, ab, 0.6f * a);
        ui.caption.draw(batch, "SYSTEM ONLINE", px + 22f, py + ph - 14f);
        ui.caption.setColor(ar, ag, ab, 0.5f * a);
        ui.caption.draw(batch, "ENTER  CONTINUE", px + PW - 22f - ui.measure(ui.caption, "ENTER  CONTINUE"), py + ph - 14f);
        ui.menu.setColor(ar, ag, ab, a);
        ui.menu.draw(batch, n.title, px + 22f, py + ph - 32f);
        int shown = Math.min(n.text.length(), (int) ((t - 0.65f) * CPS));
        if (shown > 0) {
            ui.hud.setColor(1f, 0.97f, 0.9f, a);
            ui.hud.draw(batch, n.text.substring(0, shown), px + 22f, py + ph - 60f, PW - 44f, Align.left, true);
        }
        ui.menu.setColor(1, 1, 1, 1);
        ui.hud.setColor(1, 1, 1, 1);
        ui.caption.setColor(1, 1, 1, 1);
        batch.end();
    }
}
