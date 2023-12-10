package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class SetCommandsCommand extends Command {
    @Override
    public String getName() {
        return "setCommands";
    }

    @Override
    public String[] getSyntax() {
        return new String[] {
            "use vanilla",
            "use essentials",
            "creative <command>",
            "survival <command>"
        };
    }

    @Override
    public String getDescription() {
        return "Sets the commands used to switch gamemode";
    }

    @Override
    public boolean processCommand(String args) {
        String[] split = args.split(" ");

        if (split.length < 2) return false;

        String command = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
        if (!split[0].toLowerCase().equals("use") && command.startsWith("/")) {
            command = command.substring(1);
        }

        switch (split[0].toLowerCase()) {
            case "use" -> {
                switch (command.toLowerCase()) {
                    case "vanilla" -> {
                        Config.getConfig().creativeCommand = "gamemode creative";
                        Config.getConfig().survivalCommand = "gamemode survival";
                        SongPlayer.addChatMessage("§6Now using vanilla gamemode commands");
                    }
                    case "essentials" -> {
                        Config.getConfig().creativeCommand = "gmc";
                        Config.getConfig().survivalCommand = "gms";
                        SongPlayer.addChatMessage("§6Now using essentials gamemode commands");
                    }
                    default -> {
                        return false;
                    }
                }

            }
            case "creative" -> {
                Config.getConfig().creativeCommand = command;
                SongPlayer.addChatMessage("§6Set creative command to §3/" + command);
            }
            case "survival" -> {
                Config.getConfig().survivalCommand = command;
                SongPlayer.addChatMessage("§6Set survival command to §3/" + command);
            }
            default -> {
                return false;
            }
        }

        Config.saveConfigWithErrorHandling();

        return true;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        String[] split = args.split(" ", -1);

        if (split.length <= 1) {
            return CommandSource.suggestMatching(new String[]{
                    "use",
                    "creative",
                    "survival"
            }, suggestionsBuilder);
        }

        if (split[0].toLowerCase(Locale.ROOT).equals("use") && split.length == 2) {
            return CommandSource.suggestMatching(new String[]{
                    "vanilla",
                    "essentials"
            }, suggestionsBuilder);
        }

        return null;
    }
}
