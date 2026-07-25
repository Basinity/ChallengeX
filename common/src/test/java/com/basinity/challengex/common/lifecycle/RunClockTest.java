package com.basinity.challengex.common.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RunClockTest {

    private static final long SECOND = 20L;
    private static final long MINUTE = 60L * SECOND;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;

    @Test
    @DisplayName("a run at zero reads 0s rather than empty")
    void zeroReadsAsSeconds() {
        assertEquals("0s", RunClock.format(0L));
    }

    @Test
    @DisplayName("a negative tick count is clamped rather than formatted as negative")
    void negativeClampsToZero() {
        assertEquals("0s", RunClock.format(-500L));
    }

    @Test
    @DisplayName("part of a second has not ticked over yet")
    void subSecondFloors() {
        assertEquals("0s", RunClock.format(19L));
        assertEquals("1s", RunClock.format(SECOND));
    }

    @Test
    @DisplayName("a whole unit drops the empty smaller ones")
    void wholeUnitsDropEmptyOnes() {
        assertEquals("2m", RunClock.format(2 * MINUTE));
        assertEquals("1h", RunClock.format(HOUR));
        assertEquals("1d", RunClock.format(DAY));
    }

    @Test
    @DisplayName("a partial unit keeps both parts")
    void partialUnitsKeepBoth() {
        assertEquals("2m 4s", RunClock.format(2 * MINUTE + 4 * SECOND));
    }

    @Test
    @DisplayName("leading empty units are dropped but inner zero units are too")
    void innerZeroUnitsAreDropped() {
        assertEquals("1h 5s", RunClock.format(HOUR + 5 * SECOND));
        assertEquals("1d 30m", RunClock.format(DAY + 30 * MINUTE));
    }

    @Test
    @DisplayName("every unit present reads largest to smallest")
    void allUnitsInOrder() {
        assertEquals("1d 2h 3m 4s",
                RunClock.format(DAY + 2 * HOUR + 3 * MINUTE + 4 * SECOND));
    }
}
