package ui;

import config.LayoutConfig;

import java.awt.*;

public class GameUILayout {

    public void render(Graphics2D g2) {

        drawInfoPanel(g2);
        drawGameArea(g2);
        drawBoundaries(g2);

    }

    private void drawInfoPanel(Graphics2D g2) {

        g2.setColor(Color.DARK_GRAY);

        g2.fillRect(
                0,
                0,
                LayoutConfig.INFO_PANEL_WIDTH,
                LayoutConfig.WINDOW_HEIGHT
        );

    }
    private void drawBoundaries(Graphics2D g2) {

        g2.setColor(Color.WHITE);

        // Outer screen border
        g2.drawRect(
                0,
                0,
                LayoutConfig.WINDOW_WIDTH - 1,
                LayoutConfig.WINDOW_HEIGHT - 1
        );

        // Divider between UI panel and game area
        g2.drawLine(
                LayoutConfig.INFO_PANEL_WIDTH,
                0,
                LayoutConfig.INFO_PANEL_WIDTH,
                LayoutConfig.WINDOW_HEIGHT
        );

    }

    private void drawGameArea(Graphics2D g2) {

        g2.setColor(Color.BLACK);

        g2.fillRect(
                LayoutConfig.GAME_AREA_X,
                0,
                LayoutConfig.GAME_AREA_WIDTH,
                LayoutConfig.WINDOW_HEIGHT
        );

    }

}