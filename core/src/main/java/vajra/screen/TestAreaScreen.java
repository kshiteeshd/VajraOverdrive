package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import vajra.flow.Flow;
import vajra.flow.FlowState;
import vajra.ship.ShipControls;
import vajra.world.Effect;
import vajra.world.TileMap;
import vajra.world.World;

/** WORLD, VARIANT TWO: a free-flight test area built from the old wall tiles and nothing else - no ground, no sky, no stars.
 *  The play layer is the real world's walls (they stop the hull and the rounds); behind it the same walls are stacked again,
 *  layer on layer with a gap between them, each one the SAME walls, smaller, darker and peeking out behind the one in front (see
 *  {@link LayerStack}); a layer slides across the screen by only a fraction of the camera's move, which is what reads as depth.
 *
 *  <p>Fly anywhere: no enemies, no story, no shop, the hull cannot die, and it is not drawn (you fly by the walls and the rounds). W A S D fly, Q E turn, SPACE (or the left button)
 *  fires, SHIFT boosts, G (or the right button) raises the shield, [ and ] set how fast the layers shrink with distance, , and .
 *  how far each peeks out behind the last, - and = how many there are, ESC goes back to the menu. */
public final class TestAreaScreen implements FlowState, ShipControls {
    private static final float STEP = 1f / 60f;
    /** The far world is drawn this many times larger than the depth of the stack alone would make it. */
    private static final float WORLD_SCALE = 2f;

    private final Flow flow;
    private final Ui ui;
    private final SpriteBatch batch;
    private final World world;
    private final TileImages tiles = new TileImages();
    private final FxImages fx = new FxImages();
    private final BulletImages shots = new BulletImages();
    private final OrthographicCamera camera = new OrthographicCamera(Ui.UI_W, Ui.UI_H);
    private final OrthographicCamera layerCam = new OrthographicCamera(Ui.UI_W, Ui.UI_H);
    private final FitViewport viewport = new FitViewport(Ui.UI_W, Ui.UI_H, camera);
    private final Vector3 mouse = new Vector3();
    private final float[] tint = new float[3];
    private float acc;
    private final TerrainLayer terrain;
    private final float[] haze = {0.2f, 0.26f, 0.42f};
    private float shrink = 0.98f;
    private float peek = 0f;       // how far each layer back peeks out down-left of the one in front, screen px
    private int layers = 10;

    public TestAreaScreen(Flow flow, Ui ui, SpriteBatch batch) {
        this.flow = flow;
        this.ui = ui;
        this.batch = batch;
        boolean was = World.walls;
        World.walls = true;                       // this one wants the rock
        try {
            world = new World();
        } finally {
            World.walls = was;
        }
        world.enemies.clear();
        world.voices.clear();
        world.clearBoulders();
        world.cues.clear();
        float[] spot = world.randomOpenSpot(new java.util.Random(11));
        world.ship.x = spot[0];
        world.ship.y = spot[1];
        vajra.world.GroundMap land = new vajra.world.GroundMap(vajra.world.GroundMap.SEED, world.width, world.height, world.regionStart);
        this.terrain = new TerrainLayer(world, land, new vajra.world.PropField(land, world.width, world.regionStart));
    }

    /** Wait for the far world to finish baking (screenshots and tests only). */
    public boolean finishLoading() { return terrain.finishLoading(); }

    /** The world this area flies in (tests). */
    World world() { return world; }

    // ---- the loop ------------------------------------------------------------------------------

    @Override
    public void update(float dt) {
        acc = Math.min(acc + dt, 2 * STEP);
        while (acc >= STEP) {
            world.update(STEP, this);
            world.cues.clear();
            world.enemies.clear();                // nothing hunts here
            world.ship.hp = world.ship.maxHp();   // and nothing sticks
            acc -= STEP;
        }
        float vw = camera.viewportWidth, vh = camera.viewportHeight;
        camera.position.set(Math.max(vw / 2, Math.min(world.width - vw / 2, world.ship.x)),
                Math.max(vh / 2, Math.min(world.height - vh / 2, world.ship.y)), 0);
        camera.update();
    }

    @Override
    public void render() {
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.012f, 0.014f, 0.03f, 1f);   // no background: the dark between the walls
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // behind the last layer: THE WORLD, the raycast land, far off and small
        if (terrain.ready()) {
            layerCam.viewportWidth = camera.viewportWidth;
            layerCam.viewportHeight = camera.viewportHeight;
            layerCam.position.set(camera.position);
            layerCam.zoom = 1f / (LayerStack.scale(layers + 1, shrink) * WORLD_SCALE);   // far, but twice the size it would be at the stack's depth
            layerCam.update();
            terrain.draw(batch, layerCam, 430f, 0, 0.55f, haze, 0.35f);
        }
        // the walls, back to front: the furthest layer first, each smaller and darker, the play layer last
        batch.begin();                                    // one batch for the whole stack: no fill-rate veils, no restarts
        for (int k = layers; k >= 1; k--) drawLayer(k);
        batch.setProjectionMatrix(camera.combined);
        for (TileMap m : world.tileMaps()) tiles.draw(batch, m, camera.position.x - camera.viewportWidth / 2,
                camera.position.y - camera.viewportHeight / 2, camera.position.x + camera.viewportWidth / 2,
                camera.position.y + camera.viewportHeight / 2);
        batch.end();

        // the hull, its sparks, its rounds
        ui.shapes.setProjectionMatrix(camera.combined);
        ui.beginShapes(ShapeRenderer.ShapeType.Filled);
        for (Effect e : world.effects) {
            if (e.type != Effect.SPARK) continue;
            ui.shapes.setColor(e.r, e.g, e.b, e.alpha());
            ui.shapes.circle(e.x, e.y, e.radius);
        }
        ui.shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Effect e : world.effects) if (e.type != Effect.SPARK && e.type != Effect.SURGE && e.type != Effect.DEATH) fx.draw(batch, e);
        if (!world.bullets.isEmpty()) { // (the negative filter copies the frame: only pay for it when there is a round to draw)
            shots.begin(batch);
            for (int i = 0; i < world.bullets.size(); i++) shots.draw(batch, world.bullets.get(i));
            shots.end(batch);
        }
        batch.end();

        ui.applyViewport();
        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        ui.caption.setColor(0.82f, 0.70f, 0.42f, 0.9f);
        ui.caption.draw(batch, "TEST AREA   LAYERS " + layers + "   SHRINK " + Math.round(shrink * 100) + "%   PEEK " + Math.round(peek), 24, Ui.UI_H - 22);
        ui.caption.setColor(1f, 1f, 1f, 0.55f);
        ui.caption.draw(batch, "WASD FLY   Q E TURN   SPACE FIRE   SHIFT BOOST   [ ] SHRINK   , . PEEK   - = LAYERS   ESC MENU", 24, 28);
        ui.caption.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    /** Layer k: the walls again, scaled about the screen centre by shrink^k, darker the further back, a different slice. */
    private void drawLayer(int k) {
        float s = LayerStack.scale(k, shrink);
        layerCam.viewportWidth = camera.viewportWidth;
        layerCam.viewportHeight = camera.viewportHeight;
        // the same walls, the same centre; the layer shrinks about it (so it slides by s of the camera's move) and peeks out down-left
        layerCam.position.set(camera.position.x - LayerStack.shiftX(k, peek) / s, camera.position.y - LayerStack.shiftY(k, peek) / s, 0);
        layerCam.zoom = 1f / s;
        layerCam.update();
        float w = layerCam.viewportWidth * layerCam.zoom, h = layerCam.viewportHeight * layerCam.zoom;
        float l = layerCam.position.x - w / 2, b = layerCam.position.y - h / 2;
        LayerStack.tint(k, layers, tint);
        batch.setProjectionMatrix(layerCam.combined);
        for (TileMap m : world.tileMaps()) tiles.drawTinted(batch, m, l, b, l + w, b + h, tint[0], tint[1], tint[2]);
    }

    // ---- input ---------------------------------------------------------------------------------

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            flow.pop();
            return true;
        }
        if (keycode == Input.Keys.LEFT_BRACKET) shrink = Math.max(LayerStack.MIN_SHRINK, shrink - 0.01f);
        else if (keycode == Input.Keys.RIGHT_BRACKET) shrink = Math.min(LayerStack.MAX_SHRINK, shrink + 0.01f);
        else if (keycode == Input.Keys.COMMA) peek = Math.max(0f, peek - 2f);
        else if (keycode == Input.Keys.PERIOD) peek = Math.min(LayerStack.MAX_PEEK, peek + 2f);
        else if (keycode == Input.Keys.MINUS) layers = Math.max(LayerStack.MIN_LAYERS, layers - 1);
        else if (keycode == Input.Keys.EQUALS) layers = Math.min(LayerStack.MAX_LAYERS, layers + 1);
        else return false;
        return true;
    }

    private boolean pressed(int key) { return Gdx.input.isKeyPressed(key); }

    @Override
    public float thrustY() {
        return (pressed(Input.Keys.W) || pressed(Input.Keys.UP) ? 1 : 0) + (pressed(Input.Keys.S) || pressed(Input.Keys.DOWN) ? -1 : 0);
    }

    @Override
    public float thrustX() {
        return (pressed(Input.Keys.D) || pressed(Input.Keys.RIGHT) ? 1 : 0) + (pressed(Input.Keys.A) || pressed(Input.Keys.LEFT) ? -1 : 0);
    }

    @Override
    public float rotate() { return (pressed(Input.Keys.Q) ? 1 : 0) + (pressed(Input.Keys.E) ? -1 : 0); }

    @Override
    public boolean fire() { return pressed(Input.Keys.SPACE) || Gdx.input.isButtonPressed(Input.Buttons.LEFT); }

    @Override
    public boolean boost() { return pressed(Input.Keys.SHIFT_LEFT) || pressed(Input.Keys.SHIFT_RIGHT); }

    @Override
    public boolean shield() { return pressed(Input.Keys.G) || Gdx.input.isButtonPressed(Input.Buttons.RIGHT); }

    @Override
    public boolean mouseAim() { return Gdx.input.isButtonPressed(Input.Buttons.LEFT); }

    @Override
    public float aimX() {
        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
        return mouse.x;
    }

    @Override
    public float aimY() {
        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouse, viewport.getScreenX(), viewport.getScreenY(), viewport.getScreenWidth(), viewport.getScreenHeight());
        return mouse.y;
    }

    @Override
    public void resize(int width, int height) { viewport.update(width, height); }

    @Override
    public void dispose() {
        shots.dispose();
        terrain.dispose();
    }
}
