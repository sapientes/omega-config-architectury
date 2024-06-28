package io.github.frqnny.omegaconfig.neoforge;

import io.github.frqnny.omegaconfig.OmegaConfig;
import io.github.frqnny.omegaconfig.OmegaConfigClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = OmegaConfig.MOD_ID, dist = Dist.CLIENT)
public class OmegaConfigNeoForgeClient {
    public OmegaConfigNeoForgeClient() {
        OmegaConfigClient.init();
    }
}
