package com.basinity.challengex.core.preset;

import java.util.List;

/**
 * A preset was rejected. Carries every problem found, not just the first,
 * so the user can fix the file in one pass; a preset is never partially
 * imported.
 */
public class PresetFormatException extends Exception {

    private final List<String> problems;

    public PresetFormatException(List<String> problems) {
        super("Preset rejected: " + String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
