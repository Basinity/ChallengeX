package com.basinity.challengex.core.model;

/**
 * Who has to reach a win-together goal. {@link #ANYONE} is the default: one
 * player completing it wins for everyone, with a compound goal's requirements
 * pooled across players. {@link #EVERYONE} demands each player currently in
 * the run complete the goal themselves before the run is won. A versus goal
 * has no completion choice; it always races individuals.
 */
public enum GoalCompletion {
    ANYONE,
    EVERYONE
}
