package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

class PlayCommand extends Command {
    public String getName() {
        return "play";
    }
    public String[] getSyntax() {
        return new String[] {"<song or url>"};
    }
    public String getDescription() {
        return "Plays a song";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) return false;

        SongHandler.getInstance().loadSong(args);
        return true;
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return Util.giveSongSuggestions(args, suggestionsBuilder);
    }
}
