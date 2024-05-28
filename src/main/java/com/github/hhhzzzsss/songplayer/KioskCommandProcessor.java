package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.song.Song;
import com.mojang.authlib.GameProfile;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class KioskCommandProcessor {

    public static ArrayList<KioskCommandProcessor.Command> commands = new ArrayList<>();
    public static HashMap<String, KioskCommandProcessor.Command> commandMap = new HashMap<>();
    public static HashMap<UUID, Integer> commandCooldown = new HashMap<>();

    public static void runCommand(GameProfile player, String command) {
        if (commandCooldown.containsKey(player.getId())) {
            if (commandCooldown.get(player.getId()) < 0) {
                return;
            }
        }
        String[] split = command.split(" ");
        if (!split[0].startsWith(SongPlayer.prefix)) {
            Util.broadcastMessage("{\"color\":\"red\",\"text\":\"Command does not start with the prefix " + SongPlayer.prefix + "\"}", player.getName());
            return;
        }
        String commandName = split[0].substring(SongPlayer.prefix.length()).toLowerCase();
        System.out.println(player.getName() + " issued SongPlayer Command: " + command);
        if (!commandMap.containsKey(commandName)) {
            Util.broadcastMessage("{\"color\":\"red\",\"text\":\"unrecognized command\"}", player.getName());
            return;
        }
        if (commandCooldown.get(player.getId()) < commandMap.get(commandName).cooldown()) {
            Util.broadcastMessage("{\"color\":\"red\",\"text\":\"You do not have permission to run this command or you are being rate-limited\"}", player.getName());
            commandCooldown.put(player.getId(), -20);
            return;
        }
        String args = "";
        if (split.length > 1) {
            args = command.substring(commandName.length() + 1 + SongPlayer.prefix.length());
        }
        commandCooldown.put(player.getId(), 0);
        commandMap.get(commandName).processCommand(player, args);
        return;
    }
    public static void initCommands() {
        commands.add(new KioskCommandProcessor.helpCommand());
        commands.add(new KioskCommandProcessor.aboutCommand());
        commands.add(new KioskCommandProcessor.playCommand());
        commands.add(new KioskCommandProcessor.stopCommand());
        commands.add(new KioskCommandProcessor.skipCommand());
        commands.add(new KioskCommandProcessor.songsCommand());
        commands.add(new KioskCommandProcessor.queueCommand());
        commands.add(new KioskCommandProcessor.minimumvolumeCommand());
        commands.add(new KioskCommandProcessor.gotoCommand());


        for (KioskCommandProcessor.Command command : commands) {
            commandMap.put(command.getName().toLowerCase(), command);
            for (String alias : command.getAliases()) {
                commandMap.put(alias.toLowerCase(), command);
            }
        }
    }

    private static abstract class Command {
        public abstract String getName();

        public abstract String getSyntax();

        public abstract String getDescription();

        public abstract byte getPermissionLevel();

        public abstract int cooldown();

        public String[] getAliases() {
            return new String[]{};
        }

        public abstract boolean processCommand(GameProfile player, String args);


    }

    private static class helpCommand extends KioskCommandProcessor.Command {
        public String getName() {
            return "help";
        }

        public String getSyntax() {
            return "help <page>";
        }

        public String getDescription() {
            return "Lists available information of commands";
        }

        public byte getPermissionLevel() {
            return 0;
        }

        public int cooldown() {return 5;}

        public boolean processCommand(GameProfile player, String args) {
            int page = 0;
            if (args.length() > 0) {
                String[] theArgs = args.split(" ");
                try {
                    page = Integer.parseInt(theArgs[0]);
                    page -= 1;
                } catch(NumberFormatException | NullPointerException | IndexOutOfBoundsException e) {
                    Util.broadcastMessage("[{\"color\":\"red\",\"text\":\"Invalid page\"}]", player.getName());
                    return true;
                }
            }
            int maxpage = ((commands.size() - 1) / 7);
            MutableText list = Text.empty();
            list.append("[{\"text\":\"-- Command List (" + (page + 1) +" / " + (maxpage + 1) + ") --\"},");

            if (page > maxpage || page < 0) {
                Util.broadcastMessage("[{\"color\":\"red\",\"text\":\"Invalid page\"}]", player.getName());
                return true;
            }
            for (int i = page * 7; i < (page * 7) + 7; i++) {
                if (i >= commands.size()) break;
                Command c = commands.get(i);
                list.append("{\"color\":\"gold\",\"text\":\"\\n" + SongPlayer.prefix + c.getName() + ": \",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"" + "Usage: " + SongPlayer.prefix + c.getSyntax() + "\\nPermission level: " + c.getPermissionLevel() + "\"},\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"" + SongPlayer.prefix + c.getName() +" \"}}, {\"color\":\"white\",\"text\":\"" + c.getDescription() + "\"},");
            }
            list.append("" +
                    ((page > 0) ? "{\"color\":\"white\",\"text\":\"\\n<== \",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"page " + (page) + "\"},\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"" + SongPlayer.prefix + "help " + (page) + "\"}}," : "{\"color\":\"gray\",\"text\":\"\\n<== \"},") +
                    "{\"color\":\"dark_gray\",\"text\":\"-----------------------------\"}," +
                    ((page < maxpage) ? "{\"color\":\"white\",\"text\":\" ==>\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"page " + (page + 2) + "\"},\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"" + SongPlayer.prefix + "help " + (page + 2) + "\"}}" : "{\"color\":\"gray\",\"text\":\" ==>\"}") +
                    "]");
            Util.broadcastMessage(list.getString().trim(), player.getName());
            return true;
        }
    }

    private static class aboutCommand extends Command {

        public String getName() {
            return "about";
        }

        public String getSyntax() {
            return "about";
        }

        public String getDescription() {
            return "provides information about this mod";
        }

        public boolean processCommand(GameProfile player, String args) {
            Util.broadcastMessage("\"SongPlayer v3.3.0k developed by hhhzzzsss & Sk8kman, tested by Lizard16\\nhttps://github.com/Sk8kman/SongPlayer\"", null);
            return true;
        }

        public int cooldown() {return 100;}

        public byte getPermissionLevel() {
            return 0;
        }
    }

    private static class playCommand extends Command {

        public String getName() {
            return "play";
        }

        public String getSyntax() {
            return "play <song>";
        }

        public String getDescription() {
            return "plays a song from storage";
        }

        public int cooldown() {return 50;}

        public byte getPermissionLevel() {
            return 0;
        }

        public boolean processCommand(GameProfile player, String args) {
            if (args.length() > 0) {
                SongHandler.getInstance().loadSong(args, SongPlayer.SONG_DIR);
                return true;
            }
            Util.broadcastMessage("{\"color\":\"dark_gray\",\"text\":\"Usage: " + getSyntax() + "\"}", player.getName());
            return true;
        }
    }

    private static class stopCommand extends Command {

        public String getName() {
            return "stop";
        }

        public String getSyntax() {
            return "stop";
        }

        public String getDescription() {
            return "stops playing and removes all songs in the queue";
        }

        public int cooldown() {return 100;}

        public boolean processCommand(GameProfile player, String args) {
            if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
                Util.broadcastMessage("{\"color\":\"gold\",\"text\":\"No song is currently playing\"}", player.getName());
                Util.currentPlaylist = "";
                Util.playlistSongs.clear();
                return true;
            }
            SongHandler.getInstance().paused = false;
            SongHandler.getInstance().cleanup(true);
            if (!Util.currentPlaylist.isEmpty()) {
                Util.currentPlaylist = "";
            }
            Util.sendCommandWithCommandblocks("bossbar set minecraft:songplayer visible false");
            Util.broadcastMessage("{\"color\":\"gold\",\"text\":\"SongPlayer stopped, all songs in queue are wiped\"}", null);
            return true;
        }

        public byte getPermissionLevel() {
            return 0;
        }
    }

    private static class skipCommand extends Command {

        public String getName() {
            return "skip";
        }

        public String getSyntax() {
            return "skip";
        }

        public String getDescription() {
            return "Go to the next song in the queue";
        }

        public byte getPermissionLevel() {
            return 0;
        }

        public int cooldown() {
            return 20;
        }

        public boolean processCommand(GameProfile player, String args) {
            if (SongHandler.getInstance().currentSong == null) {
                Util.broadcastMessage("{\"color\":\"red\",\"text\":\"No song is currently playing\"", player.getName());
                return true;
            }
            Util.broadcastMessage("{\"color\":\"gold\",\"text\":\"Skipped song\"}", null);
            Util.playcooldown = Calendar.getInstance().getTime().getTime() + 1500;
            SongHandler.getInstance().currentSong = null;
            Util.advancePlaylist();
            return true;
        }
    }

    private static class gotoCommand extends Command {

        public String getName() {
            return "goto";
        }

        public String getSyntax() {
            return "goto <mm:ss>";
        }

        public String getDescription() {
            return "skip ahead or backwards";
        }

        public byte getPermissionLevel() {
            return 0;
        }

        public int cooldown() {
            return 10;
        }

        public boolean processCommand(GameProfile player, String args) {
            if (SongHandler.getInstance().currentSong == null) {
                Util.broadcastMessage("{\"color\":\"red\",\"text\":\"No song is currently playing\"}", player.getName());
                return true;
            }
            if (args.length() == 0) {
                Util.broadcastMessage(" {\"color\":\"gray\",\"text\":\"Usage: " + SongPlayer.prefix + getSyntax() + "\"}", player.getName());
                return true;
            }

            try {
                long time = Util.parseTime(args);
                SongHandler.getInstance().currentSong.setTime(time);
                Util.broadcastMessage("[{\"color\":\"gold\",\"text\":\"Set song time to \"}, {\"color\":\"dark_aqua\",\"text\":\"" + Util.formatTime(time) + "\"}]", player.getName());
                return true;
            } catch (IOException e) {
                Util.broadcastMessage("{\"color\":\"red\",\"text\":\"Not a valid time stamp\"", player.getName());
                return true;
            }
        }
    }

    private static class minimumvolumeCommand extends Command {

        public String getName() {
            return "minimumvolume";
        }

        public String getSyntax() {
            return "minimumvolume <0-127>";
        }

        public String getDescription() {
            return "prevents notes with a loudness\\nvalue above the set value\\nfrom playing";
        }

        public byte getPermissionLevel() {
            return 0;
        }

        public int cooldown() {
            return 40;
        }

        public boolean processCommand(GameProfile player, String args) {
            byte newLimit;
            try {
                newLimit = Byte.parseByte(args);
            } catch (NumberFormatException e) {
                Util.broadcastMessage("[{\"color\":\"dark_red\",\"text\":\"" + args + "\"}, {\"color\":\"red\",\"text\":\" is not a number between 0-127\"}]", player.getName());
                return true;
            }
            if (newLimit < 0) {
                Util.broadcastMessage("[{\"color\":\"dark_red\",\"text\":\"" + args + "\"}, {\"color\":\"red\",\"text\":\" is not a number between 0-127\"}]", player.getName());
                return true;
            }
            SongPlayer.ignoreNoteThreshold = newLimit;
            Util.broadcastMessage("[{\"color\":\"gold\",\"text\":\"Notes below volume level \"}, {\"color\":\"dark_aqua\",\"text\":\"" + newLimit + "\"}, {\"color\":\"gold\",\"text\":\" will no longer play\"}]", null);
            return true;
        }
    }

    private static class queueCommand extends Command {

        public String getName() {
            return "queue";
        }

        public String getSyntax() {
            return "queue <page>";
        }

        public String getDescription() {
            return "returns a list of songs in the queue";
        }

        public byte getPermissionLevel() {
            return 0;
        }

        public int cooldown() {
            return 10;
        }

        public boolean processCommand(GameProfile player, String args) {
            int page = 0;
            if (args.length() > 0) {
                String[] theArgs = args.split(" ");
                try {
                    page = Integer.parseInt(theArgs[0]);
                    page -= 1;
                } catch(NumberFormatException | NullPointerException | IndexOutOfBoundsException e) {
                    Util.broadcastMessage("[{\"color\":\"red\",\"text\":\"Invalid page\"}]", player.getName());
                    return true;
                }
            }
            List<Song> queue = SongHandler.getInstance().songQueue;
            if (queue.isEmpty()) {
                Util.broadcastMessage("[{\"color\":\"gold\",\"text\":\"There are no songs in the queue\"}]", player.getName());
                return true;
            }
            int maxpage = ((queue.size() - 1) / 7);

            if (page > maxpage || page < 0) {
                Util.broadcastMessage("[{\"color\":\"red\",\"text\":\"Invalid page\"}]", player.getName());
                return true;
            }
            MutableText songs = Text.empty();
            songs.append("[{\"text\":\"-- Song Queue (" + (page + 1) +" / " + (maxpage + 1) + ") --\"},");
            for (int i = page * 7; i < (page * 7) + 7; i++) {
                if (i >= queue.size()) break;
                songs.append("{\"color\":\"dark_aqua\",\"text\":\"\\n" + (i + 1) + ". \"}, {\"color\":\"gold\",\"text\":\"" + queue.get(i).name + "\"},");
            }
            songs.append("" +
                    ((page > 0) ? "{\"color\":\"white\",\"text\":\"\\n<== \",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"page " + (page) + "\"},\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"" + SongPlayer.prefix + "queue " + (page) + "\"}}," : "{\"color\":\"gray\",\"text\":\"\\n<== \"},") +
                    "{\"color\":\"dark_gray\",\"text\":\"-----------------------------\"}," +
                    (((page) < maxpage) ? "{\"color\":\"white\",\"text\":\" ==>\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"page " + (page + 2) + "\"},\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"" + SongPlayer.prefix + "queue " + (page + 2) + "\"}}" : "{\"color\":\"gray\",\"text\":\" ==>\"}") +
                    "]");
            Util.broadcastMessage(songs.getString().trim(), player.getName());
            return true;
        }
    }

    private static class songsCommand extends Command {

        public String getName() {
            return "songs";
        }

        public String getSyntax() {
            return "songs <page> <filter>";
        }

        public String getDescription() {
            return "provides a list of songs";
        }

        public byte getPermissionLevel() {
            return 0;
        }

        public int cooldown() {
            return 5;
        }

        public boolean processCommand(GameProfile player, String args) {
            String[] theArgs = args.split(" ");
            int page = 0;
            String filter = "";
            if (args.length() > 0) {
                try {
                    page = Integer.parseInt(theArgs[0]);
                    page -= 1;
                } catch(NumberFormatException | NullPointerException | IndexOutOfBoundsException e) {
                    Util.broadcastMessage("[{\"color\":\"red\",\"text\":\"Invalid page\"}]", player.getName());
                    return true;
                }
                if (theArgs.length > 1) {
                    StringBuilder f = new StringBuilder();
                    for (int i = 1; i < theArgs.length; i++) {
                        f.append(theArgs[i] + " ");
                    }
                    filter = f.toString().trim();
                }
            }
            String finalFilter = filter;
            List<String> filenames = Arrays.stream(SongPlayer.SONG_DIR.listFiles())
                    .filter(File::isFile).filter(x->x.getName().toLowerCase().replace('_', ' ').replace('-', ' ').contains(finalFilter.toLowerCase().replace('_', ' ').replace('-', ' ')))
                    .map(File::getName)
                    .collect(Collectors.toList());
            Collections.sort(filenames);

            if (filenames.isEmpty()) {
                Util.broadcastMessage("[{\"color\":\"red\",\"text\":\"No songs in list\"}]", player.getName());
                return true;
            }
            int maxpage = (filenames.size() - 1) / 7;
            if (page > maxpage || page < 0) {
                Util.broadcastMessage("[{\"color\":\"red\",\"text\":\"Invalid page\"}]", player.getName());
                return true;
            }
            MutableText songs = Text.empty();
            songs.append("[{\"text\":\"-- Song List (" + (page + 1) +" / " + (maxpage + 1) + ") --\"},");
            for (int i = page * 7; i < (page * 7) + 7; i++) {
                if (i >= filenames.size()) break;
                songs.append("{\"color\":\"dark_aqua\",\"text\":\"\\n" + (i + 1) + ". \"}, {\"color\":\"gold\",\"text\":\"" + filenames.get(i) + "\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"" + "Click to play\"},\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"" + SongPlayer.prefix + "play " + filenames.get(i) + "\"}},");
            }
            songs.append("" +
                    ((page > 0) ? "{\"color\":\"white\",\"text\":\"\\n<== \",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"page " + (page) + "\"},\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"" + SongPlayer.prefix + "songs " + (page) + " " + filter + "\"}}," : "{\"color\":\"gray\",\"text\":\"\\n<== \"},") +
                    "{\"color\":\"dark_gray\",\"text\":\"-----------------------------\"}," +
                    (((page) < maxpage) ? "{\"color\":\"white\",\"text\":\" ==>\",\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"page " + (page + 2) + "\"},\"clickEvent\":{\"action\":\"suggest_command\",\"value\":\"" + SongPlayer.prefix + "songs " + (page + 2) + " " + filter + "\"}}" : "{\"color\":\"gray\",\"text\":\" ==>\"}") +
                    "]");
            Util.broadcastMessage(songs.getString().trim(), player.getName());
            return true;
        }
    }
}
