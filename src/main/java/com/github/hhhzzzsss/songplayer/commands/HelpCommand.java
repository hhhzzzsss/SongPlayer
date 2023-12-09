package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.github.hhhzzzsss.songplayer.commands.CommandProcessor.*;

class HelpCommand extends Command {
    public String getName() {
        return "help";
    }
    public String[] getSyntax() {
        return new String[]{"[command]"};
    }
    public String getDescription() {
        return "Lists commands or explains command";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) {
            StringBuilder helpMessage = new StringBuilder("§6Commands -");
            for (Command c : commands) {
                helpMessage.append(" " + Config.getConfig().prefix + c.getName());
            }
            SongPlayer.addChatMessage(helpMessage.toString());
            return true;
        }

        if (!commandMap.containsKey(args.toLowerCase(Locale.ROOT))) {
            SongPlayer.addChatMessage("§cCommand not recognized: " + args);
            return true;
        }


        Command c = commandMap.get(args.toLowerCase(Locale.ROOT));

        SongPlayer.addChatMessage("§6------------------------------");
        SongPlayer.addChatMessage("§6Help: §3" + c.getName());
        SongPlayer.addChatMessage("§6Description: §3" + c.getDescription());
        if (c.getSyntax().length == 0) {
            SongPlayer.addChatMessage("§6Usage: §3" + Config.getConfig().prefix + c.getName());
        }
        else if (c.getSyntax().length == 1) {
            SongPlayer.addChatMessage("§6Usage: §3" + Config.getConfig().prefix + c.getName() + " " + c.getSyntax()[0]);
        } else {
            SongPlayer.addChatMessage("§6Usage:");
            for (String syntax : c.getSyntax()) {
                SongPlayer.addChatMessage("    §3" + Config.getConfig().prefix + c.getName() + " " + syntax);
            }
        }
        if (c.getAliases().length > 0) {
            SongPlayer.addChatMessage("§6Aliases: §3" + String.join(", ", c.getAliases()));
        }
        SongPlayer.addChatMessage("§6------------------------------");


        return true;
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        return CommandSource.suggestMatching(commandCompletions, suggestionsBuilder);
    }
}