package vajra.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The layered walls of the test area: each layer behind is smaller, darker and a different slice of the world. */
class LayerStackTest {
    @Test
    void everyLayerBehindIsSmallerThanTheOneInFront() {
        assertEquals(1f, LayerStack.scale(0, 0.8f), 1e-6f, "the play layer is full size");
        for (int k = 1; k <= 10; k++) assertTrue(LayerStack.scale(k, 0.8f) < LayerStack.scale(k - 1, 0.8f), "layer " + k);
        assertEquals(0.64f, LayerStack.scale(2, 0.8f), 1e-5f);
        assertTrue(LayerStack.scale(4, 0.6f) < LayerStack.scale(4, 0.9f), "a stronger shrink makes the far layers tinier");
    }

    @Test
    void theFurtherBackTheDarker() {
        float[] a = new float[3], b = new float[3];
        LayerStack.tint(0, 6, a);
        assertEquals(1f, a[0], 1e-6f);
        float last = 3.01f;
        for (int k = 0; k <= 6; k++) {
            LayerStack.tint(k, 6, b);
            assertTrue(b[0] + b[1] + b[2] < last, "layer " + k + " is darker than the one before");
            last = b[0] + b[1] + b[2];
        }
        assertTrue(b[2] > b[0], "the haze is blue");
    }

    @Test
    void eachLayerPeeksOutDownAndLeftOfTheOneInFront() {
        assertEquals(0f, LayerStack.shiftX(0, 14f), 1e-6f);
        assertEquals(0f, LayerStack.shiftY(0, 14f), 1e-6f);
        for (int k = 1; k <= 10; k++) {
            assertTrue(LayerStack.shiftX(k, 14f) < LayerStack.shiftX(k - 1, 14f), "left of the layer in front: " + k);
            assertTrue(LayerStack.shiftY(k, 14f) < LayerStack.shiftY(k - 1, 14f), "and below it: " + k);
        }
        assertEquals(0f, LayerStack.shiftX(5, 0f), 1e-6f, "no peek, the stack is square on");
    }

    @Test
    void aFarLayerSlidesByAFractionOfTheCamerasMove() {
        // the walls shrink about the screen centre by shrink^k: a camera move of d slides layer k by d * scale
        float d = 100f;
        assertEquals(80f, d * LayerStack.scale(1, 0.8f), 1e-4f);
        assertTrue(d * LayerStack.scale(5, 0.8f) < d * LayerStack.scale(1, 0.8f));
    }

    @Test
    void theFarLayersDriftMostWhenTheVanishingPointLeans() {
        assertEquals(0f, LayerStack.leanShift(0f, 0.5f), 1e-6f, "no lean, no drift");
        assertEquals(0f, LayerStack.leanShift(50f, 1f), 1e-6f, "the play layer never moves with the lean");
        assertTrue(LayerStack.leanShift(50f, 0.5f) > LayerStack.leanShift(50f, 0.9f), "the further back, the more it drifts");
        assertTrue(LayerStack.leanShift(-50f, 0.5f) < 0f, "and it drifts the way the hull leans");
        assertTrue(LayerStack.MAX_LEAN > 0f && LayerStack.MAX_LEAN <= 100f);
    }

    @Test
    void theFarFloorSlidesAtTheSpeedOfTheLayerJustInFrontOfIt() {
        float fly = 150f, shrink = 0.98f;
        for (float layers : new float[] {4f, 12f, 15f, 20f}) {
            float speed = LayerStack.floorSpeed(layers, shrink, Altitude.ground(430f), 1f);
            assertEquals((float) Math.pow(shrink, layers + 0.5f), speed, 1e-5f, "a half layer in front of the last wall layer");
            assertTrue(speed >= LayerStack.scale((int) layers + 1, shrink) - 1e-5f, "never slower than the last layer");
            // what the player sees: the shader's own ground scale divided by the camera's zoom multiplier
            float seen = Altitude.ground(fly) / LayerStack.floorZoomMul(fly, speed);
            assertEquals(speed, seen, 1e-5f, "the floor really moves at that speed on screen");
        }
        assertEquals(0.5f, LayerStack.floorSpeed(15f, shrink, 0.5f, 0f), 1e-6f, "on the open meadow it keeps its own ground speed");
    }

    @Test
    void theLeanIsCalm() {
        assertTrue(LayerStack.MAX_LEAN <= 30f, "the layers must not swing: " + LayerStack.MAX_LEAN);
    }
}
