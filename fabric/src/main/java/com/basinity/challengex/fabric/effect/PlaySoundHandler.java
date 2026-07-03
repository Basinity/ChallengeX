package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code effect.play_sound}: plays the named sound at each target's position. */
public final class PlaySoundHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaySoundHandler.class);

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        String soundId = EffectParams.string(command, "sound");
        if (soundId == null) {
            LOGGER.warn("play_sound is missing its sound id; skipping.");
            return;
        }
        Identifier id = Identifier.tryParse(soundId);
        Holder<SoundEvent> sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id).orElse(null);
        if (sound == null) {
            LOGGER.warn("Unknown sound {}; skipping.", soundId);
            return;
        }
        for (ServerPlayer target : targets) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    sound, SoundSource.MASTER, 1.0f, 1.0f);
        }
    }
}
