package com.github.hhhzzzsss.songplayer.song;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
	
	public static Song getSongFromFile(String file) throws Exception {
		Song song = new Song();
		if (file.contains("/") || file.contains("\\")) throw new FileNotFoundException();
    	File songPath = new File(SongPlayer.SONG_DIR, file);
		if (!songPath.exists()) {
			songPath = new File(SongPlayer.SONG_DIR, file + ".txt");
		}
		if (!songPath.exists()) {
			songPath = new File(SongPlayer.SONG_DIR, file + ".mid");
		}
		if (!songPath.exists()) {
			songPath = new File(SongPlayer.SONG_DIR, file + ".midi");
		}
		if (!songPath.exists()) throw new FileNotFoundException();
    	boolean isMidi = false;
    	String extension = getExtension(songPath);
    	if (extension.equalsIgnoreCase("mid") || extension.equalsIgnoreCase("midi")) isMidi = true;
    	if (isMidi) {
    		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(songPath));
    		song.notes.clear();
        	TreeMap<Long, ArrayList<Integer>> noteMap = MidiConverter.getMidi(bis);
        	System.out.println(noteMap.size());
        	for (int i=0; i<400; i++) song.requiredNotes[i] = false;
        	for (Map.Entry<Long, ArrayList<Integer>> entry : noteMap.entrySet()) {
    			for (int note : entry.getValue()) {
    				long time = entry.getKey();
    				song.requiredNotes[note] = true;
    				song.add(new NoteEvent(note, time));
    			}
    		}
        	song.name = songPath.getName();        	
        	return song;
    	}
    	else {
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
	}
	
	public static Song getSongFromUrl(String url) throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);
		URLConnection conn = new URL(url).openConnection();
		conn.setConnectTimeout(5000);
		conn.setReadTimeout(5000);
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0");
		BufferedInputStream downloadStream = new BufferedInputStream(conn.getInputStream());
		Song song = new Song();
		song.notes.clear();
    	TreeMap<Long, ArrayList<Integer>> noteMap = MidiConverter.getMidi(downloadStream);
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
	
	private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
	
	private static String getExtension(File file) {
		   String name = file.getName();
		   if(name.lastIndexOf(".") != -1 && name.lastIndexOf(".") != 0)
		      return name.substring(name.lastIndexOf(".") + 1);
		   else
		      return "";
	}
}
