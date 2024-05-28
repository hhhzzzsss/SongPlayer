package com.github.hhhzzzsss.songplayer.config;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import java.io.*;
import java.util.Properties;

public class ModProperties {
    private static ModProperties instance = new ModProperties();
    public static ModProperties getInstance() {
        return instance;
    }
    private File cfgfile = SongPlayer.CONFIG_FILE;
    private String cfgPath = cfgfile.getPath();
    private Properties config = new Properties();

    public Properties getConfig() {
        return config;
    }
    public void createValue(String setting, String value) {
        if (config.containsKey(setting)) {
            return;
        }
        config.setProperty(setting, value);
    }
    public void updateValue(String setting, String value) {
        config.setProperty(setting, value);
        save();
    }
    public void save() {
        try {
            config.store(new FileOutputStream(cfgPath), "config file for SongPlayer");
        } catch(IOException e) {
            e.printStackTrace();
            System.out.println("Dang it I just disappointed " + SongPlayer.MC.player.getName() + "... Sorry you have to see this error message.");
            SongPlayer.addChatMessage("§cThere was an error when attempting to save your settings.");
        }
    }

    public void reset() {
        try {
            cfgfile.createNewFile();
            if (!SongPlayer.SONG_DIR.exists()) {
                SongPlayer.SONG_DIR.mkdir();
            }
            FileInputStream cfginput = new FileInputStream(cfgPath);
            config.load(cfginput);
            updateValue("prefix", "$");
            updateValue("creativeCommand", "gamemode creative");
            updateValue("survivalCommand", "gamemode survival");
            updateValue("adventureCommand", "gamemode adventure");
            updateValue("spectatorCommand", "gamemode spectator");
            updateValue("playSoundCommand", "execute as @a at @s run playsound minecraft:block.note_block.{type} record @p ~ ~ ~ {volume} {pitch}");
            updateValue("stageType", "default");
            updateValue("showProgressCommand",
                    "execute at @a unless entity @s run title @p actionbar [" +
                            "{\"color\":\"gold\",\"text\":\"Now playing \"}," +
                            "{\"color\":\"blue\",\"text\":\"{MIDI}\"}," +
                            "{\"color\":\"gold\",\"text\":\" | \"}," +
                            "{\"color\":\"dark_aqua\",\"text\":\"{CurrentTime}/{SongTime}\"}]");
            updateValue("rotate", String.valueOf(false));
            updateValue("swing", String.valueOf(false));
            updateValue("useVolume", String.valueOf(true));
            updateValue("useCommandsForPlaying", String.valueOf(false));
            updateValue("useCommandBlocks", String.valueOf(false));
            updateValue("switchGamemode", String.valueOf(true));
            updateValue("useNoteblocks", String.valueOf(true));
            updateValue("packetsWhilePlaying", String.valueOf(false));
            updateValue("countByFrames", String.valueOf(false));
            updateValue("useAlternateInstruments", String.valueOf(true));
            updateValue("noteVolumeThreshold", String.valueOf(0.0f));
            updateValue("buildDelay", String.valueOf(4));
            updateValue("disablecommandcreative", String.valueOf(false));
            updateValue("disablecommandsurvival", String.valueOf(false));
            updateValue("disablecommandadventure", String.valueOf(false));
            updateValue("disablecommandspectator", String.valueOf(false));
            updateValue("disablecommandplaynote", String.valueOf(false));
            updateValue("disablecommanddisplayprogress", String.valueOf(false));
            updateValue("cleanupstage", String.valueOf(false));
            updateValue("stopiferror", String.valueOf(false));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Dang it I just disappointed " + SongPlayer.MC.player.getName() + "... Sorry you have to see this error message.");
            SongPlayer.addChatMessage("§cThere was an error when attempting to save your settings.");
        }
    }
    public void setup() {
        try {
            cfgfile.createNewFile();
            if (!SongPlayer.SONG_DIR.exists()) {
                SongPlayer.SONG_DIR.mkdir();
            }
            FileInputStream cfginput = new FileInputStream(cfgPath);
            config.load(cfginput);
            createValue("prefix", "$");
            createValue("creativeCommand", "gamemode creative");
            createValue("survivalCommand", "gamemode survival");
            createValue("adventureCommand", "gamemode adventure");
            createValue("spectatorCommand", "gamemode spectator");
            createValue("playSoundCommand", "execute as @a[tag=!nomusic] at @s run playsound minecraft:block.note_block.{type} record @s ~ ~ ~ {volume} {pitch} 1");
            createValue("stageType", "default");
            createValue("showProgressCommand",
                    "title @a[tag=!nomusic] actionbar [" +
                    "{\"color\":\"gold\",\"text\":\"Now playing \"}," +
                    "{\"color\":\"blue\",\"text\":\"{MIDI}\"}," +
                    "{\"color\":\"gold\",\"text\":\" | \"}," +
                    "{\"color\":\"dark_aqua\",\"text\":\"{CurrentTime}/{SongTime}\"}]");
            createValue("rotate", String.valueOf(false));
            createValue("swing", String.valueOf(false));
            createValue("useVolume", String.valueOf(true));
            createValue("useCommandsForPlaying", String.valueOf(false));
            createValue("useCommandBlocks", String.valueOf(false));
            createValue("switchGamemode", String.valueOf(true));
            createValue("useNoteblocks", String.valueOf(true));
            createValue("packetsWhilePlaying", String.valueOf(false));
            createValue("countByFrames", String.valueOf(false));
            createValue("useAlternateInstruments", String.valueOf(true));
            createValue("noteVolumeThreshold", String.valueOf(0.0f));
            createValue("buildDelay", String.valueOf(4));
            createValue("disablecommandcreative", String.valueOf(false));
            createValue("disablecommandsurvival", String.valueOf(false));
            createValue("disablecommandadventure", String.valueOf(false));
            createValue("disablecommandspectator", String.valueOf(false));
            createValue("disablecommandplaynote", String.valueOf(false));
            createValue("disablecommanddisplayprogress", String.valueOf(false));
            createValue("cleanupstage", String.valueOf(false));
            createValue("stopiferror", String.valueOf(false));
            save();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Dang it I just disappointed " + SongPlayer.MC.player.getName() + "... Sorry you have to see this error message.");
            SongPlayer.addChatMessage("§cThere was an error when attempting to save your settings.");
        }
    }
}
