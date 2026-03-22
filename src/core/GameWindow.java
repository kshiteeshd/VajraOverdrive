package core;

import config.LayoutConfig;
import save.SettingsManager;
import save.SettingsProfile;

import javax.swing.*;
import java.awt.*;
import ui.theme.UIFonts;

public class GameWindow {

    private JFrame frame;
    private GameCanvas canvas;
    private boolean fullscreen = false;
    private GraphicsDevice device;

    public GameWindow() {
        UIFonts.load();

        device = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();

        // Apply saved resolution before creating window
        applyResolutionToConfig();

        frame  = new JFrame("Vajra Overdrive");
        canvas = new GameCanvas(this);

        frame.setLayout(new BorderLayout());
        frame.add(canvas, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        canvas.requestFocus();

        // Check if saved setting is fullscreen
        SettingsProfile s = SettingsManager.get();
        if (s.resolutionIdx == 3) {
            fullscreen = false;   // toggleFullscreen will flip it
            toggleFullscreen();
        }

        GameLoop loop = new GameLoop(canvas);
        loop.start();
    }

    // ── Resolution ────────────────────────────────────────────────────
    /**
     * Applies the saved resolution index to LayoutConfig at startup.
     * Index 3 = fullscreen, handled separately via toggleFullscreen().
     */
    private void applyResolutionToConfig() {
        int idx = SettingsManager.get().resolutionIdx;
        switch (idx) {
            case 1 -> LayoutConfig.setResolution(1280, 768);
            case 2 -> LayoutConfig.setResolution(1920, 1080);
            default -> LayoutConfig.setResolution(
                    LayoutConfig.DEFAULT_WIDTH,
                    LayoutConfig.DEFAULT_HEIGHT);
        }
    }

    // ── Fullscreen toggle ─────────────────────────────────────────────
    public void toggleFullscreen() {
        fullscreen = !fullscreen;
        frame.dispose();

        if (fullscreen) {
            frame.setUndecorated(true);
            device.setFullScreenWindow(frame);
        } else {
            device.setFullScreenWindow(null);
            frame.setUndecorated(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
        }

        frame.setVisible(true);
        canvas.requestFocus();
    }

    public boolean isFullscreen() { return fullscreen; }
}