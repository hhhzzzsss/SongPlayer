package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.playing.Stage;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static Config config = null;

    public static final Path CONFIG_FILE = SongPlayer.SONGPLAYER_DIR.resolve("config.json");
    private static final Gson gson = new Gson();

    public String prefix = "$";
    public String creativeCommand = "gmc";
    public String survivalCommand = "gms";
    public boolean showFakePlayer = false;
    public boolean loopPlaylists = false;
    public boolean shufflePlaylists = false;
    public Stage.StageType stageType = Stage.StageType.DEFAULT;
    public boolean swing = false;
    public boolean rotate = false;
    public int velocityThreshold = 0;
    public boolean doAnnouncement = false;
    public String announcementMessage = "&6Now playing: &3[name]";
    public double breakSpeed = 40.0;
    public double placeSpeed = 20.0;
    public boolean autoCleanup = false;
    public boolean survivalOnly = false;

    public static Config getConfig() {
        if (config == null) {
            config = new Config();
            try {
                if (Files.exists(CONFIG_FILE)) {
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
        BufferedReader reader = Files.newBufferedReader(CONFIG_FILE);
        config = gson.fromJson(reader, Config.class);
        reader.close();
    }

    public static void saveConfig() throws IOException {
        BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE);
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
