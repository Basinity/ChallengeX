package com.basinity.challengex.fabric.lifecycle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

/**
 * The palette of run-clock colors and the bold, animated gradient the action
 * bar draws in. Each named color is a short cyclic ramp of shades; a character's
 * colour is sampled from the ramp at a position that shifts every tick, so the
 * gradient scrolls across the text as a moving band. The chosen colour is stored
 * in the mod config; {@code rainbow} is the one multi-hue option.
 */
public final class TimerColors {

    /** The default colour. */
    public static final String DEFAULT = "white";

    /** Characters of text spanned by one full trip through a ramp. */
    private static final double CHARS_PER_CYCLE = 7.0;
    /** Ticks for the band to scroll one full ramp; lower is faster. */
    private static final double TICKS_PER_CYCLE = 80.0;

    /**
     * Name to ramp, insertion-ordered so suggestions and the clickable list read
     * in a deliberate order. These are the fifteen standard Minecraft colours,
     * each shimmering between the colour and a slightly darker shade of it, plus
     * a multi-hue rainbow. A two-stop cyclic ramp interpolates as a seamless
     * triangle, no wrap seam.
     */
    private static final Map<String, int[]> RAMPS = new LinkedHashMap<>();

    static {
        RAMPS.put("dark_blue", new int[] {0x0000AA, 0x00008E});
        RAMPS.put("dark_green", new int[] {0x00AA00, 0x008E00});
        RAMPS.put("dark_aqua", new int[] {0x00AAAA, 0x008E8E});
        RAMPS.put("dark_red", new int[] {0xAA0000, 0x8E0000});
        RAMPS.put("dark_purple", new int[] {0xAA00AA, 0x8E008E});
        RAMPS.put("gold", new int[] {0xFFAA00, 0xE59900});
        RAMPS.put("gray", new int[] {0xAAAAAA, 0x8E8E8E});
        RAMPS.put("dark_gray", new int[] {0x555555, 0x474747});
        RAMPS.put("blue", new int[] {0x5555FF, 0x4E4EE5});
        RAMPS.put("green", new int[] {0x55FF55, 0x4EE54E});
        RAMPS.put("aqua", new int[] {0x55FFFF, 0x4EE5E5});
        RAMPS.put("red", new int[] {0xFF5555, 0xE54E4E});
        RAMPS.put("light_purple", new int[] {0xFF55FF, 0xE54EE5});
        RAMPS.put("yellow", new int[] {0xFFFF55, 0xF2F252});
        RAMPS.put("white", new int[] {0xFFFFFF, 0xF2F2F2});
        RAMPS.put("rainbow", new int[] {
                0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5599FF, 0xAA66FF, 0xFF66CC});
    }

    private TimerColors() {
    }

    /** The colour names, in menu order. */
    public static Set<String> names() {
        return RAMPS.keySet();
    }

    public static boolean has(String name) {
        return RAMPS.containsKey(name);
    }

    /** The ramp for a name, falling back to the default for an unknown one. */
    public static int[] ramp(String name) {
        return RAMPS.getOrDefault(name, RAMPS.get(DEFAULT));
    }

    /**
     * Renders text in bold with the colour ramp scrolled by {@code animTick}: one
     * component per character, each coloured by its own sample of the ramp.
     */
    public static Component gradient(int[] ramp, String text, int animTick) {
        MutableComponent line = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double phase = (i / CHARS_PER_CYCLE) - (animTick / TICKS_PER_CYCLE);
            TextColor color = TextColor.fromRgb(sample(ramp, phase));
            line.append(Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(style -> style.withColor(color).withBold(true)));
        }
        return line;
    }

    private static int sample(int[] ramp, double phase) {
        double wrapped = phase - Math.floor(phase);
        double scaled = wrapped * ramp.length;
        int index = (int) Math.floor(scaled);
        double frac = scaled - index;
        return lerp(ramp[index % ramp.length], ramp[(index + 1) % ramp.length], frac);
    }

    private static int lerp(int from, int to, double t) {
        int red = round(((from >> 16) & 0xFF), ((to >> 16) & 0xFF), t);
        int green = round(((from >> 8) & 0xFF), ((to >> 8) & 0xFF), t);
        int blue = round((from & 0xFF), (to & 0xFF), t);
        return (red << 16) | (green << 8) | blue;
    }

    private static int round(int from, int to, double t) {
        return (int) Math.round(from + (to - from) * t);
    }
}
