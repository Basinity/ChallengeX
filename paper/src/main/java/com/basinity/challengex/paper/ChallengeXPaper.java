package com.basinity.challengex.paper;

import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.Registries;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Paper adapter's entrypoint, the Bukkit-side counterpart of
 * {@code ChallengeXFabric}. A walking skeleton for now: it proves the plugin
 * loads and that {@code core} is packaged inside the plugin jar, which is the
 * part of the scaffold that can silently be wrong.
 */
public class ChallengeXPaper extends JavaPlugin {

    @Override
    public void onEnable() {
        Registries registries = CoreCatalog.createRegistries();
        getLogger().info("ChallengeX initialized: %d triggers, %d effects, %d modifiers."
                .formatted(registries.triggers().all().size(),
                        registries.effects().all().size(),
                        registries.modifiers().all().size()));
    }
}
