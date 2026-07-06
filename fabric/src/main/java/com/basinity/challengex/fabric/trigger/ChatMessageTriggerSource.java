package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

/**
 * {@code trigger.chat_message}: a player sent a chat message. The
 * {@code message} parameter matches the message in full, exactly as typed;
 * omitting it fires on any message. Commands are not chat and never reach it.
 */
public final class ChatMessageTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, type) ->
                context.emit(GameEvent.of("trigger.chat_message", sender.getScoreboardName(),
                        Map.of("message", ParamValue.of(message.signedContent())))));
    }
}
