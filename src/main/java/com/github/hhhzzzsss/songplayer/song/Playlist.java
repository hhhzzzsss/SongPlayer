package com.github.hhhzzzsss.songplayer.song;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Playlist {
    public static final String INDEX_FILE_NAME = "index.json";
    private static final Gson gson = new Gson();

    public String name;
    public boolean loop = false;
    public boolean shuffle = false;

    public List<String> index;
    public List<File> songFiles;
    public List<Song> songs = new ArrayList<>();
    public List<Integer> ordering = null;
    public int songNumber = 0;
    public boolean loaded = false;
    public ArrayList<String> songsFailedToLoad = new ArrayList<>();

    public Playlist(File directory, boolean loop, boolean shuffle) {
        this.name = directory.getName();
        this.loop = loop;
        this.shuffle = shuffle;
        this.setShuffle(this.shuffle);
        if (directory.isDirectory()) {
            index = validateAndLoadIndex(directory);
            songFiles = index.stream()
                    .map(name -> new File(directory, name))
                    .collect(Collectors.toList());
            (new PlaylistLoaderThread()).start();
        } else {
            ordering = new ArrayList<>();
            loaded = true;
        }
    }

    private class PlaylistLoaderThread extends Thread {
        @Override
        public void run() {
            for (File file : songFiles) {
                SongLoaderThread slt = new SongLoaderThread(file);
                slt.run();
                if (slt.exception != null) {
                    songsFailedToLoad.add(file.getName());
                } else {
                    songs.add(slt.song);
                }
            }
            // Don't run loadOrdering asynchronously to avoid concurrency issues
            SongPlayer.MC.execute(() -> {
                loadOrdering(shuffle);
                loaded = true;
            });
        }
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void setShuffle(boolean shuffle) {
        if (this.shuffle != shuffle && loaded) {
            loadOrdering(shuffle);
        }
        this.shuffle = shuffle;
    }

    public void loadOrdering(boolean shouldShuffle) {
        songNumber = 0;
        ordering = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            ordering.add(i);
        }
        if (shouldShuffle) {
            Collections.shuffle(ordering);
        }
    }

    public Song getNext() {
        if (songs.isEmpty()) return null;

        if (songNumber >= songs.size()) {
            if (loop) {
                loadOrdering(shuffle);
            } else {
                return null;
            }
        }

        return songs.get(ordering.get(songNumber++));
    }

    private static List<String> validateAndLoadIndex(File directory) {
        List<String> songNames = getSongFiles(directory).stream()
                .map(File::getName)
                .collect(Collectors.toList());
        if (!getIndexFile(directory).exists()) {
            saveIndexSilently(directory, songNames);
            return songNames;
        }
        else {
            try {
                List<String> index = loadIndex(directory);
                boolean modified = false;

                // Remove nonexistent songs from index
                HashSet<String> songSet = new HashSet<>(songNames);
                ListIterator<String> indexItr = index.listIterator();
                while (indexItr.hasNext()) {
                    if (!songSet.contains(indexItr.next())) {
                        indexItr.remove();
                        modified=true;
                    }
                }

                // Add songs that aren't in the index to the index
                HashSet<String> indexSet = new HashSet<>(index);
                for (String name : songNames) {
                    if (!indexSet.contains(name)) {
                        index.add(name);
                        indexSet.add(name);
                        modified = true;
                    }
                }

                if (modified) {
                    saveIndexSilently(directory, index);
                }

                return index;
            }
            catch (Exception e) {
                return songNames;
            }
        }
    }

    public static File getIndexFile(File directory) {
        return new File(directory, INDEX_FILE_NAME);
    }

    public static List<File> getSongFiles(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        return Arrays.stream(files)
                .filter(file -> !file.getName().equals(INDEX_FILE_NAME))
                .collect(Collectors.toList());
    }

    private static List<String> loadIndex(File directory) throws IOException {
        File indexFile = getIndexFile(directory);
        FileInputStream fis = new FileInputStream(indexFile);
        InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(isr);
        Type type = new TypeToken<ArrayList<String>>(){}.getType();
        List<String> index = gson.fromJson(reader, type);
        reader.close();
        return index;
    }

    private static void saveIndex(File directory, List<String> index) throws IOException {
        File indexFile = getIndexFile(directory);
        FileOutputStream fos = new FileOutputStream(indexFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        BufferedWriter writer = new BufferedWriter(osw);
        writer.write(gson.toJson(index));
        writer.close();
    }

    private static void saveIndexSilently(File directory, List<String> index) {
        try {
            saveIndex(directory, index);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> listSongs(File directory) throws IOException {
        if (!directory.exists()) {
            throw new IOException("Playlist does not exist");
        }
        return validateAndLoadIndex(directory);
    }

    public static void createPlaylist(String playlist) {
        File playlistDir = new File(SongPlayer.PLAYLISTS_DIR, playlist);
        playlistDir.mkdir();
    }

    public static void addSong(File directory, File songFile) throws IOException {
        if (!directory.exists()) {
            throw new IOException("Playlist does not exist");
        }
        if (!songFile.exists()) {
            throw new IOException("Could not find specified song");
        }

        List<String> index = validateAndLoadIndex(directory);
        if (index.contains(songFile.getName())) {
            throw new IOException("Playlist already contains a song by this name");
        }
        Files.copy( songFile.toPath(), (new File(directory,songFile.getName())).toPath() );
        index.add(songFile.getName());
        saveIndex(directory, index);
    }

    public static void removeSong(File directory, String songName) throws IOException {
        if (!directory.exists()) {
            throw new IOException("Playlist does not exist");
        }
        File songFile = new File(directory, songName);
        if (!songFile.exists()) {
            throw new IOException("Playlist does not contain a song by this name");
        }

        List<String> index = validateAndLoadIndex(directory);
        songFile.delete();
        index.remove(songName);
        saveIndex(directory, index);
    }

    public static void deletePlaylist(File directory) throws IOException {
        if (!directory.exists()) {
            throw new IOException("Playlist does not exist");
        }
        Files.walk(directory.toPath())
                .map(Path::toFile)
                .sorted(Comparator.reverseOrder())
                .forEach(File::delete);
    }

    public static void renameSong(File directory, String oldName, String newName) throws IOException {
        List<String> index = validateAndLoadIndex(directory);
        int pos = index.indexOf(oldName);
        if (pos < 0) {
            throw new IOException("Song not found in playlist");
        }
        (new File(directory, oldName)).renameTo(new File(directory, newName));
        index.set(pos, newName);
        saveIndex(directory, index);
    }
}
