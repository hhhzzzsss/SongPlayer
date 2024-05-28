package com.github.hhhzzzsss.songplayer.song;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class TxtConverter {
    public static Song getSong(File file) throws IOException {
        if (!file.getName().endsWith(".txt")) {
            return null;
        }
        Song song = new Song(file.getName());
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;
        int lasttick = 0;
        int linen = 0;
        int startnotes = 0;

        while ((line = br.readLine()) != null) {
            linen++;
            if (line.startsWith("#") || line.isBlank()) continue;
            String[] parts = line.split(":");
            if (parts.length != 3) {
                continue;
            }
            int tick;
            int pitch;
            int instrument;
            try {
                tick = Integer.parseInt(parts[0]);
                pitch = Integer.parseInt(parts[1]);
                instrument = Integer.parseInt(parts[2]);
            } catch (NumberFormatException | ClassCastException | NullPointerException e) {
                SongPlayer.addChatMessage("§cFailed to load song: Found incorrectly formatted line at line #§4" + linen);
                return null;
            }
            if (tick < 0 || pitch < 0 || instrument < 0 || pitch > 24 || instrument > 15) {
                SongPlayer.addChatMessage("§cFailed to load song: Found incorrectly formatted line at line #§4" + linen);
                return null;
            }
            if (tick > lasttick) {
                lasttick = tick;
            }
            startnotes += 1;
            song.add(new Note((instrument * 25 + pitch), tick * 50, (byte) 127, (short) 0));
        }

        if (SongPlayer.kiosk) {
            Util.broadcastMessage("[{\"color\":\"gold\",\"text\":\"Loaded \"}, {\"color\":\"dark_aqua\",\"text\":\"" + startnotes + "\"}, {\"color\":\"gold\",\"text\":\" notes\"}]", null);
        }

        song.length = lasttick * 50;
        return song;
    }

    public static void outputSong(Song song) {
        try {
            FileWriter write = new FileWriter(SongPlayer.SONG_DIR + "/" + song.name + ".txt");
            write.write("# this file was generated with SongPlayer's conversion feature\n");
            write.write("# https://github.com/Sk8kman/SongPlayer\n");
            HashMap<Long, ArrayList<String>> notes = new HashMap<>();
            for (Note note : song.notes) {
                int instrument = note.noteId / 25;
                int pitchID = (note.noteId % 25);
                long time = note.time / 50;
                if (!notes.containsKey(time)) {
                    notes.put(time, new ArrayList<>());
                }
                if (notes.get(time).contains(pitchID + ":" + instrument)) continue;
                notes.get(time).add(pitchID + ":" + instrument);
                String line = time + ":" + pitchID + ":" + instrument;
                write.write(line + "\n");
            }
            write.close();
            notes.clear();
            SongPlayer.addChatMessage("§6wrote converted file as §3" + song.name + ".txt");
        } catch (IOException e) {
            e.printStackTrace();
            SongPlayer.addChatMessage("§cThere was an error when attempting to save your song. Please check the minecraft logs for more information.");
        }
    }
}