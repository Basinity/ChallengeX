package com.basinity.challengex.fabric.lifecycle;

import com.basinity.challengex.core.engine.RunOutcome;
import java.util.Optional;
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
 * Surfaces a finished run to every player: a big on-screen title (no time
 * beneath it), a chat line stating the outcome and the final time with a
 * clickable control that prints the full challenge composition ({@code
 * /challengex info}), and a win/loss sound.
 */
public final class RunAnnouncer {

    private RunAnnouncer() {
    }

    public static void announce(MinecraftServer server, RunOutcome outcome, long elapsedTicks,
            Optional<String> winner) {
        boolean won = outcome == RunOutcome.WIN;
        String time = RunClock.format(elapsedTicks);
        // A versus win names its winner; a shared win stays the collective line.
        String titleText = !won ? "Challenge Failed"
                : winner.map(name -> name + " Wins").orElse("Challenge Complete");
        String chatText = !won ? "Challenge failed — " + time
                : winner.map(name -> "Challenge won by " + name + " — " + time)
                        .orElse("Challenge complete — " + time);
        Component title = Component.literal(titleText)
                .withStyle(won ? ChatFormatting.GREEN : ChatFormatting.RED);
        Component chatLine = Component.literal(chatText)
                .withStyle(won ? ChatFormatting.GREEN : ChatFormatting.RED);
        Component viewConfig = Component.literal("[View challenge configuration]").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand("/challengex info"))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal("Show every trigger, effect, goal, and modifier in this challenge"))));

        SoundEvent sound = won ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ANVIL_LAND;
        float pitch = won ? 1.0f : 0.8f;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.sendSystemMessage(chatLine);
            player.sendSystemMessage(viewConfig);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.MASTER, 1.0f, pitch);
        }
    }
}
