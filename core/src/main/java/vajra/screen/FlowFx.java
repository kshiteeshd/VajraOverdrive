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
    private static final int FIRE = 0, WIND = 1, AGNI = 2;
    private final Texture[] tex = {ImageKit.texture("effects/astra_fireflow.png", true), ImageKit.texture("effects/astra_wind.png", true), ImageKit.texture("effects/astra_agni.png", true)};
    private final TextureRegion[][][] reg = new TextureRegion[3][][];

    FlowFx() {
        int[] rows = {5, 6, 4};
        for (int s = 0; s < 3; s++) {
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
        for (int i = 0; i < f.n; i++) {
            float x = f.px[i], y = f.py[i];
            if (x < l - 120 || x > l + vw + 120 || y < bt - 120 || y > bt + vh + 120) continue;
            float u = f.age[i] / f.life[i];
            int fr = f.fr[i] + (int) (f.age[i] * 14f);
            float deg = (float) Math.toDegrees(f.ph[i]);
            if (f.sheet[i] == 0) {                                          // fire: stream and lick, embers
                float wd = f.row[i] == 2 ? 70f : 112f * f.size[i] * (0.75f + 0.5f * (float) Math.sin(Math.PI * Math.min(1f, u + 0.05f)));
                sprite(b, FIRE, f.row[i], fr, x, y, wd, deg, f.row[i] == 2 ? 0.8f : 0.62f * (1f - 0.6f * Math.max(0f, u - 0.6f) / 0.4f));
            } else {                                                        // wind: gusts and wisps
                sprite(b, WIND, f.row[i], fr, x, y, 34f * f.size[i], deg, 0.9f * Math.min(1f, (f.life[i] - f.age[i]) * 3f));
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
        for (AstraFlow.Blade bl : f.blades)                                 // a crescent of cutting wind where a foe was cut
            sprite(b, WIND, 5, Math.min(7, (int) (bl.t / 0.3f * 8f)), bl.x, bl.y, 120f, (float) Math.toDegrees(bl.ang), 1f);
        b.setColor(1f, 1f, 1f, 1f);
        b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    void dispose() {
        for (Texture t : tex) if (t != null) t.dispose();
    }
}
