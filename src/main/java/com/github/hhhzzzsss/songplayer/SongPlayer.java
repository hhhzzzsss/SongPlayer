package com.github.hhhzzzsss.songplayer;

import java.io.File;

import com.github.hhhzzzsss.songplayer.config.ModProperties;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;

public class SongPlayer implements ModInitializer {
	//initialize variables
	public static final MinecraftClient MC = MinecraftClient.getInstance();
	public static final int NOTEBLOCK_BASE_ID = Block.getRawIdFromState(Blocks.NOTE_BLOCK.getDefaultState());
	public static final File SONG_DIR = new File("songs");
	public static final File CONFIG_FILE = new File("SongPlayer/songPlayer.properties");
	public static final File PLAYLISTS_DIR = new File("SongPlayer/playlists");
	public static boolean showFakePlayer = true;
	public static com.github.hhhzzzsss.songplayer.FakePlayerEntity fakePlayer;
	public static String survivalCommand;
	public static String creativeCommand;
	public static String adventureCommand;
	public static String spectatorCommand;
	public static String playSoundCommand;
	public static String stageType;
	public static GameMode oldgamemode;
	public static boolean rotate;
	public static boolean swing;
	public static boolean parseVolume;
	public static boolean useCommandsForPlaying;
	public static boolean includeCommandBlocks;
	public static boolean switchGamemode;
	public static boolean useNoteblocksWhilePlaying;
	public static boolean usePacketsOnlyWhilePlaying;
	public static boolean useFramesInsteadOfTicks;
	public static boolean requireExactInstruments;
	public static boolean disablecommandcreative;
	public static boolean disablecommandsurvival;
	public static boolean disablecommandadventure;
	public static boolean disablecommandspectator;
	public static boolean disablecommandplaynote;
	public static boolean disablecommanddisplayprogress;
	public static String showProgressCommand;
	public static String prefix;
	public static byte ignoreNoteThreshold;
	public static int buildDelay;
	public static boolean cleanupstage;
	public static boolean ignoreWarnings;
	public static boolean recording = false;
	public static boolean recordingActive = false;
	public static int recordingtick = 0;
	public static boolean kiosk = false;

	public static float[] pitchGlobal = {
		0.5F, 0.529732F, 0.561231F, 0.594604F, 0.629961F, 0.66742F, 0.707107F, 0.749154F, 0.793701F, 0.840896F, 0.890899F, 0.943874F, 1.0F, 1.059463F, 1.122462F, 1.189207F, 1.259921F, 1.33484F, 1.414214F, 1.498307F, 1.587401F, 1.681793F, 1.781797F, 1.887749F, 2.0F};
	public static String[] instrumentList = {"Harp", "Basedrum", "Snare", "Hat", "Bass", "Flute", "Bell", "Guitar", "Chime", "Xylophone", "Iron Xylophone", "Cow Bell", "Didgeridoo", "Bit", "Banjo", "Pling"};
	@Override
	public void onInitialize() {
		System.out.println("Loading SongPlayer v3.3.0 made by hhhzzzsss, forked by Sk8kman, and tested by Lizard16");
		CommandProcessor.initCommands();
		KioskCommandProcessor.initCommands();
		PLAYLISTS_DIR.mkdirs(); //make directories for everything
		ModProperties.getInstance().setup(); //set up config file
		Util.updateValuesToConfig(); //update values from config file
		/*
		Hello person looking at the source code! This is probably going to be my final build, other than bug fixes and latest version support.
		Huge thanks to LiveOverflow for motivating me to learn how to make mods and make my own bypass to his AntiHuman plugin, as well as hhhzzzsss for making the original SongPlayer

		I had fun developing this modified SongPlayer and hopefully someone can make a fork of this to further improve it later.
		I had weird suggestions that I didn't really think were useful such as playing songs in reverse. But if you want to develop that, feel free to fork this and learn a bit of java!

		...well I guess version 3.3.0 is here, guess it wasn't my last update after all
		 */
	}
	
	public static void addChatMessage(String message) {
		if (MC.world != null) {
			MC.player.sendMessage(Text.of(message), false);
		} else {
			System.out.println(message);
		}
	}

	public static void removeFakePlayer() {
		if (fakePlayer != null) {
			fakePlayer.remove(Entity.RemovalReason.DISCARDED);
			fakePlayer = null;
		}
	}
}
