package vajra.world;

import org.junit.jupiter.api.Test;
import vajra.ship.ShipControls;

/** SCRATCH: a long headless soak with a random pilot, reporting sizes and costs. */
class SoakScratchTest {
    @Test
    void soak() {
        World w = new World(0, World.CAMPAIGN);
        w.cues.clear();
        java.util.Random r = new java.util.Random(5);
        float[] ys = {300, 2500, 5000, 7500, 10000, 12500, 15000, 17500, 19000, Hataki.Y0 + 800, Hataki.Y0 + 5000, Hataki.Y0 + 12000, Hataki.Y0 + 20000};
        int maxFx = 0, maxB = 0, maxP = 0, maxE = 0, maxPop = 0, nan = 0, deaths = 0;
        long t0 = System.nanoTime();
        int frames = 0;
        for (float y : ys) {
            w.ship.x = 2600; w.ship.y = y;
            w.hangar.shards[Astra.AGNI] = 50; w.hangar.shards[Astra.AKASHA] = 50; w.hangar.shards[Astra.PRITHVI] = 50; w.hangar.shards[Astra.VAYU] = 50;
            for (int i = 0; i < 60 * 25; i++) {
                final float tx = r.nextFloat() * 2 - 1, ty = r.nextFloat() * 2 - 1;
                final boolean fire = r.nextInt(3) > 0;
                final int step = i;
                w.update(1 / 60f, new ShipControls() {
                    public float thrustY() { return ty; }
                    public float thrustX() { return tx; }
                    public float rotate() { return 0.3f; }
                    public boolean fire() { return fire; }
                    public boolean boost() { return step % 200 < 40; }
                    public boolean shield() { return step % 500 < 80; }
                    public boolean mouseAim() { return false; }
                    public float aimX() { return 0; }
                    public float aimY() { return 0; }
                });
                w.updateAstra(1 / 60f);
                if (i % 400 == 100) { w.astraSel = (w.astraSel + 1) % 5; w.astraBegin(); }
                if (i % 400 == 300) w.astraLoose();
                frames++;
                maxFx = Math.max(maxFx, w.effects.size()); maxB = Math.max(maxB, w.bullets.size()); maxP = Math.max(maxP, w.pickups.size());
                maxE = Math.max(maxE, w.enemies.size()); maxPop = Math.max(maxPop, w.popups.activeCount());
                if (Float.isNaN(w.ship.x) || Float.isNaN(w.ship.y) || Float.isNaN(w.ship.hp)) nan++;
                if (w.death.active && i % 50 == 0) deaths++;
                w.cues.clear();
            }
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("AUDIT soak frames=" + frames + " ms=" + ms + " avg=" + (ms / (double) frames) + "ms/frame maxEffects=" + maxFx + " maxBullets=" + maxB + " maxPickups=" + maxP + " maxEnemies=" + maxE + " maxPopups=" + maxPop + " NaN=" + nan + " lives=" + w.lives + " deathSamples=" + deaths + " coins=" + w.hangar.coins + " medkits=" + w.medkits);
    }
}
