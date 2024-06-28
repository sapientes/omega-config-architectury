package io.github.frqnny.omegaconfig;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.networking.NetworkManager;
import io.github.frqnny.omegaconfig.api.Config;
import io.github.frqnny.omegaconfig.api.Syncing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OmegaConfigClient {
    public static final List<Config> savedClientConfig = new ArrayList<>(); // stored config from before sync is applied


    public static void init() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OmegaConfig.SyncConfigPayload.ID, OmegaConfig.SyncConfigPayload.CODEC, (payload, context) -> {

            NbtCompound tag = payload.nbtCompound();
            savedClientConfig.clear();

            MinecraftClient.getInstance().execute(() -> {
                if (tag != null && tag.contains("Configurations")) {
                    NbtList list = tag.getList("Configurations", NbtElement.COMPOUND_TYPE);
                    list.forEach(compound -> {
                        NbtCompound syncedConfiguration = (NbtCompound) compound;
                        String name = syncedConfiguration.getString("ConfigName");
                        String json = syncedConfiguration.getString("Serialized");
                        boolean allSync = syncedConfiguration.getBoolean("AllSync");

                        // find configuration class by name
                        for (Config config : OmegaConfig.getRegisteredConfigurations()) {
                            if (config.getName().equals(name)) {
                                // bring values from server to object
                                Config serverConfig = OmegaConfig.GSON.fromJson(json, config.getClass());

                                // deep-copy original config & save it for a restore later
                                Config cachedClient = OmegaConfig.GSON.fromJson(OmegaConfig.GSON.toJson(config), config.getClass());
                                savedClientConfig.add(cachedClient);

                                // locate all fields that differ between the client and server, assign values from server to client (this will mutate the stored object)
                                for (Field field : serverConfig.getClass().getDeclaredFields()) {
                                    if (allSync || Arrays.stream(field.getAnnotations()).anyMatch(annotation -> annotation instanceof Syncing)) {
                                        try {
                                            field.setAccessible(true);
                                            Object serverValue = field.get(serverConfig);
                                            field.set(config, serverValue);
                                        } catch (IllegalAccessException e) {
                                            OmegaConfig.LOGGER.error(e);
                                        }
                                    }
                                }
                                OmegaConfig.LOGGER.info("Received {} configuration from server.", name);
                                serverConfig.onConfigSynced();
                                break;
                            }
                        }
                    });
                }
            });
        });


        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> {
            for (Config config : savedClientConfig) {
                for (Config potentiallySynced : OmegaConfig.getRegisteredConfigurations()) {
                    if (config.getName().equals(potentiallySynced.getName())) {
                        boolean allConfigSyncs = Arrays.stream(config.getClass().getAnnotations()).anyMatch(annotation -> annotation instanceof Syncing);

                        // mutate object in registered configurations
                        for (Field field : config.getClass().getDeclaredFields()) {
                            if (allConfigSyncs || Arrays.stream(field.getAnnotations()).anyMatch(annotation -> annotation instanceof Syncing)) {
                                try {
                                    field.setAccessible(true);
                                    Object preSyncValue = field.get(config);
                                    field.set(potentiallySynced, preSyncValue);
                                } catch (IllegalAccessException e) {
                                    OmegaConfig.LOGGER.error(e);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
