package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code trigger.advancement_earned}: a player completed a proper advancement. It
 * rides the criterion award: the award returns true only when it grants a
 * criterion the player did not have, so firing on a true return with the
 * advancement now done catches the grant that completes it, once. Only the five
 * real advancement trees count ({@code story}, {@code nether}, {@code end},
 * {@code adventure}, {@code husbandry}); recipe unlocks and other hidden
 * advancements are skipped. The {@code advancement} parameter matches the
 * completed advancement's id, which the earn-advancement goal matches against.
 */
@Mixin(PlayerAdvancements.class)
public class AdvancementEarnedMixin {

    private static final Set<String> CLASSICAL_TREES =
            Set.of("story", "nether", "end", "adventure", "husbandry");

    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At("RETURN"))
    private void challengex$onAward(AdvancementHolder advancement, String criterion,
            CallbackInfoReturnable<Boolean> info) {
        if (!Boolean.TRUE.equals(info.getReturnValue())) {
            return;
        }
        PlayerAdvancements self = (PlayerAdvancements) (Object) this;
        if (!self.getOrStartProgress(advancement).isDone() || !isClassical(advancement.id())) {
            return;
        }
        MixinTriggerBridge.emit(GameEvent.of("trigger.advancement_earned", player.getScoreboardName(),
                Map.of("advancement", ParamValue.of(advancement.id().toString()))));
    }

    /** True for advancements in one of the five real trees, excluding recipes. */
    private static boolean isClassical(Identifier id) {
        String path = id.getPath();
        int slash = path.indexOf('/');
        return slash > 0 && CLASSICAL_TREES.contains(path.substring(0, slash));
    }
}
