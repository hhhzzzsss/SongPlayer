package com.github.hhhzzzsss.songplayer.song;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NBSConverter {
    public static Instrument[] instrumentIndex = new Instrument[] {
            Instrument.HARP,
            Instrument.BASS,
            Instrument.BASEDRUM,
            Instrument.SNARE,
            Instrument.HAT,
            Instrument.GUITAR,
            Instrument.FLUTE,
            Instrument.BELL,
            Instrument.CHIME,
            Instrument.XYLOPHONE,
            Instrument.IRON_XYLOPHONE,
            Instrument.COW_BELL,
            Instrument.DIDGERIDOO,
            Instrument.BIT,
            Instrument.BANJO,
            Instrument.PLING,
    };

    private static class NBSNote {
        public int tick;
        public short layer;
        public byte instrument;
        public byte key;
        public byte velocity = 100;
        public byte panning = 100;
        public short pitch = 0;
    }

    private static class NBSLayer {
        public String name;
        public byte lock = 0;
        public byte volume;
        public byte stereo = 100;
    }

    public static Song getSongFromBytes(byte[] bytes, String fileName) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        short songLength = 0;
        byte format = 0;
        byte vanillaInstrumentCount = 0;
        songLength = buffer.getShort(); // If it's not 0, then it uses the old format
        if (songLength == 0) {
            format = buffer.get();
        }
        if (format >= 1) {
            vanillaInstrumentCount = buffer.get();
        }
        if (format >= 3) {
            songLength = buffer.getShort();
        }
        short layerCount = buffer.getShort();
        String songName = getString(buffer, bytes.length);
        String songAuthor = getString(buffer, bytes.length);
        String songOriginalAuthor = getString(buffer, bytes.length);
        String songDescription = getString(buffer, bytes.length);
        short tempo = buffer.getShort();
        byte autoSaving = buffer.get();
        byte autoSavingDuration = buffer.get();
        byte timeSignature = buffer.get();
        int minutesSpent = buffer.getInt();
        int leftClicks = buffer.getInt();
        int rightClicks = buffer.getInt();
        int blocksAdded = buffer.getInt();
        int blocksRemoved = buffer.getInt();
        String origFileName = getString(buffer, bytes.length);

        byte loop = 0;
        byte maxLoopCount = 0;
        short loopStartTick = 0;
        if (format >= 4) {
            loop = buffer.get();
            maxLoopCount = buffer.get();
            loopStartTick = buffer.getShort();
        }

        ArrayList<NBSNote> nbsNotes = new ArrayList<>();
        short tick = -1;
        while (true) {
            int tickJumps = buffer.getShort();
            if (tickJumps == 0) {
                break;
            }
            tick += tickJumps;

            short layer = -1;
            while (true) {
                int layerJumps = buffer.getShort();
                if (layerJumps == 0) {
                    break;
                }
                layer += layerJumps;
                NBSNote note = new NBSNote();
                note.tick = tick;
                note.layer = layer;
                note.instrument = buffer.get();
                note.key = buffer.get();
                if (format >= 4) {
                    note.velocity = buffer.get();
                    note.panning = buffer.get();
                    note.pitch = buffer.getShort();
                }
                nbsNotes.add(note);
            }
        }

        ArrayList<NBSLayer> nbsLayers = new ArrayList<>();
        if (buffer.hasRemaining()) {
            for (int i=0; i<layerCount; i++) {
                NBSLayer layer = new NBSLayer();
                layer.name = getString(buffer, bytes.length);
                if (format >= 4) {
                    layer.lock = buffer.get();
                }
                layer.volume = buffer.get();
                if (format >= 2) {
                    layer.stereo = buffer.get();
                }
                nbsLayers.add(layer);
            }
        }

        Song song = new Song(songName.trim().length() > 0 ? songName : fileName);
        if (loop > 0) {
            song.looping = true;
            song.loopPosition = getMilliTime(loopStartTick, tempo);
            song.loopCount = maxLoopCount;
        }
        int startnotes = 0;
        for (NBSNote note : nbsNotes) {
            Instrument instrument;
            if (note.instrument >= instrumentIndex.length) {
                continue;
            }
            if (note.key < 33 || note.key > 57) {
                continue;
            }

            startnotes += 1;

            instrument = instrumentIndex[note.instrument];

            byte layerVolume = 100;
            if (nbsLayers.size() > note.layer) {
                layerVolume = nbsLayers.get(note.layer).volume;
            }
            int pitch = note.key-33;
            int noteId = pitch + instrument.instrumentId*25;
            song.add(new Note(noteId, getMilliTime(note.tick, tempo), (byte) (note.velocity/100.0*layerVolume/100.0*127), note.pitch));
        }

        if (SongPlayer.kiosk) {
            Util.broadcastMessage("[{\"color\":\"gold\",\"text\":\"Loaded \"}, {\"color\":\"dark_aqua\",\"text\":\"" + startnotes + "\"}, {\"color\":\"gold\",\"text\":\" notes\"}]", null);
        }

        song.length = song.get(song.size()-1).time + 50;

        return song;
    }

    public static void outputSong(Song song) {
        ArrayList<Integer> notes = new ArrayList<>();
        ArrayList<Integer> layerOffset = new ArrayList<>();
        HashMap<Integer, Integer> LayersNeededPerInstrument = new HashMap<>();
        HashMap<Integer, Integer> instrumentsPlayedThisTick = new HashMap<>();
        ByteBuffer buffer = ByteBuffer.allocate(65 + (7 * 23) + (song.notes.size() * 12));

        int lasttick = -1;
        //check how many layers we need to add per instrument
        for (Note n : song.notes) {
            int tick = (int) (n.time / 50);
            int instrument = n.noteId / 25;
            if (tick != lasttick) {
                lasttick = tick;
                for (Map.Entry<Integer, Integer> e : instrumentsPlayedThisTick.entrySet()) {
                    if (!LayersNeededPerInstrument.containsKey(e.getKey())) {
                        LayersNeededPerInstrument.put(e.getKey(), e.getValue());
                    } else {
                        if (e.getValue() > LayersNeededPerInstrument.get(e.getKey())) {
                            LayersNeededPerInstrument.replace(e.getKey(), e.getValue());
                        }
                    }
                }
                instrumentsPlayedThisTick.clear();
            }
            if (!instrumentsPlayedThisTick.containsKey(instrument)) {
                instrumentsPlayedThisTick.put(instrument, 0);
            }
            instrumentsPlayedThisTick.merge(instrument, 1, Integer::sum);
        }
        instrumentsPlayedThisTick.clear();

        int neededLayers = 0;
        for (int i : LayersNeededPerInstrument.values()) {
            neededLayers += i;
        }
        if (neededLayers > 255) {
            SongPlayer.addChatMessage("layers needed is over 255 :(");
            return;
        }
        lasttick = 0;

        //declare offset needed for every instrument
        for (int i = 0; i < 16; i++) {
            if (!LayersNeededPerInstrument.containsKey(i)) {
                layerOffset.add(i, 0);
                continue;
            }
            layerOffset.add(i, lasttick);
            lasttick += LayersNeededPerInstrument.get(i);
        }

        HashMap<Integer, Byte> noteVolumeMap = new HashMap<>();

        lasttick = 0;
        int lastnotetick = (int) (song.notes.get(0).time / 50);
        int tick = 1;
        try {
            buffer.putShort((short) 0x0000); // 2 bytes are 0
            buffer.put((byte) 0x05); //nbs version
            buffer.put((byte) 0x10); // instruments (16)
            buffer.putShort((short) 0x1000); // song length
            buffer.put((byte) neededLayers); // layers
            buffer.put((byte) 0x00);
            buffer.putInt(0); // name
            buffer.putInt(0); // author
            buffer.putInt(0); // Song original name?
            buffer.putInt(0); // song description
            buffer.putShort((short) 0xD007); // tempo * 100 (2000)
            buffer.put((byte) 0x00); // autosave (unused)
            buffer.put((byte) 0x00); // autosave interval (unused)
            buffer.put((byte) 0x04); // time signature (4)
            buffer.putInt(0); // minutes spent
            buffer.putInt(0); // amount of left clicks
            buffer.putInt(0); // amount of right clicks
            buffer.putInt(0); // noteblocks added
            buffer.putInt(0); // noteblocks removed
            buffer.putInt(0); // MIDI / schematic file name
            buffer.put((byte) 0x00); // looping enabled
            buffer.put((byte) 0x00); // max loop count
            buffer.putShort((short) 0x0000); // Loop start tick

            int currentlayer;
            boolean firstset = true;
            for (Note note : song.notes) {
                if (lastnotetick == (int) (note.time / 50)) {
                    //add the note to the list if it isn't already
                    if (!notes.contains(note.noteId)) {
                        notes.add(note.noteId);
                        noteVolumeMap.put(note.noteId, note.volume);
                    }
                    if (song.notes.indexOf(note) != song.notes.size() - 1) {
                        continue;
                    }
                    SongPlayer.addChatMessage("Finished going thru all the notes");
                }
                lastnotetick = (int) (note.time / 50);

                //now that we have all the notes, continue to adding them to the song

                if (!firstset) {
                    buffer.putShort((short) 0x0000); // Y - is 0 go back to X
                    buffer.put((byte) (tick - lasttick)); //tick offset
                    buffer.put((byte) 0x00);
                } else {
                    buffer.put((byte) tick);
                    buffer.put((byte) 0x00);
                    firstset = false;
                }

                Collections.sort(notes);

                //add all notes from the notes list here
                int offset;
                currentlayer = 0;
                for (int id : notes) {
                    int baseinstrument = id / 25;

                    if (currentlayer < layerOffset.get(baseinstrument)) {
                        offset = layerOffset.get(baseinstrument) - currentlayer;
                        currentlayer = layerOffset.get(baseinstrument);
                    } else {
                        offset = 1;
                        currentlayer++;
                    }
                    buffer.put((byte) offset); // layer offset
                    buffer.put((byte) 0x00);
                    byte instrument = 0x00;
                    for (byte i = 0; i < instrumentIndex.length; i++) {
                        if (instrumentIndex[i].instrumentId == id / 25) {
                            instrument = i;
                            break;
                        }
                    }
                    int iv = (noteVolumeMap.get(id) * 127) / 100;
                    byte volume = (byte) (iv);
                    //volume *= 0.787401574803;
                    if (volume > 100) volume = 100;
                    buffer.put(instrument); // instrument of note
                    buffer.put((byte) ((id % 25) + 33)); // pitch / key
                    buffer.put(volume); // volume / velocity
                    buffer.put((byte) 100); // panning
                    buffer.putShort((short) 0x0000); // pitch / fine-tuning
                }
                notes.clear();
                noteVolumeMap.clear();
                notes.add(note.noteId);
                noteVolumeMap.put(note.noteId, note.volume);
                lasttick = tick;
                tick = (int) (note.time / 50);
            }
            buffer.putInt(0);
            for (int i = 0; i < neededLayers; i++) {
                buffer.putInt(0);
                buffer.put((byte) 0x00);
                buffer.put((byte) 0x64);
                buffer.put((byte) 0x00);
            }
            buffer.limit(buffer.position() + 1);
        } catch (BufferOverflowException | NumberFormatException e) {
            e.printStackTrace();
            SongHandler.getInstance().converting = false;
            SongHandler.getInstance().convertintto = "";
            SongPlayer.addChatMessage("Failed to convert to NBS: " + e.getMessage());
            return;
        }

        try {
            byte[] out = buffer.array();
            File file = new File(SongPlayer.SONG_DIR + "/" + song.name + ".nbs");
            file.createNewFile();
            FileOutputStream o = new FileOutputStream(file);
            o.write(out);
        } catch(IOException e) {
            e.printStackTrace();
            SongHandler.getInstance().converting = false;
            SongHandler.getInstance().convertintto = "";
            SongPlayer.addChatMessage("Failed to convert to NBS: " + e.getMessage());
            return;
        }
        SongPlayer.addChatMessage("§6wrote converted file as §3" + song.name + ".nbs");
    }

    private static String getString(ByteBuffer buffer, int maxSize) throws IOException {
        int length = buffer.getInt();
        if (length > maxSize) {
            throw new IOException("String is too large");
        }
        byte arr[] = new byte[length];
        buffer.get(arr, 0, length);
        return new String(arr);
    }

    private static int getMilliTime(int tick, int tempo) {
        return 1000 * tick * 100 / tempo;
    }

}
