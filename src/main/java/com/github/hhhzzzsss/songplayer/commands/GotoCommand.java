package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;

import java.io.IOException;

class GotoCommand extends Command {
    public String getName() {
        return "goto";
    }
    public String[] getSyntax() {
        return new String[] {"<mm:ss>"};
    }
    public String getDescription() {
        return "Goes to a specific time in the song";
    }

    public boolean processCommand(String args) {
        if (SongHandler.getInstance().currentSong == null) {
            SongPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }

        if (args.isEmpty()) return false;

        try {
            long time = Util.parseTime(args);
            SongHandler.getInstance().currentSong.setTime(time);
            SongPlayer.addChatMessage("§6Set song time to §3" + Util.formatTime(time));
            return true;
        } catch (IOException e) {
            SongPlayer.addChatMessage("§cNot a valid time stamp");
            return false;
        }
    }
}
