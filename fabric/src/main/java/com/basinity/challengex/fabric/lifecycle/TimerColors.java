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
 * color is sampled from the ramp at a position that shifts every tick, so the
 * gradient scrolls across the text as a moving band. The chosen color is stored
 * in the mod config; {@code rainbow} is the one multi-hue option.
 */
public final class TimerColors {

    /** The default color. */
    public static final String DEFAULT = "white";

    /**
     * Characters of text spanned by scrolling one ramp stop's distance. A full
     * band is this times the ramp's length, so the per-stop distance between
     * adjacent characters stays the same whether a ramp has two stops or the
     * rainbow's twenty-four, mirroring how TICKS_PER_STOP keeps per-stop speed
     * constant below.
     */
    private static final double CHARS_PER_STOP = 3.5;
    /**
     * Ticks to scroll from one ramp color to the next; lower is faster. A full
     * band is this times the ramp's length, so the per-color speed stays the same
     * whether a ramp has two stops or the rainbow's twenty-four.
     */
    private static final double TICKS_PER_STOP = 40.0;

    /**
     * Name to ramp, insertion-ordered so suggestions and the clickable list read
     * in a deliberate order. These are the fifteen standard Minecraft colors,
     * each shimmering between the color and a slightly darker shade of it, plus
     * a multi-hue rainbow. A two-stop cyclic ramp interpolates as a seamless
     * triangle, no wrap seam.
     */
    private static final Map<String, int[]> RAMPS = new LinkedHashMap<>();

    static {
        RAMPS.put("dark_blue", new int[] {0x0000AA, 0x000072});
        RAMPS.put("dark_green", new int[] {0x00AA00, 0x007200});
        RAMPS.put("dark_aqua", new int[] {0x00AAAA, 0x007272});
        RAMPS.put("dark_red", new int[] {0xAA0000, 0x720000});
        RAMPS.put("dark_purple", new int[] {0xAA00AA, 0x720072});
        RAMPS.put("gold", new int[] {0xFFAA00, 0xCC8800});
        RAMPS.put("gray", new int[] {0xAAAAAA, 0x727272});
        RAMPS.put("dark_gray", new int[] {0x555555, 0x3A3A3A});
        RAMPS.put("blue", new int[] {0x5555FF, 0x4545CC});
        RAMPS.put("green", new int[] {0x55FF55, 0x45CC45});
        RAMPS.put("aqua", new int[] {0x55FFFF, 0x45CCCC});
        RAMPS.put("red", new int[] {0xFF5555, 0xCC4545});
        RAMPS.put("light_purple", new int[] {0xFF55FF, 0xCC45CC});
        RAMPS.put("yellow", new int[] {0xFFFF55, 0xCCCC45});
        RAMPS.put("white", new int[] {0xFFFFFF, 0xD8D8D8});
        RAMPS.put("rainbow", new int[] {
                // red -> yellow (G rises)
                0xFF0000, 0xFF4000, 0xFF8000, 0xFFC000,
                // yellow -> green (R falls)
                0xFFFF00, 0xC0FF00, 0x80FF00, 0x40FF00,
                // green -> cyan (B rises)
                0x00FF00, 0x00FF40, 0x00FF80, 0x00FFC0,
                // cyan -> blue (G falls)
                0x00FFFF, 0x00C0FF, 0x0080FF, 0x0040FF,
                // blue -> magenta (R rises)
                0x0000FF, 0x4000FF, 0x8000FF, 0xC000FF,
                // magenta -> red (B falls)
                0xFF00FF, 0xFF00C0, 0xFF0080, 0xFF0040});
    }

    private TimerColors() {
    }

    /** The color names, in menu order. */
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
     * Renders text in bold with the color ramp scrolled by {@code animTick}: one
     * component per character, each colored by its own sample of the ramp.
     */
    public static Component gradient(int[] ramp, String text, int animTick) {
        MutableComponent line = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            double phase = (i / (CHARS_PER_STOP * ramp.length)) - (animTick / (TICKS_PER_STOP * ramp.length));
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
