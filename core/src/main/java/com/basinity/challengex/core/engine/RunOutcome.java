package com.basinity.challengex.core.engine;

/**
 * Where the run stands. A win comes from completing the goal and ends the run
 * for everyone; a loss comes from the lose-challenge effect (and, from phase
 * 6 on, a time-limit modifier expiring). Once decided, the outcome is final.
 */
public enum RunOutcome {
    ONGOING,
    WIN,
    LOSS
}
