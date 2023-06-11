package com.github.hhhzzzsss.songplayer;

import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Config {
    private static Config config = null;

    public static final File CONFIG_FILE = new File(SongPlayer.SONGPLAYER_DIR, "config.json");
    private static final Gson gson = new Gson();

    public String prefix = "$";
    public String creativeCommand = "gmc";
    public String survivalCommand = "gms";
    public boolean showFakePlayer = false;
    public boolean loopPlaylists;
    public boolean shufflePlaylists;

    public static Config getConfig() {
        if (config == null) {
            config = new Config();
            try {
                if (CONFIG_FILE.exists()) {
                    loadConfig();
                }
                else {
                    saveConfig();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return config;
    }

    public static void loadConfig() throws IOException {
        FileInputStream fis = new FileInputStream(CONFIG_FILE);
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(isr);
        config = gson.fromJson(reader, Config.class);
        reader.close();
    }

    public static void saveConfig() throws IOException {
        FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
        OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(osw);
        writer.write(gson.toJson(config));
        writer.close();
    }

    public static void saveConfigWithErrorHandling() {
        try {
            Config.saveConfig();
        }
        catch (IOException e) {
            if (SongPlayer.MC.world != null) {
                SongPlayer.addChatMessage("§cFailed to save config file");
            }
            e.printStackTrace();
        }
    }
}
