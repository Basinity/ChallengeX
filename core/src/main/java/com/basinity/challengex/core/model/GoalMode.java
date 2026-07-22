package com.basinity.challengex.core.model;

/**
 * How a goal decides the run. {@link #TOGETHER} is the cooperative default:
 * reaching the goal wins the run for every player, with {@link GoalCompletion}
 * saying who has to reach it. {@link #VERSUS} is a race: the first player to
 * complete the goal on their own wins the run over the others.
 */
public enum GoalMode {
    TOGETHER,
    VERSUS
}
