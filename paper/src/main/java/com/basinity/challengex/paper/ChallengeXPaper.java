package com.basinity.challengex.paper;

import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.paper.trigger.TriggerSource;
import com.basinity.challengex.paper.trigger.TriggerSources;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The Paper adapter's entrypoint, the Bukkit-side counterpart of
 * {@code ChallengeXFabric}. It owns the active run and the effect executor the
 * engine fires effects back through.
 *
 * <p>Trigger sources, modifier enforcement, and the command tree are not wired
 * yet, so nothing yet feeds the run: the active challenge starts empty and
 * stays that way until the command tree can import a preset.
 */
public class ChallengeXPaper extends JavaPlugin {

    private static ChallengeXPaper instance;

    private ChallengeRun activeRun;

    @Override
    public void onEnable() {
        instance = this;
        loadChallenge(Challenge.empty());
        registerTriggerSources();
        getSLF4JLogger().info("ChallengeX initialized.");
    }

    private void registerTriggerSources() {
        TriggerContext context = new PaperTriggerContext(() -> activeRun);
        for (TriggerSource source : TriggerSources.byId().values()) {
            source.register(context, this);
        }
    }

    @Override
    public void onDisable() {
        activeRun = null;
        instance = null;
    }

    /** Swaps the active run to a fresh run of the given challenge. */
    public void loadChallenge(Challenge challenge) {
        activeRun = new ChallengeRun(challenge, CoreCatalog.createRegistries(),
                new PaperEffectExecutor(getServer(), getSLF4JLogger()));
    }

    public static ChallengeXPaper instance() {
        return instance;
    }

    /** The active run, or null before the plugin has enabled. */
    public ChallengeRun activeRun() {
        return activeRun;
    }
}
