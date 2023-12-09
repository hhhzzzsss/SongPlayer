package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;

class StopCommand extends Command {
    public String getName() {
        return "stop";
    }
    public String getDescription() {
        return "Stops playing";
    }

    public boolean processCommand(String args) {
        if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
            SongPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }

        if (SongHandler.getInstance().stage != null) {
            SongHandler.getInstance().stage.movePlayerToStagePosition();
        }

        SongHandler.getInstance().restoreStateAndCleanUp();
        SongPlayer.addChatMessage("§6Stopped playing");
        return true;
    }
}