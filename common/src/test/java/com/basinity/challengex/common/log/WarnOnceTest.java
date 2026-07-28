package com.basinity.challengex.common.log;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.SubstituteLogger;

class WarnOnceTest {

    /** Counts what reached the logger, which is the whole point of the guard. */
    private static final class Counting extends SubstituteLogger {
        private final List<String> lines = new ArrayList<>();

        Counting() {
            super("counting", null, true);
        }

        @Override
        public void warn(String format, Object... arguments) {
            lines.add(format);
        }

        @Override
        public void warn(String message) {
            lines.add(message);
        }
    }

    @Test
    @DisplayName("the same subject is reported once, however often it comes round")
    void repeatsAreSwallowed() {
        Counting logger = new Counting();
        WarnOnce warned = new WarnOnce();

        for (int tick = 0; tick < 100; tick++) {
            warned.warn(logger, "minecraft:test", "Unknown time value {}.", "minecraft:test");
        }

        assertEquals(1, logger.lines.size());
    }

    @Test
    @DisplayName("a different subject still gets its own warning")
    void distinctSubjectsEachWarn() {
        Counting logger = new Counting();
        WarnOnce warned = new WarnOnce();

        warned.warn(logger, "first", "Unknown value {}.", "first");
        warned.warn(logger, "second", "Unknown value {}.", "second");
        warned.warn(logger, "first", "Unknown value {}.", "first");

        assertEquals(2, logger.lines.size());
    }

    @Test
    @DisplayName("two guards do not share a memory, so one source cannot silence another")
    void guardsAreIndependent() {
        Counting logger = new Counting();

        new WarnOnce().warn(logger, "same", "Unknown value {}.", "same");
        new WarnOnce().warn(logger, "same", "Unknown value {}.", "same");

        assertEquals(2, logger.lines.size());
    }
}
