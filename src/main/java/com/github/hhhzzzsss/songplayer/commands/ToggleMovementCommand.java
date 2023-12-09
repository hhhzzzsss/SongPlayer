package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

class ToggleMovementCommand extends Command {
    public String getName() {
        return "toggleMovement";
    }
    public String[] getAliases() {
        return new String[]{"movement"};
    }
    public String[] getSyntax() {
        return new String[] {"<swing | rotate>"};
    }
    public String getDescription() {
        return "Toggles different types of movements";
    }

    public boolean processCommand(String args) {
        switch (args.toLowerCase(Locale.ROOT)) {
            case "swing":
                Config.getConfig().swing = !Config.getConfig().swing;
                if (Config.getConfig().swing) {
                    SongPlayer.addChatMessage("§6Enabled arm swinging");
                } else {
                    SongPlayer.addChatMessage("§6Disabled arm swinging");
                }
                Config.saveConfigWithErrorHandling();
                return true;
            case "rotate":
                Config.getConfig().rotate = !Config.getConfig().rotate;
                if (Config.getConfig().rotate) {
                    SongPlayer.addChatMessage("§6Enabled player rotation");
                } else {
                    SongPlayer.addChatMessage("§6Disabled player rotation");
                }
                Config.saveConfigWithErrorHandling();
                return true;
            default:
                return false;
        }
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        if (!args.contains(" ")) {
            return CommandSource.suggestMatching(new String[]{"swing", "rotate"}, suggestionsBuilder);
        } else {
            return null;
        }
    }
}