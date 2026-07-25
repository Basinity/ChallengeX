package com.basinity.challengex.common.command;

import com.basinity.challengex.common.text.LineStyle;
import com.basinity.challengex.common.text.StyledLine;
import com.basinity.challengex.core.engine.RunState;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Renders a challenge's full composition as chat lines: every rule's trigger and
 * effect with their parameters and scope, and every modifier. This is what
 * {@code /challengex info} prints, so a player can see exactly which catalog
 * building blocks the active challenge is built from and at which values.
 */
public final class ChallengeSummary {

    private ChallengeSummary() {
    }

    public static List<StyledLine> describe(Challenge challenge, String presetName, RunState state) {
        List<StyledLine> lines = new ArrayList<>();
        String name = presetName == null ? "active challenge" : "\"" + presetName + "\"";
        lines.add(StyledLine.of("Challenge " + name + "  (" + label(state) + ")", LineStyle.GOLD));

        List<Rule> rules = challenge.rules();
        if (rules.isEmpty()) {
            lines.add(dim("Rules: none"));
        } else {
            lines.add(dim("Rules (" + rules.size() + "):"));
            for (Rule rule : rules) {
                String trigger = rule.trigger().id() + params(rule.trigger().params()) + scope(rule.trigger().scope());
                String effect = rule.effect().id() + params(rule.effect().params()) + scope(rule.effect().scope());
                lines.add(StyledLine.of("  - " + trigger + "  ->  " + effect, LineStyle.WHITE));
            }
        }

        List<Modifier> modifiers = challenge.modifiers();
        if (modifiers.isEmpty()) {
            lines.add(dim("Modifiers: none"));
        } else {
            lines.add(dim("Modifiers (" + modifiers.size() + "):"));
            for (Modifier modifier : modifiers) {
                lines.add(StyledLine.of("  - " + modifier.modifierId() + params(modifier.params())
                        + scope(modifier.scope()), LineStyle.WHITE));
            }
        }
        return List.copyOf(lines);
    }

    private static StyledLine dim(String text) {
        return StyledLine.of(text, LineStyle.GRAY);
    }

    private static String label(RunState state) {
        return switch (state) {
            case NOT_STARTED -> "not started";
            case RUNNING -> "running";
            case PAUSED -> "paused";
            case FINISHED -> "finished";
        };
    }

    private static String params(Map<String, ParamValue> params) {
        if (params.isEmpty()) {
            return "";
        }
        return " {" + params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + value(entry.getValue()))
                .collect(Collectors.joining(", ")) + "}";
    }

    private static String value(ParamValue value) {
        return switch (value) {
            case ParamValue.OfString string -> string.value();
            case ParamValue.OfInt integer -> Long.toString(integer.value());
            case ParamValue.OfDecimal decimal -> trim(decimal.value());
            case ParamValue.OfBool bool -> Boolean.toString(bool.value());
        };
    }

    private static String trim(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static String scope(Optional<? extends Scope> scope) {
        if (scope.isEmpty()) {
            return "";
        }
        return switch (scope.get()) {
            case Scope.PerPlayer ignored -> " [triggering player]";
            case Scope.EveryPlayer ignored -> " [everyone]";
            case Scope.SpecificPlayers specific -> " [" + String.join(", ", new TreeSet<>(specific.playerIds())) + "]";
        };
    }
}
