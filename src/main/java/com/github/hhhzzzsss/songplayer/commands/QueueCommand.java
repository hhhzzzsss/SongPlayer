package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.song.Song;

class QueueCommand extends Command {
    public String getName() {
        return "queue";
    }
    public String[] getAliases() {
        return new String[]{"showQueue"};
    }

    public String getDescription() {
        return "Shows the current song queue";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) return false;

        if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
            SongPlayer.addChatMessage("§6No song is currently playing");
            return true;
        }

        SongPlayer.addChatMessage("§6------------------------------");
        if (SongHandler.getInstance().currentSong != null) {
            SongPlayer.addChatMessage("§6Current song: §3" + SongHandler.getInstance().currentSong.name);
        }
        int index = 0;
        for (Song song : SongHandler.getInstance().songQueue) {
            index++;
            SongPlayer.addChatMessage(String.format("§6%d. §3%s", index, song.name));
        }
        SongPlayer.addChatMessage("§6------------------------------");
        return true;
    }
}