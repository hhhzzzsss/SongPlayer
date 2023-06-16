package com.github.hhhzzzsss.songplayer.item;

import com.github.hhhzzzsss.songplayer.conversion.SPConverter;
import com.github.hhhzzzsss.songplayer.song.Note;
import com.github.hhhzzzsss.songplayer.song.SongLoaderThread;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.io.IOException;

public class SongItemLoaderThread extends SongLoaderThread {
    public byte[] songData;
    public String displayName;
    public int maxNotesPerSecond = 0;
    public double avgNotesPerSecond = 0;

    public SongItemLoaderThread(ItemStack stack) throws IOException {
        songData = SongItemUtils.getSongData(stack);
        if (songData == null) {
            throw new IOException("Song data is missing");
        }
        NbtCompound songItemNbt = SongItemUtils.getSongItemTag(stack);
        displayName = songItemNbt.getString(SongItemUtils.DISPLAY_NAME_KEY);
        filename = songItemNbt.getString(SongItemUtils.DISPLAY_NAME_KEY);
    }

    @Override
    public void run() {
        try {
            song = SPConverter.getSongFromBytes(songData, filename);
            if (displayName == null || displayName.length() > 0) {
                song.name = displayName;
            }
            if (song.name == null || song.name.length() == 0) {
                song.name = "unnamed";
            }

            song.sort();

            int j = 0;
            int notesInSecond = 0;
            for (Note currNote : song.notes) {
                notesInSecond++;
                while (song.notes.get(j).time + 1000 < currNote.time) {
                    j++;
                    notesInSecond--;
                }
                maxNotesPerSecond = Math.max(notesInSecond, maxNotesPerSecond);
            }
            avgNotesPerSecond = song.notes.size() * 1000.0 / song.length;
        }
        catch (Exception e) {
            exception = e;
            e.printStackTrace();
        }
    }
}