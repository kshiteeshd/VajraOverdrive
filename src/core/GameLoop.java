package core;

import java.util.concurrent.locks.LockSupport;

public class GameLoop implements Runnable {

    private Thread gameThread;
    private boolean running = false;

    private final double UPDATE_RATE = 60.0;
    private final double UPDATE_INTERVAL = 1000000000.0 / UPDATE_RATE;

    private GameCanvas canvas;

    public GameLoop(GameCanvas canvas) {
        this.canvas = canvas;
    }

    public void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double delta = 0;
        int frames = 0;
        long timer = System.currentTimeMillis();

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / UPDATE_INTERVAL;
            lastTime = now;

            boolean shouldRender = false;

            while (delta >= 1) {
                canvas.update();
                delta--;
                shouldRender = true; // Only render if an update actually happened
            }

            if (shouldRender) {
                canvas.repaint();
                frames++;
            }

            if (System.currentTimeMillis() - timer >= 1000) {
                canvas.setFPS(frames);
                frames = 0;
                timer += 1000;
            }

            // High precision yield instead of Thread.sleep(1)
            // This prevents CPU melting while keeping frame-pacing strictly accurate
            LockSupport.parkNanos(1_000_000); // 1 millisecond park
        }
    }
}