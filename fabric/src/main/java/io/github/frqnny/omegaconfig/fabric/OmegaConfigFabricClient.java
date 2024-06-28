package io.github.frqnny.omegaconfig.fabric;

import io.github.frqnny.omegaconfig.OmegaConfigClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class OmegaConfigFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        OmegaConfigClient.init();
    }
}
