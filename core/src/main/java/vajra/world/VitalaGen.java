package vajra.world;

import vajra.enemy.Enemy;

import java.util.ArrayList;
import java.util.List;

/** Build-time construction of VITALA, on the roof of Atala (docs/VITALA_PLAN.md): one long river cavern from the Plunge to the Altar, five gates
 *  across it, the shrines and assay bays on the river's banks, the side caverns each given a purpose (a vault with a treasure chest, an ember well, a
 *  den that guards its chest), the fire vents, and the order of battle - courts that fly formations (docs/VITALA_ENEMIES.md). It APPENDS to a world
 *  Atala has already filled: the generic helpers (court, guards, relax, openNear, the compounds, the torches) are WorldGen's; this class decides
 *  WHERE they go. */
final class VitalaGen {
    private static final float[] GATE_Y = {Hataki.START[1], Hataki.START[2], Hataki.START[3], Hataki.START[4], Hataki.START[5]};
    private static final int[] GATE_DOOR = {6, 5, 5, 5, 6};
    /** World region of Vitala's band b is 6 + b. */
    private static final int R0 = 6;

    private final World w;
    private final WorldGen g;

    VitalaGen(World w, WorldGen g) {
        this.w = w;
        this.g = g;
    }

    /** A point `dx` off the river's centre line at height y, kept inside the world. */
    private static float rx(float y, float dx) {
        return Math.max(260f, Math.min(Hataki.WIDTH - 260f, Hataki.centre(y) + dx));
    }

    void build() {
        w.expandForVitala();
        TileMap cave = MapGen.vitala();
        cave.skin = 1;
        int atala = w.maps.length;
        TileMap[] all = java.util.Arrays.copyOf(w.maps, atala + 1 + GATE_Y.length);
        all[atala] = cave;
        for (int i = 0; i < GATE_Y.length; i++) {
            TileMap gate = MapGen.gateWall(GATE_Y[i], GATE_DOOR[i], i == GATE_Y.length - 1);
            gate.skin = 1;
            all[atala + 1 + i] = gate;
        }
        w.maps = all;
        // the sealed north door of the Altar: gold across the passage, lifted by the kill
        int c0 = (int) ((Hataki.ALTAR_X - 205f - cave.ox) / TileMap.TILE), c1 = (int) ((Hataki.ALTAR_X + 205f - cave.ox) / TileMap.TILE);
        int row = (int) (Hataki.LENGTH / TileMap.TILE) - 5;                    // just under the top of the Altar; Sutala's hall lies beyond
        for (int c = c0; c <= c1; c++) cave.set(c, row, TileMap.GOLD);
        w.sealMaps[1] = cave;
        w.sealRows[1] = row;
        w.sealC0s[1] = c0;
        w.sealC1s[1] = c1;

        List<float[]> pockets = MapGen.pockets();
        w.pockets = pockets.toArray(new float[0][]);
        w.wellUsed = new boolean[w.pockets.length];

        for (int b = 1; b <= 5; b++) { // the shrine of each band stands on the river's bank, deeper than its assay
            int i = 4 + b;                                                   // checkpoint 5..9
            float cy = Hataki.START[b] + 0.6f * (Hataki.END[b] - Hataki.START[b]);
            float cx = b == 5 ? Hataki.ALTAR_X + 360f : rx(cy, (b % 2 == 0 ? -1 : 1) * (Hataki.half(cy) + 90f));
            float[] spot = g.openNear(cx, cy, Hataki.START[b] + 40, Hataki.END[b] - 40);
            w.checkpoints[i] = new Checkpoint(spot[0], spot[1]);
        }
        for (int b = 1; b <= 5; b++) { // a cache in the first den of each band, the Altar's in its hall
            int i = 4 + b;
            float cx = Hataki.ALTAR_X - 420f, cy = Hataki.START[b] + 700f;
            for (float[] p : pockets)
                if (p[1] >= Hataki.START[b] && p[1] < Hataki.END[b] && p[5] == MapGen.DEN) { cx = p[0]; cy = p[1] + 40f; break; }
            float[] spot = g.openNear(cx, cy, Hataki.START[b] + 40, Hataki.END[b] - 40);
            w.caches[i][0] = spot[0];
            w.caches[i][1] = spot[1];
        }
        float[][] spots = MapGen.shopSpots();
        for (int i = 0; i < 4; i++) { // the Gold Assay: a forge niche cut into the bank, alternating sides
            float[] spot = g.openNearShop(spots[i][0], spots[i][1], Hataki.START[i + 1] + 300, Hataki.END[i + 1] - 300);
            w.shops[4 + i][0] = spot[0];
            w.shops[4 + i][1] = spot[1];
        }
        int firstEnemy = w.enemies.size();
        spawn();
        g.buildCompounds(4);
        int posted = w.enemies.size();
        for (int b = 1; b <= 5; b++) g.guards(w.checkpoints[4 + b].x, w.checkpoints[4 + b].y, R0 + b, Enemy.Kind.DIPAKA);
        for (int i = 0; i < 4; i++) g.guards(w.shops[4 + i][0], w.shops[4 + i][1], R0 + i + 1, Enemy.Kind.VANIJA);
        for (int j = posted; j < w.enemies.size(); j++) w.enemies.get(j).siteGuard = true;
        placePockets();
        // THE PEOPLE of Vitala: the Gold-Seller by the river in the Mouth, Gauri's Maid in the Bhuta Court
        g.addVoice(Enemy.Kind.LANCE, "SELLER", Hataki.centre(Hataki.START[1] + 1700f) + 330f, 0.37f, R0 + 1);
        g.addVoice(Enemy.Kind.LANCE, "MAID", Hataki.centre(Hataki.START[3] + 2300f) - 330f, 0.42f, R0 + 3);
        g.relax();
        ArrayList<float[]> tl = new ArrayList<>(java.util.Arrays.asList(w.torches));
        for (int i = 0; i < GATE_Y.length; i++) {
            float dx = GATE_DOOR[i] * TileMap.TILE - 16;
            g.mount(tl, Hataki.CX - dx, GATE_Y[i]);
            g.mount(tl, Hataki.CX + dx, GATE_Y[i]);
        }
        for (int i = 5; i < 10; i++) {
            Checkpoint c = w.checkpoints[i];
            g.mount(tl, c.x - 46, c.y);
            g.mount(tl, c.x + 46, c.y);
        }
        for (int i = 4; i < 8; i++) {
            float[] s = w.shops[i];
            g.mount(tl, s[0] - 46, s[1] - 24);
            g.mount(tl, s[0] + 46, s[1] - 24);
        }
        w.torches = tl.toArray(new float[0][]);
        placeVents();
        // the second stage's seal: a kill quota sized off Vitala's roster alone, so it is always obtainable
        int[] placed = new int[Enemy.Kind.values().length];
        for (int i = firstEnemy; i < w.enemies.size(); i++) placed[w.enemies.get(i).kind.ordinal()]++;
        for (Enemy.Kind k : Enemy.Kind.values())
            if (k != Enemy.Kind.BALA && !k.keeper())
                w.stageQuota[1][k.ordinal()] = Math.min(placed[k.ordinal()], Math.max(3, placed[k.ordinal()] / 8));
    }

    // ---- THE ORDER OF BATTLE -------------------------------------------------------------------------------------------------------

    /** One court of Vitala: a commander (rank 1) and its followers, which will fly its formation (VitalaBrain). */
    private void court(Enemy.Kind cmd, Enemy.Kind ring, float y, float dx, int n, int backups, float wake) {
        if (courtNo++ % 3 == 2) return;                                    // Vitala is thinned: every third court is not sent (the river is long, the fights are meant to matter)
        n = Math.max(2, n - 1);
        backups = Math.max(1, backups - 1);
        float x = rx(y, dx);
        float[] spot = g.openNear(x, y, y - 200f, y + 200f);
        g.court(cmd, ring, spot[0], spot[1], n, backups, 90f, 40f, wake);
    }

    private int courtNo;

    private void elite(Enemy.Kind k, float y, float dx) {
        float[] spot = g.openNear(rx(y, dx), y, y - 150f, y + 150f);
        Enemy e = new Enemy(k, spot[0], spot[1]);
        e.elite = true;
        e.rank = 1;
        e.hp = e.maxHp = e.maxHp * 2;
        e.wakeRange = 800;
        w.add(e);
    }

    /** THE GATE-WARDENS: at each gate across the river two of Tamra's ore-hulls stand either side of the door, the way guards would, and wake when the hull comes up the road. */
    private void wardens(float gateY, int door) {
        float dx = door * TileMap.TILE + 90f;
        for (float side : new float[] {-1f, 1f}) {
            float[] spot = g.openNear(Hataki.CX + side * dx, gateY - 240f, gateY - 420f, gateY - 120f);
            Enemy e = new Enemy(Enemy.Kind.BHAVI, spot[0], spot[1]);
            e.sentry = true;
            e.wakeRange = 760;
            w.add(e);
        }
    }

    /** A sentry pair that floats on the river itself (Kila, the nails). */
    private void pair(float y) {
        float x = Hataki.centre(y);
        for (float dx : new float[] {-90f, 90f}) {
            float[] spot = g.openNear(x + dx, y, y - 100f, y + 100f);
            Enemy e = new Enemy(Enemy.Kind.LANCE, spot[0], spot[1]);
            e.sentry = true;
            e.wakeRange = 760;
            w.add(e);
        }
    }

    private static final Enemy.Kind AGNEYA = Enemy.Kind.VAKRA, JVALA = Enemy.Kind.MAYAVI, TAMRA = Enemy.Kind.BHAVI, KILA = Enemy.Kind.LANCE,
            DHUMA = Enemy.Kind.AURIS, REKHA = Enemy.Kind.HATAKA;

    /** Vitala's order of battle: each band heavier than the last, every court a formation of its own kind (docs/VITALA_ENEMIES.md). */
    private void spawn() {
        // band 1 - Hataki Mouth: the gentle band - Dhuma, Kila and the first Agneya, on the broad gold
        float y0 = Hataki.START[1];
        pair(y0 + 350f);
        wardens(Hataki.START[2], GATE_DOOR[1]);
        wardens(Hataki.START[3], GATE_DOOR[2]);
        wardens(Hataki.START[4], GATE_DOOR[3]);
        float[] d1 = {-260f, 220f, -80f};
        Enemy.Kind[][] mouth = {{DHUMA, DHUMA}, {KILA, KILA}, {AGNEYA, DHUMA}, {DHUMA, DHUMA}, {KILA, DHUMA}, {AGNEYA, AGNEYA}, {DHUMA, KILA}};
        for (int i = 0; i < mouth.length; i++) court(mouth[i][0], mouth[i][1], y0 + 800f + i * 580f, d1[i % 3], 3 + (i & 1), 1 + (i % 3 == 0 ? 1 : 0), 900f);
        pair(Hataki.END[1] - 360f);
        // band 2 - the Smelters: Agneya wedges, Tamra's shield-lines with Kila behind them, the first Jvala; the vents teach the beat
        float y1 = Hataki.START[2];
        Enemy.Kind[][] smelt = {{AGNEYA, AGNEYA}, {TAMRA, KILA}, {JVALA, DHUMA}, {AGNEYA, AGNEYA}, {TAMRA, TAMRA}, {JVALA, AGNEYA}, {TAMRA, KILA},
                {AGNEYA, JVALA}, {JVALA, DHUMA}, {TAMRA, KILA}};
        for (int i = 0; i < smelt.length; i++) court(smelt[i][0], smelt[i][1], y1 + 450f + i * 540f, (i % 2 == 0 ? -1 : 1) * (60f + 80f * (i % 3)), 4, 2, 950f);
        elite(TAMRA, y1 + 2900f, 120f);
        pair(Hataki.END[2] - 380f);
        // band 3 - the Bhuta Court: Rekha's grids, Dhuma's packs in the smoke, Jvala's columns; the first elites
        float y2 = Hataki.START[3];
        Enemy.Kind[][] court = {{REKHA, REKHA}, {DHUMA, DHUMA}, {JVALA, DHUMA}, {REKHA, REKHA}, {TAMRA, REKHA}, {JVALA, JVALA}, {REKHA, DHUMA},
                {DHUMA, KILA}, {REKHA, REKHA}, {JVALA, REKHA}};
        for (int i = 0; i < court.length; i++) court(court[i][0], court[i][1], y2 + 500f + i * 520f, (i % 2 == 0 ? 1 : -1) * (80f + 70f * (i % 3)), 4, 2, 1000f);
        elite(REKHA, y2 + 2500f, -150f);
        elite(JVALA, y2 + 4300f, 150f);
        pair(Hataki.END[3] - 380f);
        // band 4 - the Hearth Deep: everything at once, heavier courts, elites holding the rapids
        float y3 = Hataki.START[4];
        Enemy.Kind[][] hearth = {{TAMRA, KILA}, {AGNEYA, AGNEYA}, {JVALA, TAMRA}, {REKHA, REKHA}, {AGNEYA, DHUMA}, {TAMRA, TAMRA}, {JVALA, REKHA}, {KILA, KILA},
                {AGNEYA, AGNEYA}, {JVALA, DHUMA}};
        for (int i = 0; i < hearth.length; i++) court(hearth[i][0], hearth[i][1], y3 + 420f + i * 500f, (i % 2 == 0 ? -1 : 1) * (50f + 70f * (i % 3)), 5, 2, 1000f);
        for (int i = 0; i < 3; i++) elite(i == 0 ? TAMRA : i == 1 ? AGNEYA : JVALA, y3 + 1400f + i * 1500f, (i % 2 == 0 ? 1 : -1) * 130f);
        pair(Hataki.END[4] - 380f);
        // band 5 - the Altar: the honor guard round the hearth, the boss alone behind the seal
        float ay = Hataki.ALTAR_Y;
        pair(Hataki.START[5] + 380f);
        for (float[] p : new float[][] {{Hataki.ALTAR_X + 480f, ay}, {Hataki.ALTAR_X - 480f, ay}, {Hataki.ALTAR_X, ay + 560f}, {Hataki.ALTAR_X, ay - 520f}}) {
            Enemy e = new Enemy(TAMRA, p[0], p[1]);
            e.wakeRange = 1300;
            w.add(e);
        }
        for (float[] p : new float[][] {{Hataki.ALTAR_X + 700f, ay + 260f}, {Hataki.ALTAR_X - 700f, ay + 260f}, {Hataki.ALTAR_X + 700f, ay - 260f},
                {Hataki.ALTAR_X - 700f, ay - 260f}}) {
            Enemy e = new Enemy(KILA, p[0], p[1]);
            e.sentry = true;
            e.wakeRange = 1300;
            w.add(e);
        }
    }

    // ---- THE SIDE CAVERNS (docs/TREASURE_CHESTS.md) -------------------------------------------------------------------------------

    /** Every pocket holds something: a VAULT a treasure chest, a DEN a chest guarded by an ambush that must fall first, a WELL an ember well that
     *  mends the hull once. */
    private void placePockets() {
        for (int i = 0; i < w.pockets.length; i++) {
            float[] p = w.pockets[i];
            int type = (int) p[5];
            float[] spot = g.openNear(p[0] + p[4] * 40f, p[1], p[1] - 160f, p[1] + 160f);
            if (type == MapGen.WELL) {                                      // the well sits at the pocket's heart
                w.pockets[i][0] = spot[0];
                w.pockets[i][1] = spot[1];
                continue;
            }
            Chest c = new Chest(spot[0], spot[1], 1, i);
            if (type == MapGen.DEN) {                                       // the ambush: a leader and two, bound to the chest
                c.guards = 1;
                Enemy.Kind lead = i % 3 == 0 ? TAMRA : i % 3 == 1 ? AGNEYA : REKHA;
                Enemy L = new Enemy(lead, spot[0], spot[1] + 40f);
                L.rank = 1;
                L.siteGuard = true;
                L.wakeRange = 520;
                L.sentry = true;
                w.add(L);
                for (float dx : new float[] {-70f, 70f}) {
                    Enemy f = new Enemy(i % 2 == 0 ? KILA : DHUMA, spot[0] + dx, spot[1] + 10f);
                    f.commander = L;
                    f.siteGuard = true;
                    f.wakeRange = 520;
                    w.add(f);
                }
            }
            w.chests.add(c);
        }
    }

    /** THE FIRE VENTS: in the Smelters (one clock, so the beat can be learned) and the Hearth Deep (two clocks half a beat apart), a vent in
     *  the bank wall every few hundred px, alternating sides, breathing across the river. Not within 280 px of a shrine or an assay. */
    private void placeVents() {
        java.util.ArrayList<float[]> out = new java.util.ArrayList<>();
        int k = 0;
        for (int band : new int[] {2, 4}) {
            float step = band == 2 ? 440f : 360f;
            for (float y = Hataki.START[band] + 400f; y < Hataki.END[band] - 400f; y += step) {
                float side = (k & 1) == 0 ? -1f : 1f;
                float cx = Hataki.centre(y), x = cx;
                while (Math.abs(x - cx) < 900f && !w.blockedPx(x, y)) x += side * 8f;           // walk to the wall face
                if (!w.blockedPx(x, y) || Math.abs(x - cx) < Hataki.half(y) + 40f) { k++; continue; }
                float vx = x - side * 6f;
                boolean near = false;
                for (Checkpoint c : w.checkpoints) near |= c != null && Math.hypot(c.x - vx, c.y - y) < 280f;
                for (float[] s : w.shops) near |= Math.hypot(s[0] - vx, s[1] - y) < 280f;
                if (near) { k++; continue; }
                float offset = band == 2 ? 0f : (k % 2) * 1.5f;
                out.add(new float[] {vx, y, side < 0 ? 0f : (float) Math.PI, offset});
                k++;
            }
        }
        // the Altar's rim: six vents in the hall wall breathing inward, open only while Bhairava kindles (the boss's second phase)
        for (int a = 0; a < 6; a++) {
            double ang = Math.toRadians(a * 60 + 30);
            float dx = (float) Math.cos(ang), dy = (float) Math.sin(ang);
            float r = 200f;
            while (r < 1000f && !w.blockedPx(Hataki.ALTAR_X + dx * r, Hataki.ALTAR_Y + dy * r)) r += 8f;
            if (r >= 1000f) continue;
            float vx = Hataki.ALTAR_X + dx * (r - 6f), vy = Hataki.ALTAR_Y + dy * (r - 6f);
            out.add(new float[] {vx, vy, (float) (ang + Math.PI), 0f, 1f});
        }
        w.vents.at = out.toArray(new float[0][]);
    }
}
