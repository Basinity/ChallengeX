package com.basinity.challengex.common.log;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * Warns once per distinct subject, for the sources and enforcers that run every
 * tick.
 *
 * <p>A bad id in a preset is a standing condition rather than something that
 * happens: the poll that notices it runs twenty times a second and will keep
 * noticing it for as long as the challenge is loaded. Warning on every pass
 * fills the log at that rate and buries everything else in it, while saying
 * nothing the first line did not.
 *
 * <p>The subject is the offending value, so a different bad value still gets its
 * own warning. Nothing forgets: one line per bad value per server run is the
 * point.
 */
public final class WarnOnce {

    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    /** Logs the message the first time this subject is seen, and never again. */
    public void warn(Logger logger, String subject, String message, Object... args) {
        if (warned.add(subject)) {
            logger.warn(message, args);
        }
    }
}
