package io.github.frqnny.omegaconfig.api;

import io.github.frqnny.omegaconfig.OmegaConfig;
import net.minecraft.nbt.NbtCompound;

import java.util.Arrays;

public interface Config {

    /**
     * Writes this configuration file instance to disk.
     * Useful for saving modified values during runtime.
     */
    default void save() {
        OmegaConfig.writeConfig((Class<Config>) getClass(), this);
    }

    default NbtCompound writeSyncingTag() {
        NbtCompound tag = new NbtCompound();
        tag.putString("ConfigName", getName());

        // all config vs. individual fields
        if (Arrays.stream(getClass().getAnnotations()).anyMatch(annotation -> annotation instanceof Syncing)) {
            // write ALL fields to tag
            String json = OmegaConfig.GSON.toJson(this);
            tag.putString("Serialized", json);
            tag.putBoolean("AllSync", true);
        } else {
            // write all syncable fields to tag
            String json = OmegaConfig.SYNC_ONLY_GSON.toJson(this);
            tag.putString("Serialized", json);
            tag.putBoolean("AllSync", false);
        }

        return tag;
    }

    /**
     * @return true if this {@link Config} has any values that should be synced to the client
     */
    default boolean hasAnySyncable() {
        boolean hasSyncingField = Arrays.stream(getClass().getDeclaredFields()).anyMatch(field -> Arrays.stream(field.getDeclaredAnnotations()).anyMatch(annotation -> annotation instanceof Syncing));
        boolean classSyncs = Arrays.stream(getClass().getDeclaredAnnotations()).anyMatch(annotation -> annotation instanceof Syncing);
        return hasSyncingField || classSyncs;
    }

    /**
     * Returns the name of this config, which is used for the name of the config file saved to disk, and syncing.
     *
     * <p>
     * The name returned by this method should generally follow Identifier conventions, but this is not enforced:
     * <ul>
     *     <li>Lowercase
     *     <li>No special characters ($, %, ^, etc.)
     *     <li>No spaces
     *
     * @return the name of this config, which is used for the name of the config file saved to disk.
     */
    String getName();

    /**
     * Returns the file extension of this config.
     *
     * <p>
     * The file extension is used while serializing this config to a local file.
     * The primary use-case of switching this would be supporting existing config files
     * when porting from other json5 config libraries.
     *
     * @return the file extension of this config
     */
    default String getExtension() {
        return "json";
    }

    /**
     * Returns the directory of this config, assuming the base directory is the instance config directory.
     *
     * <p>
     * By default, a config such as 'my_config' will appear at /config/my_config.json.
     * If this method specifies a directory, such as 'configurations',
     * the config file will appear at /config/configurations/my_config.json.
     * <p>
     * Nested directories can be specified by using a string such as 'configurations/client'.
     *
     * @return the directory of this config
     */
    default String getDirectory() {
        return "";
    }

    /**
     * A method called when the configuration has been synced from the server to the client.
     * Override it to have custom logic run when the configuration is synced.
     */
    default void onConfigSynced() {

    }
}
