package com.github.hhhzzzsss.songplayer;

import java.io.File;

import com.github.hhhzzzsss.songplayer.noteblocks.Stage;
import com.github.hhhzzzsss.songplayer.song.Song;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

public class SongPlayer implements ModInitializer {
	
	public static final MinecraftClient MC = MinecraftClient.getInstance();
	public static final int NOTEBLOCK_BASE_ID = Block.getRawIdFromState(Blocks.NOTE_BLOCK.getDefaultState());

	public static final File SONG_DIR = new File("songs");
	public static boolean showFakePlayer = false;
	public static FakePlayerEntity fakePlayer;
	public static String creativeCommand = "gmc";
	public static String survivalCommand = "gms";
	
	@Override
	public void onInitialize() {
		if (!SONG_DIR.exists()) {
			SONG_DIR.mkdir();
		}

		CommandProcessor.initCommands();
	}
	
	public static void addChatMessage(String message) {
		MC.player.sendMessage(Text.of(message), false);
	}

	public static void removeFakePlayer() {
		if (fakePlayer != null) {
			fakePlayer.remove(Entity.RemovalReason.DISCARDED);
			fakePlayer = null;
		}
	}
}
