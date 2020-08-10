package com.github.hhhzzzsss.songplayer.song;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import com.github.hhhzzzsss.songplayer.SongPlayer;

public class Song {
	public ArrayList<NoteEvent> notes = new ArrayList<>();
	public String name;
	public int position = 0;
	public boolean[] requiredNotes = new boolean[400];
	public boolean loop = false;
	public int gotoTime = -1;
	
	private Song() {}
	
	public NoteEvent get(int i) {
		return notes.get(i);
	}
	
	public void add(NoteEvent e) {
		notes.add(e);
	}
	
	public int size() {
		return notes.size();
	}
	
	public static Song getSongFromFile(String file) throws IOException {
		Song song = new Song();
		if (file.contains("/") || file.contains("\\")) throw new FileNotFoundException();
    	File songPath = new File(SongPlayer.SONG_DIR, file);
		if (!songPath.exists()) songPath = new File(SongPlayer.SONG_DIR, file + ".txt");
    	if (!songPath.exists()) throw new FileNotFoundException();
    	song.notes.clear();
    	BufferedReader br = new BufferedReader(new FileReader(songPath));
    	String line;
    	while ((line = br.readLine()) != null) {
    		String[] split = line.split(" ");
    		long time = Long.parseLong(split[0]);
    		int note = Integer.parseInt(split[1]);
    		song.requiredNotes[note] = true;
    		song.notes.add(new NoteEvent(note, time));
    	}
    	br.close();
    	song.name = songPath.getName();
    	return song;
	}
	
	public static Song getSongFromUrl(String url) throws Exception {
		Song song = new Song();
		song.notes.clear();
    	TreeMap<Long, ArrayList<Integer>> noteMap = MidiConverter.getMidi(url);
    	System.out.println(noteMap.size());
    	for (int i=0; i<400; i++) song.requiredNotes[i] = false;
    	for (Map.Entry<Long, ArrayList<Integer>> entry : noteMap.entrySet()) {
			for (int note : entry.getValue()) {
				long time = entry.getKey();
				song.requiredNotes[note] = true;
				song.add(new NoteEvent(note, time));
			}
		}
    	song.name = url.substring(url.lastIndexOf('/')+1, url.length());
    	
    	return song;
	}
}
