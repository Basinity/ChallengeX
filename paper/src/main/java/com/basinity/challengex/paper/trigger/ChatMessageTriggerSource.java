package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Map;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * {@code trigger.chat_message}: a player sent a chat message. The
 * {@code message} parameter matches the message in full, exactly as typed;
 * omitting it fires on any message. Commands are not chat and never reach it.
 *
 * <p>Paper delivers chat off the main thread, so unlike every other source this
 * one hops back before emitting: the engine and everything an effect touches
 * are main-thread-only.
 */
public final class ChatMessageTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChatMessage(AsyncChatEvent event) {
        String name = event.getPlayer().getName();
        String text = PlainTextComponentSerializer.plainText().serialize(event.message());
        emitOnMainThread(() -> context().emit(GameEvent.of("trigger.chat_message", name,
                Map.of("message", ParamValue.of(text)))));
    }
}
