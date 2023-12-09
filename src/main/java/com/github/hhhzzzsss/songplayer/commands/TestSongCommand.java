package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.song.Note;
import com.github.hhhzzzsss.songplayer.song.Song;

class TestSongCommand extends Command {
    public String getName() {
        return "testSong";
    }
    public String getDescription() {
        return "Creates a song for testing";
    }

    public boolean processCommand(String args) {
        if (!args.isEmpty()) return false;

        Song song = new Song("test_song");
        for (int i=0; i<400; i++) {
            song.add(new Note(i, i*50));
        }
        song.length = 400*50;
        SongHandler.getInstance().setSong(song);
        return true;
    }
}