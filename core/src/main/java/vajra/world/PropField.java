package vajra.world;

import java.util.List;

/** THINGS THAT STAND ON THE LAND: where the trees, mushrooms, lamp posts and dream-pillars are. A pure, seeded function of
 *  the world (same chunk, same props), kept off water and road by {@link GroundMap#surface}. The renderer draws each one as
 *  a stack of slices with depth (screen/PropLayer, docs/WORLD_BUILD_PLAN.md section 1).
 *
 *  <p>Heights are in world px above the ground; the flight plane is 130 up. Anything {@link Prop#high} reaches above it
 *  and is drawn over the hull; everything else the hull flies over. */
public final class PropField {
    public static final int TREE = 0, MUSHROOM = 1, LAMP = 2, PILLAR = 3;

    /** One prop: its base on the ground, how big it is, and a seed for its own variation. */
    public static final class Prop {
        public final int kind, band, seed;
        public final float x, y, size;

        public Prop(int kind, int band, float x, float y, float size, int seed) {
            this.kind = kind;
            this.band = band;
            this.x = x;
            this.y = y;
            this.size = size;
            this.seed = seed;
        }

        /** Total height, px. */
        public float height() {
            switch (kind) {
                case TREE: return 50f + 55f * size;
                case MUSHROOM: return 30f + 70f * size;
                case LAMP: return 118f;
                default: return 180f;
            }
        }

        /** True when it rises above the flight plane. */
        public boolean high() { return height() > 130f; }
    }

    private static final int CELL = 72;
    private final GroundMap map;
    private final float worldW;
    private final float[] starts;
    private final int seed = 0x5EED;

    public PropField(GroundMap map, float worldW, float[] regionStart) {
        this.map = map;
        this.worldW = worldW;
        this.starts = regionStart.clone();
    }

    /** Every prop whose base lies in the square [x0, x0 + span) x [y0, y0 + span). */
    public void chunk(float x0, float y0, float span, List<Prop> out) {
        int cx0 = (int) Math.floor(x0 / CELL), cx1 = (int) Math.floor((x0 + span) / CELL);
        int cy0 = (int) Math.floor(y0 / CELL), cy1 = (int) Math.floor((y0 + span) / CELL);
        for (int cy = cy0; cy <= cy1; cy++)
            for (int cx = cx0; cx <= cx1; cx++) {
                float px = (cx + 0.15f + 0.7f * GroundMap.rnd(cx, cy, seed + 1)) * CELL;
                float py = (cy + 0.15f + 0.7f * GroundMap.rnd(cx, cy, seed + 2)) * CELL;
                if (px < x0 || px >= x0 + span || py < y0 || py >= y0 + span) continue;
                if (px < 0 || px > worldW || py < 0) continue;
                cell(cx, cy, px, py, out);
            }
        lamps(x0, y0, span, out);
        pillars(x0, y0, span, out);
    }

    private void cell(int cx, int cy, float px, float py, List<Prop> out) {
        int band = map.bandAt(px, py);
        Biome b = Biome.of(band);
        float cluster = GroundMap.fbm(cx * 0.12f, cy * 0.12f, seed + 3, 2);       // woods and clearings
        float roll = GroundMap.rnd(cx, cy, seed + 4);
        if (b.grove > 0f && cluster < 0.30f + 0.5f * b.grove && roll < 0.22f + 0.5f * b.grove
                && map.surface(px, py) == GroundMap.LAND) {
            float r = GroundMap.rnd(cx, cy, seed + 5);
            out.add(new Prop(TREE, band, px, py, r * r, hash(cx, cy)));
            return;
        }
        if (band == 2 && cluster > 0.52f && roll < 0.30f && map.surface(px, py) == GroundMap.LAND)  // mushroom rings in the mirror-wood
            out.add(new Prop(MUSHROOM, band, px, py, GroundMap.rnd(cx, cy, seed + 6), hash(cx, cy)));
    }

    /** Lamp posts, a pair every 260 px up the gold road of Svarna Dvara. */
    private void lamps(float x0, float y0, float span, List<Prop> out) {
        float from = Math.max(y0, starts.length > 1 ? starts[1] : 1600f), to = Math.min(y0 + span, starts.length > 2 ? starts[2] : 4100f);
        for (int k = (int) Math.ceil(from / 260f); k * 260f < to; k++) {
            float y = k * 260f, c = map.roadCentre(y);
            for (int side = -1; side <= 1; side += 2) {
                float x = c + side * (Biome.of(1).roadWidth * 0.5f + 24f);
                if (x >= x0 && x < x0 + span && y >= y0 && y < y0 + span)
                    out.add(new Prop(LAMP, 1, x, y, 0.5f, hash(k, side)));
            }
        }
    }

    /** The dream-pillars: twelve round each mandala of Svapna Mandala, on its outer ring. */
    private void pillars(float x0, float y0, float span, List<Prop> out) {
        if (starts.length < 5) return;
        pillarBand(x0, y0, span, out, 3);                       // Svapna Mandala
        if (starts.length > 10) pillarBand(x0, y0, span, out, 9); // and the Bhuta Court, Vitala's own circle of rings
    }

    private void pillarBand(float x0, float y0, float span, List<Prop> out, int band) {
        float lo = Math.max(y0, starts[band]), hi = Math.min(y0 + span, starts[band + 1]);
        if (lo >= hi) return;
        for (float probe = lo; probe < hi + 2400f; probe += 1200f) {
            float cy = map.mandalaCentreY(Math.min(probe, hi - 1f));
            for (int k = 0; k < 12; k++) {
                float a = k * (float) Math.PI / 6f, x = worldW * 0.5f + (float) Math.cos(a) * 1050f, y = cy + (float) Math.sin(a) * 1050f;
                if (x >= x0 && x < x0 + span && y >= y0 && y < y0 + span && y >= starts[band] && y < starts[band + 1]
                        && !contains(out, x, y))
                    out.add(new Prop(PILLAR, band, x, y, 0.5f, hash(k, (int) cy)));
            }
        }
    }

    private static boolean contains(List<Prop> out, float x, float y) {
        for (Prop p : out) if (p.kind == PILLAR && Math.abs(p.x - x) < 1f && Math.abs(p.y - y) < 1f) return true;
        return false;
    }

    private static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }
}
