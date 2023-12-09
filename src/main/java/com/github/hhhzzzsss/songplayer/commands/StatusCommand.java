package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.song.Song;

class StatusCommand extends Command {
    public String getName() {
        return "status";
    }
    public String[] getAliases() {
        return new String[]{"current"};
    }
    public String getDescription() {
        return "Gets the status of the song that is currently playing";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) return false;

        if (SongHandler.getInstance().currentSong == null) {
            SongPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }

        Song currentSong = SongHandler.getInstance().currentSong;
        long currentTime = Math.min(currentSong.time, currentSong.length);
        long totalTime = currentSong.length;

        SongPlayer.addChatMessage(String.format("§6Currently playing %s §3(%s/%s)", currentSong.name, Util.formatTime(currentTime), Util.formatTime(totalTime)));
        return true;
    }
}
