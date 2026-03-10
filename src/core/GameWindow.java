package core;

import config.LayoutConfig;

import javax.swing.JFrame;

public class GameWindow {

    private JFrame frame;

    public GameWindow() {

        frame = new JFrame("Vajra Overdrive");
        GameCanvas canvas = new GameCanvas();
        frame.add(canvas);

        frame.setSize(LayoutConfig.WINDOW_WIDTH, LayoutConfig.WINDOW_HEIGHT);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        frame.setVisible(true);

        GameLoop loop = new GameLoop(canvas);
        loop.start();
    }
}