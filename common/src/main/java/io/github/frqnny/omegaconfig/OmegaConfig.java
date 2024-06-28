package io.github.frqnny.omegaconfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import io.github.frqnny.omegaconfig.api.Comment;
import io.github.frqnny.omegaconfig.api.Config;
import io.github.frqnny.omegaconfig.gson.SyncableExclusionStrategy;
import io.github.frqnny.omegaconfig.util.Utils;
import net.fabricmc.api.EnvType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class OmegaConfig {
    public static final String MOD_ID = "omegaconfig";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Gson SYNC_ONLY_GSON = new GsonBuilder().addSerializationExclusionStrategy(new SyncableExclusionStrategy()).setPrettyPrinting().create();
    public static final Logger LOGGER = LogManager.getLogger();
    private static final List<Config> REGISTERED_CONFIGURATIONS = new ArrayList<>();

    public static void init() {
        if (Platform.getEnv() == EnvType.SERVER) {
            NetworkManager.registerS2CPayloadType(SyncConfigPayload.ID, SyncConfigPayload.CODEC);
        }
        PlayerEvent.PLAYER_JOIN.register(serverPlayer -> {
            var server = serverPlayer.server;
            if (server instanceof IntegratedServer integratedServer && !integratedServer.isRemote()) {
                return; // do not sync config in an integrated server, unless it's open to connections
            }
            server.execute(() -> {
                NbtCompound root = new NbtCompound();
                NbtList configurations = new NbtList();

                // Iterate over each configuration.
                // Find values that should be synced and send the value over.
                OmegaConfig.getRegisteredConfigurations().forEach(config -> {
                    if (config.hasAnySyncable()) {
                        configurations.add(config.writeSyncingTag());
                    }
                });

                // save to packet and send to user
                root.put("Configurations", configurations);
                NetworkManager.sendToPlayer(serverPlayer, new SyncConfigPayload(root));
            });
        });
    }


    public static <T extends Config> T register(Class<T> configClass) {
        try {
            // Attempt to instantiate a new instance of this class.
            T config = configClass.getDeclaredConstructor().newInstance();

            // Exceptions will have been thrown at this point.
            // We want to provide access to the config as soon as it is created, so we:
            //    1. serialize to disk if the config does not already exist
            //    2. read from disk if it does exist
            if (!configExists(config)) {
                config.save();
                REGISTERED_CONFIGURATIONS.add(config);
            } else {
                try {
                    // Read from the disk config file to populate the correct values into our config object.
                    List<String> lines = Files.readAllLines(Utils.getConfigFolder(config));
                    lines.removeIf(line -> line.trim().startsWith("//"));
                    StringBuilder res = new StringBuilder();
                    lines.forEach(res::append);
                    T object = GSON.fromJson(res.toString(), configClass);

                    // re-write the config to add new values
                    object.save();
                    REGISTERED_CONFIGURATIONS.add(object);
                    return object;
                } catch (Exception e) {
                    LOGGER.error(e);
                    LOGGER.info(String.format("Encountered an error while reading %s config, falling back to default values.", config.getName()));
                    LOGGER.info(String.format("If this problem persists, delete the config file %s and try again.", config.getName() + "." + config.getExtension()));
                }
            }

            return config;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException exception) {
            LOGGER.error(exception);
            throw new RuntimeException("No valid constructor found for: " + configClass.getName());
        }
    }

    public static <T extends Config> void writeConfig(Class<T> configClass, T instance) {
        // Write the config to disk with the default values.
        String json = GSON.toJson(instance);

        // Cursed time.
        List<String> lines = new ArrayList<>(Arrays.asList(json.split("\n")));
        Map<Integer, String> insertions = new TreeMap<>();
        Map<String, String> keyToComments = new HashMap<>();

        // populate key -> comments map
        for (Field field : configClass.getDeclaredFields()) {
            addFieldComments(field, keyToComments);
        }

        // "flattens" all the inner classes of the Config class into a single list
        for (Class<?> clazz : flatten(configClass.getDeclaredClasses())) {
            for (Field field : clazz.getDeclaredFields()) {
                addFieldComments(field, keyToComments);
            }
        }

        // Find areas we should insert comments into...
        for (int i = 0; i < lines.size(); i++) {
            String at = lines.get(i);
            String startingWhitespace = getStartingWhitespace(at);

            for (var entry : keyToComments.entrySet()) {
                String comment = entry.getValue();
                // Check if we should insert comment
                if (at.trim().startsWith(String.format("\"%s\"", entry.getKey()))) {
                    if (comment.contains("\n")) {
                        comment = startingWhitespace + "//" + String.join(String.format("\n%s//", startingWhitespace), comment.split("\n"));
                    } else {
                        comment = String.format("%s//%s", startingWhitespace, comment);
                    }
                    insertions.put(i + insertions.size(), comment);
                    break;
                }
            }
        }

        // insertions -> list
        for (var entry : insertions.entrySet()) {
            Integer key = entry.getKey();
            String value = entry.getValue();
            lines.add(key, value);
        }

        // list -> string
        StringBuilder res = new StringBuilder();
        lines.forEach(str -> res.append(String.format("%s%n", str)));

        try {
            Path configPath = Utils.getConfigFolder(instance);
            configPath.toFile().getParentFile().mkdirs();
            Files.write(configPath, res.toString().getBytes());
        } catch (IOException ioException) {
            LOGGER.error(ioException);
            LOGGER.info(String.format("Write error, using default values for config %s.", configClass));
        }
    }

    public static List<Class<?>> flatten(Class<?>[] array) {
        List<Class<?>> list = new ArrayList<>();

        for (Class<?> clazz : array) {
            populateRecursively(list, clazz);
        }

        return list;
    }

    private static void populateRecursively(List<Class<?>> list, Class<?> aClass) {
        list.add(aClass);

        Class<?>[] classes = aClass.getDeclaredClasses();

        for (Class<?> clazz : classes) {
            populateRecursively(list, clazz);
        }
    }

    private static void addFieldComments(Field field, Map<String, String> keyToComments) {
        String fieldName = field.getName();
        Annotation[] annotations = field.getDeclaredAnnotations();

        // Find comment
        for (Annotation annotation : annotations) {
            if (annotation instanceof Comment) {
                keyToComments.put(fieldName, ((Comment) annotation).value());
                break;
            }
        }
    }

    /**
     * Returns a string with the left-side whitespace characters of the given input, up till the first non-whitespace character.
     *
     * <p>
     * "   hello" -> "   "
     * "p" -> ""
     * " p" -> " "
     *
     * @param input input to retrieve whitespaces from
     * @return starting whitespaces from the given input
     */
    private static String getStartingWhitespace(String input) {
        int index = -1;

        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char at = chars[i];

            if (at != ' ') {
                index = i;
                break;
            }
        }

        if (index != -1) {
            return input.substring(0, index);
        } else {
            return "";
        }
    }

    public static boolean configExists(Config config) {
        return Files.exists(Utils.getConfigFolder(config));
    }

    public static List<Config> getRegisteredConfigurations() {
        return REGISTERED_CONFIGURATIONS;
    }

    public record SyncConfigPayload(NbtCompound nbtCompound) implements CustomPayload {
        public static final Identifier CONFIG_SYNC_PACKET = Identifier.of(OmegaConfig.MOD_ID, "sync");
        public static final CustomPayload.Id<SyncConfigPayload> ID = new Id<>(CONFIG_SYNC_PACKET);
        public static final PacketCodec<PacketByteBuf, SyncConfigPayload> CODEC = PacketCodecs.NBT_COMPOUND.xmap(SyncConfigPayload::new, SyncConfigPayload::nbtCompound).cast();

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

}
