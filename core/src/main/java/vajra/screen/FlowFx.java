package vajra.screen;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vajra.world.AstraFlow;
import vajra.world.World;

/** THE FLOW, drawn: the sprites of Agni's pyre bloom and Vayu's tornado (world/AstraFlow is the simulation), additive, one sprite a particle, turned to its heading.
 *  Sheets (32 x 32 cells, 8 frames a row, made by tools/enemy_art/flowfx.py and astra.py): effects/astra_fireflow.png (STREAM, LICK, EMBER, BLOOM, SCORCH),
 *  effects/astra_wind.png (GUST, SWIRL, WISP, DEBRIS, FUNNEL, BLADE) and effects/astra_agni.png (HELD, BALL, BURST, BURN: the flame that rides a burning foe).
 *  A missing sheet draws nothing, never crashes. */
final class FlowFx {
    private static final int FIRE = 0, WIND = 1, AGNI = 2, EARTH = 3, LOTUS = 4;
    private final Texture[] tex = {ImageKit.texture("effects/astra_fireflow.png", true), ImageKit.texture("effects/astra_wind.png", true), ImageKit.texture("effects/astra_agni.png", true),
            ImageKit.texture("effects/astra_earth.png", true), ImageKit.texture("effects/astra_lotus.png", true)};
    private final TextureRegion[][][] reg = new TextureRegion[5][][];

    FlowFx() {
        int[] rows = {5, 6, 4, 11, 6};
        for (int s = 0; s < 5; s++) {
            reg[s] = new TextureRegion[rows[s]][8];
            if (tex[s] == null) continue;
            for (int r = 0; r < rows[s]; r++)
                for (int f = 0; f < 8; f++) reg[s][r][f] = new TextureRegion(tex[s], f * 32, r * 32, 32, 32);
        }
    }

    private void sprite(SpriteBatch b, int sheet, int row, int frame, float cx, float cy, float w, float deg, float alpha) {
        TextureRegion r = reg[sheet][row][frame & 7];
        if (r == null || alpha <= 0.01f) return;
        b.setColor(1f, 1f, 1f, Math.min(1f, alpha));
        b.draw(r, cx - w / 2f, cy - w / 2f, w / 2f, w / 2f, w, w, 1f, 1f, deg);
    }

    void draw(SpriteBatch b, World w, float l, float bt, float vw, float vh) {
        AstraFlow f = w.flow;
        if (!f.active() && f.scorch.isEmpty()) return;                       // (a burning foe smokes through the ordinary effects: AstraFlow.tickBurn)
        float time = w.time();
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        for (float[] s : f.scorch) {                                       // the ground the fire has crossed
            if (s[0] < l - 60 || s[0] > l + vw + 60 || s[1] < bt - 60 || s[1] > bt + vh + 60) continue;
            sprite(b, FIRE, 4, (int) (time * 6f), s[0], s[1], 84f, 0f, Math.min(0.75f, s[2] / 1.6f));
        }
        for (int pass = 0; pass < 2; pass++) {                              // pass 0: light (fire, wind, petals); pass 1: SOLID rock, drawn with ordinary blending so it is stone, not glow
            if (pass == 1) b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            for (int i = 0; i < f.n; i++) {
                if ((f.sheet[i] == 3) != (pass == 1)) continue;
                float x = f.px[i], y = f.py[i];
                if (x < l - 120 || x > l + vw + 120 || y < bt - 120 || y > bt + vh + 120) continue;
                float u = f.age[i] / f.life[i];
                int fr = f.fr[i] + (int) (f.age[i] * 14f);
                float deg = (float) Math.toDegrees(f.ph[i]);
                if (f.sheet[i] == 3) {                                          // PRITHVI: a boulder forming, a boulder, a chunk, a pebble
                    int m = f.mode[i], rot = ((int) (f.aux[i] * 8f / 6.2831853f) % 8 + 8) % 8;
                    if (m == 4) sprite(b, EARTH, 0, Math.min(7, (int) (f.age[i] / 0.25f * 8f)), x, y, 96f * f.size[i], 0f, 1f);
                    else if (m == 5) sprite(b, EARTH, f.row[i], rot, x, y, 100f * f.size[i], 0f, 1f);
                    else if (m == 6) sprite(b, EARTH, f.row[i], rot, x, y, 66f * f.size[i], 0f, 1f);
                    else sprite(b, EARTH, 8, rot, x, y, 36f, 0f, Math.min(1f, (f.life[i] - f.age[i]) * 4f));
                    continue;
                }
                if (f.sheet[i] == 4) {                                          // BRAHMA: a fleck of petal left behind, tinted by its petal's element
                    float[] c = World.TATVA_RGB[f.variant[i]];
                    b.setColor(c[0], c[1], c[2], 0.8f * (1f - u));
                    TextureRegion r = reg[LOTUS][5][fr & 7];
                    if (r != null) b.draw(r, x - 20f, y - 20f, 20f, 20f, 40f, 40f, 1f, 1f, deg);
                    continue;
                }
                if (f.sheet[i] == 0) {                                          // fire: stream and lick, embers
                    float wd = f.row[i] == 2 ? 70f : 112f * f.size[i] * (0.75f + 0.5f * (float) Math.sin(Math.PI * Math.min(1f, u + 0.05f)));
                    sprite(b, FIRE, f.row[i], fr, x, y, wd, deg, f.row[i] == 2 ? 0.8f : 0.62f * (1f - 0.6f * Math.max(0f, u - 0.6f) / 0.4f));
                } else {                                                        // wind: gusts and wisps
                    sprite(b, WIND, f.row[i], fr, x, y, 34f * f.size[i], deg, 0.9f * Math.min(1f, (f.life[i] - f.age[i]) * 3f));
                }

            }
            if (pass == 1) {
                for (AstraFlow.Burst bu : f.bursts) {                               // a boulder or a chunk breaking, dust rising (solid too)
                    if (bu.x < l - 80 || bu.x > l + vw + 80 || bu.y < bt - 80 || bu.y > bt + vh + 80) continue;
                    float sz = bu.row == 5 ? 90f : bu.row == 9 ? 100f : 64f;
                    sprite(b, EARTH, bu.row, Math.min(7, (int) (bu.t / bu.life * 8f)), bu.x, bu.y, sz * bu.size * 1.3f, 0f, 1f - 0.4f * bu.t / bu.life);
                }
                b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            }
        }
        if (f.kind == AstraFlow.AGNI && f.ship != null) {                   // the fire at the hull, the root of every stream
            float k = Math.min(1f, f.t / 0.5f);
            sprite(b, FIRE, 3, (int) (time * 12f), f.ship.x, f.ship.y, 110f + 90f * k, 0f, f.t < 2.2f ? 1f : Math.max(0f, 1f - (f.t - 2.2f) / 0.8f));
        }
        if (f.kind == AstraFlow.VAYU && f.ship != null && f.t < AstraFlow.T_THROW) {   // the eye of the tornado
            float a = f.t < AstraFlow.T_EYE ? Math.min(1f, f.t / 0.4f) : Math.max(0f, 1f - (f.t - AstraFlow.T_EYE) / 0.6f);
            sprite(b, WIND, 4, (int) (time * 14f), f.ship.x, f.ship.y, 110f, 0f, 0.9f * a);
        }
        if (f.kind == AstraFlow.LOTUS && f.ship != null) {                  // the lotus: a bud blooming at the hull and five petals, each tinted its element's colour
            if (f.t < AstraFlow.T_BLOOM + 0.5f)
                sprite(b, LOTUS, 0, Math.min(7, (int) (f.t / AstraFlow.T_BLOOM * 8f)), f.ship.x, f.ship.y, 330f, 0f, Math.max(0f, 1f - Math.max(0f, f.t - AstraFlow.T_BLOOM) / 0.5f));
            for (int i = 0; i < AstraFlow.PETALS; i++) {
                if (f.lstate[i] == 4) continue;
                float[] c = World.TATVA_RGB[i];
                int st = f.lstate[i];
                int row = st == 0 ? 1 : st == 2 ? 3 : 2;
                int frame = st == 0 ? Math.min(7, (int) (f.lt[i] / AstraFlow.T_BLOOM * 8f)) : (int) (time * (st == 2 ? 18f : 14f)) + i * 2;
                float size = st == 2 ? 200f : 260f;
                float deg = st == 2 ? 0f : (float) Math.toDegrees(st == 0 ? f.la[i] : f.lh[i]);
                TextureRegion r = reg[LOTUS][row][frame & 7];
                if (r == null) continue;
                b.setColor(c[0], c[1], c[2], 1f);                           // additive, near-white art: the tint is the petal's colour
                b.draw(r, f.lx[i] - size / 2f, f.ly[i] - size / 2f, size / 2f, size / 2f, size, size, 1f, 1f, deg);
                b.setColor(1f, 1f, 1f, 0.35f);                              // and a pale core on top
                b.draw(r, f.lx[i] - size * 0.3f, f.ly[i] - size * 0.3f, size * 0.3f, size * 0.3f, size * 0.6f, size * 0.6f, 1f, 1f, deg);
            }
        }
        for (AstraFlow.Blade bl : f.blades)                                 // a crescent of cutting wind where a foe was cut
            sprite(b, WIND, 5, Math.min(7, (int) (bl.t / 0.3f * 8f)), bl.x, bl.y, 120f, (float) Math.toDegrees(bl.ang), 1f);
        b.setColor(1f, 1f, 1f, 1f);
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    void dispose() {
        for (Texture t : tex) if (t != null) t.dispose();
    }
}
