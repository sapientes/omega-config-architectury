package io.github.frqnny.omegaconfig.util;

import dev.architectury.platform.Platform;
import io.github.frqnny.omegaconfig.api.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Utils {

    public static Path getConfigFolder(Config config) {
        return Paths.get(Platform.getConfigFolder().toString(), config.getDirectory(), String.format("%s.%s", config.getName(), config.getExtension()));
    }
}
