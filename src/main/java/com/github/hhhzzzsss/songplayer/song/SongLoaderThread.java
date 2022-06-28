package com.github.hhhzzzsss.songplayer.song;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SongLoaderThread extends Thread{
	
	private String location;
	private File songPath;
	private URL songUrl;
	public Exception exception;
	public Song song;

	private boolean isUrl = false;

	public SongLoaderThread(String location) throws IOException {
		this.location = location;
		if (location.startsWith("http://") || location.startsWith("https://")) {
			isUrl = true;
			songUrl = new URL(location);
		}
		else if (location.contains("/") || location.contains("\\")) {
			throw new IOException("Invalid characters in song name: " + location);
		}
		else if (getSongFile(location).exists()) {
			songPath = getSongFile(location);
		}
		else if (getSongFile(location+".mid").exists()) {
			songPath = getSongFile(location+".mid");
		}
		else if (getSongFile(location+".midi").exists()) {
			songPath = getSongFile(location+".midi");
		}
		else if (getSongFile(location+".nbs").exists()) {
			songPath = getSongFile(location+".nbs");
		}
		else {
			throw new IOException("Could not find song: " + location);
		}
	}
	
	public void run() {
		try {
			byte[] bytes;
			String name;
			if (isUrl) {
				bytes = DownloadUtils.DownloadToByteArray(songUrl, 10*1024*1024);
				name = Paths.get(songUrl.toURI().getPath()).getFileName().toString();
			}
			else {
				bytes = Files.readAllBytes(songPath.toPath());
				name = songPath.getName();
			}

			try {
				song = MidiConverter.getSongFromBytes(bytes, name);
			}
			catch (Exception e) {}

			if (song == null) {
				try {
					song = NBSConverter.getSongFromBytes(bytes, name);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (song == null) {
				throw new IOException("Invalid song format");
			}

		}
		catch (Exception e) {
			exception = e;
		}
	}

	private File getSongFile(String name) {
		return new File(SongPlayer.SONG_DIR, name);
	}
}
