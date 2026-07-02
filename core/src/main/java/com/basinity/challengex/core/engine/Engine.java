package com.basinity.challengex.core.engine;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.Goal;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.registry.ChallengeValidation;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.GoalDefinition;
import com.basinity.challengex.core.registry.GoalRequirement;
import com.basinity.challengex.core.registry.ParamBinding;
import com.basinity.challengex.core.registry.Registries;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Dispatches abstract game events against the active challenge and tracks the
 * run's outcome. Platform adapters feed events in, execute the returned effect
 * commands, poll {@link #activeModifiersFor} to enforce modifiers, and advance
 * the tick counter; the engine itself knows nothing about Minecraft.
 *
 * <p>An event matches a rule when the trigger id matches, the trigger's scope
 * includes the acting player, and every configured trigger parameter equals
 * the event's context value (an omitted parameter matches anything). The
 * lose-challenge effect is consumed here rather than dispatched: it ends the
 * run as a loss. Once an outcome is decided, further events dispatch nothing.
 */
public final class Engine {

    private final Challenge challenge;
    private final Registries registries;
    private final Set<Integer> metGoalRequirements = new HashSet<>();
    private long elapsedTicks;
    private RunOutcome outcome = RunOutcome.ONGOING;

    public Engine(Challenge challenge, Registries registries) {
        List<String> problems = ChallengeValidation.problemsOf(challenge, registries);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Invalid challenge: " + String.join("; ", problems));
        }
        this.challenge = challenge;
        this.registries = registries;
    }

    public List<EffectCommand> onEvent(GameEvent event) {
        if (outcome != RunOutcome.ONGOING) {
            return List.of();
        }
        List<EffectCommand> commands = new ArrayList<>();
        for (Rule rule : challenge.rules()) {
            if (!matches(rule, event)) {
                continue;
            }
            if (CoreCatalog.EFFECT_LOSE_CHALLENGE.equals(rule.effect().id())) {
                outcome = RunOutcome.LOSS;
                continue;
            }
            commands.add(new EffectCommand(rule.effect().id(), rule.effect().params(),
                    targetFor(rule.effect().scope(), event)));
        }
        evaluateGoal(event);
        return List.copyOf(commands);
    }

    /** Advances the run's clock; modifier expiry is measured against it. */
    public void tick(long ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Ticks must be positive");
        }
        elapsedTicks += ticks;
    }

    public long elapsedTicks() {
        return elapsedTicks;
    }

    /**
     * The modifiers currently in force for a player: scoped to them and not
     * expired. A playerless modifier applies to the run as a whole, so it is
     * in force regardless of the player asked about.
     */
    public List<Modifier> activeModifiersFor(String playerId) {
        return challenge.modifiers().stream()
                .filter(modifier -> modifier.scope().map(scope -> scope.includes(playerId)).orElse(true))
                .filter(modifier -> modifier.expiryTicks().isEmpty()
                        || elapsedTicks < modifier.expiryTicks().getAsLong())
                .toList();
    }

    public RunOutcome outcome() {
        return outcome;
    }

    private boolean matches(Rule rule, GameEvent event) {
        if (!rule.trigger().id().equals(event.triggerId())) {
            return false;
        }
        // A playerless trigger has no scope and does no player filtering.
        if (rule.trigger().scope().orElse(null) instanceof Scope.SpecificPlayers scope) {
            // A specific-player trigger watches only those players, so an
            // event with no acting player can never satisfy it.
            if (event.playerId().isEmpty() || !scope.includes(event.playerId().get())) {
                return false;
            }
        }
        for (Map.Entry<String, ParamValue> filter : rule.trigger().params().entrySet()) {
            if (!filter.getValue().equals(event.context().get(filter.getKey()))) {
                return false;
            }
        }
        return true;
    }

    private EffectCommand.Target targetFor(Optional<Scope> effectScope, GameEvent event) {
        if (effectScope.isEmpty()) {
            // A playerless effect acts on the world or the run; the adapter
            // gets the symbolic everyone-target and applies it globally.
            return EffectCommand.Target.ALL_PLAYERS;
        }
        return switch (effectScope.get()) {
            // A per-player effect on a playerless event has no triggering
            // player to hit, so it falls back to everyone.
            case Scope.PerPlayer ignored -> event.playerId()
                    .map(EffectCommand.Target::player)
                    .orElse(EffectCommand.Target.ALL_PLAYERS);
            case Scope.EveryPlayer ignored -> EffectCommand.Target.ALL_PLAYERS;
            case Scope.SpecificPlayers specific -> new EffectCommand.Target.Players(specific.playerIds());
        };
    }

    private void evaluateGoal(GameEvent event) {
        if (outcome != RunOutcome.ONGOING || challenge.goal().isEmpty()) {
            return;
        }
        Goal goal = challenge.goal().get();
        GoalDefinition definition = registries.goals().require(goal.goalId());
        List<GoalRequirement> requirements = definition.requirements();
        for (int i = 0; i < requirements.size(); i++) {
            if (!metGoalRequirements.contains(i) && requirementMet(requirements.get(i), goal, event)) {
                metGoalRequirements.add(i);
            }
        }
        if (metGoalRequirements.size() == requirements.size()) {
            outcome = RunOutcome.WIN;
        }
    }

    private boolean requirementMet(GoalRequirement requirement, Goal goal, GameEvent event) {
        if (!requirement.eventIds().contains(event.triggerId())) {
            return false;
        }
        for (Map.Entry<String, ParamBinding> entry : requirement.contextMatch().entrySet()) {
            ParamValue expected = switch (entry.getValue()) {
                case ParamBinding.Literal literal -> literal.value();
                case ParamBinding.FromGoalParam ref -> goal.params().get(ref.goalParamName());
            };
            if (expected == null || !expected.equals(event.context().get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }
}
