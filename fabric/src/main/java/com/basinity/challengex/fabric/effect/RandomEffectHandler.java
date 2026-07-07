package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * {@code effect.random_effect}: applies a random status effect to each target.
 * The {@code type} parameter narrows the pool to {@code negative} (harmful) or
 * {@code positive} (beneficial); omitting it, or {@code any}, draws from all.
 * Each target rolls its own effect.
 */
public final class RandomEffectHandler implements EffectHandler {

    private static final int DURATION_TICKS = 300;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        String type = EffectParams.string(command, "type");
        List<MobEffect> pool = BuiltInRegistries.MOB_EFFECT.stream()
                .filter(effect -> matchesType(effect, type))
                .toList();
        if (pool.isEmpty()) {
            return;
        }
        for (ServerPlayer target : targets) {
            MobEffect chosen = pool.get(target.getRandom().nextInt(pool.size()));
            Holder<MobEffect> holder = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(chosen);
            target.addEffect(new MobEffectInstance(holder, DURATION_TICKS, 0));
        }
    }

    private static boolean matchesType(MobEffect effect, String type) {
        if (type == null) {
            return true;
        }
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "negative" -> effect.getCategory() == MobEffectCategory.HARMFUL;
            case "positive" -> effect.getCategory() == MobEffectCategory.BENEFICIAL;
            default -> true;
        };
    }
}
