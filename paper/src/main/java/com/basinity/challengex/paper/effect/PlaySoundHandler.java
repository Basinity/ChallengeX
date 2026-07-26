package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code effect.play_sound}: plays the named sound at each target's position. */
public final class PlaySoundHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaySoundHandler.class);

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        String soundId = EffectParams.string(command, "sound");
        if (soundId == null) {
            LOGGER.warn("play_sound is missing its sound id; skipping.");
            return;
        }
        Sound sound = GameIds.sound(soundId);
        if (sound == null) {
            LOGGER.warn("Unknown sound {}; skipping.", soundId);
            return;
        }
        for (Player target : targets) {
            target.getWorld().playSound(target.getLocation(), sound, SoundCategory.MASTER, 1.0f, 1.0f);
        }
    }
}
