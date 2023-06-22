package com.github.hhhzzzsss.songplayer.song;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.conversion.MidiConverter;
import com.github.hhhzzzsss.songplayer.conversion.NBSConverter;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SongLoaderThread extends Thread{
	
	private String location;
	private Path songPath;
	private URL songUrl;
	public Exception exception;
	public Song song;
	public String filename;

	private boolean isUrl = false;

	protected SongLoaderThread() {}

	public SongLoaderThread(String location) throws IOException {
		this.location = location;
		if (location.startsWith("http://") || location.startsWith("https://")) {
			isUrl = true;
			songUrl = new URL(location);
		}
		else if (Files.exists(getSongFile(location))) {
			songPath = getSongFile(location);
		}
		else if (Files.exists(getSongFile(location+".mid"))) {
			songPath = getSongFile(location+".mid");
		}
		else if (Files.exists(getSongFile(location+".midi"))) {
			songPath = getSongFile(location+".midi");
		}
		else if (Files.exists(getSongFile(location+".nbs"))) {
			songPath = getSongFile(location+".nbs");
		}
		else {
			throw new IOException("Could not find song: " + location);
		}
	}

	public SongLoaderThread(Path file) {
		this.songPath = file;
	}
	
	public void run() {
		try {
			byte[] bytes;
			if (isUrl) {
				bytes = DownloadUtils.DownloadToByteArray(songUrl, 10*1024*1024);
				filename = Paths.get(songUrl.toURI().getPath()).getFileName().toString();
			}
			else {
				bytes = Files.readAllBytes(songPath);
				filename = songPath.getFileName().toString();
			}

			try {
				song = MidiConverter.getSongFromBytes(bytes, filename);
			}
			catch (Exception e) {}

			if (song == null) {
				try {
					song = NBSConverter.getSongFromBytes(bytes, filename);
				}
				catch (Exception e) {}
			}

			if (song == null) {
				throw new IOException("Invalid song format");
			}
		}
		catch (Exception e) {
			exception = e;
		}
	}

	private Path getSongFile(String name) throws IOException {
		return Util.resolveWithIOException(SongPlayer.SONG_DIR, name);
	}
}
