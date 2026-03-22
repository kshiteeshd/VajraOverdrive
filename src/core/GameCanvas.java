package core;

import campaign.CampaignManager;
import config.LayoutConfig;
import entity.EntityManager;
import gfx.FxLayer;
import gfx.ScreenShake;
import gfx.SpriteRegistry;
import physics.CollisionSystem;
import player.PlayerShip;
import save.ProfileManager;
import save.SettingsManager;
import save.SettingsProfile;
import score.ScoreManager;
import ui.*;
import ui.menu.*;
import ui.theme.UIFonts;
import ui.theme.UITheme;
import wave.WaveManager;
import input.InputManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

public class GameCanvas extends JPanel {

    // ── Core ──────────────────────────────────────────────────────────
    private final GameWindow    window;
    private       int           fps;
    private final BufferedImage gameBuffer;
    private final InputManager  input;

    // ── Game systems — rebuilt each session ───────────────────────────
    private EntityManager   entityManager;
    private PlayerShip      player;
    private CampaignManager campaignManager;

    // ── Backgrounds ───────────────────────────────────────────────────
    private final SpaceBackground spaceBackground;
    private       GameState       lastState = null;

    // ── HUD ───────────────────────────────────────────────────────────
    private final GameUILayout uiLayout;

    // ── In-game screens ───────────────────────────────────────────────
    private final CampaignIntroScreen    campaignIntro;
    private final RegionIntroScreen      regionIntro;
    private final LevelLoadScreen        levelLoad;
    private final LevelClearScreen       levelClear;
    private final GameOverScreen         gameOver;
    private final CampaignCompleteScreen campaignComplete;

    // ── Menu screens ──────────────────────────────────────────────────
    private final MainMenuScreen     mainMenu;
    private final NewGameMenuScreen  newGameMenu;
    private final NameEntryScreen    nameEntry;
    private final LoadGameMenuScreen loadGameMenu;
    private final SettingsMenuScreen settingsMenu;
    private final ControlsMenuScreen controlsMenu;   // kept for direct nav if needed

    public GameCanvas(GameWindow window) {
        this.window = window;

        SpriteRegistry.load();
        SettingsManager.get();          // load settings early

        gameBuffer = new BufferedImage(
                LayoutConfig.VIRTUAL_WIDTH,
                LayoutConfig.VIRTUAL_HEIGHT,
                BufferedImage.TYPE_INT_ARGB
        );

        setBackground(Color.BLACK);
        setFocusable(true);
        setPreferredSize(new Dimension(
                LayoutConfig.VIRTUAL_WIDTH,
                LayoutConfig.VIRTUAL_HEIGHT));

        input = new InputManager();
        addKeyListener(input);

        // Default dummy session — rebuilt on real game start
        entityManager   = new EntityManager();
        uiLayout        = new GameUILayout();
        player          = PlayerShip.createDefault(entityManager);
        campaignManager = new CampaignManager(entityManager, player, 1);
        FxLayer.get().init(entityManager);

        spaceBackground = new SpaceBackground();
        spaceBackground.setRegion(campaignManager.getCurrentRegion());

        // In-game
        campaignIntro    = new CampaignIntroScreen();
        regionIntro      = new RegionIntroScreen();
        levelLoad        = new LevelLoadScreen();
        levelClear       = new LevelClearScreen();
        gameOver         = new GameOverScreen();
        campaignComplete = new CampaignCompleteScreen();

        // Menus
        mainMenu     = new MainMenuScreen();
        newGameMenu  = new NewGameMenuScreen();
        nameEntry    = new NameEntryScreen();
        loadGameMenu = new LoadGameMenuScreen();
        settingsMenu = new SettingsMenuScreen();
        controlsMenu = new ControlsMenuScreen();

        GameStateManager.setState(GameState.MAIN_MENU);
    }

    // ── Session rebuild ───────────────────────────────────────────────
    private void rebuildSession(boolean isNewGame) {
        int startLevel = PendingGameStart.level;

        entityManager   = new EntityManager();
        player          = PlayerShip.createDefault(entityManager);
        campaignManager = new CampaignManager(entityManager, player, startLevel);
        FxLayer.get().init(entityManager);
        WaveManager.resetSession(3);

        if (isNewGame) {
            ScoreManager.get().reset();
        } else {
            ScoreManager.get().reset();
            if (ProfileManager.getProfile() != null)
                ScoreManager.get().setScore(ProfileManager.getProfile().score);
        }

        spaceBackground.setRegion(campaignManager.getCurrentRegion());
    }

    // ── Update ────────────────────────────────────────────────────────
    public void update() {

        // Global fullscreen toggle
        if (InputManager.isKeyPressed(KeyEvent.VK_F))
            window.toggleFullscreen();

        GameState state = GameStateManager.getState();

        if (state != lastState) {
            onStateEntered(state);
            lastState = state;
        }

        switch (state) {

            case MAIN_MENU      -> mainMenu.update();
            case NEW_GAME_MENU  -> newGameMenu.update();
            case NAME_ENTRY     -> nameEntry.update();
            case LOAD_GAME_MENU -> loadGameMenu.update();
            case SETTINGS_MENU  -> settingsMenu.update();
            case CONTROL_MENU   -> controlsMenu.update();

            case CAMPAIGN_INTRO -> {
                campaignIntro.update();
                if (InputManager.isKeyPressed(KeyEvent.VK_SPACE))
                    GameStateManager.setState(GameState.REGION_INTRO);
            }

            case REGION_INTRO -> regionIntro.update();
            case LEVEL_LOAD   -> levelLoad.update();

            case PLAYING -> {
                if (WaveManager.isGameOver()) {
                    GameStateManager.setState(GameState.GAME_OVER);
                    break;
                }
                spaceBackground.update();
                uiLayout.setRegion(campaignManager.getCurrentRegion());
                entityManager.update();
                campaignManager.update();
                CollisionSystem.checkCollisions(entityManager.getEntities());
            }

            case LEVEL_TRANSITION -> {
                spaceBackground.update();
                campaignManager.update();
            }

            case GAME_OVER -> {
                gameOver.update();
                if (InputManager.isKeyPressed(KeyEvent.VK_ENTER))
                    GameStateManager.setState(GameState.MAIN_MENU);
            }

            case CAMPAIGN_COMPLETE -> {
                campaignComplete.update();
                if (InputManager.isKeyPressed(KeyEvent.VK_ENTER))
                    GameStateManager.setState(GameState.MAIN_MENU);
            }
        }
    }

    // ── State entry hooks ─────────────────────────────────────────────
    private void onStateEntered(GameState state) {
        switch (state) {

            case MAIN_MENU      -> mainMenu.enter();
            case NEW_GAME_MENU  -> newGameMenu.enter();
            case NAME_ENTRY     -> nameEntry.enter();
            case LOAD_GAME_MENU -> loadGameMenu.enter();
            case SETTINGS_MENU  -> settingsMenu.enter();

            case CAMPAIGN_INTRO -> {
                rebuildSession(true);
                campaignIntro.reset();
            }

            case LEVEL_LOAD -> {
                rebuildSession(false);
                levelLoad.enter(
                        campaignManager.getCurrentLevel(),
                        campaignManager.getCurrentRegion()
                );
                spaceBackground.setRegion(campaignManager.getCurrentRegion());
            }

            case REGION_INTRO -> {
                String region = campaignManager.getCurrentRegion();
                regionIntro.enter(region);
                spaceBackground.setRegion(region);
            }

            case PLAYING ->
                    spaceBackground.setRegion(campaignManager.getCurrentRegion());

            case LEVEL_TRANSITION -> {
                levelClear.enter(
                        campaignManager.getCurrentLevel(),
                        campaignManager.getCurrentRegion()
                );
                saveProgress();
            }

            case GAME_OVER -> {
                saveProgress();
                gameOver.enter();
            }

            case CAMPAIGN_COMPLETE -> {
                saveProgress();
                campaignComplete.enter(ScoreManager.get().getScore());
            }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────
    private void saveProgress() {
        if (ProfileManager.getProfile() != null) {
            ProfileManager.getProfile().level  = campaignManager.getCurrentLevel();
            ProfileManager.getProfile().region = campaignManager.getCurrentRegion();
            ProfileManager.getProfile().score  = ScoreManager.get().getScore();
            ProfileManager.save();
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    public void setFPS(int fps) { this.fps = fps; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D gb = gameBuffer.createGraphics();
        gb.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gb.setColor(Color.BLACK);
        gb.fillRect(0, 0, LayoutConfig.VIRTUAL_WIDTH, LayoutConfig.VIRTUAL_HEIGHT);

        switch (GameStateManager.getState()) {

            case MAIN_MENU      -> mainMenu.render(gb);
            case NEW_GAME_MENU  -> newGameMenu.render(gb);
            case NAME_ENTRY     -> nameEntry.render(gb);
            case LOAD_GAME_MENU -> loadGameMenu.render(gb);
            case SETTINGS_MENU  -> settingsMenu.render(gb);
            case CONTROL_MENU   -> controlsMenu.render(gb);
            case CAMPAIGN_INTRO -> campaignIntro.render(gb);
            case REGION_INTRO   -> regionIntro.render(gb);
            case LEVEL_LOAD     -> levelLoad.render(gb);

            case PLAYING -> {
                spaceBackground.render(gb,
                        LayoutConfig.GAME_AREA_X, 0,
                        LayoutConfig.GAME_AREA_WIDTH,
                        LayoutConfig.GAME_AREA_HEIGHT);
                uiLayout.render(gb);
                entityManager.render(gb);
                gb.setFont(UIFonts.SMALL);
                gb.setColor(UITheme.TEXT_FAINT);
                gb.drawString(fps + " fps",
                        LayoutConfig.GAME_AREA_X + 6,
                        LayoutConfig.GAME_AREA_HEIGHT - 6);
            }

            case LEVEL_TRANSITION -> {
                spaceBackground.render(gb,
                        LayoutConfig.GAME_AREA_X, 0,
                        LayoutConfig.GAME_AREA_WIDTH,
                        LayoutConfig.GAME_AREA_HEIGHT);
                uiLayout.render(gb);
                entityManager.render(gb);
                levelClear.render(gb);
            }

            case GAME_OVER         -> gameOver.render(gb);
            case CAMPAIGN_COMPLETE -> campaignComplete.render(gb);
        }

        // ── Brightness overlay ────────────────────────────────────────
        applyBrightness(gb);

        gb.dispose();

        // ── Scale to window ───────────────────────────────────────────
        // Replace the scale + drawImage block at the end of paintComponent():

        gb.dispose();

        Graphics2D g2 = (Graphics2D) g;

        // Update and apply screen shake
        ScreenShake.get().update();
        int shakeX = ScreenShake.get().getOffsetX();
        int shakeY = ScreenShake.get().getOffsetY();

        int sw = getWidth(), sh = getHeight();
        double scale = Math.min(
                sw / (double) LayoutConfig.VIRTUAL_WIDTH,
                sh / (double) LayoutConfig.VIRTUAL_HEIGHT);
        int rw = (int)(LayoutConfig.VIRTUAL_WIDTH  * scale);
        int rh = (int)(LayoutConfig.VIRTUAL_HEIGHT * scale);
        int ox = (sw - rw) / 2 + shakeX;
        int oy = (sh - rh) / 2 + shakeY;

        g2.drawImage(gameBuffer, ox, oy, rw, rh, null);
    }

    // ── Brightness post-pass ──────────────────────────────────────────
    private void applyBrightness(Graphics2D g) {
        float b = SettingsManager.get().brightness;
        if (b == 1.0f) return;   // no-op at neutral

        Composite old = g.getComposite();
        if (b < 1.0f) {
            // darken — black overlay, alpha = how dark
            float alpha = 1.0f - b;   // b=0.5 → alpha=0.5
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
            g.setColor(Color.BLACK);
        } else {
            // lighten — white overlay, alpha = how light
            float alpha = b - 1.0f;   // b=1.5 → alpha=0.5
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, alpha));
            g.setColor(Color.WHITE);
        }
        g.fillRect(0, 0, LayoutConfig.VIRTUAL_WIDTH, LayoutConfig.VIRTUAL_HEIGHT);
        g.setComposite(old);
    }
}