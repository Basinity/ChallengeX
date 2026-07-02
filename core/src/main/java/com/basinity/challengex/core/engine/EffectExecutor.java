package com.basinity.challengex.core.engine;

/**
 * The platform's half of the adapter contract: it carries out an effect command
 * against the real game. The engine resolves each effect's scope to a
 * {@link EffectCommand.Target} and hands the command here; turning the target
 * into concrete players and applying the effect is the platform's job.
 *
 * <p>Core owns this interface so every adapter (Fabric now, Paper later) speaks
 * the same contract.
 */
@FunctionalInterface
public interface EffectExecutor {

    void execute(EffectCommand command);
}
