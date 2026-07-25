package com.basinity.challengex.common.text;

import java.util.Objects;

/**
 * A line of chat text with the one color it is drawn in. Adapter-neutral on
 * purpose: composing what a line says and deciding its emphasis is shared
 * logic, while turning it into a platform text object is not.
 */
public record StyledLine(String text, LineStyle style) {

    public StyledLine {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(style, "style");
    }

    public static StyledLine of(String text, LineStyle style) {
        return new StyledLine(text, style);
    }
}
