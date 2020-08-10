package com.github.hhhzzzsss.songplayer.noteblocks;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.song.Song;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;

public class PlayingThread extends Thread{
	private final ClientPlayerEntity player = SongPlayer.MC.player;
	private final Stage stage = SongPlayer.stage;
	private final Song song = SongPlayer.song;
	
	public void run() {
		player.sendChatMessage(SongPlayer.survivalCommand);
		while (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
			if (SongPlayer.mode != SongPlayer.Mode.PLAYING) {return;}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
		stage.rebuild = false;
		
		player.abilities.allowFlying = true;
		player.abilities.flying = true;
		SongPlayer.stage.movePlayerToStagePosition();
		
		long songStartTime = System.currentTimeMillis() - song.get(song.position).time;
		while (song.position < song.size()) {
			long playTime = System.currentTimeMillis() - songStartTime;
			while (song.position < song.size() && song.get(song.position).time <= playTime) {
				if (SongPlayer.mode != SongPlayer.Mode.PLAYING) {return;}
				SongPlayer.MC.interactionManager.attackBlock(stage.tunedNoteblocks[song.get(song.position).note], Direction.UP);
				SongPlayer.MC.interactionManager.cancelBlockBreaking();
				song.position++;
				if (stage.rebuild) {
					SongPlayer.addChatMessage("§6Stage has been modified. Retuning!");
					SongPlayer.mode = SongPlayer.Mode.BUILDING;
					(new BuildingThread()).start();
					return;
				}
				if (song.gotoTime > -1) {
					for (int i = 0; i < song.size(); i++) {
						if (song.get(i).time >= song.gotoTime) {
							song.position = i;
							song.gotoTime = -1;
							SongPlayer.addChatMessage("§6Changed song position");
				    		(new PlayingThread()).start();
							return;
						}
					}
					SongPlayer.addChatMessage("§cNot a valid time stamp");
					song.gotoTime = -1;
				}
			}
			if (song.position < song.size()) {
				playTime = System.currentTimeMillis() - songStartTime;
				long sleepTime = playTime - song.get(song.position).time;
				if (sleepTime > 0) {
					if (sleepTime > 200) {
						System.out.println("Big sleep time: " + sleepTime);
					}
					try {
						Thread.sleep(playTime-song.get(song.position).time);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			else {
				if (song.loop) {
					song.position = 0;
		    		songStartTime = System.currentTimeMillis();
				}
				else {
					// Do nothing. While loop condition is false so loop exits.
				}
			}
		}

		player.abilities.allowFlying = true;
		player.abilities.flying = true;
		SongPlayer.stage.movePlayerToStagePosition();
		
		SongPlayer.addChatMessage("§6Finished playing.");
		SongPlayer.mode = SongPlayer.Mode.IDLE;
	}
}
