package com.github.hhhzzzsss.songplayer.conversion;

import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.song.Note;
import com.github.hhhzzzsss.songplayer.song.Song;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SPConverter {
    public static final byte[] FILE_TYPE_SIGNATURE = {-53, 123, -51, -124, -122, -46, -35, 38};
    public static final long MAX_UNCOMPRESSED_SIZE = 50*1024*1024;

    public static Song getSongFromBytes(byte[] bytes, String fileName) throws IOException {
        InputStream is = new Util.LimitedSizeInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)), MAX_UNCOMPRESSED_SIZE);
        bytes = is.readAllBytes();
        is.close();

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (byte b : FILE_TYPE_SIGNATURE) {
            if (b != buffer.get()) {
                throw new IOException("Invalid file type signature");
            }
        }

        byte version = buffer.get();
        // Currently on format version 1
        if (version != 1) {
            throw new IOException("Unsupported format version!");
        }

        long songLength = buffer.getLong();
        String songName = getString(buffer, bytes.length);
        int loop = buffer.get() & 0xFF;
        int loopCount = buffer.get() & 0xFF;
        long loopPosition = buffer.getLong();

        Song song = new Song(songName.trim().length() > 0 ? songName : fileName);
        song.length = songLength;
        song.looping = loop > 0;
        song.loopCount = loopCount;
        song.loopPosition = loopPosition;

        long time = 0;
        while (true) {
            int noteId = buffer.getShort();
            if (noteId >= 0 && noteId < 400) {
                time += getVarLong(buffer);
                song.add(new Note(noteId, time));
            }
            else if ((noteId & 0xFFFF) == 0xFFFF) {
                break;
            }
            else {
                throw new IOException("Song contains invalid note id of " + noteId);
            }
        }

        return song;
    }

    public static byte[] getBytesFromSong(Song song) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream os = new GZIPOutputStream(byteArrayOutputStream);

        byte version = 1;

        os.write(FILE_TYPE_SIGNATURE);
        os.write(version);
        writeLong(os, song.length);
        writeString(os, song.name);
        os.write(song.looping ? 1 : 0);
        os.write(Math.min(song.loopCount, 0xFF));
        writeLong(os, song.loopPosition);

        song.sort();
        long prevTime = 0;
        for (Note note : song.notes) {
            writeShort(os, note.noteId);
            writeVarLong(os, note.time - prevTime);
            prevTime = note.time;
        }
        writeShort(os, 0xFFFF);

        os.close();

        return byteArrayOutputStream.toByteArray();
    }

    private static String getString(ByteBuffer buffer, int maxSize) throws IOException {
        int length = buffer.getInt();
        if (length > maxSize) {
            throw new IOException("String is too large");
        }
        byte[] arr = new byte[length];
        buffer.get(arr, 0, length);
        System.out.println(new String(arr, StandardCharsets.UTF_8));
        return new String(arr, StandardCharsets.UTF_8);
    }

    private static void writeString(OutputStream os, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeInt(os, bytes.length);
        os.write(bytes);
    }

    private static long getVarLong(ByteBuffer buffer) {
        long val = 0;
        long mult = 1;
        int flag = 1;
        while (flag != 0) {
            int b = buffer.get() & 0xFF;
            val += (b & 0x7F) * mult;
            mult <<= 7;
            flag = b >>> 7;
        }
        return val;
    }

    private static void writeVarLong(OutputStream os, long val) throws IOException {
        do {
            int b = (int) (val & 0x7F);
            val >>>= 7;
            if (val > 0) {
                b |= 0x80;
            }
            os.write((byte) b);
        } while (val > 0);
    }

    private static void writeShort(OutputStream os, int val) throws IOException {
        os.write(val & 0xFF);
        os.write((val >>> 8) & 0xFF);
    }

    private static void writeInt(OutputStream os, int val) throws IOException {
        os.write(val & 0xFF);
        os.write((val >>> 8) & 0xFF);
        os.write((val >>> 16) & 0xFF);
        os.write((val >>> 24) & 0xFF);
    }

    private static void writeLong(OutputStream os, long val) throws IOException {
        os.write((int) val & 0xFF);
        os.write((int) (val >>> 8) & 0xFF);
        os.write((int) (val >>> 16) & 0xFF);
        os.write((int) (val >>> 24) & 0xFF);
        os.write((int) (val >>> 32) & 0xFF);
        os.write((int) (val >>> 40) & 0xFF);
        os.write((int) (val >>> 48) & 0xFF);
        os.write((int) (val >>> 56) & 0xFF);
    }
}
