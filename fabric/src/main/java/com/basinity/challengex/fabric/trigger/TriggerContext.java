package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.List;

/**
 * What a {@link TriggerSource} is handed at registration: a way to feed events
 * into the active run, and a live view of what the active challenge watches for.
 *
 * <p>Both are read live rather than captured, because a source registers once at
 * mod init while the run and its challenge come and go with the server and, from
 * the import phase on, change under a running server.
 */
public interface TriggerContext {

    /** Feeds an event into the active run. A no-op when no run is active. */
    void emit(GameEvent event);

    /**
     * The distinct values the active challenge configures for a trigger
     * parameter, across every rule using that trigger. Threshold and schedule
     * sources ask this to learn what to watch for, since "below 5 hearts" and
     * "every 300 seconds" describe a condition rather than something that
     * happened. Empty when no run is active or no rule configures it.
     */
    List<ParamValue> configured(String triggerId, String paramName);
}
