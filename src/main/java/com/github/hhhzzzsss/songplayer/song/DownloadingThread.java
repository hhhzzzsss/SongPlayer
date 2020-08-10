package com.github.hhhzzzsss.songplayer.song;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.noteblocks.BuildingThread;

public class DownloadingThread extends Thread{
	
	private String url;
	public DownloadingThread(String url) {
		this.url = url;
	}
	
	public void run() {
		try {
			SongPlayer.song = Song.getSongFromUrl(url);
			SongPlayer.addChatMessage("§6Finished downloading song");
			SongPlayer.mode = SongPlayer.Mode.BUILDING;
			SongPlayer.addChatMessage("§6Starting building.");
			(new BuildingThread()).start();
			return;
		} catch (Exception e) {
			SongPlayer.addChatMessage("§cError getting song from url: " + e.getMessage());
			SongPlayer.mode = SongPlayer.Mode.IDLE;
		}
	}
}
