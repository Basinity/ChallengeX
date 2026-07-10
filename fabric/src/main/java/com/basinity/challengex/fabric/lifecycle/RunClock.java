package com.basinity.challengex.fabric.lifecycle;

/**
 * Formats a run clock's tick count in compact unit form: the largest non-zero
 * unit down to the smallest, each with its letter (d/h/m/s), leading and empty
 * units dropped. Two whole minutes reads {@code 2m}, not {@code 02m 00s};
 * {@code 2m 4s} keeps both. A run at zero reads {@code 0s}.
 */
public final class RunClock {

    private static final long TICKS_PER_SECOND = 20L;
    private static final long SECONDS_PER_DAY = 86400L;
    private static final long SECONDS_PER_HOUR = 3600L;
    private static final long SECONDS_PER_MINUTE = 60L;

    private RunClock() {
    }

    public static String format(long ticks) {
        long totalSeconds = Math.max(0L, ticks) / TICKS_PER_SECOND;
        long days = totalSeconds / SECONDS_PER_DAY;
        long hours = (totalSeconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
        long minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE;
        long seconds = totalSeconds % SECONDS_PER_MINUTE;

        StringBuilder out = new StringBuilder();
        appendUnit(out, days, 'd');
        appendUnit(out, hours, 'h');
        appendUnit(out, minutes, 'm');
        appendUnit(out, seconds, 's');
        return out.isEmpty() ? "0s" : out.toString();
    }

    private static void appendUnit(StringBuilder out, long value, char unit) {
        if (value == 0L) {
            return;
        }
        if (!out.isEmpty()) {
            out.append(' ');
        }
        out.append(value).append(unit);
    }
}
