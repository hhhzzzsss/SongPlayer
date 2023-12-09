package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.commands.CommandProcessor;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;

public class SongPlayer implements ModInitializer {

	public static final MinecraftClient MC = MinecraftClient.getInstance();
	public static final int NOTEBLOCK_BASE_ID = Block.getRawIdFromState(Blocks.NOTE_BLOCK.getDefaultState())-1;

	public static final Path SONG_DIR = Path.of("songs");
	public static final Path SONGPLAYER_DIR = Path.of("SongPlayer");
	public static final Path PLAYLISTS_DIR = Path.of("SongPlayer/playlists");
	public static FakePlayerEntity fakePlayer;

	@Override
	public void onInitialize() {
		if (!Files.exists(SONG_DIR)) {
			Util.createDirectoriesSilently(SONG_DIR);
		}
		if (!Files.exists(SONGPLAYER_DIR)) {
			Util.createDirectoriesSilently(SONGPLAYER_DIR);
		}
		if (!Files.exists(PLAYLISTS_DIR)) {
			Util.createDirectoriesSilently(PLAYLISTS_DIR);
		}

		CommandProcessor.initCommands();
	}

	public static void addChatMessage(String message) {
		MC.player.sendMessage(Text.of(message), false);
	}

	public static void addChatMessage(Text text) {
		MC.player.sendMessage(text, false);
	}

	public static void removeFakePlayer() {
		if (fakePlayer != null) {
			fakePlayer.remove(Entity.RemovalReason.DISCARDED);
			fakePlayer = null;
		}
	}
}
