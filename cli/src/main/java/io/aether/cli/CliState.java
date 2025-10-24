package io.aether.cli;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.aether.logger.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the persistent JSON state file (~/.aether-cli-state.json)
 * for storing user preferences like aliases.
 */
public class CliState {
    private static final String STATE_FILE_NAME = ".aether-cli-state.json";
    private final Path stateFilePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Эта структура будет напрямую сериализована в/из JSON
    public static class StateData {
        public Map<String, String> aliases = new ConcurrentHashMap<>();
    }

    private StateData data = new StateData();

    public CliState() {
        this.stateFilePath = Paths.get(System.getProperty("user.home"), STATE_FILE_NAME);
    }

    /**
     * Loads the state from the JSON file on disk.
     */
    public void load() {
        try {
            if (Files.exists(stateFilePath)) {
                String json = Files.readString(stateFilePath);
                if (json.isBlank()) {
                    Log.warn("CLI state file is empty, initializing new one.");
                    save(); // Save empty default state
                    return;
                }

                // Парсим JSON
                StateData loadedData = gson.fromJson(json, StateData.class);

                if (loadedData != null) {
                    this.data = loadedData;
                    if (this.data.aliases == null) {
                        this.data.aliases = new ConcurrentHashMap<>();
                    }
                    Log.info("Loaded " + this.data.aliases.size() + " user aliases from " + stateFilePath);
                }
            } else {
                Log.info("No CLI state file found, creating a new one at " + stateFilePath);
                save(); // Create the file with default (empty) content
            }
        } catch (Exception e) {
            Log.error("Failed to load CLI state from " + stateFilePath + ". Error: " + e.getMessage());
        }
    }

    /**
     * Saves the current state (including all aliases) to the JSON file.
     */
    public synchronized void save() {
        try {
            String json = gson.toJson(this.data);
            Files.writeString(stateFilePath, json);
        } catch (Exception e) {
            Log.error("Failed to save CLI state to " + stateFilePath, e);
        }
    }

    /**
     * Adds a new alias and persists it to disk.
     * @param alias The alias name.
     * @param uuid The UUID string.
     */
    public void addAlias(String alias, String uuid) {
        if (alias == null || alias.isBlank() || uuid == null || uuid.isBlank()) {
            return;
        }
        data.aliases.put(alias, uuid);
        save();
    }

    /**
     * @return The map of user-defined aliases.
     */
    public Map<String, String> getAliases() {
        return data.aliases;
    }

    /**
     * Checks if a given alias exists.
     * @param alias The alias name.
     * @return true if the alias exists.
     */
    public boolean hasAlias(String alias) {
        return data.aliases.containsKey(alias);
    }

    /**
     * Gets the UUID string for a given alias.
     * @param alias The alias name.
     * @return The UUID string or null if not found.
     */
    public String getUuidForAlias(String alias) {
        return data.aliases.get(alias);
    }
}