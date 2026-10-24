package vajra.screen;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import vajra.world.GroundMap;

/** The ground under everything: {@link GroundMap}'s painted land, laid down in square chunks of 128 x 128 texels
 *  (512 world px) as the camera nears them. A chunk is painted once, kept in a small LRU, and drawn as one quad; at most
 *  a few are painted a frame so a fast flight never hitches (a chunk still unpainted draws as nothing for a moment). */
final class GroundLayer {
    static final int CHUNK = 128, SPAN = CHUNK * GroundMap.TEXEL;
    private static final int KEEP = 96;

    private final GroundMap map;
    private final int[] scratch = new int[CHUNK * CHUNK];
    private final LinkedHashMap<Long, Texture> chunks = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Texture> eldest) {
            if (size() <= KEEP) return false;
            eldest.getValue().dispose();
            return true;
        }
    };

    GroundLayer(GroundMap map) { this.map = map; }

    /** Draw every chunk touching the view, painting up to `budget` missing ones, nearest the middle first. */
    void draw(SpriteBatch batch, float l, float b, float w, float h, int budget) {
        int cx0 = (int) Math.floor(l / SPAN), cx1 = (int) Math.floor((l + w) / SPAN);
        int cy0 = (int) Math.floor(b / SPAN), cy1 = (int) Math.floor((b + h) / SPAN);
        float midX = l + w / 2f, midY = b + h / 2f;
        int missing = 0;
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) if (!chunks.containsKey(key(cx, cy))) missing++;
        if (missing > 6) budget = Math.max(budget, missing); // a jump (a respawn, the gate): the whole view at once
        batch.setColor(1f, 1f, 1f, 1f);
        for (int budgetLeft = budget; ; ) {
            int bx = 0, by = 0;
            float bd = Float.MAX_VALUE;
            if (budgetLeft > 0)
                for (int cy = cy0; cy <= cy1; cy++)
                    for (int cx = cx0; cx <= cx1; cx++) {
                        if (chunks.containsKey(key(cx, cy))) continue;
                        float dx = (cx + 0.5f) * SPAN - midX, dy = (cy + 0.5f) * SPAN - midY, d = dx * dx + dy * dy;
                        if (d < bd) { bd = d; bx = cx; by = cy; }
                    }
            if (bd == Float.MAX_VALUE) break;
            chunks.put(key(bx, by), paint(bx, by));
            budgetLeft--;
        }
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                Texture t = chunks.get(key(cx, cy));
                if (t != null) batch.draw(t, cx * SPAN, cy * SPAN, SPAN, SPAN);
            }
    }

    private Texture paint(int cx, int cy) {
        map.fillChunk(cx * CHUNK, cy * CHUNK, CHUNK, scratch);
        Pixmap pm = new Pixmap(CHUNK, CHUNK, Pixmap.Format.RGBA8888);
        for (int j = 0; j < CHUNK; j++)
            for (int i = 0; i < CHUNK; i++) {
                int argb = scratch[j * CHUNK + i];
                int rgba = ((argb & 0xFFFFFF) << 8) | 0xFF;
                pm.drawPixel(i, CHUNK - 1 - j, rgba);       // the pixmap's row 0 is the TOP; the chunk's row 0 is the bottom
            }
        Texture t = new Texture(pm);
        pm.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private static long key(int cx, int cy) { return ((long) cx << 32) ^ (cy & 0xFFFFFFFFL); }

    void dispose() {
        for (Iterator<Texture> it = chunks.values().iterator(); it.hasNext(); ) it.next().dispose();
        chunks.clear();
    }
}
