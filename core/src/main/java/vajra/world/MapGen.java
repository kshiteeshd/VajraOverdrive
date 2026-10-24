package vajra.world;

import java.util.Random;

/** Worldgen factories: the static tile grids a run is built from. Everything here runs once,
 *  at build time — the game only ever reads the finished grids. */
final class MapGen {
    private MapGen() {}

    /** Svarna Dvara: open golden canyon. */
    static TileMap svarnaDvara(float ox, float oy, int cols, int rows) {
        return cavern(ox, oy, cols, rows, 11, 0.78, 1.0, 0);
    }

    /** Organic cavern. Noise + flank bias makes solid bounds that dissolve toward a winding centre corridor.
     *  fill: higher = more open. stretchY: higher = taller vertical trunks. Mirrored about the centre column. */
    static TileMap cavern(float ox, float oy, int cols, int rows, long seed, double fill, double stretchY, int seams) {
        TileMap m = new TileMap(ox, oy, cols, rows);
        Random rng = new Random(seed);
        int mid = cols / 2, gap = Math.max(2, cols / 18);
        int gw = Math.max(4, cols / 10), gh = Math.max(4, (int) (rows / 10 / stretchY));
        double[][] lat = new double[gw + 2][mid + 2];
        for (int j = 0; j < lat[0].length; j++)
            for (int i = 0; i < lat.length; i++) lat[i][j] = rng.nextDouble();
        for (int ty = 0; ty < rows; ty++) {
            for (int tx = 0; tx < cols; tx++) {
                int mx = Math.min(tx, cols - 1 - tx);
                double fx = (double) mx / (cols - 1) * gw, fy = (double) ty / (rows - 1) * gh;
                int ix = (int) fx, iy = (int) fy;
                double ax = fx - ix, ay = fy - iy;
                double n = lat[ix][iy] * (1 - ax) * (1 - ay) + lat[ix + 1][iy] * ax * (1 - ay)
                        + lat[ix][iy + 1] * (1 - ax) * ay + lat[ix + 1][iy + 1] * ax * ay;
                double flank = (double) Math.abs(tx - mid) / mid;
                boolean rock = n + flank * flank * 0.7 > fill;
                if (tx <= 1 || tx >= cols - 2) rock = true;
                if ((ty == 0 || ty == rows - 1) && Math.abs(tx - mid) > gap) rock = true;
                if ((ty == 0 || ty == rows - 1) && Math.abs(tx - mid) <= gap) rock = false;
                if (rock) m.set(tx, ty, TileMap.ROCK);
            }
        }
        for (int pass = 0; pass < 2; pass++) smooth(m, mid, gap);
        removeSpecks(m);
        carveChambers(m, seed, seams);
        for (int c = mid - gap; c <= mid + gap; c++) { m.set(c, 0, TileMap.EMPTY); m.set(c, rows - 1, TileMap.EMPTY); }
        trim(m);
        return m;
    }

    /** Chamber chain: wide open ellipses joined by narrow necks, mirrored side pockets off each chamber.
     *  First and last chambers are widened — the vista chambers at region boundaries. */
    private static void carveChambers(TileMap m, long seed, int seams) {
        Random rng = new Random(seed ^ 0x5F3759DFL);
        int mid = m.cols / 2;
        int bands = Math.max(2, m.rows / 16);
        int[] cys = new int[bands];
        int[] halfHs = new int[bands];
        int[] halfWs = new int[bands];
        int[] neckWs = new int[bands];
        for (int b = 0; b < bands; b++) {
            int bandH = m.rows / bands;
            cys[b] = bandH * b + bandH / 2;
            halfHs[b] = Math.max(3, bandH / 2 - 1);
            halfWs[b] = 8 + rng.nextInt(Math.max(4, m.cols / 10));
            if (b == 0 || b == bands - 1) halfWs[b] = (int) (halfWs[b] * 1.5);
        }
        for (int b = 0; b < bands; b++) {
            carveEllipse(m, mid, cys[b], halfWs[b], halfHs[b]);
            if (b + 1 < bands) {
                neckWs[b] = 2 + rng.nextInt(2);
                int neck = neckWs[b];
                for (int x = mid - neck; x <= mid + neck; x++)
                    for (int y = cys[b]; y <= cys[b + 1]; y++) m.set(x, y, TileMap.EMPTY);
            }
            if (rng.nextBoolean()) {
                int px = Math.min(m.cols - 12, mid + halfWs[b] + 6 + rng.nextInt(6));
                int py = cys[b] + (rng.nextInt(5) - 2);
                int phw = 3 + rng.nextInt(3), phh = 3 + rng.nextInt(3);
                carveEllipse(m, px, py, phw, phh);
                carveEllipse(m, m.cols - 1 - px, py, phw, phh);
                for (int y = py - 1; y <= py + 1; y++)
                    for (int x = mid; x <= px; x++) m.set(x, y, TileMap.EMPTY);
                for (int y = py - 1; y <= py + 1; y++)
                    for (int x = m.cols - 1 - px; x <= mid; x++) m.set(x, y, TileMap.EMPTY);
            }
        }
        // Maya's lie: gold seams across some necks look like solid walls but hide two small notches.
        for (int b = 0; b + 1 < bands && seams > 0; b++) {
            if (rng.nextBoolean()) continue;
            seams--;
            int y = (cys[b] + cys[b + 1]) / 2;
            int neck = neckWs[b];
            for (int x = mid - 6; x <= mid + 6; x++) {
                boolean notch = (x >= mid - neck && x <= mid - neck + 1) || (x >= mid + neck - 1 && x <= mid + neck);
                if (!notch) m.set(x, y, TileMap.GOLD);
            }
        }
    }

    private static void carveEllipse(TileMap m, int cx, int cy, int hw, int hh) {
        for (int ty = cy - hh; ty <= cy + hh; ty++)
            for (int tx = cx - hw; tx <= cx + hw; tx++) {
                if (tx < 2 || tx > m.cols - 3 || ty < 0 || ty >= m.rows) continue;
                double nx = (tx - cx) / (double) hw, ny = (ty - cy) / (double) hh;
                if (nx * nx + ny * ny <= 1.0) m.set(tx, ty, TileMap.EMPTY);
            }
    }

    private static void smooth(TileMap m, int mid, int gap) {
        int[] next = new int[m.cols * m.rows];
        for (int ty = 0; ty < m.rows; ty++) {
            for (int tx = 0; tx < m.cols; tx++) {
                if (tx <= 1 || tx >= m.cols - 2) { next[ty * m.cols + tx] = m.get(tx, ty); continue; }
                if ((ty == 0 || ty == m.rows - 1) && Math.abs(tx - mid) <= gap) { next[ty * m.cols + tx] = TileMap.EMPTY; continue; }
                int n = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++)
                        if ((dx != 0 || dy != 0) && m.get(tx + dx, ty + dy) != TileMap.EMPTY) n++;
                next[ty * m.cols + tx] = (m.get(tx, ty) != TileMap.EMPTY) ? (n >= 4 ? TileMap.ROCK : TileMap.EMPTY) : (n >= 5 ? TileMap.ROCK : TileMap.EMPTY);
            }
        }
        System.arraycopy(next, 0, m.cells, 0, m.cells.length);
    }

    /** Deletes floating rock blobs smaller than 8 cells. Bound-touching rock always stays. */
    private static void removeSpecks(TileMap m) {
        boolean[][] seen = new boolean[m.cols][m.rows];
        final int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
        for (int ty = 0; ty < m.rows; ty++) {
            for (int tx = 0; tx < m.cols; tx++) {
                if (seen[tx][ty] || m.get(tx, ty) == TileMap.EMPTY) continue;
                java.util.ArrayList<int[]> blob = new java.util.ArrayList<>();
                java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
                q.add(new int[] { tx, ty });
                seen[tx][ty] = true;
                boolean bound = false;
                while (!q.isEmpty()) {
                    int[] c = q.poll();
                    blob.add(c);
                    if (c[0] <= 1 || c[0] >= m.cols - 2) bound = true;
                    for (int[] d : dirs) {
                        int nx = c[0] + d[0], ny = c[1] + d[1];
                        if (nx < 0 || ny < 0 || nx >= m.cols || ny >= m.rows || seen[nx][ny]) continue;
                        if (m.get(nx, ny) == TileMap.EMPTY) continue;
                        seen[nx][ny] = true;
                        q.add(new int[] { nx, ny });
                    }
                }
                if (!bound && blob.size() < 8) {
                    for (int[] c : blob) m.set(c[0], c[1], TileMap.EMPTY);
                }
            }
        }
    }

    /** Rock touching sky gets gold trim; rock buried deep goes dark. */
    private static void trim(TileMap m) {
        for (int ty = 0; ty < m.rows; ty++) {
            for (int tx = 0; tx < m.cols; tx++) {
                if (m.get(tx, ty) == TileMap.EMPTY) continue;
                if (m.get(tx + 1, ty) == TileMap.EMPTY || m.get(tx - 1, ty) == TileMap.EMPTY
                        || m.get(tx, ty + 1) == TileMap.EMPTY || m.get(tx, ty - 1) == TileMap.EMPTY) {
                    m.set(tx, ty, TileMap.GOLD);
                    continue;
                }
                boolean deep = true;
                for (int dy = -2; dy <= 2 && deep; dy++)
                    for (int dx = -2; dx <= 2 && deep; dx++)
                        if (m.get(tx + dx, ty + dy) == TileMap.EMPTY) deep = false;
                m.set(tx, ty, deep ? TileMap.DARK : TileMap.ROCK);
            }
        }
    }

    /** Maya Vana: dense illusion forest with vertical trunks. */
    static TileMap mayaVana(float ox, float oy, int cols, int rows) {
        return cavern(ox, oy, cols, rows, 22, 0.78, 2.2, 1);
    }

    /** Svapna Mandala: drifting dream circle, open with soft pockets. */
    static TileMap svapnaMandala(float ox, float oy, int cols, int rows) {
        return cavern(ox, oy, cols, rows, 33, 0.80, 1.5, 2);
    }

    /** Bala Mukha: the approach darkens, heavier rock that still leaves sky. */
    static TileMap balaMukha(float ox, float oy, int cols, int rows) {
        return cavern(ox, oy, cols, rows, 44, 0.74, 1.2, 2);
    }

    /** Bala arena: open fighting pit ringed by a noise-broken rock annulus with four passages. */
    static TileMap balaArena(float ox, float oy, int cols, int rows) {
        TileMap m = new TileMap(ox, oy, cols, rows);
        Random rng = new Random(55);
        int mid = cols / 2, cy = rows / 2, gap = Math.max(2, cols / 18);
        double baseR = Math.min(cols, rows) * 0.38;
        double[] wobble = new double[36];
        for (int i = 0; i < wobble.length; i++) wobble[i] = rng.nextDouble() * 6 - 3;
        for (int ty = 0; ty < rows; ty++) {
            for (int tx = 0; tx < cols; tx++) {
                double dx = tx - mid, dy = (ty - cy) * 1.4;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double ang = (Math.atan2(dy, Math.abs(dx)) + Math.PI) / (Math.PI * 2) * wobble.length;
                double r = baseR + wobble[(int) ang % wobble.length];
                boolean cardinal = Math.abs(tx - mid) <= 6 || Math.abs(ty - cy) <= 5;
                if (!cardinal && Math.abs(dist - r) < 3.5) m.set(tx, ty, TileMap.ROCK);
                if (tx <= 1 || tx >= cols - 2) m.set(tx, ty, TileMap.ROCK);
                if (ty == 0 || ty == rows - 1) {
                    if (Math.abs(tx - mid) > gap) m.set(tx, ty, TileMap.ROCK);
                }
            }
        }
        for (int c = mid - gap; c <= mid + gap; c++) m.set(c, 0, TileMap.EMPTY);
        trim(m);
        return m;
    }

    // ---- VITALA: the river cavern (docs/VITALA_PLAN.md) --------------------------------------------------------------

    /** What a side cavern is FOR (docs/TREASURE_CHESTS.md): every pocket off the river holds something. */
    public static final int VAULT = Chest.VAULT, WELL = Chest.WELL, DEN = Chest.DEN;

    /** The side caverns of Vitala, {cx, cy, rx, ry, side, type}: one off the river every ~600 px through the four river bands, alternating
     *  sides. A VAULT holds a treasure chest, a WELL an ember well that mends the hull, a DEN a chest guarded by an ambush that wakes when it is
     *  touched. */
    static java.util.List<float[]> pockets() {
        java.util.ArrayList<float[]> out = new java.util.ArrayList<>();
        Random rng = new Random(0x417A1AL);
        int k = 0;
        for (float y = Hataki.Y0 + 3300f; y < Hataki.START[5] - 500f; y += 560f + rng.nextInt(160)) {
            boolean nearGate = false;
            for (int g = 2; g <= 4; g++) nearGate |= Math.abs(y - Hataki.START[g]) < 460f;
            if (nearGate) continue;
            float side = (k & 1) == 0 ? -1f : 1f;
            float rx = 250f + rng.nextInt(110), ry = 150f + rng.nextInt(80);
            float reach = Hataki.half(y) + 190f + 300f + rx + rng.nextInt(80);
            float cx = Math.max(rx + 120f, Math.min(Hataki.WIDTH - rx - 120f, Hataki.centre(y) + side * reach));
            int type = new int[] {VAULT, WELL, DEN, VAULT, DEN, WELL}[k % 6];
            out.add(new float[] {cx, y, rx, ry, side, type});
            k++;
        }
        return out;
    }

    /** Half the width of the river cavern at a height (river, banks and the swelling chambers). */
    static float cavernHalf(float y) {
        float chamber = 320f * (float) Math.pow(Math.max(0f, Math.sin(y / 700f + 0.4f)), 2.0);
        return Hataki.half(y) + 190f + chamber;
    }

    /** The Gold Assay bays: one in each river band, on alternating banks, each with a hall of its own cut into the bank. {x, y} */
    static float[][] shopSpots() {
        float[][] out = new float[4][2];
        for (int i = 0; i < 4; i++) {
            int r = i + 1;
            float y = Hataki.START[r] + 0.40f * (Hataki.END[r] - Hataki.START[r]);
            out[i][0] = Math.max(420f, Math.min(Hataki.WIDTH - 420f, Hataki.centre(y) + (i % 2 == 0 ? 1f : -1f) * (cavernHalf(y) + 150f)));
            out[i][1] = y;
        }
        return out;
    }

    private static final float[][] SHOPS = shopSpots();

    /** Is the world open air at this point: the shaft of the Plunge, the river cavern with its chambers and side caverns, the Altar hall. */
    static boolean vitalaOpen(float wx, float wy, java.util.List<float[]> pockets) {
        float c = Hataki.centre(wy), ly = wy - Hataki.Y0;
        float rough = 40f * (GroundMap.rnd((int) (wx / 64f), (int) (wy / 64f), 0x771A) - 0.5f);
        boolean open;
        if (wy < Hataki.START[1]) {
            float half = 330f + 130f * (float) Math.sin(wy / 330f) + Math.max(0f, 900f - ly) * 0.5f;
            open = Math.abs(wx - c) < half + rough;
        } else if (wy < Hataki.START[5]) {
            float half = cavernHalf(wy);
            if (wy > Hataki.START[5] - 700f) half = Math.max(half, 300f);
            open = Math.abs(wx - c) < half + rough;
        } else {
            open = false;
        }
        if (!open && wy >= Hataki.START[5] - 40f) { // the Altar: a round forge-hall, and the north passage to the sealed gate
            float dx = (wx - Hataki.ALTAR_X) / 900f, dy = (wy - Hataki.ALTAR_Y) / 780f;
            open = dx * dx + dy * dy < 1f || (wy > Hataki.ALTAR_Y + 600f && Math.abs(wx - Hataki.ALTAR_X) < 200f)
                    || (wy < Hataki.ALTAR_Y && Math.abs(wx - Hataki.ALTAR_X) < 300f);
        }
        if (wy >= Hataki.HEIGHT - 300f) {                                         // SUTALA, COMING SOON: the passage runs on past the gold door into a closed hall
            float hx = (wx - Hataki.HALL_X) / 1000f, hy = (wy - Hataki.HALL_Y) / 720f;
            open = (Math.abs(wx - Hataki.ALTAR_X) < 200f && wy < Hataki.HEIGHT + 500f) || hx * hx + hy * hy < 1f;
        }
        if (!open && wy < Hataki.HEIGHT)
            for (float[] s : SHOPS) {
                float dx = (wx - s[0]) / 330f, dy = (wy - s[1]) / 300f;
                if (dx * dx + dy * dy < 1f) { open = true; break; }
            }
        if (!open)
            for (float[] p : pockets) {
                float dx = (wx - p[0]) / p[2], dy = (wy - p[1]) / p[3];
                if (dx * dx + dy * dy < 1f) { open = true; break; }
                // the tunnel from the cavern's edge to the pocket, three tiles tall
                float lo = Math.min(p[0], c), hi = Math.max(p[0], c);
                if (Math.abs(wy - p[1]) < 52f && wx > lo && wx < hi) { open = true; break; }
            }
        return open;
    }

    /** Vitala's whole cavern as ONE map from its floor to its roof (a pure function of the world position, so no band seams): the shaft, the
     *  river cavern and the Altar hall, then the usual smoothing and trim. It sits on Atala's roof (oy = Hataki.Y0), on the same axis. */
    static TileMap vitala() {
        int cols = 161, rows = (int) Math.ceil((Hataki.LENGTH + Hataki.SUTALA_LEN) / TileMap.TILE);
        TileMap m = new TileMap(24, Hataki.Y0, cols, rows);
        java.util.List<float[]> pockets = pockets();
        for (int ty = 0; ty < rows; ty++)
            for (int tx = 0; tx < cols; tx++) {
                float wx = 24 + tx * TileMap.TILE + 16, wy = Hataki.Y0 + ty * TileMap.TILE + 16;
                if (tx <= 1 || tx >= cols - 2 || !vitalaOpen(wx, wy, pockets)) m.set(tx, ty, TileMap.ROCK);
            }
        smooth(m, cols / 2, 2);
        for (int tx = 0; tx < cols; tx++) m.set(tx, rows - 1, TileMap.ROCK);          // smoothing leaves a gap at each end for a map above to join: Sutala's hall is the roof of the world
        removeSpecks(m);
        trim(m);
        return m;
    }

    /** Full-width gate wall across a band boundary, doorway on the centre column so it lands
     *  on both neighbouring caverns' mouth (both are open at mid, cols=161 maps: x 2328..2872).
     *  The world spans x 0..5200: this grid runs x -8..5208, so no ship can slip past the ends.
     *  The doorway is open for the wall's full height (a lintel row would seal it); grand gates
     *  step the opening in over the top two rows for an arch, with gold pillars flanking it. */
    static TileMap gateWall(float y, int halfDoor, boolean grand) {
        return gateWall(-8, 163, y, halfDoor, grand);
    }

    /** A gate wall of any width: `cols` columns from `ox`, its doorway on the middle column. */
    static TileMap gateWall(float ox, int cols, float y, int halfDoor, boolean grand) {
        int rows = 6;
        TileMap m = new TileMap(ox, y - rows * TileMap.TILE / 2f, cols, rows);
        int mid = cols / 2;
        for (int ty = 0; ty < rows; ty++)
            for (int tx = 0; tx < cols; tx++) {
                int hd = (grand && ty >= rows - 2) ? halfDoor - 2 : halfDoor;
                if (Math.abs(tx - mid) > hd) m.set(tx, ty, TileMap.ROCK);
            }
        trim(m); // golds every rock cell that touches the opening: the doorframe
        if (grand) { // gold pillars either side of the arch, full height
            for (int ty = 0; ty < rows; ty++)
                for (int s = -1; s <= 1; s += 2)
                    for (int d = halfDoor + 1; d <= halfDoor + 2; d++)
                        m.set(mid + s * d, ty, TileMap.GOLD);
        }
        return m;
    }
}
