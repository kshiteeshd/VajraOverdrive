package vajra.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import vajra.SaveGame;
import vajra.flow.Flow;
import vajra.flow.FlowState;
import vajra.ship.ShipControls;
import vajra.weapon.Bullet;
import vajra.world.World;

/** The play loop on the Flow stack: samples input, updates the world, drives the camera,
 *  then hands off to WorldRenderer (world space) and Hud (UI space). Only the top of the
 *  stack updates, so any overlay pushed above it (pause) freezes the run for free. */
public class GameScreen implements FlowState, ShipControls {
    // DYNAMIC ZOOM — SPEEDING PULLS THE VIEW BACK.
    //
    //  libGDX OrthographicCamera.update() builds its projection as
    //      setToOrtho(zoom * -viewportWidth/2, zoom * +viewportWidth/2, ...)
    //  so the VISIBLE WORLD SPAN IS zoom x viewportWidth. That makes zoom > 1 the direction
    //  that shows MORE world (zoomed out) and zoom < 1 the direction that MAGNIFIES. It is the
    //  opposite of the usual "zoom in" intuition, and getting it backwards zooms IN when you
    //  asked to pull back. Rest is ZOOM = 1 (1:1); the far end of the pull is ZOOM_OUT above 1.
    public static final float ZOOM = 1f;        // the resting level, 1:1 with the world
    public static final float ZOOM_OUT = 1.3f;  // 1.3 x the world span: 1664 x 936 px visible
    private static final float ZOOM_SPEED = 520f; // px/s of ship speed that counts as "flat out"
    private static final float ZOOM_THREAT = 6;   // hostile rounds within THREAT_R that count as busy
    private static final float THREAT_R = 620f;
    private float zoom = ZOOM;

    private final SpriteBatch batch;
    private final Ui ui;
    private final Flow flow;
    private final WorldRenderer worldR;
    private final Hud hud;
    // THE WEAPON WHEEL: tap the weapon key to step, hold it to open the wheel (see WeaponWheel)
    private final WeaponWheel wheel;
    private final Blur blur = new Blur();
    private boolean wheelDown, wheelOpen;
    private float wheelHold;
    private int wheelKey, wheelHover = -1;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private ShatterFx shatter;
    private VajraPanel vajraPanel;
    private HudReveal reveal = new HudReveal();
    private final vajra.world.Arena.Scenario scenario; // the test arena's scenario; null in a real run (nothing of an arena flight is saved)
    private boolean godMode, arenaBar = true, tourQueued;
    private int arenaKind;
    private String arenaNote = "";
    private final Vector3 mouseV = new Vector3();
    private World world;
    private final Director director;
    private final SpeechBubbles talk;
    private vajra.coach.Coach coach; // the first flight: hands the controls over one at a time

    /** The run this screen is showing. Public so a GL screenshot test can seed the hangar and
     *  fire the Astra without a key-injection path that cannot set up state. */
    public World world() { return world; }
    private BayUi shopUi;  // the bay's layout: cards, wheel, hit boxes, every label
    private BayController shop; // purchases: what a pressed key or card actually does
    private boolean armed; // a mouse button still down from the click that started the run must not fire
    private boolean endSaved; // the run's end writes the save once
    private int accentLoka = -1; // the loka whose colour the screen line-work currently wears
    private float bootFade = 1f; // white wash in from the title launch (0.3 s)
    private String resume; // a saved run to wake, until the first world has used it
    private final long seed;  // a multiplayer race's shared seed; 0 = the legacy world
    private float autosaveT;
    static final float AUTOSAVE = 20f; // seconds between quiet saves of the solo run
    private float simAcc;     // fixed-step accumulator: the sim runs the same steps everywhere

    /** The sim's fixed timestep. Shadow races must fly identical steps on every machine,
     *  so the world advances in 1/60 s bites, never real frame time. */
    static final float STEP = 1 / 60f;
    private static final int MAX_STEPS = 5; // a stalled frame sheds backlog, never spirals

    public GameScreen(SpriteBatch batch, Flow flow, Ui ui) {
        this(batch, flow, ui, 0);
    }

    /** CONTINUE: the pilot's saved solo run (vajra.world.RunSave's text), poured over a fresh world. */
    public static GameScreen resume(SpriteBatch batch, Flow flow, Ui ui, String savedRun) {
        GameScreen g = new GameScreen(batch, flow, ui, 0, savedRun, null);
        return g;
    }

    /** The multiplayer handoff: every pilot enters the run with the session's one seed. */
    public GameScreen(SpriteBatch batch, Flow flow, Ui ui, long seed) {
        this(batch, flow, ui, seed, null, null);
    }

    /** A scenario of the test arena (Settings > TEST): a campaign world arranged for it, never saved. */
    public static GameScreen arena(SpriteBatch batch, Flow flow, Ui ui, vajra.world.Arena.Scenario scenario) {
        return new GameScreen(batch, flow, ui, 0, null, scenario);
    }

    private GameScreen(SpriteBatch batch, Flow flow, Ui ui, long seed, String savedRun, vajra.world.Arena.Scenario scenario) {
        this.scenario = scenario;
        this.resume = savedRun;
        this.batch = batch;
        this.flow = flow;
        this.ui = ui;
        this.director = new Director(ui);
        this.talk = new SpeechBubbles(ui);
        this.seed = seed;
        this.camera = worldCamera();
        this.viewport = new FitViewport(Ui.UI_W, Ui.UI_H, camera);
        this.shopUi = new BayUi(ui);
        this.world = loadWorld();
        world.sink = vajra.audio.Audio.get();
        this.worldR = new WorldRenderer(batch, ui, world);
        WorldThumb.of(world);   // bake the minimap's land now, while loading, not in the first frame
        this.hud = new Hud(ui);
        this.wheel = new WeaponWheel(ui);
        this.shop = new BayController(world, shopUi);
        this.coach = coachFor();
        newReveal();
        if (vajra.net.Net.active()) // a live co-op seat: the game planes land on the current world
            vajra.net.Net.session.listen((tag, sender, body, off, len) ->
                    vajra.net.GameWire.apply(vajra.net.Net.session, this.world, tag, sender, body, off, len));
    }

    /** The arena's pilot has learned everything (but the scenario that teaches it): no prompts over a test. */
    private vajra.coach.Coach coachFor() {
        if (scenario != null && !scenario.name.startsWith("FIRST LESSONS")) return new vajra.coach.Coach(true, true);
        return newCoach();
    }

    /** A pilot who has not learned to fly is held to the first flight; a shared race never is. */
    private static vajra.coach.Coach newCoach() {
        vajra.profile.Profile p = vajra.profile.Profiles.active();
        boolean free = vajra.net.Net.session != null || !p.settings.hints;
        return new vajra.coach.Coach(free || p.learned(vajra.coach.Lessons.THRUST),
                free || p.learned(vajra.coach.Lessons.FIRE));
    }

    /** A fresh run. The hangar starts at its DEFAULTS every time — coins, tiers, patterns,
     *  forged charges, all of it is built inside the run and dies with it. The save
     *  only contributes the best score. */
    private World loadWorld() {
        if (scenario != null) return vajra.world.Arena.build(scenario);
        World w = new World(seed, World.CAMPAIGN);   // the campaign: Atala, and Vitala on its roof
        SaveGame.load(w.hangar);
        w.selectShip(w.hangar.shipIndex);
        w.selectWeapon(w.hangar.weaponIndex);
        w.multiplayer = vajra.net.Net.session != null; // its own book, not the campaign one
        if (resume != null && !w.multiplayer) { // CONTINUE: the saved run over the fresh world, once
            vajra.world.RunSave.restore(w, resume);
            resume = null;
        }
        if (vajra.net.Net.active()) // co-op: the guest runs only its hull; the host runs the fight
            if (!vajra.net.Net.session.isHost()) w.seatAsClient();
        return w;
    }

    @Override
    public boolean keyDown(int keycode) {
        return onKey(keycode);
    }

    private boolean onKey(int keycode) {
        if (director.active()) { // a cutscene owns every key: ESC leaves it, anything else reads on
            if (keycode == Input.Keys.ESCAPE) director.skip();
            else director.press();
            return true;
        }
        if (scenario != null && !shop.isOpen() && arenaKey(keycode)) return true;
        if (keycode == Input.Keys.ESCAPE && shop.isOpen()) {
            shop.close(); // parks that bay until the ship flies out — no reopening
            return true;
        }
        if (keycode == Input.Keys.ESCAPE && !shop.isOpen()) {
            if (vajra.net.Net.session != null
                    && vajra.net.Net.session.state() == vajra.net.Session.State.LOST) {
                // THE HOST IS GONE: this run can never end — the world waits on a run-over
                // bit that will never come. Leave it now, and leave it through the lobby,
                // where the room can be closed properly.
                vajra.profile.Profiles.endRun(world);
                flow.set(new LobbyScreen(flow, ui, batch));
                return true;
            }
            dropWheel();
            world.astraCancel(); // a half-held invocation is let go for free
            if (scenario != null) { flow.pop(); return true; }   // the test arena: ESC is the way back to the list
            flow.push(new PauseScreen(flow, ui, batch, world)); // the run freezes underneath
            return true;
        }
        if (shop.isOpen()) return shop.keyDown(keycode);
        if (runEnded() && keycode == Input.Keys.R) {
            restart(); // a fresh run, hangar back at its defaults, the story beats replay
            return true;
        }
        if (keycode == K(vajra.Keymap.ASTRA)) {
            // THE ASTRA RITE: F down begins the invocation (the cage gathers); F up looses it (see keyUp).
            if (!runEnded()) world.astraBegin();
            return true;
        }
        if (keycode == K(vajra.Keymap.ASTRA_CYCLE)) { // choose which astra the next F invokes
            world.cycleAstra();
            return true;
        }
        if (keycode == K(vajra.Keymap.HULL_NEXT)) { // next hull you own (Q and E are the nose, always)
            world.selectShip(world.hangar.cycleShip(world.hangar.shipIndex, 1));
            return true;
        }
        if (keycode == K(vajra.Keymap.REPAIR)) { // a stored repair: 40% of the hull, spent on the spot
            world.useMedkit();
            return true;
        }
        if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_7) {
            world.selectShip(keycode - Input.Keys.NUM_1); // 2-7 are hulls 2-7; the first hull is Z's, and a hull you do not own is a no-op
            return true;
        }
        // THE WEAPON KEY: V, or B while a session owns V for push-to-talk. A tap steps (V next, B previous), a hold
        // opens the wheel; which of the two it was is only known on release (keyUp) or after OPEN_AFTER seconds.
        boolean session = vajra.net.Net.active();
        int wk = session ? K(vajra.Keymap.WEAPON_PREV) : K(vajra.Keymap.WEAPON_NEXT);
        if (keycode == wk) {
            wheelDown = true;
            wheelHold = 0;
            wheelKey = wk;
            return true;
        }
        if (keycode == K(vajra.Keymap.WEAPON_PREV)) { // solo: a plain "previous weapon"
            world.selectWeapon(world.hangar.cycleWeapon(world.hangar.weaponIndex, -1));
            return true;
        }
        return false; // V in a session is push-to-talk
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == K(vajra.Keymap.ASTRA)) { // LET GO: the astra flies (or the mantra falters, if it was not held long enough)
            world.astraLoose();
            return true;
        }
        if (!wheelDown || keycode != wheelKey) return false;
        wheelDown = false;
        if (wheelOpen) { // let go on the wheel: fit what the mouse points at
            int[] own = WeaponWheel.owned(world.hangar);
            if (wheelHover >= 0 && wheelHover < own.length) world.selectWeapon(own[wheelHover]);
            wheelOpen = false;
            wheelHover = -1;
        } else { // a tap: step to the next (V) or the previous (B in a session)
            world.selectWeapon(world.hangar.cycleWeapon(world.hangar.weaponIndex, wheelKey == K(vajra.Keymap.WEAPON_PREV) ? -1 : 1));
        }
        return true;
    }

    /** Closes the wheel without choosing (a menu took the keys, a scene started, the run ended). */
    // ---- the test arena's keys and bar ----------------------------------------------------------------------------------------------

    private boolean arenaKey(int k) {
        vajra.ship.Hangar h = world.hangar;
        int n = vajra.weapon.WeaponDef.ALL.length;
        switch (k) {
            case Input.Keys.F4: arenaBar = !arenaBar; return true;
            case Input.Keys.F5: godMode = !godMode; arenaNote = "GOD MODE " + (godMode ? "ON" : "OFF"); return true;
            case Input.Keys.F6: {
                boolean back = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);
                int i = Math.floorMod(h.weaponIndex + (back ? -1 : 1), n);
                h.weaponOwned[i] = true;
                world.selectWeapon(i);
                arenaNote = (i + 1) + ". " + vajra.weapon.WeaponDef.ALL[i].name.toUpperCase();
                return true;
            }
            case Input.Keys.F7: {
                int i = h.weaponIndex;
                h.weaponRank[i] = (h.weaponRank[i] + 1) % vajra.weapon.WeaponDef.RANKS;
                world.selectWeapon(i);
                arenaNote = vajra.weapon.WeaponDef.ALL[i].name.toUpperCase() + " RANK " + BayUi.roman(h.weaponRank[i]);
                return true;
            }
            case Input.Keys.F8:
                arenaKind = (arenaKind + 1) % vajra.world.Arena.packKinds();
                vajra.world.Arena.pack(world, arenaKind);
                arenaNote = "SPAWNED 3 " + vajra.world.Arena.packName(arenaKind);
                return true;
            case Input.Keys.F9: vajra.world.Arena.clear(world); arenaNote = "FOES CLEARED"; return true;
            case Input.Keys.F10: vajra.world.Arena.refill(world); arenaNote = "REFILLED"; return true;
            case Input.Keys.F2: restart(); arenaNote = "SCENARIO RESET"; return true;
            default: return false;
        }
    }

    private void renderArenaBar() {
        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        ui.caption.setColor(1f, 0.84f, 0.35f, 0.85f);
        String line = "TEST ARENA  " + scenario.name + (godMode ? "   [GOD]" : "") + (arenaNote.isEmpty() ? "" : "   -   " + arenaNote);
        ui.caption.draw(batch, line, Ui.UI_W / 2f - ui.measure(ui.caption, line) / 2f, 40);
        ui.caption.setColor(1, 1, 1, 0.5f);
        String keys = "F4 BAR  F5 GOD  F6 PATTERN  F7 RANK  F8 FOES  F9 CLEAR  F10 REFILL  F2 RESET  ESC BACK";
        ui.caption.draw(batch, keys, Ui.UI_W / 2f - ui.measure(ui.caption, keys) / 2f, 22);
        ui.caption.setColor(1, 1, 1, 1);
        batch.end();
    }

    /** Where the hull is on the screen, in UI units (the camera is centred on it unless the world's edge holds it back). */
    private float shipUiX() { return Math.max(70f, Math.min(Ui.UI_W - 70f, Ui.UI_W / 2f + (world.ship.x - camera.position.x) / Math.max(0.1f, camera.zoom))); }

    private float shipUiY() { return Math.max(70f, Math.min(Ui.UI_H - 70f, Ui.UI_H / 2f + (world.ship.y - camera.position.y) / Math.max(0.1f, camera.zoom))); }

    private void dropWheel() {
        wheelDown = wheelOpen = false;
        wheelHover = -1;
    }

    @Override
    public boolean touchDown(int sx, int sy, int pointer, int button) {
        Vector2 m = ui.at(sx, sy);
        if (director.active()) {
            director.press();
            return true;
        }
        if (shop.isOpen()) return shop.click(m);
        return false; // a click never restarts a run: R does, on purpose
    }

    @Override
    public boolean mouseMoved(int sx, int sy) {
        if (shop.isOpen()) shop.hover(ui.at(sx, sy));
        return false;
    }

    /** The run is over: dead, the blade whole, or Bala fallen and his banner lap played out.
     *  The world freezes, the results card stays up, R (or a click) starts a fresh run. */
    private boolean runEnded() {
        return world.runEnded();
    }

    /** A new game starts with an empty screen: the instruments come on as the flight needs them (docs/TUTORIAL.md). A continued run, a shared one and the arena have them all. */
    private void newReveal() {
        reveal = new HudReveal();
        if (scenario == null && vajra.net.Net.session == null && hintsOn(vajra.profile.Profiles.active()) && world.cues.contains(vajra.world.Cue.OPENING)) reveal.begin();
        hud.reveal(reveal);
    }

    /** A fresh run: a new world from the saved hangar, and the story beats replay with it. */
    /** Put `nw` in play (a restart): its bay controller, the thumb of its land. The renderer's land is the campaign world's, which every run shares. */
    private void swapWorld(World nw) {
        world = nw;
        world.sink = vajra.audio.Audio.get();
        WorldThumb.of(world);
        shop = new BayController(world, shopUi);
        director.skip();
        simAcc = 0;
    }

    private void restart() {
        resume = null; // a restart is a new game, never the old save
        World nw = loadWorld();
        swapWorld(nw);
        shop = new BayController(world, shopUi);
        coach = coachFor();
        newReveal();
        endSaved = false;
    }


    /** World camera, y pointing UP: increasing y is screen-up. Bhu Loka sits at the bottom of
     *  the world (spawn y≈800) and the boss arena at the top (y 17600–20600), so you climb upward.
     *  UI uses the same y-up convention. */
    static OrthographicCamera worldCamera() {
        return new OrthographicCamera(Ui.UI_W, Ui.UI_H);
    }

    /** DYNAMIC ZOOM, one pull with two feeds, and it only ever pulls OUT. Speed is normalized
     *  against the ship's own top speed, so a fast hull and a slow one reach the same pull;
     *  pressure counts hostile rounds near the ship, which is exactly when you want to see
     *  further. Whichever feed asks for more wins, and the level eases so it never snaps. */
    private void updateZoom(float dt) {
        float sp = (float) Math.sqrt(world.ship.vx * world.ship.vx + world.ship.vy * world.ship.vy);
        float speed = Math.min(1f, sp / ZOOM_SPEED);
        int threat = 0;
        for (int i = 0; i < world.bullets.size(); i++) {
            Bullet b = world.bullets.get(i);
            if (!b.hostile) continue;
            float dx = b.x - world.ship.x, dy = b.y - world.ship.y;
            if (dx * dx + dy * dy < THREAT_R * THREAT_R && ++threat >= ZOOM_THREAT) break;
        }
        float pressure = Math.min(1f, threat / (float) ZOOM_THREAT);
        // the Astra's release punches the view BACK, which is the only direction this camera
        // ever moves - the moment opens the fight up instead of closing in on it
        float pull = Math.max(Math.max(speed, pressure), world.astraPunch);
        float want = ZOOM + pull * (ZOOM_OUT - ZOOM); // always >= ZOOM: out, never in
        want = Math.max(want, world.astraView());      // and an astra at work pulls the view back far enough to see all of it
        zoom += (want - zoom) * (1f - (float) Math.exp(-(want > zoom ? (world.astraSlowView() ? 0.3f : 3f) : 1.4f) * dt)); // ~1/3 s to settle going out, a slower, easier return
    }

    @Override
    public void update(float delta) {
        if (scenario != null) {
            if (godMode) { world.ship.hp = world.ship.maxHp(); world.ship.invuln = 1f; }
            if (scenario.tour && !tourQueued) {                       // the tutorial of a new game, on demand
                tourQueued = true;
                reveal.demo();
            }
        }
        // Apply the FitViewport before the sim runs: world.update() (and aimX/Y under a
        // held mouse button) unprojects through it, and a 0x0 viewport turns aim into NaN.
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (accentLoka != world.lokaIndex) { // the run's loka colour code, straight from the table
            accentLoka = world.lokaIndex;
            float[] c = world.elementRgb();
            Template.setAccent(c[0], c[1], c[2]);
        }
        if (!armed && !Gdx.input.isButtonPressed(Input.Buttons.LEFT) && !Gdx.input.isButtonPressed(Input.Buttons.RIGHT))
            armed = true; // the click that pressed PLAY is released: the mouse is the pilot's now
        if (bootFade > 0) bootFade = Math.max(0, bootFade - delta / 0.3f);
        if (wheelDown) {
            if (shop.isOpen() || runEnded() || director.active()) dropWheel();
            else {
                wheelHold += delta;
                if (!wheelOpen && wheelHold >= WeaponWheel.OPEN_AFTER) { wheelOpen = true; vajra.audio.Audio.get().ui("ui_wheel_open"); }
                if (wheelOpen) {
                    Vector2 m = ui.at(Gdx.input.getX(), Gdx.input.getY());
                    int hv = WeaponWheel.pick(WeaponWheel.owned(world.hangar).length, m.x - WeaponWheel.CX, m.y - WeaponWheel.CY);
                    if (hv != wheelHover && hv >= 0) vajra.audio.Audio.get().ui("ui_wheel_tick");
                    wheelHover = hv;
                }
            }
        }
        if (world.astraInvoking && (shop.isOpen() || director.active())) world.astraCancel(); // a menu or a scene took the keys
        // the wheel PAUSES a solo flight; a shared one cannot stop (a pause would be a free dodge), so it crawls
        float simDelta = wheelOpen ? (vajra.net.Net.session == null ? 0f : delta * WeaponWheel.SLOW) : delta;
        if (!shop.isOpen() && !runEnded() && !director.active()) {
            // RELEASE FREEZE. The world holds still for a fraction of a second the instant the
            // Astra lets go. Impact is mostly the absence of motion: without this the cubes and
            // the shake arrive in the same frame and neither reads. (Presentation time: it
            // burns real dt outside the fixed steps, like the boot fade.)
            if (world.hitStop > 0) {
                world.hitStop = Math.max(0f, world.hitStop - delta);
            } else {
                // FIXED-STEP SIM: same 1/60 s bites on every machine, so a seeded shadow race
                // flies identical frames everywhere. A slow frame sheds its backlog instead
                // of spiralling; a fast frame banks at most one step (feels identical, keeps
                // the accumulator from drifting ahead of real time).
                simAcc += simDelta;
                if (simAcc > 2 * STEP) simAcc = 2 * STEP; // never run more than 2 steps/frame
                int steps = 0;
                while (simAcc >= STEP && steps < MAX_STEPS) {
                    world.update(STEP, this);
                    simAcc -= STEP;
                    steps++;
                }
                if (world.shopReady()) { shop.open(); world.snd(vajra.audio.Snd.DOCK, world.ship.x, world.ship.y, 0, 0f); } // docking just finished: the hangar comes up
            }
        }
        // The session rides the run: hosts and guests pump the co-op planes on their world's
        // cadence here (hearts ride Flow), and the talk key feeds push-to-talk either way.
        if (vajra.net.Net.active()) {
            vajra.net.GameWire.pump(vajra.net.Net.session, world, delta);
            vajra.net.Net.session.voice.push(pressed(Input.Keys.V));
        }
        if (!endSaved && runEnded() && scenario != null) endSaved = true;   // an arena flight folds into nothing
        if (!endSaved && runEnded()) {
            vajra.profile.Profiles.endRun(world); // folds the run into the pilot + the save
            if (vajra.net.Net.active()) // a race result: the roster's book keeps the best flight
                vajra.net.Net.session.report(world.score.points);
            endSaved = true;
        }
        updateZoom(delta);

        float viewW = camera.viewportWidth * zoom;
        float viewH = camera.viewportHeight * zoom;
        camera.zoom = zoom;
        camera.position.set(
                Math.max(viewW / 2, Math.min(world.width - viewW / 2, world.ship.x)),
                Math.max(viewH / 2, Math.min(world.height - viewH / 2, world.ship.y)),
                0
        );
        float[] cf = world.camFocus(vajra.profile.Profiles.active().settings.gentleCamera);
        if (cf[2] > 0.001f) {                                      // the camera is drawn to someone else for a while
            camera.position.x += (Math.max(viewW / 2, Math.min(world.width - viewW / 2, cf[0])) - camera.position.x) * cf[2];
            camera.position.y += (Math.max(viewH / 2, Math.min(world.height - viewH / 2, cf[1])) - camera.position.y) * cf[2];
        }
        float s2 = world.ship.trauma * world.ship.trauma * vajra.profile.Profiles.active().settings.shake;
        camera.position.x += (world.fxRng.nextFloat() * 2 - 1) * 16 * s2;
        camera.position.y += (world.fxRng.nextFloat() * 2 - 1) * 16 * s2;
        if (scenario == null && vajra.net.Net.session == null && !director.active() && !shop.isOpen() && !runEnded() && !world.death.active && !world.maya.active && world.yielder == null) {
            autosaveT += delta; // CONTINUE stays close to where the pilot is, even if the window is killed
            if (autosaveT >= AUTOSAVE) {
                autosaveT = 0;
                vajra.profile.Profiles.saveRun(world);
            }
        }
        if (!director.active() && !shop.isOpen() && !runEnded()) {
            int learned = coach.update(delta, world.ship.x, world.ship.y, rawFire());
            if (learned >= 0 && scenario == null) { // the pilot has flown / fired: that lesson is theirs for good
                vajra.profile.Profiles.active().learn(learned);
                vajra.profile.Profiles.save();
            }
        }
        if (!director.active() && !shop.isOpen() && !runEnded()) reveal.update(world, coach.canFire(), delta);
        vajra.audio.Audio.get().update(world, delta, director.active(), shop.isOpen(), false);
        singScene();
        stageCues(); // after the live camera is set, so a scene eases out of what was on screen
        director.steer(delta, camera, world); // a cutscene, if one plays, takes the camera
        camera.update();
    }

    /** Story cues the world raised become cutscenes, one at a time, when nothing else has the
     *  screen. A shared race has no pause to give, so its cues are simply dropped. */
    private static boolean hintsOn(vajra.profile.Profile p) { return p.settings.hints && vajra.Settings.tutorialAllowed; }

    private String sung;   // the line of a scene being sung now (so each line is begun once)

    /** A scene's words are sung (docs/SOUND_PLAN.md 1.6): when a new line begins to be typed, the speaker's instrument begins to sing it. */
    private void singScene() {
        vajra.scene.CutscenePlayer p = director.player();
        if (p == null) { sung = null; return; }
        vajra.scene.Shot sh = p.shot();
        if (sh == null || p.text().isEmpty()) return;
        String full = p.fullText();
        if (full.equals(sung)) return;
        sung = full;
        vajra.audio.Audio.get().sing(sh.player ? "PILOT" : sh.speaker == null ? "INDRA" : sh.speaker, full, vajra.scene.CutscenePlayer.CPS * vajra.scene.CutscenePlayer.speed);
    }

    @Override
    public boolean silentKeys() { return true; }

    private void stageCues() {
        if (vajra.net.Net.session != null) { // no pause to give: settle what a scene would have settled
            while (!world.cues.isEmpty()) vajra.scene.Scenes.finish(world.cues.poll(), world);
            return;
        }
        if (director.active() || shop.isOpen() || world.cues.isEmpty()) return;
        vajra.world.Cue cue = world.cues.poll();
        vajra.profile.Profile pilot = vajra.profile.Profiles.active();
        if (cue == vajra.world.Cue.FIRST_LAMP || cue == vajra.world.Cue.FIRST_BAY) {          // told once to a pilot, and never to one who has asked for no hints
            int beat = cue == vajra.world.Cue.FIRST_LAMP ? vajra.profile.Profile.BEAT_LAMP : vajra.profile.Profile.BEAT_BAY;
            if (scenario == null) {
                if (!hintsOn(pilot) || pilot.seen(beat)) return;
                pilot.see(beat);
                vajra.profile.Profiles.save();
            }
        }
        vajra.audio.Audio.get().scene(cue, true);
        sung = null;
        try {
            director.start(vajra.scene.Scenes.build(cue, world), camera, () -> {
            vajra.audio.Audio.get().scene(cue, false);
            vajra.scene.Scenes.finish(cue, world);
            });
        } catch (RuntimeException e) {
            if (scenario == null) throw e;
            arenaNote = "SCENE FAILED: " + e.getClass().getSimpleName();   // a scenario that cannot stage its scene says so and flies on
            vajra.scene.Scenes.finish(cue, world);
        }
    }

    @Override
    public void render() {
        if (!flow.isTop(this)) armed = false; // under a menu: the next click on one must not fire when it closes
        // pushed on the Flow without a resize event, the FitViewport never applied itself and
        // Lights' viewport.apply() left glViewport at 0x0 — every frame after the first drew
        // nothing. Apply it here every frame: boot, resize and the lights' letterbox all land.
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Gdx.gl.glClearColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (world.maya.active && shatter != null) shatter.prepare(world, this::captureFrom);   // (doors: the far side of each piece, drawn and kept before the real frame)
        Gdx.gl.glClearColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        worldR.render(world, camera, viewport);
        if (wheelOpen && !director.active()) {
            blur.render(batch, 1.6f);   // the held wheel: the flight behind it goes soft
            ui.applyViewport();
        }
        if (director.active()) director.render(batch);
        else {
            hud.render(batch, world, worldR.ships(), shop.isOpen() ? shopUi : null);
            if (vajraPanel == null) vajraPanel = new VajraPanel(ui);
            if (!shop.isOpen()) vajraPanel.render(batch, world, Gdx.graphics.getDeltaTime());   // the piece shown big, then the 1/7 panel in the corner
            if (wheelOpen) wheel.render(batch, world, wheelHover);
            if (!shop.isOpen()) talk.render(batch, world, camera);
            if (coach.lesson() >= 0 && !shop.isOpen() && !runEnded() && world.pieceShowT < 0f)
                CoachPrompt.render(ui, batch, coach.lesson(), world.clock);
        }
        if (!director.active() && !shop.isOpen()) reveal.render(ui, batch);
        if (scenario != null && arenaBar) renderArenaBar();
        if (world.maya.active) { if (shatter == null) shatter = new ShatterFx(); shatter.render(world, camera, viewport); }   // Bala's broken mirror: the whole frame, in pieces
        if (Probe.showFrame) perfReadout();
        if (bootFade > 0) { // the white the title's launch left behind, fading off the run
            ui.shapes.setProjectionMatrix(ui.camera.combined);
            ui.beginShapes(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
            ui.shapes.setColor(1, 1, 1, bootFade);
            ui.shapes.rect(0, 0, Ui.UI_W, Ui.UI_H);
            ui.shapes.end();
        }
    }

    private final OrthographicCamera snapCam = new OrthographicCamera();

    /** Draws the world as seen from (x, y) on the back buffer and copies the picture into `into`: the hull is put there for the moment of the drawing and put back (the frame that follows
     *  clears and draws the real view). */
    private void captureFrom(com.badlogic.gdx.graphics.Texture into, float x, float y) {
        vajra.ship.Ship s = world.ship;
        float ox = s.x, oy = s.y;
        snapCam.viewportWidth = camera.viewportWidth;
        snapCam.viewportHeight = camera.viewportHeight;
        snapCam.zoom = Math.max(1f, camera.zoom);
        snapCam.position.set(x, y, 0f);
        snapCam.update();
        s.x = x;
        s.y = y;
        Gdx.gl.glClearColor(UiTheme.INK_R, UiTheme.INK_G, UiTheme.INK_B, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        worldR.render(world, snapCam, viewport);
        s.x = ox;
        s.y = oy;
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        into.bind();
        Gdx.gl.glCopyTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, 0, 0, into.getWidth(), into.getHeight());
    }

    private final float[] frameMs = new float[60];
    private int frameAt;
    private long lastFrame;
    private String perfText = "";

    /** F3: the average and the worst frame of the last second, the zoom and the particle count, so lag can be named on any machine. */
    private void perfReadout() {
        long now = System.nanoTime();
        if (lastFrame != 0) frameMs[frameAt++ % frameMs.length] = (now - lastFrame) / 1e6f;
        lastFrame = now;
        if (frameAt % 20 == 0) {
            float sum = 0, worst = 0;
            int n = Math.min(frameAt, frameMs.length);
            for (int i = 0; i < n; i++) { sum += frameMs[i]; worst = Math.max(worst, frameMs[i]); }
            perfText = String.format("%.1f ms avg   %.0f ms worst   %d fps   zoom %.2f   flow %d", sum / Math.max(1, n), worst, Gdx.graphics.getFramesPerSecond(), zoom, world.flow.n);
        }
        batch.setProjectionMatrix(ui.camera.combined);
        batch.begin();
        ui.caption.setColor(1f, 1f, 0.4f, 1f);
        ui.caption.draw(batch, perfText, 330f, 20f);
        ui.caption.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    /** The key this pilot has for an action. */
    private static int K(vajra.Keymap a) { return a.key(vajra.profile.Profiles.active().settings); }

    private boolean pressed(int key) {
        return Gdx.input.isKeyPressed(key);
    }

    @Override
    public float thrustY() {
        return (pressed(K(vajra.Keymap.UP)) || pressed(Input.Keys.UP) ? 1 : 0)
                + (pressed(K(vajra.Keymap.DOWN)) || pressed(Input.Keys.DOWN) ? -1 : 0);
    }

    @Override
    public float thrustX() {
        return (pressed(K(vajra.Keymap.RIGHT)) || pressed(Input.Keys.RIGHT) ? 1 : 0)
                + (pressed(K(vajra.Keymap.LEFT)) || pressed(Input.Keys.LEFT) ? -1 : 0);
    }

    @Override
    public float rotate() {
        return (pressed(K(vajra.Keymap.TURN_LEFT)) ? 1 : 0) + (pressed(K(vajra.Keymap.TURN_RIGHT)) ? -1 : 0);
    }

    @Override
    public boolean fire() {
        return !wheelOpen && coach.canFire() && rawFire(); // the trigger is handed over once the pilot has flown
    }

    private boolean rawFire() {
        return pressed(K(vajra.Keymap.FIRE)) || (armed && Gdx.input.isButtonPressed(Input.Buttons.LEFT));
    }

    @Override
    public boolean boost() {
        return pressed(K(vajra.Keymap.BOOST)) || pressed(Input.Keys.SHIFT_RIGHT);
    }

    @Override
    public boolean shield() {
        // F belongs to the Akasha Astra now, so the shield lives on G and the right mouse
        // button. G was the Tatva Lance's key and the Astra replaced it.
        return pressed(K(vajra.Keymap.SHIELD)) || (armed && Gdx.input.isButtonPressed(Input.Buttons.RIGHT));
    }

    @Override
    public boolean mouseAim() {
        return !wheelOpen && armed && coach.canFire() && Gdx.input.isButtonPressed(Input.Buttons.LEFT);
    }

    @Override
    public float aimX() {
        mouseV.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouseV, viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        return mouseV.x;
    }

    @Override
    public float aimY() {
        mouseV.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        camera.unproject(mouseV, viewport.getScreenX(), viewport.getScreenY(),
                viewport.getScreenWidth(), viewport.getScreenHeight());
        return mouseV.y;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void dispose() {
        vajra.audio.Audio.get().leaveGame();
        if (shatter != null) shatter.dispose();
        if (vajraPanel != null) vajraPanel.dispose();
        if (vajra.net.Net.session == null && scenario == null) vajra.profile.Profiles.saveRun(world); // closed mid-flight: parked
        if (vajra.net.Net.active()) vajra.net.Net.session.listen(null); // no planes to a dead world
        wheel.dispose();
        blur.dispose();
        worldR.dispose();
        hud.dispose();
    }
}
