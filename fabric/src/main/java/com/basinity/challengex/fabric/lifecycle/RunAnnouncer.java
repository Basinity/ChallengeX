package com.basinity.challengex.fabric.lifecycle;

import com.basinity.challengex.common.lifecycle.RunAnnouncement;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.fabric.text.Lines;
import java.util.List;
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
 * <p>What the run says is {@link RunAnnouncement}'s call, including who is being
 * congratulated. This draws it: the title packet, the sound, and the clickable
 * control are all Fabric's own.
 */
public final class RunAnnouncer {

    private RunAnnouncer() {
    }

    public static void announce(MinecraftServer server, ChallengeRun run) {
        RunAnnouncement announcement = RunAnnouncement.of(run);

        Component viewConfig = Component.literal("[View challenge configuration]").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand("/challengex info"))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal("Show every trigger, effect, and modifier in this challenge"))));

        List<Component> results = announcement.results().stream().map(Lines::render).toList();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean celebrates = announcement.celebrates(player.getScoreboardName());
            ChatFormatting color = celebrates ? ChatFormatting.GREEN : ChatFormatting.RED;
            SoundEvent sound = celebrates ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE
                    : SoundEvents.ANVIL_LAND;
            float pitch = celebrates ? 1.0f : 0.8f;

            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(announcement.title()).withStyle(color)));
            player.sendSystemMessage(Component.literal(announcement.chatLine()).withStyle(color));
            results.forEach(player::sendSystemMessage);
            player.sendSystemMessage(viewConfig);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    sound, SoundSource.MASTER, 1.0f, pitch);
        }
    }
}
