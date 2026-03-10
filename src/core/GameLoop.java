package core;

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
        int updates = 0;
        long timer = System.currentTimeMillis();

        while (running) {

            long now = System.nanoTime();
            delta += (now - lastTime) / UPDATE_INTERVAL;
            lastTime = now;

            while (delta >= 1) {
                update();
                updates++;
                delta--;
            }

            render();
            frames++;

            // FPS / UPS counter
            if (System.currentTimeMillis() - timer >= 1000) {
                canvas.setFPS(frames);

                System.out.println("FPS: " + frames + " | UPS: " + updates);

                frames = 0;
                updates = 0;
                timer += 1000;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {

        // Game logic updates
        canvas.update();
    }

    private void render() {

        canvas.repaint();

    }
}