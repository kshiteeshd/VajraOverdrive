package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import vajra.world.World;

/** The Vajrayudha (assets/vajrayudha/vj.png) cut into seven bands — band i = loka i, top band
 *  is Atala, bottom is Patala. The world pickup draws the band you just earned. All seven: the
 *  run is over (World.vajraDone). Missing file: gold bands, never a crash. */
public class VajraImages {
    public static final int BANDS = vajra.world.LokaTable.COUNT;
    private final Texture tex;
    private final TextureRegion[] band = new TextureRegion[BANDS];

    public VajraImages() {
        tex = load();
        int h = tex.getHeight();
        for (int i = 0; i < BANDS; i++)
            band[i] = new TextureRegion(tex, 0, bandTop(i, h), tex.getWidth(), bandH(i, h));
    }

    private Texture load() {
        Texture t = ImageKit.texture("vajrayudha/vj.png", true);
        if (t == null) {
            Pixmap p = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
            for (int i = 0; i < BANDS; i++) { // gold bands with a hairline gap: the shape reads without art
                p.setColor(1f, 0.84f - i * 0.06f, 0.3f, 1f);
                p.fillRectangle(0, bandTop(i, 32), 32, Math.max(1, bandH(i, 32) - 1));
            }
            t = new Texture(p);
            p.dispose();
        }
        return t;
    }

    /** Band i's first texel row, measured from the top of the image. Integer split: the seven
     *  heights never overlap, never leave a gap, and sum back to h. */
    public static int bandTop(int i, int h) {
        return h * i / BANDS;
    }

    public static int bandH(int i, int h) {
        return h * (i + 1) / BANDS - bandTop(i, h);
    }

    /** The world drop: just its band, drawn at 2x so a 4–5 px slice still reads. */
    public void drawBand(SpriteBatch batch, int loka, float x, float y) {
        TextureRegion r = band[loka];
        float w = r.getRegionWidth() * 2f, h = r.getRegionHeight() * 2f;
        batch.draw(r, x - w / 2, y - h / 2, w, h);
    }

    /** The whole blade, `h` tall and centred on (cx, cy), one band at a time from Atala (top) to Patala: a held band in gold, one not yet held a dim ghost; band `glow` (-1 for none)
     *  burns, `pulse` 0..1 breathing, over a halo drawn as light. `alpha` is the overall strength. */
    public void drawBlade(SpriteBatch batch, float cx, float cy, float h, boolean[] have, int glow, float pulse, float alpha) {
        int th = tex.getHeight();
        float w = h * tex.getWidth() / (float) th;
        if (glow >= 0 && glow < BANDS) {
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
            TextureRegion r = band[glow];
            float bh = h * r.getRegionHeight() / th, by = cy + h / 2f - h * bandTop(glow, th) / th - bh;
            for (int i = 0; i < 3; i++) {
                float grow = 1.25f + 0.35f * i + 0.12f * pulse;
                batch.setColor(1f, 0.82f, 0.38f, (0.34f - 0.09f * i) * alpha);
                batch.draw(r, cx - w * grow / 2f, by + bh / 2f - bh * grow / 2f, w * grow, bh * grow);
            }
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        for (int i = 0; i < BANDS; i++) {
            TextureRegion r = band[i];
            float bh = h * r.getRegionHeight() / th, by = cy + h / 2f - h * bandTop(i, th) / th - bh;
            if (i == glow) batch.setColor(1f, 0.97f + 0.03f * pulse, 0.78f + 0.2f * pulse, alpha);
            else if (have != null && i < have.length && have[i]) batch.setColor(1f, 0.92f, 0.62f, alpha);
            else batch.setColor(0.45f, 0.45f, 0.55f, 0.32f * alpha);
            batch.draw(r, cx - w / 2f, by, w, bh);
        }
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void dispose() {
        if (tex != null) tex.dispose();
    }
}
