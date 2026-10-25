package vajra.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.graphics.OrthographicCamera;
import vajra.world.Checkpoint;
import vajra.world.Effect;
import vajra.enemy.Enemy;
import vajra.world.Pickup;
import vajra.world.Popups;
import vajra.world.TileMap;
import vajra.world.World;
import vajra.world.LokaTable;
import vajra.ship.Ship;

/** Every world-space draw pass in one place, in fixed order: nebula, backdrop, plants, tiles,
 *  torches, enemies, loot, signs, bullets, sparks, hp bars, boss bar, hull, effects, shield, lights.
 *  Owns all image resources for the world (one instance per play session). The camera is
 *  positioned by PlayState before this runs; lights close the frame so overlays can dim it. */
public final class WorldRenderer {
    private final SpriteBatch batch;
    private final Ui ui;
    private final ShapeRenderer shapes;
    private final TileImages tiles;
    private final OrbImages orbs;
    private final PickupImages loot;
    private final CheckpointImages cps;
    private final VentImages ventArt;
    private final CheckpointImages cpsV;     // the same lamp-altar in Vitala's palette
    private final ShopImages shopSignV;      // and the Gold Assay
    private final ChestImages chestArt;
    private final HazardFx hazardFx;
    private final FlowFx flowFx = new FlowFx();
    private final ShopImages shopSign;
    private final NebulaImages nebula;
    private final EnemyImages foes;
    private final NpcImages people;
    private final BulletImages shots;
    private final FxImages fx;
    private final ShipRenderer shipR;
    private final GroundLayer ground;
    private final TerrainLayer terrain;
    private final PropIndex propIndex;
    private final java.util.ArrayList<float[]> glows = new java.util.ArrayList<>();
    private final float[] haze = new float[3];
    private float fly = Altitude.fly(0f);
    private final float[] ambient = new float[3], mistC = new float[4];
    private final Lights lights;
    private final PopupArt popupArt; // the pre-rendered Devanagari phrases

    public WorldRenderer(SpriteBatch batch, Ui ui, World world) {
        this.batch = batch;
        this.ui = ui;
        this.shapes = ui.shapes;
        this.tiles = new TileImages();
        this.orbs = new OrbImages();
        this.loot = new PickupImages();
        this.cps = new CheckpointImages();
        this.ventArt = new VentImages();
        this.cpsV = new CheckpointImages(1);
        this.shopSignV = new ShopImages(1);
        this.chestArt = new ChestImages();
        this.hazardFx = new HazardFx();
        this.shopSign = new ShopImages();
        this.nebula = new NebulaImages();
        this.foes = new EnemyImages();
        this.people = new NpcImages();
        this.shots = new BulletImages();
        this.fx = new FxImages();
        this.shipR = new ShipRenderer(fx);
        vajra.world.GroundMap land = new vajra.world.GroundMap(vajra.world.GroundMap.SEED, world.width, world.height, world.regionStart);
        this.ground = new GroundLayer(land);
        vajra.world.PropField field = new vajra.world.PropField(land, world.width, world.regionStart);
        this.propIndex = new PropIndex(field);
        this.terrain = new TerrainLayer(world, land, field);
        this.lights = new Lights();
        this.popupArt = new PopupArt();
        this.moteTex = makeMote();
        this.mirage = new MirageFx(moteTex);
        this.chunks = new TileChunks(tiles);
    }

    /** The ship art, borrowed by the hangar for its hull preview. */
    public ShipRenderer ships() {
        return shipR;
    }

    public void render(World world, OrthographicCamera camera, FitViewport viewport) {
        float viewW = camera.viewportWidth * camera.zoom;
        float viewH = camera.viewportHeight * camera.zoom;
        float l = camera.position.x - viewW / 2;
        float b = camera.position.y - viewH / 2;

        tiles.setHull(world.ship.x, world.ship.y, world.time());
        chunksOk = false;
        if (world.walled) {                                    // the walls are baked in 512 px chunks: make sure the view (and the deepest layer's wider view) is covered
            float reach = 1f / LayerStack.scale(10, STACK_SHRINK) + 0.08f;
            // a pulled-back camera (an astra at work) shows far more world: bake a few a frame, not eight, so the frame never stalls...
            chunksOk = chunks.ensure(world, camera.position.x - viewW * reach / 2 - 120, camera.position.y - viewH * reach / 2 - 120,
                    camera.position.x + viewW * reach / 2 + 120, camera.position.y + viewH * reach / 2 + 120, camera.zoom > 1.3f ? 3 : 8);
            if (chunks.baked > 0) viewport.apply();            // baking set the viewport to a chunk's; put the window's back
            float warm = world.astraWarm();                    // ...and while an astra is being formed, bake the view it is about to pull back to, two chunks a frame, ahead of time
            if (warm > camera.zoom + 0.05f) {
                float wvw = camera.viewportWidth * warm, wvh = camera.viewportHeight * warm;
                chunks.ensure(world, camera.position.x - wvw * reach / 2 - 120, camera.position.y - wvh * reach / 2 - 120,
                        camera.position.x + wvw * reach / 2 + 120, camera.position.y + wvh * reach / 2 + 120, 2);
                if (chunks.baked > 0) viewport.apply();
            }
        }
        float wood = vajra.world.Biome.bandWeight(world.ship.y, world.regionStart, 2);   // how much the Mirage Wood holds here (0..1)
        scatter += ((world.ship.boosting ? 1f : 0f) - scatter) * Math.min(1f, Math.min(0.05f, com.badlogic.gdx.Gdx.graphics.getDeltaTime()) * 3.5f);
        vajra.world.Biome.ambient(world.ship.y, world.regionStart, ambient);
        vajra.world.Biome.mist(world.ship.y, world.regionStart, mistC);
        fly = Altitude.fly(world.ship.y);
        OrthographicCamera low = groundCam(camera);   // the ground lies below the flight plane: smaller, so it drifts slower
        float hazeAmt = Math.max(0f, Math.min(0.26f, (fly - 150f) / 900f));   // high over the land, the air thickens
        haze[0] = 0.35f + 0.3f * mistC[0];
        haze[1] = 0.38f + 0.3f * mistC[1];
        haze[2] = 0.42f + 0.3f * mistC[2];
        // the vanishing point leans toward where the hull is going (screen px, eased): the far layers drift with it
        float dt = Math.min(0.05f, com.badlogic.gdx.Gdx.graphics.getDeltaTime());
        float lk = Math.min(1f, dt * 4f);
        leanX += (Math.max(-LayerStack.MAX_LEAN, Math.min(LayerStack.MAX_LEAN, world.ship.vx * 0.05f)) - leanX) * lk;
        leanY += (Math.max(-LayerStack.MAX_LEAN, Math.min(LayerStack.MAX_LEAN, world.ship.vy * 0.05f)) - leanY) * lk;
        Probe.start();
        boolean raycast = terrain.ready();
        // THE STACK (ATALA_PLAN.md): below the meadow the walls are repeated behind the play layer, layer on layer, over a far land.
        float stackN = world.walled ? vajra.world.Biome.layers(world.ship.y, world.regionStart) : 0f;
        float open = Math.min(1f, stackN / 10f);                               // 0 on the meadow, 1 once the shaft is whole
        if (raycast) {                                                          // THE LAND, raycast: the floor, nearer or far
            OrthographicCamera floorCam = camera;
            float floorFly = fly, floorAmb = 1f, floorHaze = hazeAmt;
            if (open > 0f) {
                floorFly = fly + (FLOOR_FLY - fly) * open;
                // the floor slides at the speed of the layer just in front of it (one number, so the two cannot disagree)
                float speed = LayerStack.floorSpeed(stackN, STACK_SHRINK, Altitude.ground(fly), open);
                floorCam = stackFloorCam(camera, LayerStack.floorZoomMul(floorFly, speed));
                floorCam.position.x += LayerStack.leanShift(leanX, speed) * floorCam.zoom;      // and leans with them
                floorCam.position.y += LayerStack.leanShift(leanY, speed) * floorCam.zoom;
                floorCam.update();
                floorAmb = 1f - 0.55f * open;
                floorHaze = hazeAmt + (0.30f - hazeAmt) * open;
            }
            if (open > 0.3f) terrain.drawHalf(batch, floorCam, floorFly, floorAmb, haze, floorHaze, viewport::apply);   // deep in the shaft: half resolution
            else terrain.draw(batch, floorCam, floorFly, 0, floorAmb, haze, floorHaze);
        } else groundLayer(low);                                                // (flat, until the bake lands)
        Probe.lap(0);
        if (stackN > 0.01f) stackLayers(world, camera, stackN, wood);
        else leanKeep();
        Probe.lap(1);
        mistLayer(world, camera, l, b, viewW, viewH);
        backdropLayer(world, camera, l, b, viewW, viewH);
        shadowLayer(world, open > 0.5f ? camera : low);
        Probe.lap(2);
        submergedLayer(world, camera, l, b, viewW, viewH);
        worldSpriteLayer(world, camera, l, b, viewW, viewH);
        Probe.lap(3);
        sparkAndBarLayer(world, camera, l, b, viewW, viewH);
        Probe.lap(4);
        glows.clear();
        for (int i = 0; i < world.shops.length; i++) { // the bay-master's lamps: amber on the sign, a warm bloom on the berth once a hull sits in it
            float[] sp = world.shops[i];
            if (Math.abs(sp[0] - camera.position.x) > viewW * 0.8f || Math.abs(sp[1] - camera.position.y) > viewH * 0.8f) continue;
            float seat = ShopImages.seat(world.shopState[i], world.shopTimer[i]), fl = 0.9f + 0.1f * (float) Math.sin(world.time() * 6f + sp[0]);
            glows.add(new float[] {sp[0] - 42f, sp[1] - 14f, 120f, 0.8f * fl, 0.55f * fl, 0.22f * fl});
            glows.add(new float[] {sp[0] + 40f, sp[1] - 14f, 120f, 0.8f * fl, 0.55f * fl, 0.22f * fl});
            if (seat > 0f) glows.add(new float[] {sp[0] + ShopImages.BERTH_DX, sp[1] + ShopImages.BERTH_DY, 230f, 1f * seat * fl, 0.8f * seat * fl, 0.4f * seat * fl});
        }
        for (vajra.world.Checkpoint cp : world.checkpoints) { // the lamps are the world's light: a warm pool when lit, a faint pink one when cold
            if (Math.abs(cp.x - camera.position.x) > viewW * 0.8f || Math.abs(cp.y - camera.position.y) > viewH * 0.8f) continue;
            float tm = world.time();
            if (cp.collected) {
                float fl = 0.9f + 0.1f * (float) Math.sin(tm * 7f + cp.x);
                glows.add(new float[] {cp.x, cp.y + 6f, 250f, 1f * fl, 0.78f * fl, 0.38f * fl});
            } else {
                float p = 0.55f + 0.45f * (float) Math.sin(tm * 2f);
                glows.add(new float[] {cp.x, cp.y + 10f, 140f, 0.9f * p * 0.7f, 0.35f * p * 0.7f, 0.65f * p * 0.7f});
            }
        }
        chestArt.glows(world.chests, world.pockets, world.wellUsed, l, b, viewW, viewH, world.time(), glows);   // a chest and an ember well glow where they lie
        for (int i = 0; i < world.hazards.patches.size(); i++) {                                               // burning ground is light
            vajra.world.Hazards.Patch hp = world.hazards.patches.get(i);
            if (hp.x < l - 200 || hp.x > l + viewW + 200 || hp.y < b - 200 || hp.y > b + viewH + 200) continue;
            float k = Math.min(1f, hp.life / 1.2f);
            glows.add(new float[] {hp.x, hp.y, hp.r * 2.6f, 0.9f * k, 0.38f * k, 0.08f * k});
        }
        if (world.campaign && world.ship.y > vajra.world.Hataki.Y0 - 1200f) { // Vitala's light is fire and gold: the river lights its own banks, the vents throw their flame
            ventArt.glows(world, l, b, viewW, viewH, glows);
            for (float y = (float) Math.floor((b - 120f) / 150f) * 150f; y < b + viewH + 120f; y += 150f)
                if (vajra.world.Hataki.flows(y)) glows.add(new float[] {vajra.world.Hataki.centre(y), y, 300f, 0.55f, 0.36f, 0.09f});
        }
        if (wood > 0.05f) mirage.glows(world, l, b, viewW, viewH, world.time(), wood, glows);  // the wood's blossoms and mushrooms are its light
        if (open < 0.5f) propIndex.glows(camera.position.x, camera.position.y, low.position.x - low.viewportWidth * low.zoom / 2,
                low.position.y - low.viewportHeight * low.zoom / 2, low.viewportWidth * low.zoom, low.viewportHeight * low.zoom,
                fly, world.time(), glows);
        lights.render(batch, ui.camera, viewport, world, l, b, viewW, viewH, ambient, glows);
        Probe.lap(5);
        shipLayer(world, camera);
        shieldLayer(world, camera);
        Probe.lap(6);
        foregroundLayer(world, camera);
        Probe.lap(7);
        if (raycast && open < 0.5f) terrain.draw(batch, camera, fly, 1, (ambient[0] + ambient[1] + ambient[2]) / 3f * 1.25f, haze, hazeAmt); // what rises above the hull
        Probe.lap(8);
        moteLayer(world, camera, l, b, viewW, viewH);
        Probe.lap(9);
    }

    private final com.badlogic.gdx.graphics.Texture moteTex;
    private final MirageFx mirage;
    private final TileChunks chunks;
    private boolean chunksOk;
    private final OrthographicCamera floorCamBuf = new OrthographicCamera();
    private final OrthographicCamera stackCam = new OrthographicCamera();
    private final float[] deep = new float[3], layerTint = new float[3];
    static final float STACK_SHRINK = 0.98f;

    /** The camera for the far land under a stack: as the shaft opens the land drops away and is drawn twice the size the stack's
     *  depth alone would give it (so it stays the thing you see at the bottom). `open` is 0..1. */
    private OrthographicCamera stackFloorCam(OrthographicCamera camera, float zoomMul) {
        floorCamBuf.viewportWidth = camera.viewportWidth;
        floorCamBuf.viewportHeight = camera.viewportHeight;
        floorCamBuf.position.set(camera.position);
        floorCamBuf.zoom = camera.zoom * zoomMul;
        floorCamBuf.update();
        return floorCamBuf;
    }

    /** How high the far floor's own flight plane is: low, so its tall things do not swing against the layers. */
    private static final float FLOOR_FLY = 150f;

    /** The walls again, behind the play layer: `n` layers (the farthest fractional, fading in), each smaller by 0.96^k about the screen
     *  centre, darker toward the band's deep colour. One batch for the lot. */
    private void stackLayers(World world, OrthographicCamera camera, float n, float wood) {
        vajra.world.Biome.deepTint(world.ship.y, world.regionStart, deep);
        float[] accent = vajra.world.Biome.of(vajra.world.Biome.band(world.ship.y, world.regionStart)).accent;
        int count = (int) Math.ceil(n);
        stackCam.viewportWidth = camera.viewportWidth;
        stackCam.viewportHeight = camera.viewportHeight;
        batch.begin();
        for (int k = count; k >= 1; k--) {
            float s = LayerStack.scale(k, STACK_SHRINK);
            // the vanishing point leans: this layer drifts by lean * (1 - s) on the screen, which is zoom/s world px per screen px
            float wpp = camera.zoom / s;
            stackCam.position.set(camera.position.x - LayerStack.leanShift(leanX, s) * wpp, camera.position.y - LayerStack.leanShift(leanY, s) * wpp, 0);
            stackCam.zoom = wpp;
            stackCam.update();
            float w = stackCam.viewportWidth * stackCam.zoom, h = stackCam.viewportHeight * stackCam.zoom;
            float l = stackCam.position.x - w / 2, b = stackCam.position.y - h / 2;
            LayerStack.tint(k, n, deep, layerTint);
            float a = k == count ? Math.min(1f, n - (count - 1)) : 1f;           // the last one fades in as the shaft opens
            batch.setProjectionMatrix(stackCam.combined);
            if (chunksOk) {                                                        // a handful of baked quads: shadow, side face and walls in one
                chunks.draw(batch, l, b, l + w, b + h, layerTint[0], layerTint[1], layerTint[2], a);
            } else {
                if (k <= SHADOW_LAYERS) // contact shadow: this layer darkens the one behind it, and a thin dark side face makes it thick
                    for (TileMap m : world.tileMaps()) tiles.drawDark(batch, m, l, b, l + w, b + h, 9f, -12f, 0.30f * a);
                if (k <= EDGE_LAYERS)
                    for (TileMap m : world.tileMaps()) tiles.drawDark(batch, m, l, b, l + w, b + h, 3f, -4f, 0.5f * a);
                for (TileMap m : world.tileMaps()) tiles.drawTinted(batch, m, l, b, l + w, b + h, layerTint[0], layerTint[1], layerTint[2], a);
            }
            if (k <= MOTE_LAYERS) motes(stackCam, k, accent, a * (1f - 0.08f * k), world.time(), w, h);
            if (k <= BEACON_LAYERS) beacons(world, l, b, w, h, k, a);
            if (world.campaign && b + h > vajra.world.Hataki.Y0 - 200f) forgeLayer(world, l, b, w, h, k, a);   // Vitala: the river and the vents' fire, down the shaft
            if (wood > 0.02f) {                                                    // THE MIRAGE WOOD: flowers, mushrooms, the swarm, glass
                mirage.layer(batch, l, b, w, h, k, world.time(), wood, a);
                if (k <= 3) mirage.wisps(batch, k, world.ship.x, world.ship.y, world.time(), wood, a, scatter);
                if (k <= GLASS_LAYERS && k < count) reflect(world, camera, k, a * wood);
            }
        }
        stackCam.position.set(camera.position);                                    // the air between the first layer and the play layer
        stackCam.zoom = camera.zoom;
        stackCam.update();
        batch.setProjectionMatrix(stackCam.combined);
        motes(stackCam, 0, accent, 1f, world.time(), camera.viewportWidth * camera.zoom, camera.viewportHeight * camera.zoom);
        if (wood > 0.02f) {
            float vw = camera.viewportWidth * camera.zoom, vh = camera.viewportHeight * camera.zoom;
            mirage.layer(batch, camera.position.x - vw / 2, camera.position.y - vh / 2, vw, vh, 0, world.time(), wood, 1f);
            mirage.wisps(batch, 0, world.ship.x, world.ship.y, world.time(), wood, 1f, scatter);
        }
        batch.end();
    }

    private static final int GLASS_LAYERS = 4;
    private final com.badlogic.gdx.math.Matrix4 glassMat = new com.badlogic.gdx.math.Matrix4();
    private final OrthographicCamera glassCam = new OrthographicCamera();
    private float scatter;

    /** GLASS: layer k is a pane that holds a faint, drifting, MIRRORED picture of the layer behind it (k+1): the walls flipped about a
     *  vertical axis that wanders a little, drawn pale and teal over the pane. */
    private void reflect(World world, OrthographicCamera camera, int k, float strength) {
        float s = LayerStack.scale(k + 1, STACK_SHRINK), wpp = camera.zoom / s;
        glassCam.viewportWidth = camera.viewportWidth;
        glassCam.viewportHeight = camera.viewportHeight;
        glassCam.position.set(camera.position.x - LayerStack.leanShift(leanX, s) * wpp, camera.position.y - LayerStack.leanShift(leanY, s) * wpp, 0);
        glassCam.zoom = wpp;
        glassCam.update();
        float axis = glassCam.position.x + 170f * (float) Math.sin(world.time() * 0.17f + k * 1.3f);
        glassMat.set(glassCam.combined).translate(axis, 0f, 0f).scale(-1f, 1f, 1f).translate(-axis, 0f, 0f);
        float w = glassCam.viewportWidth * glassCam.zoom + 360f, h = glassCam.viewportHeight * glassCam.zoom;
        float l = glassCam.position.x - w / 2, b = glassCam.position.y - h / 2;
        batch.setProjectionMatrix(glassMat);
        if (chunksOk) chunks.draw(batch, l, b, l + w, b + h, 0.55f, 0.85f, 0.9f, 0.17f * strength);
        else for (TileMap m : world.tileMaps()) tiles.drawTinted(batch, m, l, b, l + w, b + h, 0.55f, 0.85f, 0.9f, 0.17f * strength);
    }

    /** The lean has nothing to ease toward while there is no stack: let it settle. */
    private void leanKeep() {
        leanX *= 0.9f;
        leanY *= 0.9f;
    }

    private static final int BEACON_LAYERS = 6;

    /** VITALA'S LAYERS: the Hataki shows as a gold stripe at its own world positions on the nearest four layers (the same trick as the beacons),
     *  and each vent's flame stands up the shaft as a column on the nearest three. */
    private void forgeLayer(World world, float l, float b, float w, float h, int k, float a) {
        if (k <= 4) {
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
            float fall = Math.max(0f, 1f - 0.2f * k);
            for (float y = (float) Math.floor((b - 120f) / 150f) * 150f; y < b + h + 120f; y += 150f) {
                if (!vajra.world.Hataki.flows(y)) continue;
                float x = vajra.world.Hataki.centre(y), hw = vajra.world.Hataki.half(y);
                if (x + hw < l - 200 || x - hw > l + w + 200) continue;
                batch.setColor(1f, 0.62f, 0.18f, 0.20f * fall * a * (0.85f + 0.15f * (float) Math.sin(world.time() * 1.6f + y * 0.01f)));
                batch.draw(moteTex, x - hw * 1.5f, y - 130f, hw * 3f, 260f);
            }
            batch.setColor(1f, 1f, 1f, 1f);
            batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
        if (k <= 3 && world.vents.count() > 0) ventArt.columns(batch, world, l, b, w, h, k, a);
    }

    /** A checkpoint's lamp shines down the shaft: a soft glow at its spot on each of the nearest layers (gold once lit, pink while cold),
     *  so you can see where one is, deep in the dark, long before you reach it. */
    private void beacons(World world, float l, float b, float w, float h, int k, float a) {
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
        for (vajra.world.Checkpoint cp : world.checkpoints) {
            if (cp.x < l - 120 || cp.x > l + w + 120 || cp.y < b - 120 || cp.y > b + h + 120) continue;
            float tm = world.time(), fall = Math.max(0f, 1f - 0.13f * k);
            if (cp.collected) batch.setColor(1f, 0.72f, 0.3f, 0.32f * fall * a * (0.9f + 0.1f * (float) Math.sin(tm * 7f + cp.x)));
            else batch.setColor(1f, 0.45f, 0.78f, 0.26f * fall * a * (0.6f + 0.4f * (float) Math.sin(tm * 2f)));
            float size = 170f;
            batch.draw(moteTex, cp.x - size / 2, cp.y - size / 2, size, size);
        }
        for (float[] sp : world.shops) {               // a shop is amber: its sign shines down the shaft too
            if (sp[0] < l - 120 || sp[0] > l + w + 120 || sp[1] < b - 120 || sp[1] > b + h + 120) continue;
            float fall = Math.max(0f, 1f - 0.16f * k);
            batch.setColor(1f, 0.66f, 0.24f, 0.24f * fall * a);
            batch.draw(moteTex, sp[0] - 100f, sp[1] - 100f, 200f, 200f);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static final int SHADOW_LAYERS = 5, EDGE_LAYERS = 6, MOTE_LAYERS = 8, LAYER_MOTES = 48;
    private float leanX, leanY;

    /** Soft motes of light pinned to ONE layer (so they slide at that layer's speed): a fixed field in the layer's own world that
     *  tiles every 3600 px and drifts slowly, in the band's accent colour. Added, not laid over. */
    private void motes(OrthographicCamera cam, int layer, float[] accent, float alpha, float time, float w, float h) {
        if (alpha <= 0.02f) return;
        final int P = 3600;
        float l = cam.position.x - w / 2, b = cam.position.y - h / 2;
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
        for (int i = 0; i < LAYER_MOTES; i++) {
            float ux = hash01(i, layer * 7 + 1), uy = hash01(i, layer * 7 + 2);
            int dx = (int) (ux * P + time * (6f + 10f * hash01(i, layer * 7 + 3)));
            int dy = (int) (uy * P + time * (3f + 5f * hash01(i, layer * 7 + 4)) + 18f * (float) Math.sin(time * 0.5f + i));
            float px = l + Math.floorMod(dx - (int) l, P), py = b + Math.floorMod(dy - (int) b, P);
            if (px > l + w || py > b + h) continue;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(time * (0.8f + 1.4f * hash01(i, layer * 7 + 5)) + i * 1.7f));
            float size = 14f + 22f * hash01(i, layer * 7 + 6);
            batch.setColor(accent[0], accent[1], accent[2], 0.55f * tw * alpha);
            batch.draw(moteTex, px - size / 2, py - size / 2, size, size);
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private static float hash01(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFF) / 16777216f;
    }

    private com.badlogic.gdx.graphics.Texture makeMote() {
        com.badlogic.gdx.graphics.Pixmap p = new com.badlogic.gdx.graphics.Pixmap(32, 32, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        for (int y = 0; y < 32; y++)
            for (int x = 0; x < 32; x++) {
                float d = (float) Math.hypot(x - 15.5f, y - 15.5f) / 16f, a = Math.max(0f, 1f - d);
                a = a * a * a;
                p.setColor(1f, 1f, 1f, a);
                p.drawPixel(x, y);
            }
        com.badlogic.gdx.graphics.Texture t = new com.badlogic.gdx.graphics.Texture(p);
        p.dispose();
        t.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
        return t;
    }

    /** Wait for the land to finish baking (screenshots and tests only; the game never waits). */
    public boolean finishLoading() { return terrain.finishLoading(); }

    private OrthographicCamera lowCam;

    /** The camera for things ON THE GROUND: the same centre, pulled back by the ground's depth (Depth.GROUND). */
    private OrthographicCamera groundCam(OrthographicCamera camera) {
        if (lowCam == null) lowCam = new OrthographicCamera(camera.viewportWidth, camera.viewportHeight);
        lowCam.viewportWidth = camera.viewportWidth;
        lowCam.viewportHeight = camera.viewportHeight;
        lowCam.position.set(camera.position);
        lowCam.zoom = camera.zoom / Altitude.ground(fly);
        lowCam.update();
        return lowCam;
    }

    /** THE LAND: the painted ground, opaque, under everything (the world is something you fly OVER). The first frame paints
     *  the whole view at once; after that a few chunks a frame keep up with the flight. */
    private void groundLayer(OrthographicCamera cam) {
        float w = cam.viewportWidth * cam.zoom, h = cam.viewportHeight * cam.zoom;
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        batch.disableBlending();
        ground.draw(batch, cam.position.x - w / 2, cam.position.y - h / 2, w, h, 8);
        batch.enableBlending();
        batch.end();
    }

    private void mistLayer(World world, OrthographicCamera camera,
                            float l, float b, float viewW, float viewH) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // the MIST: slow jewel-light drifting between the ground and the ship; it moves against the ground (0.95), and
        // that gap is the ship's height above the land
        // (added, not laid over: mist only ever brings light, it never muddies the ground)
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE);
        batch.setColor(mistC[0], mistC[1], mistC[2], Math.min(1f, mistC[3] * 1.6f));
        nebula.draw(batch, l, b, viewW, viewH, camera.position.x, camera.position.y, world.time());
        batch.setColor(1, 1, 1, 1);
        batch.setBlendFunction(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.end();
    }

    /** What flies casts a shadow on the land: the hull and every body, thrown down and right (the light hangs on the
     *  vault overhead), the higher the thing the further it falls. */
    private void shadowLayer(World world, OrthographicCamera camera) {
        shapes.setProjectionMatrix(camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0.01f, 0.03f, 0.30f);
        float l = camera.position.x - camera.viewportWidth * camera.zoom / 2 - 60, r = l + camera.viewportWidth * camera.zoom + 120;
        float b = camera.position.y - camera.viewportHeight * camera.zoom / 2 - 60, t = b + camera.viewportHeight * camera.zoom + 120;
        for (int i = 0; i < world.enemies.size(); i++) {
            Enemy e = world.enemies.get(i);
            if (e.dead || e.bossHidden || e.depth > 0.05f || e.x < l || e.x > r || e.y < b || e.y > t) continue;
            float s = e.drawSize();
            shapes.ellipse(e.x - s * 0.30f + 9, e.y - s * 0.22f - 14, s * 0.60f, s * 0.44f, 16);
        }
        Ship[] hulls = new Ship[2 + world.extras.size()];
        hulls[0] = world.ship;
        hulls[1] = world.partner;
        for (int i = 0; i < world.extras.size(); i++) hulls[2 + i] = world.extras.get(i).ship;
        for (Ship h : hulls) {
            if (h == null || h.hp <= 0) continue;
            shapes.ellipse(h.x - 15 + 11, h.y - 8 - 18, 30, 16, 16);
        }
        shapes.end();
    }

    /** Jewel-dust hanging in the air between the ship and the camera: it slides past faster than the ground does,
     *  which is what makes the ship read as flying low over land. */
    private void moteLayer(World world, OrthographicCamera camera, float l, float b, float viewW, float viewH) {
        shapes.setProjectionMatrix(camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        float[] ac = vajra.world.Biome.of(vajra.world.Biome.band(world.ship.y, world.regionStart)).accent;
        float time = world.time();
        for (int i = 0; i < MOTES; i++) {
            float ux = mote(i, 1), uy = mote(i, 2);
            float depth = 1.2f + 0.5f * mote(i, 3);                       // nearer motes slide faster
            float sx = ((ux * viewW - camera.position.x * depth + time * 5f * (mote(i, 4) - 0.5f)) % viewW + viewW) % viewW;
            float sy = ((uy * viewH - camera.position.y * depth) % viewH + viewH) % viewH;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(time * (1.2f + 2f * mote(i, 5)) + i));
            shapes.setColor(ac[0], ac[1], ac[2], 0.5f * tw * (0.5f + 0.5f * (depth - 1.2f) / 0.5f));
            float s = 1.2f + 1.4f * (depth - 1.2f);
            shapes.rect(l + sx - s / 2, b + sy - s / 2, s, s);
        }
        shapes.end();
    }

    private static final int MOTES = 90;

    private static float mote(int i, int k) {
        int h = i * 374761393 + k * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFF) / 16777216f;
    }

    /** The Hataki's current, made visible: rows of pale-gold chevrons slide down-river on the gold at the river's own speed (shapes are open). */
    private void riverChevrons(World world, float l, float b, float viewH) {
        float t = world.time();
        float[] f = new float[2];
        for (float y = (float) Math.floor((b - 80f) / 64f) * 64f; y < b + viewH + 80f; y += 64f) {
            if (!vajra.world.Hataki.flows(y)) continue;
            float sp = vajra.world.Hataki.speed(y), cx = vajra.world.Hataki.centre(y), hw = vajra.world.Hataki.half(y);
            float yy = y + ((t * sp) % 64f);
            for (int col = -1; col <= 1; col++) {
                float x = cx + col * hw * 0.55f;
                float a = (world.quietT > 0f ? 0.08f : 0.28f) * (1f - Math.abs(col) * 0.3f);
                shapes.setColor(1f, 0.88f, 0.45f, a);
                shapes.rectLine(x - 7, yy - 5, x, yy + 2, 2.2f);
                shapes.rectLine(x + 7, yy - 5, x, yy + 2, 2.2f);
            }
        }
    }

    private void backdropLayer(World world, OrthographicCamera camera,
                              float l, float b, float viewW, float viewH) {
        shapes.setProjectionMatrix(camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < world.checkpoints.length; i++) { // behind the stack, never over it
            Checkpoint c = world.checkpoints[i];
            if (c.x + 90 > l && c.x - 90 < l + viewW && c.y + 90 > b && c.y - 90 < b + viewH) {
                (c.y >= vajra.world.Hataki.Y0 ? cpsV : cps).drawParticles(shapes, c, world.time());
            }
        }
        if (world.campaign) riverChevrons(world, l, b, viewH);
        shapes.end();
    }

    /** The bodies that are down in the shaft (depth above 0.05): each over a dark pit of its own, small and dim, UNDER the play layer's walls
     *  - drawn before them - so a wall hides what is beneath it and a reserve is seen climbing out. */
    private void submergedLayer(World world, OrthographicCamera camera, float l, float b, float viewW, float viewH) {
        boolean any = false;
        for (int i = 0; i < world.enemies.size() && !any; i++) any = world.enemies.get(i).depth > 0.05f;
        if (!any) return;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (int i = 0; i < world.enemies.size(); i++) {
            Enemy e = world.enemies.get(i);
            if (e.depth <= 0.05f || e.dead || e.x + e.w < l || e.x - e.w > l + viewW || e.y + e.h < b || e.y - e.h > b + viewH) continue;
            batch.setColor(0f, 0f, 0.02f, 0.55f * Math.min(1f, e.depth * 1.6f));
            batch.draw(moteTex, e.x - e.w * 0.9f, e.y - e.h * 0.9f, e.w * 1.8f, e.h * 1.8f);      // the pit it is in
            batch.setColor(1f, 1f, 1f, 1f);
            foes.draw(batch, e, world.time());
        }
        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    private void worldSpriteLayer(World world, OrthographicCamera camera,
                                 float l, float b, float viewW, float viewH) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        // plants first: the wall tiles draw over their tucked roots — they grow from behind
        for (float[] p : world.plants) {
            if (p[0] + 80 > l && p[0] - 80 < l + viewW && p[1] + 80 > b && p[1] - 80 < b + viewH)
                fx.drawPlant(batch, p[0], p[1], (int) p[2], world.time(), p[3], p[4], p[5]);
        }
        if (world.walled && chunksOk) {
            chunks.draw(batch, l, b, l + viewW, b + viewH, 1f, 1f, 1f, 1f);                       // the baked walls, shadow and side face included
            tiles.setGhostMode(TileImages.GHOST_ONLY);                                             // the mirages stay live: they thin out as the hull nears
            for (TileMap m : world.tileMaps()) if (m.hasGhosts()) tiles.draw(batch, m, l, b, l + viewW, b + viewH);
            tiles.setGhostMode(TileImages.GHOST_ALL);
        } else if (world.walled) {
            for (TileMap m : world.tileMaps()) tiles.drawShadow(batch, m, l, b, l + viewW, b + viewH);
            for (TileMap m : world.tileMaps()) tiles.drawDark(batch, m, l, b, l + viewW, b + viewH, 3f, -4f, 0.55f); // the side face: a wall is thick
            for (TileMap m : world.tileMaps()) tiles.draw(batch, m, l, b, l + viewW, b + viewH);
        }
        for (int i = 0; i < world.torches.length; i++) { // wall-mounted props, behind everything
            float[] t = world.torches[i];
            if (t[0] + 40 > l && t[0] - 40 < l + viewW && t[1] + 40 > b && t[1] - 40 < b + viewH)
                fx.drawFlame(batch, t[0], t[1], t[2], world.time(), i);
        }
        for (int i = 0; i < world.wrecks.length && World.sideStory; i++) { // MAYA'S GROVE: the fallen walls she wants home (side story only)
            float[] wk = world.wrecks[i];
            if (!world.wreckTaken[i]
                    && wk[0] + 40 > l && wk[0] - 40 < l + viewW && wk[1] + 40 > b && wk[1] - 40 < b + viewH)
                fx.drawWreck(batch, wk[0], wk[1], i % 2);
        }
        for (int i = 0; i < world.enemies.size(); i++) {
            Enemy e = world.enemies.get(i);
            if (e.bossHidden || e.depth > 0.05f) continue; // mid-blink: the body is elsewhere; below: drawn under the walls
            if (e.x + e.w / 2 > l && e.x - e.w / 2 < l + viewW && e.y + e.h / 2 > b && e.y - e.h / 2 < b + viewH) {
                foes.draw(batch, e, world.time());
            }
        }
        for (Enemy v : world.voices) { // THE VOICES: people wear the eye's blue, off the roster
            if (v.x + 40 > l && v.x - 40 < l + viewW && v.y + 60 > b && v.y - 40 < b + viewH) {
                if (people.has(v)) {
                    people.draw(batch, world, v);
                } else { // no art for this person yet: the older tinted soldier body, never silent
                    batch.setColor(0.55f, 0.95f, 1f, 1f);
                    foes.draw(batch, v, world.time());
                    batch.setColor(1, 1, 1, 1);
                }
                personTag(v, world);
            }
        }
        for (int i = 0; i < world.orbs.length; i++) {
            if (world.orbs[i].visible) orbs.draw(batch, world.orbs[i]);
        }
        for (int i = 0; i < world.pickups.size(); i++) {
            Pickup p = world.pickups.get(i);
            if (p.x + 32 > l && p.x - 32 < l + viewW && p.y + 32 > b && p.y - 32 < b + viewH)
                loot.draw(batch, p, world.time());
        }
        for (int i = 0; i < world.shops.length; i++) { // the hanging SHOP sign + bay, behind the checkpoint stack
            float[] s = world.shops[i];
            if (s[0] + 70 > l && s[0] - 70 < l + viewW && s[1] + 110 > b && s[1] - 70 < b + viewH) {
                (s[1] >= vajra.world.Hataki.Y0 ? shopSignV : shopSign).draw(batch, s[0], s[1], world.shopState[i], world.shopTimer[i], world.time());
            }
        }
        for (int i = 0; i < world.checkpoints.length; i++) {
            Checkpoint c = world.checkpoints[i];
            if (c.x + 80 > l && c.x - 80 < l + viewW && c.y + 80 > b && c.y - 80 < b + viewH) {
                (c.y >= vajra.world.Hataki.Y0 ? cpsV : cps).draw(batch, c, world.time());
            }
        }
        for (int i = 0; i < world.chests.size(); i++) {                       // the treasure of every loka
            vajra.world.Chest ch = world.chests.get(i);
            if (ch.x + 40 > l && ch.x - 40 < l + viewW && ch.y + 50 > b && ch.y - 40 < b + viewH) chestArt.draw(batch, ch, world.time());
        }
        for (int i = 0; i < world.pockets.length; i++) {                      // and the ember wells of the side caverns
            float[] pk = world.pockets[i];
            if (pk[5] == vajra.world.Chest.WELL && pk[0] + 40 > l && pk[0] - 40 < l + viewW && pk[1] + 40 > b && pk[1] - 40 < b + viewH)
                chestArt.drawWell(batch, pk[0], pk[1], world.wellUsed[i], world.time());
        }
        if (world.vents.count() > 0) ventArt.draw(batch, world, l, b, viewW, viewH);
        hazardFx.draw(batch, world, l, b, viewW, viewH);
        flowFx.draw(batch, world, l, b, viewW, viewH);        // the pyre bloom and the tornado
        batch.end();
    }

    private void sparkAndBarLayer(World world, OrthographicCamera camera,
                                 float l, float b, float viewW, float viewH) {
        shapes.setProjectionMatrix(camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        for (Effect e : world.effects) {
            if (e.type == Effect.SURGE) { surgeLayer(shapes, e); continue; }
            if (e.type != Effect.SPARK) continue;
            shapes.setColor(e.r, e.g, e.b, e.alpha());
            shapes.circle(e.x, e.y, e.radius);
        }
        astraLayer(shapes, world, l, b, viewW, viewH);
        for (int i = 0; i < world.enemies.size(); i++) {
            Enemy foe = world.enemies.get(i);
            if (foe.dead || foe.boss || foe.depth > 0.05f) continue; // the commander has his own, bigger bar
            if (foe.x + foe.w < l || foe.x - foe.w > l + viewW || foe.y + foe.h < b || foe.y - foe.h > b + viewH) continue;
            hpBar(shapes, foe);
        }
        // Bala's bar rides above his hull like everyone else's — bigger, segmented, phase-tinted,
        // notched at both thresholds. Appears the moment the ship enters the boss region.
        if (world.currentRegionIndex() == 5 && world.boss != null && !world.boss.dead
                && !world.boss.bossHidden) {
            Enemy bal = world.boss;
            float bw = 240, bh = 16, bx = bal.x - bw / 2, by = bal.y + bal.h / 2 + 10;
            if (bx + bw > l && bx < l + viewW && by < b + viewH && by + bh > b) {
                float frac = Math.max(0f, bal.dispHp / bal.maxHp);
                shapes.setColor(1f, 0.78f, 0.2f, 1f);          // gold frame
                shapes.rect(bx - 3, by - 3, bw + 6, bh + 6);
                shapes.setColor(0.07f, 0.05f, 0.05f, 0.95f);   // dark backing
                shapes.rect(bx, by, bw, bh);
                if (bal.bossPhase == 1) shapes.setColor(1f, 0.78f, 0.2f, 1f);       // gold
                else if (bal.bossPhase == 2) shapes.setColor(1f, 0.5f, 0.12f, 1f);  // ember
                else shapes.setColor(0.95f, 0.22f, 0.1f, 1f);                      // blood
                float seg = bw / 20f; // segmented fill: an honor bar, not a plain strip
                for (int s = 0; s < 20; s++) {
                    float left = frac * bw - s * seg;
                    if (left <= 0) break;
                    shapes.rect(bx + s * seg, by + 2, Math.min(seg - 2, left), bh - 4);
                }
                shapes.setColor(1, 1, 1, 0.7f);                // threshold notches
                shapes.rect(bx + bw / 3f - 1, by, 2, bh);
                shapes.rect(bx + 2 * bw / 3f - 1, by, 2, bh);
            }
        }
        if (!shipR.hasArt(world.ship)) shipR.drawHull(shapes, world.ship);
        shapes.end();
    }

    /** SURGE: two stacked discs — a wide faint body and a tighter, faster inner front. Overlapping
     *  a slow swell with a quick one is what stops an expanding circle reading as a flat halo:
     *  the eye sees a body with a crest running through it. Drawn in the shape pass AFTER the
     *  enemies, so the release washes OVER them.
     *
     *  <p>The release is drawn in the loka's own tatva, so nothing here may bias the hue: the
     *  body is darkened and the crest lifted by UNIFORM factors. Shading a single channel - as
     *  the old water version did, holding blue at 0.8 - turned Atala's violet Akasha into blue. */
    private void surgeLayer(ShapeRenderer s, Effect e) {
        float reach = e.surgeReach();
        if (reach <= 0.001f) return;
        float rad = e.radius * reach;
        float a = e.alpha();
        s.setColor(e.r * 0.62f, e.g * 0.62f, e.b * 0.68f, e.bodyAlpha * a);
        s.circle(e.x, e.y, rad);
        float front = 1f - (1f - reach) * (1f - reach); // leads the body
        s.setColor(e.r + (1f - e.r) * 0.6f, e.g + (1f - e.g) * 0.6f, e.b + (1f - e.b) * 0.6f,
                0.5f * a * Math.min(1f, front * 1.6f));
        s.circle(e.x, e.y, rad * 0.82f);
    }

    /** The foes an invocation has locked: four corner brackets round each, in the astra's own colour, breathing. */
    private void astraLocks(ShapeRenderer s, World w) {
        if (!w.winding() || w.astraLockCount == 0) return;
        float pulse = 0.65f + 0.35f * (float) Math.sin(w.clock * 10f);
        s.setColor(w.crestR(), w.crestG(), w.crestB(), pulse);
        for (int i = 0; i < w.astraLockCount; i++) {
            vajra.enemy.Enemy e = w.astraLocks[i];
            if (e == null || e.dead) continue;
            float r = Math.max(e.w, e.h) * 0.5f + 7f, t = r * 0.45f, th = 1.6f;
            s.rectLine(e.x - r, e.y + r, e.x - r + t, e.y + r, th);
            s.rectLine(e.x - r, e.y + r, e.x - r, e.y + r - t, th);
            s.rectLine(e.x + r, e.y + r, e.x + r - t, e.y + r, th);
            s.rectLine(e.x + r, e.y + r, e.x + r, e.y + r - t, th);
            s.rectLine(e.x - r, e.y - r, e.x - r + t, e.y - r, th);
            s.rectLine(e.x - r, e.y - r, e.x - r, e.y - r + t, th);
            s.rectLine(e.x + r, e.y - r, e.x + r - t, e.y - r, th);
            s.rectLine(e.x + r, e.y - r, e.x + r, e.y - r + t, th);
        }
    }

    private void astraLayer(ShapeRenderer s, World w, float l, float b, float viewW, float viewH) {
        astraLocks(s, w);
        AstraFx.draw(s, w, l, b, viewW, viewH);
    }

    private void shipLayer(World world, OrthographicCamera camera) {
        shapes.setProjectionMatrix(camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        // post-lights visibility pass: the hull (or, if art is missing, its fallback) must
        // stay bright on top of the dim frame alongside the sprite.
        if (!shipR.hasArt(world.ship)) shipR.drawHull(shapes, world.ship);
        // no decorative halo / rim circle around the hull (was obscuring the ship, and the
        // state ring it replaced): state now reads through the hull's dip and its flicker
        shipR.drawCharge(shapes, world.ship);
        if (world.partner != null) { // co-op: the other hull reads the same way
            if (!shipR.hasArt(world.partner)) shipR.drawHull(shapes, world.partner);
            shipR.drawCharge(shapes, world.partner);
        }
        for (World.SeatHull gh : world.extras) { // the extra hulls read like the partner's
            if (gh.ship == null) continue;
            if (!shipR.hasArt(gh.ship)) shipR.drawHull(shapes, gh.ship);
            shipR.drawCharge(shapes, gh.ship);
        }
        // every hull wears its pilot's colour bar below it: who is who at a glance
        if (vajra.net.Net.active()) {
            vajra.net.Session s = vajra.net.Net.session;
            seatBar(shapes, world.ship, s.myId()); // my own seat, not seat 0's
            if (world.partner != null) seatBar(shapes, world.partner, world.partnerSeat);
            for (World.SeatHull gh : world.extras) if (gh.ship != null) seatBar(shapes, gh.ship, gh.seat);
        }
        shapes.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        shipR.draw(batch, world.ship, world.time());
        for (int i = 0; i < world.shops.length; i++) // the berth's clamps close over a seated hull
            if (world.shopState[i] != World.UNDOCKED) (world.shops[i][1] >= vajra.world.Hataki.Y0 ? shopSignV : shopSign).drawClamps(batch, world.shops[i][0], world.shops[i][1], world.shopState[i], world.shopTimer[i]);
        if (world.partner != null) shipR.draw(batch, world.partner, world.time()); // co-op: the other hull
        for (World.SeatHull gh : world.extras) // co-op: the extra hulls
            if (gh.ship != null) shipR.draw(batch, gh.ship, world.time());
        for (Effect e : world.effects) {
            if (e.type == Effect.DEATH) foes.drawDeath(batch, e);
            else fx.draw(batch, e);
        }
        batch.end();
    }

    private void shieldLayer(World world, OrthographicCamera camera) {
        shapes.setProjectionMatrix(camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Line);
        if (world.ship.shieldOn && world.ship.shieldEnergy > 0) {
            shapes.setColor(0.3f, 0.6f, 1, 0.3f + 0.4f * (world.ship.shieldEnergy / world.ship.shieldCap()));
            shapes.circle(world.ship.x, world.ship.y, 30 + 2 * (float) Math.sin(world.time() * 6));
        }
        shapes.end();
    }

    private void foregroundLayer(World world, OrthographicCamera camera) {
        // bullets again, over the dimmed frame: shots must read in the dark
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (!world.bullets.isEmpty()) { // (the negative filter copies the frame: only pay for it when there is a round to draw)
            shots.begin(batch);
            for (int i = 0; i < world.bullets.size(); i++) shots.draw(batch, world.bullets.get(i));
            shots.end(batch);
        }
        drawPopups(world);
        batch.end();
    }

    /** A hull's seat bar: their colour, right under the hull. */
    private void seatBar(ShapeRenderer sh, Ship h, int seat) {
        sh.setColor(SeatColors.of(seat));
        sh.rect(h.x - 14, h.y - 30, 28, 5);
    }

    /** A person's mark: a gold ! bobbing over them until they have been met, and their name once the
     *  pilot is near - so nobody has to guess that the blue shape is a person, or who. */
    private void personTag(Enemy v, vajra.world.World world) {
        float t = world.time();
        if (!v.voiceMet) {
            ui.hud.setColor(1f, 0.84f, 0.2f, 0.9f);
            ui.hud.draw(batch, "!", v.x - 3, v.y + 52 + (float) Math.sin(t * 3.2f) * 3f);
        }
        vajra.ship.Ship hull = world.nearestHull(v.x, v.y);
        float dx = hull.x - v.x, dy = hull.y - v.y;
        if (dx * dx + dy * dy < 420f * 420f && !world.talking(v)) {
            String n = v.voiceName == null || v.voiceName.equals("SCOUT") ? "THE SCOUT"
                    : v.voiceName.equals("MAYA") ? "THE NINTH MAYA" : "KALIYA";
            ui.caption.setColor(0.6f, 0.86f, 1f, 0.8f);
            ui.caption.draw(batch, n, v.x - ui.measure(ui.caption, n) / 2f, v.y - 32);
        }
        ui.hud.setColor(1, 1, 1, 1);
        ui.caption.setColor(1, 1, 1, 1);
    }

    /** Floating popups (Popups): world-space, topmost, punch-in scale and end-of-life fade.
     *  ONE LANGUAGE per popup, both small: with an `img` the popup is the pre-rendered
     *  Devanagari phrase alone — centred, tinted, drop-shadowed — with no English caption;
     *  without one it is the plain English text (centred, outline shadow for legibility over
     *  any backdrop). The settings' popup scale stretches both.
     *  The logic lives in Popups, this only paints. */
    private static final float TEXT_SCALE = 0.62f;  // words render small
    private static final float VERSE_SCALE = 0.45f; // verses smaller still: 66px art lands ~30px tall

    private final com.badlogic.gdx.graphics.g2d.GlyphLayout popLayout =
            new com.badlogic.gdx.graphics.g2d.GlyphLayout();

    private void drawPopups(World world) {
        float pScale = vajra.profile.Profiles.active().settings.popupScale;
        for (Popups.Spec s : world.popups.slots()) {
            if (s.text == null) continue;
            float fade = world.popups.alphaOf(s);
            com.badlogic.gdx.graphics.g2d.TextureRegion img = popupArt.get(s.img);
            float scale = world.popups.punchOf(s) * pScale
                    * (img != null ? VERSE_SCALE : TEXT_SCALE);
            if (img != null) { // the verse: drop-shadowed, tinted, centred on the push point
                float w = img.getRegionWidth() * scale, h = img.getRegionHeight() * scale;
                batch.setColor(0, 0, 0, fade * 0.8f);
                batch.draw(img, s.x - w / 2 + 1f, s.y - h / 2 - 1f, w, h);
                batch.setColor(s.r, s.g, s.b, fade);
                batch.draw(img, s.x - w / 2, s.y - h / 2, w, h);
            } else if (!s.text.isEmpty()) {
                com.badlogic.gdx.graphics.g2d.BitmapFont f = ui.hud;
                f.getData().setScale(scale);
                popLayout.setText(f, s.text);
                float tx = s.x - popLayout.width / 2;
                f.setColor(0, 0, 0, fade * 0.8f);
                f.draw(batch, s.text, tx + 1f, s.y - 1f);
                f.setColor(s.r, s.g, s.b, fade);
                f.draw(batch, s.text, tx, s.y);
                f.getData().setScale(1f);
            }
        }
        ui.hud.setColor(1, 1, 1, 1);
    }

    /** Thin bar above a hull: green→red with real hp, drawn to the eased dispHp, so a hit
     *  burns a white-hot slice at the leading edge that cools away as the bar drains onto hp. */
    private void hpBar(ShapeRenderer s, Enemy e) {
        float bw = e.w * 0.9f, bh = 2f;
        float x = e.x - bw / 2, y = e.y + e.h / 2 + 5;
        float frac = Math.max(0, e.hp / e.maxHp);
        float disp = Math.max(0, Math.min(1, e.dispHp / e.maxHp));
        s.setColor(0.05f, 0.05f, 0.08f, 0.6f);
        s.rect(x - 1, y - 1, bw + 2, bh + 2);
        s.setColor(1f - 0.75f * frac, 0.3f + 0.7f * frac, 0.2f, 0.95f);
        s.rect(x, y, bw * disp, bh);
        if (disp > frac) {
            s.setColor(1, 1, 1, 0.9f);
            s.rect(x + bw * frac, y, bw * (disp - frac), bh);
        }
    }

    public void dispose() {
        chunks.dispose();
        mirage.dispose();
        moteTex.dispose();
        ground.dispose();
        terrain.dispose();
        foes.dispose();
        people.dispose();
        nebula.dispose();
        orbs.dispose();
        loot.dispose();
        cps.dispose();
        cpsV.dispose();
        shopSignV.dispose();
        chestArt.dispose();
        hazardFx.dispose();
        flowFx.dispose();
        ventArt.dispose();
        shopSign.dispose();
        shots.dispose();
        fx.dispose();
        shipR.dispose();
        tiles.dispose();
        lights.dispose();
        popupArt.dispose();
    }
}
