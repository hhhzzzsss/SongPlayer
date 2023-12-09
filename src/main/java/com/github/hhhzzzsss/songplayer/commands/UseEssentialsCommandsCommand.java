package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;

class UseEssentialsCommandsCommand extends Command {
    public String getName() {
        return "useEssentialsCommands";
    }
    public String[] getAliases() {
        return new String[]{"essentials", "useEssentials", "essentialsCommands"};
    }
    public String getDescription() {
        return "Switches to using essentials gamemode commands";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) return false;

        Config.getConfig().creativeCommand = "gmc";
        Config.getConfig().survivalCommand = "gms";

        SongPlayer.addChatMessage("§6Now using essentials gamemode commands");
        Config.saveConfigWithErrorHandling();
        return true;
    }
}