package com.johnymuffin.beta.updater;

import org.bukkit.util.config.Configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MuffinUpdaterConfig {
    private MuffinUpdater plugin;
    private HashMap<String, Boolean> enabledPlugins = new HashMap<String, Boolean>();
    private Configuration config;
    private ArrayList<String> unknownPlugins = new ArrayList<String>();

    public MuffinUpdaterConfig(MuffinUpdater plugin) {
        this.plugin = plugin;

        config = new Configuration(new File(plugin.getDataFolder(), "config.yml"));
        config.load();
        isDebugModeEnabled();
        if (config.getProperty("updatePlugins") == null) {
            config.setProperty("updatePlugins", (Object) new HashMap());
        }

        enabledPlugins = (HashMap<String, Boolean>) config.getProperty("updatePlugins");

    }

    public boolean isDebugModeEnabled() {
        String key = "debugMode";
        if(config.getProperty(key) == null) {
            config.setProperty(key, false);
        }
        return config.getBoolean(key, false);
    }

    public boolean isPluginUpdatingEnabled(String pluginName) {
        if (enabledPlugins.containsKey(pluginName)) {
            return enabledPlugins.get(pluginName);
        }
        unknownPlugins.add(pluginName);
        return false;
    }


    public void serverShutdown() {
        for (String s : unknownPlugins) {
            if(s.equals(plugin.getDescription().getName())) {
                enabledPlugins.put(s, true);
            } else {
                enabledPlugins.put(s, false);
            }
        }
        saveConfig();
    }

    public void saveConfig() {
        config.setProperty("updatePlugins", enabledPlugins);
        config.save();
    }

}
