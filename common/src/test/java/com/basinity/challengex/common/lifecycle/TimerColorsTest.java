package com.basinity.challengex.common.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimerColorsTest {

    @Test
    @DisplayName("the default color is one of the named ramps")
    void defaultIsNamed() {
        assertTrue(TimerColors.has(TimerColors.DEFAULT));
    }

    @Test
    @DisplayName("an unknown name falls back to the default ramp rather than failing")
    void unknownNameFallsBack() {
        assertSame(TimerColors.ramp(TimerColors.DEFAULT), TimerColors.ramp("not_a_color"));
        assertFalse(TimerColors.has("not_a_color"));
    }

    @Test
    @DisplayName("the fifteen standard colors and rainbow are offered, in menu order")
    void namesAreOrdered() {
        List<String> names = List.copyOf(TimerColors.names());
        assertEquals(16, names.size());
        assertEquals("dark_blue", names.getFirst());
        assertEquals("rainbow", names.getLast());
    }

    @Test
    @DisplayName("a sampled color is a packed RGB value inside 24 bits")
    void samplesArePackedRgb() {
        int[] ramp = TimerColors.ramp("red");
        for (int tick = 0; tick < 40; tick++) {
            int color = TimerColors.colorAt(ramp, tick % 8, tick);
            assertTrue(color >= 0 && color <= 0xFFFFFF, "out of range: " + color);
        }
    }

    @Test
    @DisplayName("the ramp is cyclic, so scrolling a whole period returns the same color")
    void scrollingAFullPeriodWraps() {
        int[] ramp = TimerColors.ramp("rainbow");
        // TICKS_PER_STOP * ramp.length is one full band; the phase is periodic in it.
        int period = (int) (40.0 * ramp.length);
        assertEquals(TimerColors.colorAt(ramp, 3, 0), TimerColors.colorAt(ramp, 3, period));
    }

    @Test
    @DisplayName("a two-stop ramp stays between its two shades")
    void twoStopRampStaysInRange() {
        int[] ramp = TimerColors.ramp("white");
        assertEquals(2, ramp.length);
        for (int tick = 0; tick < 80; tick++) {
            int red = (TimerColors.colorAt(ramp, 0, tick) >> 16) & 0xFF;
            assertTrue(red >= 0xD8 && red <= 0xFF, "red channel drifted: " + red);
        }
    }
}
