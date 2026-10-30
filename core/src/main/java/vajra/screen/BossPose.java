package vajra.screen;

import vajra.enemy.BhairavaBrain;
import vajra.enemy.Enemy;

/** HOW BHAIRAVA MOVES (pure numbers, no GL): the transform his sprite wears this frame. He breathes and sways at rest, leans into his flight, rears back before a blow, stretches in a charge,
 *  pops and shakes in a roar, raises himself to cast, and reels in a stagger; a heavy hit makes him flinch. The phase sets the heat of his aura. */
final class BossPose {
    private BossPose() {}

    /** {offsetX, offsetY, scaleX, scaleY, rotationDegrees, auraR, auraG, auraB, auraAlpha, shake} */
    static float[] of(Enemy e, float t) {
        float ox = 0f, oy = 0f, sx = 1f, sy = 1f, rot = 0f, shake = 0f;
        float breath = (float) Math.sin(t * 1.6f + e.phase), sway = (float) Math.sin(t * 0.9f + e.phase * 2f);
        sx += 0.022f * breath;
        sy += 0.022f * breath;
        rot += 2.6f * sway;
        oy += 5f * (float) Math.sin(t * 1.3f + e.phase);
        rot += Math.max(-9f, Math.min(9f, -e.vx * 0.022f));                    // leans into where he is going
        float heat = Math.min(1f, e.bossPhase / 4f);
        float auraA = 0.20f + 0.18f * heat + 0.05f * breath;
        switch (e.pose) {
            case BhairavaBrain.WINDUP: {                                         // reared back: squashed, drawn away from the blow
                float k = Math.min(1f, e.poseT / 0.5f);
                sx += 0.10f * k;
                sy -= 0.12f * k;
                oy -= 7f * k;
                shake = 1.2f;
                auraA += 0.15f * k;
                break;
            }
            case BhairavaBrain.LUNGE:                                            // stretched along the run
                sx -= 0.10f;
                sy += 0.18f;
                auraA += 0.25f;
                float sp = (float) Math.hypot(e.vx, e.vy);
                if (sp > 30f) rot = (float) Math.toDegrees(Math.atan2(e.vy, e.vx)) - 90f;
                break;
            case BhairavaBrain.ROAR: {                                           // a pop and a shudder
                float pulse = (float) Math.sin(t * 34f);
                sx += 0.09f + 0.03f * pulse;
                sy += 0.09f + 0.03f * pulse;
                shake = 3.2f;
                auraA += 0.35f;
                break;
            }
            case BhairavaBrain.CAST:                                             // lifted, the eye wide
                sy += 0.05f;
                oy += 8f;
                rot += 3f * (float) Math.sin(t * 5f);
                auraA += 0.25f;
                break;
            case BhairavaBrain.STAGGER: {                                        // reeling, dropped
                float k = Math.max(0f, Math.min(1f, e.poseT / 0.8f));
                rot += 16f * (float) Math.sin(t * 7f) * (0.4f + 0.6f * k);
                oy -= 12f;
                sx += 0.05f;
                sy -= 0.07f;
                shake = 1.8f;
                break;
            }
            default:
                break;
        }
        if (e.flinchT > 0f) {                                                    // a heavy blow: knocked back an instant
            float k = e.flinchT / 0.3f;
            sx -= 0.07f * k;
            sy += 0.04f * k;
            rot += 5f * k * (e.vx >= 0 ? -1f : 1f);
            shake += 1.5f * k;
        }
        if (shake > 0f) {
            ox += shake * (float) Math.sin(t * 91f + e.x);
            oy += shake * (float) Math.cos(t * 77f + e.y);
        }
        float[] aura = e.bossPhase <= 1 ? new float[] {1f, 0.78f, 0.25f} : e.bossPhase == 2 ? new float[] {1f, 0.5f, 0.14f}
                : e.bossPhase == 3 ? new float[] {1f, 0.3f, 0.12f} : new float[] {1f, 0.92f, 0.7f};
        return new float[] {ox, oy, sx, sy, rot, aura[0], aura[1], aura[2], auraA, shake};
    }
}
