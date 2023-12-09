package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;

class ToggleFakePlayerCommand extends Command {
    public String getName() {
        return "toggleFakePlayer";
    }
    public String[] getAliases() {
        return new String[]{"fakePlayer", "fp"};
    }
    public String getDescription() {
        return "Shows a fake player representing your true position when playing songs";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) return false;

        Config.getConfig().showFakePlayer = !Config.getConfig().showFakePlayer;

        if (Config.getConfig().showFakePlayer) {
            SongPlayer.addChatMessage("§6Enabled fake player");
        } else {
            SongPlayer.addChatMessage("§6Disabled fake player");
        }

        Config.saveConfigWithErrorHandling();
        return true;
    }
}
