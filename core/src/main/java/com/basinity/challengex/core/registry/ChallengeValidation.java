package com.basinity.challengex.core.registry;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Checks a challenge against the registries: every id must be known, every
 * parameter declared, correctly typed, and present where required, and every
 * block's scope must match its catalog entry: required where the entry is
 * scoped, forbidden where it is playerless. Shared by preset import and engine
 * construction so both entry points enforce identical semantics. Returns
 * problems instead of throwing, so a caller can report all of them at once.
 */
public final class ChallengeValidation {

    private ChallengeValidation() {
    }

    public static List<String> problemsOf(Challenge challenge, Registries registries) {
        List<String> problems = new ArrayList<>();

        List<Rule> rules = challenge.rules();
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            String where = "rule " + (i + 1);
            checkBlock(registries.triggers(), rule.trigger().id(), rule.trigger().params(),
                    rule.trigger().scope(), where + " trigger", problems);
            checkBlock(registries.effects(), rule.effect().id(), rule.effect().params(),
                    rule.effect().scope(), where + " effect", problems);
        }

        challenge.goal().ifPresent(goal ->
                checkBlock(registries.goals(), goal.goalId(), goal.params(), Optional.empty(),
                        "goal", problems));

        List<Modifier> modifiers = challenge.modifiers();
        for (int i = 0; i < modifiers.size(); i++) {
            Modifier modifier = modifiers.get(i);
            checkBlock(registries.modifiers(), modifier.modifierId(), modifier.params(),
                    modifier.scope(), "modifier " + (i + 1), problems);
        }

        return problems;
    }

    private static <D extends Definition> void checkBlock(Registry<D> registry, String id,
            Map<String, ParamValue> params, Optional<? extends Scope> scope,
            String where, List<String> problems) {
        Optional<D> definition = registry.find(id);
        if (definition.isEmpty()) {
            problems.add(unknownId(where, id));
            return;
        }
        checkScope(definition.get(), scope, where, problems);
        checkParams(definition.get(), params, where, problems);
    }

    private static void checkScope(Definition definition, Optional<? extends Scope> scope,
            String where, List<String> problems) {
        if (definition.scoped() && scope.isEmpty()) {
            problems.add(where + ": missing scope for '" + definition.id() + "'");
        } else if (!definition.scoped() && scope.isPresent()) {
            problems.add(where + ": '" + definition.id()
                    + "' has no player dimension and takes no scope");
        }
    }

    private static void checkParams(Definition definition, Map<String, ParamValue> params,
            String where, List<String> problems) {
        for (Map.Entry<String, ParamValue> entry : params.entrySet()) {
            Optional<ParamSpec> spec = definition.param(entry.getKey());
            if (spec.isEmpty()) {
                problems.add(where + ": unknown parameter '" + entry.getKey()
                        + "' for '" + definition.id() + "'");
            } else if (!spec.get().type().matches(entry.getValue())) {
                problems.add(where + ": parameter '" + entry.getKey() + "' of '" + definition.id()
                        + "' expects " + spec.get().type());
            }
        }
        for (ParamSpec spec : definition.params()) {
            if (spec.required() && !params.containsKey(spec.name())) {
                problems.add(where + ": missing required parameter '" + spec.name()
                        + "' for '" + definition.id() + "'");
            }
        }
    }

    private static String unknownId(String where, String id) {
        return where + ": unknown id '" + id
                + "' — not in this build's catalog (check the id for typos, or the preset may need a newer ChallengeX version)";
    }
}
