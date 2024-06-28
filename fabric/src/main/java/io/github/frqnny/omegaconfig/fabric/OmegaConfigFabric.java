package io.github.frqnny.omegaconfig.fabric;

import io.github.frqnny.omegaconfig.OmegaConfig;
import net.fabricmc.api.ModInitializer;

public final class OmegaConfigFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        OmegaConfig.init();
    }
}
