package entity;

import config.CombatArea;
import gfx.ImageSequenceAnimation;
import gfx.ImageSequenceSet;
import gfx.ShipRegistry;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ProjectileEntity extends Entity {

    public enum BulletType {
        PLAYER,     // cyan bolt
        BASIC,      // orange oval
        FAST,       // small orange dart
        TANK,       // large dark red ball
        SNIPER      // thin purple needle
    }

    public enum ProjectileModifier {
        NORMAL,
        PIERCING,
        EXPLOSIVE,
        CHAIN,
        WAVE
    }

    private ProjectileModifier modifier = ProjectileModifier.NORMAL;

    // for special behaviors
    private float waveTime = 0f;
    private float baseX, baseY;

    private int        damage;
    private boolean    fromPlayer;
    private BulletType bulletType;
    private ImageSequenceSet anim;

    // ── Constructors ──────────────────────────────────────────────────

    /** Full constructor with explicit bullet type. */
    public ProjectileEntity(double x, double y,
                            double vx, double vy,
                            int w, int h,
                            int damage,
                            boolean fromPlayer,
                            BulletType type) {
        super(x, y, w, h);
        this.velocityX  = vx;
        this.velocityY  = vy;
        this.damage     = damage;
        this.fromPlayer = fromPlayer;
        this.bulletType = type;
        this.anim       = buildAnim(type);
    }

    /** Backward-compat: fromPlayer=true → PLAYER, false → BASIC. */
    public ProjectileEntity(double x, double y,
                            double vx, double vy,
                            int w, int h,
                            int damage,
                            boolean fromPlayer) {
        this(x, y, vx, vy, w, h, damage, fromPlayer,
                fromPlayer ? BulletType.PLAYER : BulletType.BASIC);
    }

    // ── Anim builder ──────────────────────────────────────────────────
    private ImageSequenceSet buildAnim(BulletType type) {
        String key = switch (type) {
            case PLAYER -> "bullet_player";
            case TANK   -> "bullet_tank";
            case SNIPER -> "bullet_sniper";
            case FAST   -> "bullet_fast";
            default     -> "bullet_enemy";
        };
        // Uses ShipRegistry which falls back gracefully if PNG missing
        ImageSequenceSet set = new ImageSequenceSet();
        set.register("default", new ImageSequenceAnimation(
                "resources/sprites/bullets/", key + "_", "png",
                2, 80, true
        ));
        return set;
    }

    public boolean isFromPlayer() { return fromPlayer; }
    public int     getDamage()    { return damage;     }

    // ── Update ────────────────────────────────────────────────────────
    @Override
    public void update() {
        anim.update();
        x += velocityX;
        y += velocityY;
        if (y < CombatArea.TOP_BOUND - 20
                || y > CombatArea.BOTTOM_BOUND + 20
                || x < CombatArea.LEFT_BOUND  - 20
                || x > CombatArea.RIGHT_BOUND + 20) {
            removable = true;
        }
    }

    // ── Render ────────────────────────────────────────────────────────
    @Override
    public void render(Graphics g) {
        // Sprite first
        BufferedImage frame = anim.getFrame();
        if (frame != null) {
            g.drawImage(frame, (int) x, (int) y, width, height, null);
            return;
        }

        // Fallback shapes per type
        Graphics2D g2 = (Graphics2D) g;
        switch (bulletType) {

            case PLAYER -> {
                // Cyan energy bolt
                g2.setColor(new Color(100, 255, 255, 200));
                g2.fillOval((int)x, (int)y, width, height);
                g2.setColor(Color.WHITE);
                g2.fillOval((int)x + 1, (int)y + 2, width - 2, height - 4);
            }

            case FAST -> {
                // Small bright orange dart
                g2.setColor(new Color(255, 160, 20));
                g2.fillOval((int)x, (int)y, width, height);
                g2.setColor(new Color(255, 220, 80));
                g2.fillOval((int)x + 1, (int)y + 1, width - 2, height - 2);
            }

            case TANK -> {
                // Large dark red ball with inner glow
                g2.setColor(new Color(180, 20, 20));
                g2.fillOval((int)x, (int)y, width, height);
                g2.setColor(new Color(240, 80, 80));
                g2.fillOval((int)x + 2, (int)y + 2, width - 4, height - 4);
                g2.setColor(new Color(255, 140, 140, 160));
                g2.fillOval((int)x + 4, (int)y + 4, width - 8, height - 8);
            }

            case SNIPER -> {
                // Thin tall purple needle
                g2.setColor(new Color(180, 60, 255, 220));
                g2.fillRect((int)x, (int)y, width, height);
                g2.setColor(new Color(230, 160, 255));
                g2.fillRect((int)x + 1, (int)y + 2, width - 2, height - 4);
                // Glow tip
                g2.setColor(new Color(255, 200, 255, 180));
                g2.fillOval((int)x - 1, (int)y, width + 2, 4);
            }

            default -> {
                // Basic enemy bullet — orange/red oval
                g2.setColor(new Color(255, 100, 20));
                g2.fillOval((int)x, (int)y, width, height);
                g2.setColor(new Color(255, 220, 100));
                g2.fillOval((int)x + 1, (int)y + 2, width - 2, height - 4);
            }
        }
    }
    // Add this method to the bottom of the class
    // Add this to the bottom of ProjectileEntity.java
    public void reset(double newX, double newY, double vx, double vy, int w, int h, int newDamage, boolean isFromPlayer, BulletType type) {
        this.x = newX;
        this.y = newY;
        this.velocityX = vx;
        this.velocityY = vy;
        this.width = w;
        this.height = h;
        this.damage = newDamage;
        this.fromPlayer = isFromPlayer;
        this.bulletType = type;
        this.anim = buildAnim(type); // Rebuild the animation for the new bullet type
        this.removable = false;      // Resurrect the bullet!
    }
}