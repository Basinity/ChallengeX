package com.basinity.challengex.core.engine;

import java.util.Objects;

/**
 * One player finishing the challenge, and the clock reading when they did. The
 * order completions were recorded in is the finishing order, so a race can
 * report places and times.
 */
public record Completion(String playerId, long atTick) {

    public Completion {
        Objects.requireNonNull(playerId, "playerId");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("A completion requires a player");
        }
        if (atTick < 0) {
            throw new IllegalArgumentException("atTick must not be negative");
        }
    }
}
