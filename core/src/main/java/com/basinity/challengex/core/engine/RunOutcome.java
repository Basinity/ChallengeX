package com.basinity.challengex.core.engine;

/**
 * Where the run stands. A loss comes from the lose-challenge effect or from a
 * time-limit modifier expiring. Once decided, the outcome is final.
 */
public enum RunOutcome {
    ONGOING,
    WIN,
    LOSS
}
