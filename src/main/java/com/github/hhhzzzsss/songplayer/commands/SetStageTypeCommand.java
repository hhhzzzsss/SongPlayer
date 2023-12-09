package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

class SetStageTypeCommand extends Command {
    public String getName() {
        return "setStageType";
    }
    public String[] getAliases() {
        return new String[]{"setStage", "stageType"};
    }
    public String[] getSyntax() {
        return new String[] {"<DEFAULT | WIDE | SPHERICAL>"};
    }
    public String getDescription() {
        return "Sets the type of noteblock stage to build";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) return false;

        try {
            Stage.StageType stageType = Stage.StageType.valueOf(args.toUpperCase(Locale.ROOT));
            Config.getConfig().stageType = stageType;
            SongPlayer.addChatMessage("§6Set stage type to §3" + stageType.name());
            Config.saveConfigWithErrorHandling();
        } catch (IllegalArgumentException e) {
            SongPlayer.addChatMessage("§cInvalid stage type");
        }

        return true;
    }

    public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
        if (!args.contains(" ")) {
            return CommandSource.suggestMatching(Arrays.stream(Stage.StageType.values()).map(Stage.StageType::name), suggestionsBuilder);
        } else {
            return null;
        }
    }
}