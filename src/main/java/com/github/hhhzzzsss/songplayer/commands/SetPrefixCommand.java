package com.github.hhhzzzsss.songplayer.commands;
import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;

class SetPrefixCommand extends Command {
    public String getName() {
        return "setPrefix";
    }
    public String[] getAliases() {
        return new String[]{"prefix"};
    }
    public String[] getSyntax() {
        return new String[] {"<prefix>"};
    }
    public String getDescription() {
        return "Sets the command prefix used by SongPlayer";
    }

    public boolean processCommand(String args) {
        if (args.isEmpty()) return false;

        if (args.contains(" ")) {
            SongPlayer.addChatMessage("§cPrefix cannot contain a space");
            return true;
        }

        if (args.startsWith("/")) {
            SongPlayer.addChatMessage("§cPrefix cannot be '/'");
            return true;
        }

        Config.getConfig().prefix = args;
        SongPlayer.addChatMessage("§6Set prefix to " + args);
        Config.saveConfigWithErrorHandling();

        return true;
    }
}
