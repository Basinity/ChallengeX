package com.basinity.challengex.paper.text;

import com.basinity.challengex.common.text.LineStyle;
import com.basinity.challengex.common.text.StyledLine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Draws the shared modules' {@link StyledLine}s as Adventure components. The
 * Paper half of the text boundary: shared code decides what a line says and how
 * much it stands out, this turns that into the platform's own text type.
 */
public final class Lines {

    private Lines() {
    }

    public static Component render(StyledLine line) {
        return Component.text(line.text(), color(line.style()));
    }

    public static NamedTextColor color(LineStyle style) {
        return switch (style) {
            case WHITE -> NamedTextColor.WHITE;
            case GRAY -> NamedTextColor.GRAY;
            case GOLD -> NamedTextColor.GOLD;
            case GREEN -> NamedTextColor.GREEN;
            case RED -> NamedTextColor.RED;
        };
    }
}
