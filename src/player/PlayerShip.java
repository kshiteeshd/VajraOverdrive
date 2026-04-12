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

/**
 * Player-controlled ship.
 *
 * ── Changes in this version ───────────────────────────────────────────
 *
 * FIRING:
 *   weaponSystem.update() now receives the fireHeld boolean each frame
 *   so the tempo model knows whether the trigger is down.
 *   Old call was update() with no args — that was a stub that couldn't
 *   distinguish hold from release.
 *
 * ANIMATION:
 *   Fixed idle/thrust stuck bug. Previously the code only handled the
 *   idle→thrust transition, never the thrust→idle transition, so the
 *   ship stayed on the thrust animation forever once it moved.
 *   Both directions are now handled explicitly.
 *
 * HIT FLASH:
 *   Added hitFlashFrames counter (mirrors BossEntity pattern).
 *   takeDamage() sets it. update() decrements it before the early
 *   respawn return so it keeps ticking even during respawn.
 *   render() draws a red overlay while it's active.
 *   Also triggers the hit animation state if sprites are loaded.
 *
 * SOUND:
 *   SoundManager call is present as a TODO comment. The class doesn't
 *   exist yet — commented out to prevent a runtime crash. Wire it back
 *   once SoundManager is implemented.
 */
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

    // ── Hit flash ─────────────────────────────────────────────────────
    private int hitFlashFrames = 0;
    private static final int HIT_FLASH_DURATION = 6;  // frames

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
    public float   getWeaponHeat()     { return weaponSystem.getTempo();  }
    public boolean isOverheated()      { return weaponSystem.isHot();     }

    // ── Weapon mode access (for upgrade cards) ────────────────────────
    public WeaponSystem getWeaponSystem() { return weaponSystem; }

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
        int x = CombatArea.LEFT_BOUND + CombatArea.WIDTH / 2 - 16;
        int y = CombatArea.BOTTOM_BOUND - 80;
        PlayerShip p = new PlayerShip(x, y, em);
        em.add(p);
        return p;
    }

    // ── Damage ────────────────────────────────────────────────────────
    /**
     * Shield absorbs first. If shield ate it, no HP loss, no flash.
     * When health reaches zero a life is lost and respawn triggers.
     * Individual hits chip health — player survives multiple hits.
     */
    public void takeDamage(int dmg) {
        if (respawning || invincible) return;
        if (shieldSystem.absorbDamage(dmg)) return;

        // TODO: SoundManager.get().play("sfx_player_hit");

        // Trigger hit flash and hit animation
        hitFlashFrames = HIT_FLASH_DURATION;
        if (anim.has("hit")) anim.play("hit", "idle");

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

        // Hit flash always ticks — even during respawn, so it doesn't
        // freeze if damage lands on the same frame as death.
        if (hitFlashFrames > 0) hitFlashFrames--;

        // ── Respawn sequence ──────────────────────────────────────────
        if (respawning) {
            if (now - respawnStart > RESPAWN_TIME) {
                respawning      = false;
                invincible      = true;
                invincibleStart = now;
                currentHealth   = maxHealth;

                x = CombatArea.LEFT_BOUND + CombatArea.WIDTH / 2.0 - width / 2.0;
                y = CombatArea.BOTTOM_BOUND - 80;

                if (anim.has("idle")) anim.play("idle");
            }
            // Pass fireHeld=false while respawning so tempo decays
            weaponSystem.update(false);
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

        // ── Thrust animation — FIXED: handles both directions ─────────
        boolean moving = (velocityX != 0 || velocityY != 0);
        if (moving) {
            // Started moving — switch idle → thrust
            if (anim.has("thrust") && "idle".equals(anim.getCurrentName()))
                anim.play("thrust", "idle");
        } else {
            // Stopped — switch thrust → idle
            if (anim.has("idle") && "thrust".equals(anim.getCurrentName()))
                anim.play("idle");
        }

        // ── Fire key ──────────────────────────────────────────────────
        boolean fireKey = InputManager.isKeyPresent(KeyEvent.VK_Z)
                || InputManager.isKeyPresent(KeyEvent.VK_SPACE);

        // ── Subsystem updates ─────────────────────────────────────────
        // weaponSystem.update() receives the held state for tempo tracking
        weaponSystem.update(fireKey);

        boolean shieldKey = InputManager.isKeyPresent(KeyEvent.VK_X);
        shieldSystem.update(shieldKey);

        // ── Fire ──────────────────────────────────────────────────────
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

        // ── Ship sprite or fallback shape ─────────────────────────────
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g2.drawImage(frame, (int) x, (int) y, width, height, null);
        } else {
            renderFallback(g2);
        }

        // ── Hit flash — red overlay on damage ─────────────────────────
        if (hitFlashFrames > 0) {
            Composite old = g2.getComposite();
            // Alpha fades as frames tick down: max at first frame, gone at 0
            float alpha = hitFlashFrames * (0.55f / HIT_FLASH_DURATION);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(new Color(255, 40, 40));
            g2.fillRect((int) x, (int) y, width, height);
            g2.setComposite(old);
        }

        // ── System overlays — isolated composite state ────────────────
        Graphics2D overlay = (Graphics2D) g2.create();
        shieldSystem.renderOverlay(overlay, x, y, width, height);
        weaponSystem.renderOverlay(overlay, x, y, width, height);
        overlay.dispose();

        // ── Invincibility shimmer ring ────────────────────────────────
        if (invincible) {
            long  now    = System.currentTimeMillis();
            float shimmer = 0.3f + 0.4f * (float) Math.abs(Math.sin(now * 0.01));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shimmer));
            g2.setColor(new Color(0, 255, 255));
            g2.drawOval((int) x - 4, (int) y - 4, width + 8, height + 8);
            g2.setComposite(old);
        }
    }

    // ── Fallback shape — drawn when no sprite is loaded ───────────────
    private void renderFallback(Graphics2D g2) {
        int cx = (int) x + width / 2;

        // Main body — cyan triangle
        g2.setColor(new Color(0, 220, 255));
        int[] bx = { cx,     (int) x,         (int) x + width };
        int[] by = { (int) y, (int) y + height, (int) y + height };
        g2.fillPolygon(bx, by, 3);

        // Inner highlight
        g2.setColor(new Color(180, 255, 255));
        int[] hx = { cx,       cx - 4,      cx + 4    };
        int[] hy = { (int) y + 4, (int) y + 14, (int) y + 14 };
        g2.fillPolygon(hx, hy, 3);

        // Single center cannon tip
        g2.setColor(new Color(0, 180, 220));
        g2.fillRect(cx - 1, (int) y, 3, 6);

        // Engine glow when moving
        if (velocityX != 0 || velocityY != 0) {
            g2.setColor(new Color(255, 140, 0, 180));
            g2.fillOval(cx - 4, (int) y + height - 4, 8, 8);
        }
    }
}