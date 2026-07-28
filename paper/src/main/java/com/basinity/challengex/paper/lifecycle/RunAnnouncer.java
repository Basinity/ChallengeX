package com.basinity.challengex.paper.lifecycle;

import com.basinity.challengex.common.lifecycle.RunAnnouncement;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.paper.text.Lines;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Surfaces a finished run to every player: a big on-screen title, a chat line
 * stating the outcome and the final time with a clickable control that prints
 * the full challenge composition ({@code /challengex info}), the results, and a
 * win/loss sound.
 *
 * <p>What the run says is {@link RunAnnouncement}'s call, including who is being
 * congratulated. This draws it, which on Paper means Adventure titles and
 * sounds rather than raw packets.
 */
public final class RunAnnouncer {

    private static final Title.Times TIMES =
            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3500), Duration.ofSeconds(1));

    private RunAnnouncer() {
    }

    public static void announce(Server server, ChallengeRun run) {
        RunAnnouncement announcement = RunAnnouncement.of(run);

        Component viewConfig = Component.text("[View challenge configuration]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/challengex info"))
                .hoverEvent(HoverEvent.showText(
                        Component.text("Show every trigger, effect, and modifier in this challenge")));

        List<Component> results = announcement.results().stream().map(Lines::render).toList();

        for (Player player : server.getOnlinePlayers()) {
            boolean celebrates = announcement.celebrates(player.getName());
            NamedTextColor color = celebrates ? NamedTextColor.GREEN : NamedTextColor.RED;
            Sound sound = celebrates ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.BLOCK_ANVIL_LAND;
            float pitch = celebrates ? 1.0f : 0.8f;

            player.showTitle(Title.title(Component.text(announcement.title(), color),
                    Component.empty(), TIMES));
            player.sendMessage(Component.text(announcement.chatLine(), color));
            results.forEach(player::sendMessage);
            player.sendMessage(viewConfig);
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        }
    }
}
