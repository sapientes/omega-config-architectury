package io.github.frqnny.omegaconfig.neoforge;

import net.neoforged.fml.common.Mod;

import io.github.frqnny.omegaconfig.OmegaConfig;

@Mod(OmegaConfig.MOD_ID)
public final class OmegaConfigNeoForge {
    public OmegaConfigNeoForge() {
        // Run our common setup.
        OmegaConfig.init();
    }
}
