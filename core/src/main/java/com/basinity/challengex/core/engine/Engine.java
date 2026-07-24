package com.basinity.challengex.core.engine;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.registry.ChallengeValidation;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.Registries;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;

/**
 * Dispatches abstract game events against the active challenge and tracks the
 * run's outcome. Platform adapters feed events in, execute the returned effect
 * commands, poll {@link #activeModifiersFor} to enforce modifiers, refresh the
 * roster, and advance the tick counter; the engine itself knows nothing about
 * Minecraft.
 *
 * <p>An event matches a rule when the trigger id matches, the trigger's scope
 * includes the acting player, and every configured trigger parameter equals
 * the event's context value (an omitted parameter matches anything).
 *
 * <p>The win-challenge and lose-challenge effects are consumed here rather than
 * dispatched, because they act on the run instead of the world. Winning and
 * losing are per player: a completion is always credited to the player whose
 * action triggered it, while the win rule's <em>scope</em> decides who is
 * awarded the win once the run ends. That split is what makes the four shapes
 * work. Scope deciding the completion instead would make "everyone must finish"
 * impossible, since one player's trigger would finish it for the whole group.
 *
 * <p>A run ends when a win rule set to end on the first completion records one,
 * when the roster empties (resolving to a win if anybody completed and a loss
 * otherwise, which is what makes an all-eliminated run a loss), or when a time
 * limit expires. Once an outcome is decided, further events dispatch nothing.
 */
public final class Engine {

    private static final long TICKS_PER_MINUTE = 60L * 20L;

    private final Challenge challenge;
    private final OptionalLong timeLimitTicks;
    private long elapsedTicks;
    private RunOutcome outcome = RunOutcome.ONGOING;

    private final List<Completion> completions = new ArrayList<>();
    private final Set<String> completed = new HashSet<>();
    private final Set<String> eliminated = new LinkedHashSet<>();
    private final Set<String> roster = new HashSet<>();
    private boolean rosterReported;

    public Engine(Challenge challenge, Registries registries) {
        List<String> problems = ChallengeValidation.problemsOf(challenge, registries);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("Invalid challenge: " + String.join("; ", problems));
        }
        this.challenge = challenge;
        this.timeLimitTicks = computeTimeLimit(challenge);
    }

    /**
     * The run's time budget in ticks, taken from the shortest {@code time_limit}
     * modifier's minutes, or empty when the run has none. The clock counts down
     * from it and the run is lost when it reaches zero.
     */
    private static OptionalLong computeTimeLimit(Challenge challenge) {
        long shortest = Long.MAX_VALUE;
        boolean found = false;
        for (Modifier modifier : challenge.modifiers()) {
            if (!CoreCatalog.MODIFIER_TIME_LIMIT.equals(modifier.modifierId())) {
                continue;
            }
            if (modifier.params().get("minutes") instanceof ParamValue.OfInt minutes && minutes.value() > 0) {
                shortest = Math.min(shortest, minutes.value() * TICKS_PER_MINUTE);
                found = true;
            }
        }
        return found ? OptionalLong.of(shortest) : OptionalLong.empty();
    }

    /** The challenge being run; platform trigger sources read what it watches for. */
    public Challenge challenge() {
        return challenge;
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
            String effectId = rule.effect().id();
            if (CoreCatalog.EFFECT_LOSE_CHALLENGE.equals(effectId)) {
                eliminate(rule, event);
                continue;
            }
            if (CoreCatalog.EFFECT_WIN_CHALLENGE.equals(effectId)) {
                complete(rule, event);
                continue;
            }
            commands.add(new EffectCommand(effectId, rule.effect().params(),
                    targetFor(rule.effect().scope(), event)));
        }
        return List.copyOf(commands);
    }

    /**
     * Records a completion for whoever triggered the win rule. A specific-player
     * scope also gates who may complete it at all, so a win condition aimed at
     * named players does not count when anybody else meets it. A playerless
     * trigger has nobody to credit, so it completes for the whole roster.
     */
    private void complete(Rule rule, GameEvent event) {
        for (String player : creditedBy(rule, event)) {
            if (completed.add(player)) {
                completions.add(new Completion(player, elapsedTicks));
            }
            // Finishing puts a player back in good standing: a win outranks a
            // loss they had picked up earlier in the run.
            eliminated.remove(player);
        }
        if (!completions.isEmpty() && endsOnFirstCompletion(rule)) {
            outcome = RunOutcome.WIN;
        }
    }

    private Set<String> creditedBy(Rule rule, GameEvent event) {
        Optional<String> actor = event.playerId();
        if (actor.isEmpty()) {
            return Set.copyOf(roster);
        }
        if (rule.effect().scope().orElse(null) instanceof Scope.SpecificPlayers named
                && !named.playerIds().contains(actor.get())) {
            return Set.of();
        }
        return Set.of(actor.get());
    }

    /** Takes the lose rule's scoped players out of the run. */
    private void eliminate(Rule rule, GameEvent event) {
        eliminated.addAll(playersOf(rule.effect().scope(), event));
        resolveEmptyRoster();
    }

    /**
     * The concrete players an effect's scope names right now. Every-player
     * means the roster, and a per-player effect under a playerless trigger falls
     * back to everyone, exactly as a dispatched effect's target does.
     */
    private Set<String> playersOf(Optional<Scope> scope, GameEvent event) {
        if (scope.isEmpty()) {
            return Set.copyOf(roster);
        }
        return switch (scope.get()) {
            case Scope.PerPlayer ignored -> event.playerId()
                    .<Set<String>>map(Set::of)
                    .orElseGet(() -> Set.copyOf(roster));
            case Scope.EveryPlayer ignored -> Set.copyOf(roster);
            case Scope.SpecificPlayers named -> named.playerIds();
        };
    }

    private static boolean endsOnFirstCompletion(Rule rule) {
        return !(rule.effect().params().get("end") instanceof ParamValue.OfString end)
                || !CoreCatalog.END_AFTER_ALL_COMPLETE.equals(end.value());
    }

    /**
     * Advances the run's clock. When a time-limit budget is set and the elapsed
     * time reaches it, an ongoing run ends as a loss (a run already decided keeps
     * its outcome).
     */
    public void tick(long ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Ticks must be positive");
        }
        elapsedTicks += ticks;
        if (outcome == RunOutcome.ONGOING
                && timeLimitTicks.isPresent()
                && elapsedTicks >= timeLimitTicks.getAsLong()) {
            outcome = RunOutcome.LOSS;
        }
    }

    public long elapsedTicks() {
        return elapsedTicks;
    }

    /** The run's time budget in ticks, or empty when it has no time limit. */
    public OptionalLong timeLimitTicks() {
        return timeLimitTicks;
    }

    /**
     * What the run clock should read: the ticks remaining down from a time
     * limit when one is set (never below zero), otherwise the ticks elapsed.
     */
    public long displayTicks() {
        if (timeLimitTicks.isEmpty()) {
            return elapsedTicks;
        }
        return Math.max(0L, timeLimitTicks.getAsLong() - elapsedTicks);
    }

    /**
     * The modifiers currently in force for a player: those scoped to them. A
     * playerless modifier applies to the run as a whole, so it is in force
     * regardless of the player asked about.
     *
     * <p>A player outside the roster gets none of them. That is the whole of the
     * "nothing in the run touches a player who has finished" rule on the modifier
     * side: the adapter's enforcer already diffs each player's modifiers against
     * their previous set every tick, so an empty list tears theirs down when they
     * leave and rebuilds them if they come back. An engine no adapter has told
     * the roster to yet does not filter, so a run enforces modifiers normally
     * until the first roster report arrives.
     */
    public List<Modifier> activeModifiersFor(String playerId) {
        if (rosterReported && !roster.contains(playerId)) {
            return List.of();
        }
        return challenge.modifiers().stream()
                .filter(modifier -> modifier.scope().map(scope -> scope.includes(playerId)).orElse(true))
                .toList();
    }

    public RunOutcome outcome() {
        return outcome;
    }

    /**
     * Refreshes who is still playing. The adapter reports it each tick from game
     * mode, so a player who won, lost, or simply switched themselves out of
     * survival leaves the roster, and one who switches back rejoins it.
     *
     * <p>Rejoining clears a recorded loss, since the player is back in play and
     * can still win or be eliminated again. A recorded win is permanent and
     * keeps its place and time, so a player who already finished cannot place
     * twice, though rejoining does hold an after-all-complete run open until
     * they leave again.
     */
    public void updateRoster(Collection<String> playerIds) {
        roster.clear();
        roster.addAll(playerIds);
        rosterReported = true;
        eliminated.removeAll(roster);
        resolveEmptyRoster();
    }

    /**
     * Ends a run nobody is left playing: a win when anybody completed it, a loss
     * otherwise. A roster that is empty because nothing has happened yet (an
     * empty server, a run that has not been joined) decides nothing, since there
     * is a difference between everyone having finished and nobody having started.
     */
    private void resolveEmptyRoster() {
        if (outcome != RunOutcome.ONGOING || !rosterReported || !roster.isEmpty()) {
            return;
        }
        if (completions.isEmpty() && eliminated.isEmpty()) {
            return;
        }
        outcome = completions.isEmpty() ? RunOutcome.LOSS : RunOutcome.WIN;
    }

    /** Who is still playing, as the adapter last reported it. */
    public Set<String> roster() {
        return Set.copyOf(roster);
    }

    /** Everyone who finished, in the order they finished, with the clock reading at each finish. */
    public List<Completion> completions() {
        return List.copyOf(completions);
    }

    /** Everyone currently out of the run through the lose-challenge effect. */
    public Set<String> eliminated() {
        return Set.copyOf(eliminated);
    }

    /**
     * Whether a win is shared by everyone rather than kept by whoever finished.
     * Read off the challenge instead of stored, so it survives a restore without
     * being persisted: any win rule that awards every player wins for the group.
     */
    public boolean winsTogether() {
        return challenge.rules().stream()
                .filter(rule -> CoreCatalog.EFFECT_WIN_CHALLENGE.equals(rule.effect().id()))
                .anyMatch(rule -> rule.effect().scope().orElse(null) instanceof Scope.EveryPlayer);
    }

    /**
     * Rebuilds an engine mid-run from a saved snapshot's state without replaying
     * the events that produced it: the elapsed clock, the decided outcome, the
     * finishing order, and who is out are restored directly. The challenge is
     * validated as it is on a fresh engine, and the time-limit budget is
     * recomputed from it rather than stored. The roster is not restored, since
     * the adapter reports it afresh from game mode on the next tick.
     */
    public static Engine restore(Challenge challenge, Registries registries,
            long elapsedTicks, RunOutcome outcome, List<Completion> completions,
            Collection<String> eliminated) {
        Objects.requireNonNull(outcome, "outcome");
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks must not be negative");
        }
        Engine engine = new Engine(challenge, registries);
        engine.elapsedTicks = elapsedTicks;
        engine.outcome = outcome;
        for (Completion completion : completions) {
            if (engine.completed.add(completion.playerId())) {
                engine.completions.add(completion);
            }
        }
        engine.eliminated.addAll(eliminated);
        return engine;
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
}
