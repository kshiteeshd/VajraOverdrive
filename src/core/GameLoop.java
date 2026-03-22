
package core;

/**
 * Fixed timestep game loop.
 */
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

            while (delta >= 1) {
                canvas.update();
                delta--;
            }

            canvas.repaint();
            frames++;

            if (System.currentTimeMillis() - timer >= 1000) {
                canvas.setFPS(frames);
                frames = 0;
                timer += 1000;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException ignored) {}
        }
    }
}
