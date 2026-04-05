package player;

import config.CombatArea;
import entity.*;
import gfx.FxLayer;
import gfx.ImageSequenceSet;
import gfx.ShipRegistry;
import input.InputManager;
import score.ScoreManager;
import wave.WaveManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;

public class PlayerShip extends Entity {

    // ── Movement ──────────────────────────────────────────────────────
    private double speed = 5;

    // ── Health ────────────────────────────────────────────────────────
    private int maxHealth     = 100;
    private int currentHealth = 100;

    // ── References ────────────────────────────────────────────────────
    private EntityManager entityManager;

    // ── Respawn / invincibility ───────────────────────────────────────
    private boolean respawning      = false;
    private long    respawnStart    = 0;
    private boolean invincible      = false;
    private long    invincibleStart = 0;

    private static final long INVINCIBLE_TIME = 3000;
    private static final long RESPAWN_TIME    = 2000;

    // ── Firing: twin cannons ──────────────────────────────────────────
    // Two bullets fire simultaneously from the left and right cannon tips,
    // slightly angled inward so they converge ~120px ahead of the ship.
    private long lastShotTime = 0;
    private static final long FIRE_DELAY       = 160;   // ms between shots
    private static final double TWIN_CONVERGE  = 0.4;   // inward vx per cannon
    private static final double TWIN_SPEED     = 9.0;

    // ── Firing: charge shot ───────────────────────────────────────────
    // Hold Z/SPACE for CHARGE_TIME ms to release a wide, powerful blast.
    // Visual: muzzle glow that pulses brighter as charge builds.
    private long chargeStart         = 0;
    private boolean charging         = false;
    private static final long CHARGE_TIME   = 550;  // ms to full charge
    private static final int  CHARGE_DMG    = 20;
    private static final double CHARGE_SPEED = 13.0;

    // ── Animation ─────────────────────────────────────────────────────
    private ImageSequenceSet anim;
    private int              shipTier = 1;

    // ── Constructor ───────────────────────────────────────────────────
    public PlayerShip(double x, double y, EntityManager em) {
        super(x, y, 32, 32);
        this.entityManager = em;
        this.anim          = ShipRegistry.buildShipSet(shipTier);
        this.health        = maxHealth;
        this.currentHealth = maxHealth;
    }

    // ── Health accessors ──────────────────────────────────────────────
    public int getCurrentHealth() { return currentHealth; }
    public int getMaxHealth()     { return maxHealth;     }

    // ── Ship tier upgrade ─────────────────────────────────────────────
    public void upgradeTier(int newTier) {
        this.shipTier = Math.max(1, Math.min(newTier, 5));
        String current = anim.getCurrentName();
        this.anim = ShipRegistry.buildShipSet(shipTier);
        anim.play(current);
    }

    public int getShipTier() { return shipTier; }

    // ── Factory ───────────────────────────────────────────────────────
    public static PlayerShip createDefault(EntityManager em) {
        int x = CombatArea.WIDTH / 2 - 16;
        int y = CombatArea.BOTTOM_BOUND - 80;
        PlayerShip p = new PlayerShip(x, y, em);
        em.add(p);
        return p;
    }

    // ── Damage ────────────────────────────────────────────────────────
    // Each hit chips currentHealth by dmg. A life is lost only when
    // health reaches 0 — not on every hit. This lets the player tank
    // a few bullets before dying, making the HP bar meaningful.
    public void takeDamage(int dmg) {
        if (respawning || invincible) return;

        double cx = x + width  / 2.0;
        double cy = y + height / 2.0;
        FxLayer.get().playerHit(cx, cy);

        if (anim.has("hit")) anim.play("hit", "idle");

        currentHealth = Math.max(0, currentHealth - dmg);
        ScoreManager.get().playerDamaged();

        if (currentHealth <= 0) {
            // Life lost — trigger respawn sequence
            respawning   = true;
            respawnStart = System.currentTimeMillis();
            WaveManager.loseLife();
        }
        // else: health chipped but still alive; no respawn
    }

    // ── Update ────────────────────────────────────────────────────────
    @Override
    public void update() {
        long now = System.currentTimeMillis();

        anim.update();

        // ── Respawn sequence ──────────────────────────────────────────
        if (respawning) {
            if (now - respawnStart > RESPAWN_TIME) {
                respawning      = false;
                invincible      = true;
                invincibleStart = now;

                currentHealth = maxHealth;   // full HP on respawn

                x = CombatArea.LEFT_BOUND + CombatArea.WIDTH / 2 - width / 2.0;
                y = CombatArea.BOTTOM_BOUND - 80;

                if (anim.has("idle")) anim.play("idle");
            }
            return;
        }

        // ── Invincibility timer ───────────────────────────────────────
        if (invincible && now - invincibleStart > INVINCIBLE_TIME)
            invincible = false;

        // ── Movement ──────────────────────────────────────────────────
        velocityX = 0;
        velocityY = 0;
        if (InputManager.isKeyPresent(KeyEvent.VK_LEFT))  velocityX = -speed;
        if (InputManager.isKeyPresent(KeyEvent.VK_RIGHT)) velocityX =  speed;
        if (InputManager.isKeyPresent(KeyEvent.VK_UP))    velocityY = -speed;
        if (InputManager.isKeyPresent(KeyEvent.VK_DOWN))  velocityY =  speed;

        x += velocityX;
        y += velocityY;

        if (x < CombatArea.LEFT_BOUND)          x = CombatArea.LEFT_BOUND;
        if (x > CombatArea.RIGHT_BOUND - width)  x = CombatArea.RIGHT_BOUND - width;
        if (y < CombatArea.TOP_BOUND)            y = CombatArea.TOP_BOUND;
        if (y > CombatArea.BOTTOM_BOUND - height) y = CombatArea.BOTTOM_BOUND - height;

        // ── Thrust animation ──────────────────────────────────────────
        boolean moving = (velocityX != 0 || velocityY != 0);
        if (moving && anim.has("thrust") && "idle".equals(anim.getCurrentName()))
            anim.play("thrust", "idle");

        // ── Shooting ─────────────────────────────────────────────────
        boolean fireKey = InputManager.isKeyPresent(KeyEvent.VK_Z)
                || InputManager.isKeyPresent(KeyEvent.VK_SPACE);

        if (fireKey) {
            if (!charging) {
                // Start charge timer on first frame the key is held
                charging    = true;
                chargeStart = now;
            }

            long heldMs = now - chargeStart;

            if (heldMs >= CHARGE_TIME) {
                // ── Charge shot release ───────────────────────────────
                // Only fires if enough time has passed since last shot,
                // so repeated tap-releases don't spray charge shots.
                if (now - lastShotTime > FIRE_DELAY) {
                    lastShotTime = now;
                    charging     = false;
                    chargeStart  = 0;
                    fireChargeShot();
                }
            } else if (now - lastShotTime > FIRE_DELAY) {
                // ── Twin cannon burst ─────────────────────────────────
                // Left cannon: slight rightward convergence
                // Right cannon: slight leftward convergence
                lastShotTime = now;
                fireTwinCannons();
            }
        } else {
            // Key released — cancel charge (no charge-on-release; hold for effect)
            if (charging && now - chargeStart >= CHARGE_TIME
                    && now - lastShotTime > FIRE_DELAY) {
                // Released after full charge — fire
                lastShotTime = now;
                fireChargeShot();
            }
            charging    = false;
            chargeStart = 0;
        }
    }

    // ── Fire helpers ──────────────────────────────────────────────────

    private void fireTwinCannons() {
        double leftX  = x + width * 0.20 - 2;
        double rightX = x + width * 0.80 - 2;
        double muzzleY = y + 2;

        // Left cannon: slight rightward angle toward center
        entityManager.add(ProjectilePool.get(
                leftX, muzzleY,
                TWIN_CONVERGE, -TWIN_SPEED,
                5, 12, 5, true,
                ProjectileEntity.BulletType.PLAYER
        ));
        // Right cannon: slight leftward angle toward center
        entityManager.add(ProjectilePool.get(
                rightX, muzzleY,
                -TWIN_CONVERGE, -TWIN_SPEED,
                5, 12, 5, true,
                ProjectileEntity.BulletType.PLAYER
        ));
    }

    private void fireChargeShot() {
        double cx = x + width / 2.0 - 6;
        entityManager.add(ProjectilePool.get(
                cx, y - 4,
                0, -CHARGE_SPEED,
                12, 20, CHARGE_DMG, true,
                ProjectileEntity.BulletType.PLAYER
        ));
    }

    // ── Render ────────────────────────────────────────────────────────
    @Override
    public void render(Graphics g) {
        // Blink while respawning or invincible
        if ((respawning || invincible) && System.currentTimeMillis() % 300 < 150) return;

        Graphics2D g2 = (Graphics2D) g;

        // Sprite — use if loaded
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g2.drawImage(frame, (int) x, (int) y, width, height, null);
        } else {
            renderFallback(g2);
        }

        // ── Charge glow overlay (drawn on top of sprite or fallback) ──
        if (charging && chargeStart > 0) {
            long now    = System.currentTimeMillis();
            float prog  = Math.min((now - chargeStart) / (float) CHARGE_TIME, 1f);
            float pulse = (float)(0.3 + 0.4 * Math.abs(Math.sin(now * 0.014)));
            float alpha = prog * pulse;

            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            int cx  = (int) x + width  / 2;
            int glowR = (int)(4 + prog * 12);
            // Outer charge ring — fades from cyan to white at full charge
            Color glowCol = prog >= 1f
                    ? new Color(255, 255, 200)
                    : new Color(0, 220, 255);
            g2.setColor(glowCol);
            g2.fillOval(cx - glowR, (int)y - glowR / 2, glowR * 2, glowR * 2);

            // Inner bright core
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, prog * 0.8f));
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - 3, (int)y - 2, 6, 6);

            g2.setComposite(old);
        }

        // ── Invincible shimmer ring ───────────────────────────────────
        if (invincible) {
            long now = System.currentTimeMillis();
            float shimmer = 0.3f + 0.4f * (float) Math.abs(Math.sin(now * 0.01));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shimmer));
            g2.setColor(new Color(0, 255, 255));
            g2.drawOval((int)x - 4, (int)y - 4, width + 8, height + 8);
            g2.setComposite(old);
        }
    }

    private void renderFallback(Graphics2D g2) {
        int cx = (int) x + width / 2;

        // Main body — cyan triangle
        g2.setColor(new Color(0, 220, 255));
        int[] bx = { cx,     (int)x,          (int)x + width };
        int[] by = { (int)y, (int)y + height,  (int)y + height };
        g2.fillPolygon(bx, by, 3);

        // Inner highlight
        g2.setColor(new Color(180, 255, 255));
        int[] hx = { cx,     cx - 4,    cx + 4   };
        int[] hy = { (int)y+4, (int)y+14, (int)y+14 };
        g2.fillPolygon(hx, hy, 3);

        // Cannon tips — two small rectangles at wing positions
        g2.setColor(new Color(0, 180, 220));
        int leftTip  = (int)(x + width * 0.20) - 1;
        int rightTip = (int)(x + width * 0.80) - 1;
        g2.fillRect(leftTip,  (int)y + 2, 3, 6);
        g2.fillRect(rightTip, (int)y + 2, 3, 6);

        // Engine glow when moving
        if (velocityX != 0 || velocityY != 0) {
            g2.setColor(new Color(255, 140, 0, 180));
            g2.fillOval(cx - 4, (int)y + height - 4, 8, 8);
        }
    }
}
