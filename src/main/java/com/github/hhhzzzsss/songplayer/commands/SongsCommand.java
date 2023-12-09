package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

class SongsCommand extends Command {
    public String getName() {
        return "songs";
    }
    public String[] getAliases() {
        return new String[]{"list"};
    }
    public String[] getSyntax() {
        return new String[] {
                "",
                "<subdirectory>"};
    }
    public String getDescription() {
        return "Lists available songs. If an argument is provided, lists all songs in the subdirectory.";
    }

    public boolean processCommand(String args) {
        if (args.contains(" ")) return false;

        Path dir;
        if (args.isEmpty()) dir = SongPlayer.SONG_DIR;

        else {
            dir = SongPlayer.SONG_DIR.resolve(args);
            if (!Files.isDirectory(dir)) {
                SongPlayer.addChatMessage("§cDirectory not found");
                return true;
            }
        }

        List<String> subdirectories;
        List<String> songs;
        try {
            subdirectories = Files.list(dir)
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(str -> str + "/")
                    .collect(Collectors.toList());
            songs = Files.list(dir)
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        }
        catch (IOException e) {
            SongPlayer.addChatMessage("§cError reading folder: §4" + e.getMessage());
            return true;
        }

        if (subdirectories.isEmpty() && songs.isEmpty()) {
            SongPlayer.addChatMessage("§bNo songs found. You can put midi or nbs files in the §3.minecraft/songs §6folder.");
        } else {
            SongPlayer.addChatMessage("§6----------------------------------------");
            SongPlayer.addChatMessage("§eContents of .minecraft/songs/" + args);
            if (!subdirectories.isEmpty()) {
                SongPlayer.addChatMessage("§6Subdirectories: §3" + String.join(" ", subdirectories));
            }

            if (!songs.isEmpty()) {
                SongPlayer.addChatMessage("§6Songs: §7" + String.join(", ", songs));
            }
            SongPlayer.addChatMessage("§6----------------------------------------");
        }
        return true;
    }
    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return Util.giveSongDirectorySuggestions(args, suggestionsBuilder);
    }
}