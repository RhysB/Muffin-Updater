package com.johnymuffin.beta.updater;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.johnymuffin.beta.updater.ManifestFetcher.getManifest;

public class MuffinUpdater extends JavaPlugin {
    private MuffinUpdater plugin;
    private Logger log;
    private String pluginName;
    private PluginDescriptionFile pdf;
    private JSONArray jsonManifest;
    private boolean debug = true;
    private MuffinUpdaterConfig muffinUpdaterConfig;

    @Override
    public void onEnable() {
        plugin = this;
        log = this.getServer().getLogger();
        pdf = this.getDescription();
        pluginName = pdf.getName();
        log.info("[" + pluginName + "] Is Loading, Version: " + pdf.getVersion());

        muffinUpdaterConfig = new MuffinUpdaterConfig(plugin);
        debug = muffinUpdaterConfig.isDebugModeEnabled();
        this.getCommand("muffinupdater").setExecutor(new MuffinUpdaterCommand(plugin));


        jsonManifest = getManifest(plugin, "https://api.johnymuffin.com/updater/manifest.json", false);
        if (jsonManifest == null) {
            Bukkit.getServer().getPluginManager().disablePlugin(this);
            return;
        }
        logInfo(Level.INFO, "Fetched update manifest successfully");
        //Ensure function runs after server loading
        Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            scanPlugins();
            muffinUpdaterConfig.saveConfig();
        }, 0);


    }

    @Override
    public void onDisable() {
        logInfo(Level.INFO, "Disabling");
        if (jsonManifest == null) {
            return;
        }
        muffinUpdaterConfig.serverShutdown();

    }

    public boolean isAutomaticUpdateEnabled(String pluginName) {
        return muffinUpdaterConfig.isPluginUpdatingEnabled(pluginName);
    }

    private void scanPlugins() {
        //Loop through all plugins
        for (Plugin p : Bukkit.getServer().getPluginManager().getPlugins()) {
            //Check if plugin is in manifest
            for (int i = 0; i < jsonManifest.size(); i++) {
                JSONObject tmp = (JSONObject) jsonManifest.get(i);
                //Plugin is in the manifest
                if (((String) tmp.get("pluginName")).equals(p.getDescription().getName())) {
                    checkForUpdates(p, ((String) tmp.get("newestVersion")), ((String) tmp.get("url")));
                }
            }
        }
    }


    public void checkUpdateCommand(String updateRequester) {
        final JSONArray temp = getManifest(plugin, "https://api.johnymuffin.com/updater/manifest.json", false);
        if (temp == null) {
            logInfo(Level.WARNING, "MuffinUpdater was unable to fetch the update manifest");
            return;
        }
        logInfo(Level.INFO, "Checking for plugin updates as request by: " + updateRequester);
        scanPlugins();
    }


    public void logInfo(Level level, String text) {
        pluginName = pdf.getName();
        plugin.getServer().getLogger().log(level, "[" + pluginName + "] " + text);
    }


    private void checkForUpdates(Plugin p, String manifestVersion, String downloadURL) {
        if(!muffinUpdaterConfig.isPluginUpdatingEnabled(p.getDescription().getName())) {
            logInfo(Level.INFO, "Automatic updating for \"" + p.getDescription().getName() + "\" is disabled. It can be enabled in the MuffinUpdater config.");
            return;
        }


        int diff = compareVersions(p.getDescription().getVersion(), manifestVersion);
        //Check Difference, return if server has a current or newest version
        if (!(diff < 0)) {
            if (debug) {
                logInfo(Level.INFO, "No update is available for: " + p.getDescription().getName());
            }
            return;
        }
        logInfo(Level.INFO, "The plugin " + p.getDescription().getName() + " has an update. Downloading version " + manifestVersion + " to replace " + p.getDescription().getVersion());
        handleUpdate(p, downloadURL);


    }


    private void handleUpdate(Plugin p, String downloadURL) {
        final File pluginFile = new File(p.getClass().getProtectionDomain().getCodeSource().getLocation().getFile());
        if (validateFileExistence(pluginFile)) {
            logInfo(Level.INFO, "Unable to locate the jar " + pluginFile.getName() + " for the plugin \"" + p.getDescription().getName() + "\", skipping.");
            return;
        }
        File downloadLocation = new File(plugin.getDataFolder().getParent() + File.separator + "update" + File.separator + pluginFile.getName());
        if (downloadLocation.exists()) {
            logInfo(Level.INFO, "A jar called " + pluginFile.getName() + " is already present in the updates folder, skipping.");
            return;
        }
        try {
            URL url = new URL(downloadURL);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setConnectTimeout(10 * 1000);
            httpURLConnection.setReadTimeout(60 * 1000);

            InputStream is = httpURLConnection.getInputStream();
            downloadLocation.getParentFile().mkdirs();
            downloadLocation.createNewFile();
            OutputStream os = new FileOutputStream(downloadLocation);
            byte data[] = new byte[1024];
            int count;
            while ((count = is.read(data, 0, 1024)) != -1) {
                os.write(data, 0, count);
            }
            os.flush();
            is.close();
            os.close();
            logInfo(Level.INFO, "Downloaded the update for " + p.getDescription().getName() + ". The update should be automatically applied by Bukkit on next restart");

        } catch (IOException e) {
            if (downloadLocation.exists()) {
                downloadLocation.delete();
            }
            if (debug) {
                logInfo(Level.WARNING, "Downloading the updated version of " + p.getDescription().getName() + " failed: " + e + " - " + e.getMessage());
            } else {
                logInfo(Level.WARNING, "Downloading the updated version of " + p.getDescription().getName() + " failed");
            }
        }
    }


    private boolean validateFileExistence(File file) {
        String fileName = file.getName();
        File temp = new File(plugin.getDataFolder().getParent(), fileName + ".jar");
        if (temp.exists()) {
            return true;
        }
        return false;
    }


    private int compareVersions(String version1, String version2) {

        String[] levels1 = version1.split("\\.");
        String[] levels2 = version2.split("\\.");

        int length = Math.max(levels1.length, levels2.length);
        for (int i = 0; i < length; i++) {
            Integer v1 = i < levels1.length ? Integer.parseInt(levels1[i]) : 0;
            Integer v2 = i < levels2.length ? Integer.parseInt(levels2[i]) : 0;
            int compare = v1.compareTo(v2);
            if (compare != 0) {
                return compare;
            }
        }

        return 0;
    }


}
