package com.github.hhhzzzsss.songplayer;

import java.io.File;

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
	public static final File SONGPLAYER_DIR = new File("SongPlayer");
	public static FakePlayerEntity fakePlayer;

	@Override
	public void onInitialize() {
		if (!SONG_DIR.exists()) {
			SONG_DIR.mkdir();
		}
		if (!SONGPLAYER_DIR.exists()) {
			SONGPLAYER_DIR.mkdir();
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
