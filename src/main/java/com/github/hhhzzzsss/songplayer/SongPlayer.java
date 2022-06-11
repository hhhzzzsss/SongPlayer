package com.github.hhhzzzsss.songplayer;

import java.io.File;

import com.github.hhhzzzsss.songplayer.noteblocks.Stage;
import com.github.hhhzzzsss.songplayer.song.Song;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.LiteralTextContent;
import net.minecraft.text.Text;

public class SongPlayer implements ModInitializer {
	
	public static final MinecraftClient MC = MinecraftClient.getInstance();
	Freecam freecam;

	public static final File SONG_DIR = new File("songs");
	public static Song song;
	public static Stage stage;
	public static boolean showFakePlayer = false;
	public static FakePlayerEntity fakePlayer;
	public static String creativeCommand = "/gmc";
	public static String survivalCommand = "/gms";
	
	public static enum Mode {
		IDLE,
		BUILDING,
		PLAYING,
		DOWNLOADING,
	}
	public static Mode mode = Mode.IDLE;
	
	@Override
	public void onInitialize() {
		if (!SONG_DIR.exists()) {
			SONG_DIR.mkdir();
		}
		
		freecam = Freecam.getInstance();
		CommandProcessor.initCommands();
	}
	
	public static void addChatMessage(String message) {
		MC.player.sendMessage(Text.of(message), false);
	}
}
