package player;

import config.CombatArea;
import entity.*;
import gfx.FxLayer;
import gfx.ImageSequenceSet;
import gfx.ShipRegistry;
import input.InputManager;
import wave.WaveManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.KeyEvent;

public class PlayerShip extends Entity {

    private double speed      = 5;
    private long   lastShotTime = 0;
    private long   fireDelay    = 200;

    private EntityManager entityManager;

    private boolean respawning      = false;
    private long    respawnStart    = 0;
    private boolean invincible      = false;
    private long    invincibleStart = 0;

    private static final long INVINCIBLE_TIME = 3000;
    private static final long RESPAWN_TIME    = 2000;

    // ── Animation — now uses ImageSequenceSet ─────────────────────────
    private ImageSequenceSet anim;
    private int              shipTier = 1;

    public PlayerShip(double x, double y, EntityManager em) {
        super(x, y, 32, 32);
        this.entityManager = em;
        this.anim          = ShipRegistry.buildShipSet(shipTier);
    }

    /** Call when the player upgrades their ship tier (1–5). */
    public void upgradeTier(int newTier) {
        this.shipTier = Math.max(1, Math.min(newTier, 5));
        String currentAnim = anim.getCurrentName();
        this.anim = ShipRegistry.buildShipSet(shipTier);
        // Resume the same animation state on the new set
        anim.play(currentAnim);
    }

    public int getShipTier() { return shipTier; }

    // ── Damage / respawn ──────────────────────────────────────────────
    public void takeDamage(int dmg) {
        if (respawning || invincible) return;

        double cx = x + width  / 2.0;
        double cy = y + height / 2.0;
        FxLayer.get().playerHit(cx, cy);

        if (anim.has("hit")) anim.play("hit", "idle");

        respawning   = true;
        respawnStart = System.currentTimeMillis();
        WaveManager.loseLife();
    }

    public static PlayerShip createDefault(EntityManager em) {
        int x = CombatArea.LEFT_BOUND + CombatArea.WIDTH / 2 - 16;
        int y = CombatArea.BOTTOM_BOUND - 80;
        PlayerShip p = new PlayerShip(x, y, em);
        em.add(p);
        return p;
    }

    // ── Update ────────────────────────────────────────────────────────
    @Override
    public void update() {
        long now = System.currentTimeMillis();

        anim.update();

        // Respawn
        if (respawning) {
            if (now - respawnStart > RESPAWN_TIME) {
                respawning      = false;
                invincible      = true;
                invincibleStart = now;
                x = CombatArea.LEFT_BOUND + CombatArea.WIDTH / 2 - width / 2.0;
                y = CombatArea.BOTTOM_BOUND - 80;
                if (anim.has("idle")) anim.play("idle");
            }
            return;
        }

        if (invincible) {
            if (now - invincibleStart > INVINCIBLE_TIME) invincible = false;
        }

        // Movement
        velocityX = 0;
        velocityY = 0;
        if (InputManager.isKeyPresent(KeyEvent.VK_LEFT))  velocityX = -speed;
        if (InputManager.isKeyPresent(KeyEvent.VK_RIGHT)) velocityX =  speed;
        if (InputManager.isKeyPresent(KeyEvent.VK_UP))    velocityY = -speed;
        if (InputManager.isKeyPresent(KeyEvent.VK_DOWN))  velocityY =  speed;

        x += velocityX;
        y += velocityY;

        // Clamp to combat area
        if (x < CombatArea.LEFT_BOUND)              x = CombatArea.LEFT_BOUND;
        if (x > CombatArea.RIGHT_BOUND - width)     x = CombatArea.RIGHT_BOUND - width;
        if (y < CombatArea.TOP_BOUND)               y = CombatArea.TOP_BOUND;
        if (y > CombatArea.BOTTOM_BOUND - height)   y = CombatArea.BOTTOM_BOUND - height;

        // Thrust animation
        boolean moving = (velocityX != 0 || velocityY != 0);
        if (moving
                && anim.has("thrust")
                && "idle".equals(anim.getCurrentName()))
            anim.play("thrust", "idle");

        // Shooting
        if (InputManager.isKeyPresent(KeyEvent.VK_Z) ||
                InputManager.isKeyPresent(KeyEvent.VK_SPACE)) {
            if (now - lastShotTime > fireDelay) {
                lastShotTime = now;
                entityManager.add(new ProjectileEntity(
                        x + width / 2.0 - 3, y,
                        0, -8, 6, 12, 5, true
                ));
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    @Override
    public void render(Graphics g) {
        // Blink while respawning or invincible
        if ((respawning || invincible)
                && System.currentTimeMillis() % 300 < 150) return;

        // Sprite
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g.drawImage(frame, (int) x, (int) y, width, height, null);
            return;
        }

        // Fallback: cyan triangle
        Graphics2D g2 = (Graphics2D) g;
        int cx = (int) x + width / 2;

        g2.setColor(new Color(0, 220, 255));
        int[] bx = { cx,         (int)x,          (int)x + width };
        int[] by = { (int)y,     (int)y + height,  (int)y + height };
        g2.fillPolygon(bx, by, 3);

        g2.setColor(new Color(180, 255, 255));
        int[] cx2 = { cx,      cx - 4,      cx + 4      };
        int[] cy2 = { (int)y+4, (int)y+14,   (int)y+14  };
        g2.fillPolygon(cx2, cy2, 3);

        if (velocityY != 0 || velocityX != 0) {
            g2.setColor(new Color(255, 140, 0, 180));
            g2.fillOval(cx - 4, (int)y + height - 4, 8, 8);
        }
    }
}
