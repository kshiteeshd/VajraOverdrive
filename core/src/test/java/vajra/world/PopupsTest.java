package vajra.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** THE POPUP STACK: lines landing on one spot cascade one pitch down each, so a voice's
 *  same-frame lines and the run's opening pages read in order instead of as one pile of ink. */
class PopupsTest {

    private static Popups.Spec line(String t) {
        return new Popups.Spec().text(t).at(100, 200);
    }

    @Test
    void stackedLinesCascadeAndTheChainResets() {
        Popups p = new Popups();
        p.push(line("ONE"));
        assertEquals(200, p.peek(0).y, 1e-6);
        assertEquals(100, p.peek(0).x, 1e-6);
        p.push(line("TWO"));
        assertEquals(174, p.peek(0).y, 1e-6, "same spot: one pitch lower");
        assertEquals(200, p.peek(1).y, 1e-6, "the first line keeps its spot");
        p.push(new Popups.Spec().text("FAR").at(900, 600)); // a different spot starts fresh
        assertEquals(600, p.peek(0).y, 1e-6);
        p.push(line("BACK")); // the old spot again: no stale chain, the head is where it stood
        assertEquals(200, p.peek(0).y, 1e-6);
    }

    @Test
    void lifeColorsAndScaleRideTheCopy() {
        Popups p = new Popups();
        p.push(new Popups.Spec().text("S").life(3.2f).rise(6)
                .color(0.55f, 0.95f, 1f).scale(1.15f));
        Popups.Spec s = p.peek(0);
        assertEquals(3.2f, s.lifeMax, 1e-6);
        assertEquals(0.55f, s.r, 1e-6);
        assertEquals(1.15f, s.scale, 1e-6);
    }
}
