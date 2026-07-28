package com.basinity.challengex.common.support;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.Rule;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Which catalog entries each platform does not run. The website reads this to
 * mark the pieces a challenge cannot use on a given platform, and each adapter
 * reads its own set to tell a host in game when an imported preset uses one.
 *
 * <p>The sets are named the other way round from what an adapter holds: an
 * adapter knows what it wired, and what matters here is the difference from the
 * catalog, which is nearly always empty and is the only part worth shipping.
 *
 * <p>Paper's set is machine-checked. Its coverage test derives the catalog
 * minus everything its registration tables wire, minus what the engine settles
 * itself, and asserts the remainder is exactly what is declared here, so the
 * declaration cannot drift from what the adapter really does.
 *
 * <p>Fabric's is declared rather than derived, and that asymmetry is real:
 * roughly half its triggers and several of its modifiers are implemented as
 * Mixins woven into vanilla classes, which appear in no registration table and
 * cannot be enumerated at runtime. Fabric currently runs the whole catalog, so
 * the set is empty; anything cut there in future has to be added by hand.
 */
public final class PlatformSupport {

    /** A platform's id, as the website names it. */
    public static final String FABRIC = "fabric";
    public static final String PAPER = "paper";

    /**
     * Bumped when the shape of the exported document changes, not when an entry
     * moves in or out of a platform's set.
     */
    public static final int SUPPORT_VERSION = 1;

    private static final Map<String, Set<String>> UNSUPPORTED = unsupported();

    private PlatformSupport() {
    }

    private static Map<String, Set<String>> unsupported() {
        Map<String, Set<String>> byPlatform = new LinkedHashMap<>();
        byPlatform.put(FABRIC, Set.of());
        // Fabric points every member's inventory at one object through a Mixin.
        // Bukkit backs a player's inventory with its own object and gives no way
        // to swap it, leaving only a per-tick copy that would invite the item
        // duplication a shared inventory must not have.
        byPlatform.put(PAPER, Set.of("modifier.share_inventory"));
        return Map.copyOf(byPlatform);
    }

    /** The platforms, in the order the website should offer them. */
    public static Set<String> platforms() {
        return UNSUPPORTED.keySet();
    }

    /** The catalog ids this platform does not run. Empty for a platform running all of it. */
    public static Set<String> unsupportedOn(String platform) {
        return UNSUPPORTED.getOrDefault(platform, Set.of());
    }

    /** Whether this platform runs the given catalog id. */
    public static boolean supports(String platform, String id) {
        return !unsupportedOn(platform).contains(id);
    }

    /**
     * The entries this challenge uses that the platform does not run, in
     * catalog-id order.
     *
     * <p>This is what an adapter reports when a preset is loaded. Preset
     * validation cannot catch it: {@code core} is shared by every adapter, so an
     * id only one of them runs is still a known id everywhere and imports
     * clean. The miss would otherwise surface only as the piece quietly doing
     * nothing.
     */
    public static Set<String> unsupportedUsedBy(Challenge challenge, String platform) {
        Set<String> gap = unsupportedOn(platform);
        if (gap.isEmpty()) {
            return Set.of();
        }
        Set<String> used = new TreeSet<>();
        for (Rule rule : challenge.rules()) {
            addIfUnsupported(used, gap, rule.trigger().id());
            addIfUnsupported(used, gap, rule.effect().id());
        }
        for (Modifier modifier : challenge.modifiers()) {
            addIfUnsupported(used, gap, modifier.modifierId());
        }
        return used;
    }

    private static void addIfUnsupported(Set<String> used, Set<String> gap, String id) {
        if (gap.contains(id)) {
            used.add(id);
        }
    }
}
