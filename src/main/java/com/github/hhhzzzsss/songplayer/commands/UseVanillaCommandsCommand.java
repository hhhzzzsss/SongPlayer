package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;

class UseVanillaCommandsCommand extends Command {
    public String getName() {
        return "useVanillaCommands";
    }
    public String[] getAliases() {
        return new String[]{"vanilla", "useVanilla", "vanillaCommands"};
    }
    public String getDescription() {
        return "Switches to using vanilla gamemode commands";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) return false;

        Config.getConfig().creativeCommand = "gamemode creative";
        Config.getConfig().survivalCommand = "gamemode survival";

        SongPlayer.addChatMessage("§6Now using vanilla gamemode commands");
        Config.saveConfigWithErrorHandling();
        return true;
    }
}