package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.song.Song;

class LoopCommand extends Command {
    public String getName() {
        return "loop";
    }
    public String getDescription() {
        return "Toggles song looping";
    }

    public boolean processCommand(String args) {
        if (SongHandler.getInstance().currentSong == null) {
            SongPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }

        Song currentSong = SongHandler.getInstance().currentSong;
        boolean looping = currentSong.looping;
        currentSong.looping = !looping;
        currentSong.loopCount = 0;

        if (looping) {
            SongPlayer.addChatMessage("§6Disabled looping");
        } else {
            SongPlayer.addChatMessage("§6Enabled looping");
        }

        return true;
    }
}
