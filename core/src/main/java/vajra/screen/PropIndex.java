package vajra.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import vajra.world.PropField;
import vajra.world.PropField.Prop;

/** The props near the view, found by chunk (and remembered). The terrain bake has already painted them into the land; what
 *  the renderer still needs them for is LIGHT: lamps, glowing mushrooms and pillar crowns throw pools into the lightmap. */
final class PropIndex {
    private static final int SPAN = 512, KEEP = 160;

    private final PropField field;
    private final LinkedHashMap<Long, List<Prop>> chunks = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, List<Prop>> eldest) { return size() > KEEP; }
    };

    PropIndex(PropField field) { this.field = field; }

    /** Light pools of the props whose feet are in [l, l + w] x [b, b + h], projected for a flight plane at `fly`, camera centre
     *  (cx, cy): {x, y, radius, r, g, b} in flight-plane coordinates. */
    void glows(float cx, float cy, float l, float b, float w, float h, float fly, float time, List<float[]> out) {
        int cx0 = (int) Math.floor((l - 120) / SPAN), cx1 = (int) Math.floor((l + w + 120) / SPAN);
        int cy0 = (int) Math.floor((b - 120) / SPAN), cy1 = (int) Math.floor((b + h + 120) / SPAN);
        for (int cj = cy0; cj <= cy1; cj++)
            for (int ci = cx0; ci <= cx1; ci++) {
                long key = ((long) ci << 32) ^ (cj & 0xFFFFFFFFL);
                List<Prop> list = chunks.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                    field.chunk(ci * SPAN, cj * SPAN, SPAN, list);
                    chunks.put(key, list);
                }
                for (Prop p : list) {
                    float z, r, cr, cg, cb;
                    switch (p.kind) {
                        case PropField.LAMP: z = 118f; r = 150f; cr = 1f; cg = 0.8f; cb = 0.4f; break;
                        case PropField.MUSHROOM: z = 30f + 70f * p.size; r = 105f; cr = 0.62f; cg = 0.45f; cb = 1f; break;
                        case PropField.PILLAR: z = 182f; r = 130f; cr = 0.9f; cg = 0.7f; cb = 0.5f; break;
                        default: continue;
                    }
                    float k = Altitude.scale(z, fly), flick = 0.88f + 0.12f * (float) Math.sin(time * 4f + (p.seed & 255));
                    out.add(new float[] {cx + (p.x - cx) * k, cy + (p.y - cy) * k, r * k, cr * flick, cg * flick, cb * flick});
                }
            }
    }
}
