package com.basinity.challengex.fabric.text;

import com.basinity.challengex.common.text.LineStyle;
import com.basinity.challengex.common.text.StyledLine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Draws the shared modules' {@link StyledLine}s as vanilla chat components. The
 * Fabric half of the text boundary: shared code decides what a line says and
 * how much it stands out, this turns that into the platform's own text type.
 */
public final class Lines {

    private Lines() {
    }

    public static Component render(StyledLine line) {
        return Component.literal(line.text()).withStyle(format(line.style()));
    }

    public static ChatFormatting format(LineStyle style) {
        return switch (style) {
            case WHITE -> ChatFormatting.WHITE;
            case GRAY -> ChatFormatting.GRAY;
            case GOLD -> ChatFormatting.GOLD;
            case GREEN -> ChatFormatting.GREEN;
            case RED -> ChatFormatting.RED;
        };
    }
}
