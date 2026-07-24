package com.basinity.challengex.fabric.lifecycle;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.Completion;
import com.basinity.challengex.core.engine.RunOutcome;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Surfaces a finished run to every player: a big on-screen title, a chat line
 * stating the outcome and the final time with a clickable control that prints
 * the full challenge composition ({@code /challengex info}), the results, and a
 * win/loss sound.
 *
 * <p>How the results read follows how the win was awarded. A win shared by
 * everyone reports one group time, which is the moment the last player finished
 * rather than whenever the run happened to stop. A win kept by whoever finished
 * reports a placing, a name and a time per line, in the order they finished, and
 * splits the presentation: the players who finished get the green title and the
 * win sound, everyone else sees the same title in red and hears the loss anvil,
 * since for them the run was lost.
 */
public final class RunAnnouncer {

    private RunAnnouncer() {
    }

    public static void announce(MinecraftServer server, ChallengeRun run) {
        boolean won = run.outcome() == RunOutcome.WIN;
        List<Completion> completions = run.completions();
        boolean together = run.winsTogether();

        String time = RunClock.format(groupTicks(run, completions));
        String titleText = won ? "Challenge Complete" : "Challenge Failed";
        String chatText = (won ? "Challenge complete — " : "Challenge failed — ") + time;

        Component viewConfig = Component.literal("[View challenge configuration]").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand("/challengex info"))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal("Show every trigger, effect, and modifier in this challenge"))));

        List<Component> results = results(won, together, completions, run.eliminated());

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Under a per-player win only the finishers won; for everybody else
            // the run ended without them, so they are told as much.
            boolean celebrates = won && (together || finished(completions, player.getScoreboardName()));
            ChatFormatting color = celebrates ? ChatFormatting.GREEN : ChatFormatting.RED;
            SoundEvent sound = celebrates ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE
                    : SoundEvents.ANVIL_LAND;
            float pitch = celebrates ? 1.0f : 0.8f;

            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(titleText).withStyle(color)));
            player.sendSystemMessage(Component.literal(chatText).withStyle(color));
            results.forEach(player::sendSystemMessage);
            player.sendSystemMessage(viewConfig);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.MASTER, 1.0f, pitch);
        }
    }

    /**
     * The time the run is reported at: the moment the last player finished when
     * anybody did, so a group's time is the finish that completed it rather than
     * whenever the clock happened to stop afterwards. A run nobody finished
     * reports the clock itself.
     */
    private static long groupTicks(ChallengeRun run, List<Completion> completions) {
        return completions.isEmpty() ? run.elapsedTicks() : completions.getLast().atTick();
    }

    private static boolean finished(List<Completion> completions, String name) {
        return completions.stream().anyMatch(completion -> completion.playerId().equals(name));
    }

    private static List<Component> results(boolean won, boolean together,
            List<Completion> completions, Set<String> eliminated) {
        List<Component> lines = new ArrayList<>();
        // A shared win has already been reported as one group time; listing the
        // same finishes again would say nothing new.
        if (!together && !completions.isEmpty()) {
            for (int place = 0; place < completions.size(); place++) {
                Completion completion = completions.get(place);
                lines.add(Component.literal("  " + (place + 1) + ". " + completion.playerId()
                        + " — " + RunClock.format(completion.atTick()))
                        .withStyle(place == 0 ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            }
        }
        if (!eliminated.isEmpty()) {
            lines.add(Component.literal("  Out: " + String.join(", ", eliminated.stream().sorted().toList()))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (won && together && completions.size() > 1) {
            lines.add(Component.literal("  Everyone finished.").withStyle(ChatFormatting.GRAY));
        }
        return List.copyOf(lines);
    }
}
