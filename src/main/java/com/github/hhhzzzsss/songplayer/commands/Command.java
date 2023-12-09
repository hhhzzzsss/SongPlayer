package com.github.hhhzzzsss.songplayer.commands;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

abstract class Command {
    public abstract String getName();
    public String[] getSyntax() {
        return new String[0];
    }
    public abstract String getDescription();
    public abstract boolean processCommand(String args);
    public String[] getAliases() {
        return new String[0];
    }
    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return null;
    }
}
