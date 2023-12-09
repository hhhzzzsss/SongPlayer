package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;

class SetCreativeCommandCommand extends Command {
    public String getName() {
        return "setCreativeCommand";
    }
    public String[] getAliases() {
        return new String[]{"sc"};
    }
    public String[] getSyntax() {
        return new String[] {"<command>"};
    }
    public String getDescription() {
        return "Sets the command used to go into creative mode";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) return false;

        String command;
        if (args.startsWith("/")) {
            command = args.substring(1);
        } else {
            command = args;
        }

        Config.getConfig().creativeCommand = command;
        SongPlayer.addChatMessage("§6Set creative command to §3/" + command);
        Config.saveConfigWithErrorHandling();
        return true;
    }
}
