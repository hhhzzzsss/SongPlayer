package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;

class SkipCommand extends Command {
    public String getName() {
        return "skip";
    }
    public String getDescription() {
        return "Skips current song";
    }

    public boolean processCommand(String args) {
        if (SongHandler.getInstance().currentSong == null) {
            SongPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }

        if (!args.isEmpty()) return false;

        SongHandler.getInstance().currentSong = null;
        return true;
    }
}
