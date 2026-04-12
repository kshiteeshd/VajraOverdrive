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

    // ── Subsystems ────────────────────────────────────────────────────
    private final WeaponSystem weaponSystem;
    private final ShieldSystem shieldSystem;

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
        this.weaponSystem  = new WeaponSystem();
        this.shieldSystem  = new ShieldSystem();
    }

    // ── Health / shield accessors (HUD wiring) ────────────────────────
    public int     getCurrentHealth()  { return currentHealth;            }
    public int     getMaxHealth()      { return maxHealth;                }
    public float   getShieldCharge()   { return shieldSystem.getCharge(); }
    public boolean isShieldActive()    { return shieldSystem.isActive();  }
    public float   getWeaponHeat()     { return weaponSystem.getHeat();   }
    public boolean isOverheated()      { return weaponSystem.isOverheated(); }

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
    // Shield intercept first — absorbed hits skip HP deduction entirely.
    // When health reaches zero a life is lost and respawn triggers.
    // Individual hits only chip health; the player survives multiple hits.
    public void takeDamage(int dmg) {
        if (respawning || invincible) return;
        if (shieldSystem.absorbDamage(dmg)) return;   // shield ate it — no SFX

        audio.SoundManager.get().play("sfx_player_hit");

        currentHealth -= dmg;
        if (currentHealth <= 0) {
            currentHealth = 0;
            respawning    = true;
            respawnStart  = System.currentTimeMillis();
            WaveManager.loseLife();
        }
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
                currentHealth   = maxHealth;   // full HP on respawn

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

        if (x < CombatArea.LEFT_BOUND)           x = CombatArea.LEFT_BOUND;
        if (x > CombatArea.RIGHT_BOUND - width)   x = CombatArea.RIGHT_BOUND - width;
        if (y < CombatArea.TOP_BOUND)             y = CombatArea.TOP_BOUND;
        if (y > CombatArea.BOTTOM_BOUND - height) y = CombatArea.BOTTOM_BOUND - height;

        // ── Thrust animation ──────────────────────────────────────────
        boolean moving = (velocityX != 0 || velocityY != 0);
        if (moving && anim.has("thrust") && "idle".equals(anim.getCurrentName()))
            anim.play("thrust", "idle");

        // ── Subsystem updates ─────────────────────────────────────────
        weaponSystem.update();
        boolean shieldKey = InputManager.isKeyPresent(KeyEvent.VK_X);
        shieldSystem.update(shieldKey);

        // ── Fire — single centered cannon, gated by WeaponSystem ──────
        boolean fireKey = InputManager.isKeyPresent(KeyEvent.VK_Z)
                || InputManager.isKeyPresent(KeyEvent.VK_SPACE);
        if (fireKey && weaponSystem.canFire(now)) {
            weaponSystem.fire(x, y, width, entityManager);
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    @Override
    public void render(Graphics g) {
        // Blink while respawning or invincible
        if ((respawning || invincible) && System.currentTimeMillis() % 300 < 150) return;

        Graphics2D g2 = (Graphics2D) g;

        // Ship sprite or fallback shape
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g2.drawImage(frame, (int) x, (int) y, width, height, null);
        } else {
            renderFallback(g2);
        }

        // System overlays drawn on a fresh Graphics2D so composite state
        // is isolated and always restored cleanly
        Graphics2D overlay = (Graphics2D) g2.create();
        shieldSystem.renderOverlay(overlay, x, y, width, height);
        weaponSystem.renderOverlay(overlay, x, y, width, height);
        overlay.dispose();

        // Invincibility shimmer ring
        if (invincible) {
            long  now    = System.currentTimeMillis();
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
        int[] hx = { cx,       cx - 4,      cx + 4    };
        int[] hy = { (int)y+4, (int)y+14,   (int)y+14 };
        g2.fillPolygon(hx, hy, 3);

        // Single center cannon tip
        g2.setColor(new Color(0, 180, 220));
        g2.fillRect(cx - 1, (int)y, 3, 6);

        // Engine glow when moving
        if (velocityX != 0 || velocityY != 0) {
            g2.setColor(new Color(255, 140, 0, 180));
            g2.fillOval(cx - 4, (int)y + height - 4, 8, 8);
        }
    }
}
