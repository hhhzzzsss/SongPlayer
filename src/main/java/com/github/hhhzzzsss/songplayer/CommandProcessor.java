package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.config.ModProperties;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.song.Note;
import com.github.hhhzzzsss.songplayer.song.Song;
import com.github.hhhzzzsss.songplayer.song.SongLoaderThread;
import com.google.common.io.Files;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandProcessor {
	public static ArrayList<Command> commands = new ArrayList<>();
	public static HashMap<String, Command> commandMap = new HashMap<>();
	public static ArrayList<String> commandCompletions = new ArrayList<>();
	private static ArrayList<String> possibleArguments = new ArrayList<>();

	public static void initCommands() {
		commands.add(new helpCommand());
		commands.add(new playCommand());
		commands.add(new pauseCommand());
		commands.add(new resumeCommand());
		commands.add(new stopCommand());
		commands.add(new skipCommand());
		commands.add(new gotoCommand());
		commands.add(new loopCommand());
		commands.add(new convertCommand());
		commands.add(new playlistCommand());
		commands.add(new statusCommand());
		commands.add(new queueCommand());
		commands.add(new songsCommand());
		commands.add(new playmodeCommand());
		commands.add(new setStage());
		commands.add(new setCommandCommand());
		commands.add(new setCreativeCommandCommand());
		commands.add(new setSurvivalCommandCommand());
		commands.add(new useEssentialsCommandsCommand());
		commands.add(new useVanillaCommandsCommand());
		commands.add(new toggleFakePlayerCommand());
		commands.add(new testSongCommand());
		commands.add(new toggleCommand());
		commands.add(new prefixCommand());
		commands.add(new noteLoudnessThresholdCommand());
		commands.add(new buildDelayCommand());
		commands.add(new RecordCommand());
		commands.add(new toggleKioskCommand());

		for (Command command : commands) {
			commandMap.put(command.getName().toLowerCase(), command);
			commandCompletions.add(command.getName());
			for (String alias : command.getAliases()) {
				commandMap.put(alias.toLowerCase(), command);
				commandCompletions.add(alias);
			}
		}
	}

	// returns true if it is a command and should be cancelled
	public static boolean processChatMessage(String message) {
		if (!message.startsWith(String.valueOf(SongPlayer.prefix))) {
			return false;
		}
		String[] parts = message.substring(SongPlayer.prefix.length()).split(" ");
		String name = parts.length>0 ? parts[0] : "";
		String args = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
		Command c = commandMap.get(name.toLowerCase());
		if (c == null) {
			SongPlayer.addChatMessage("§cUnrecognized command");
		} else {
			boolean success = c.processCommand(args);
			if (!success) {
				SongPlayer.addChatMessage("§6Syntax - §c" + SongPlayer.prefix + c.getSyntax());
			}
		}
		return true;
	}

	private static abstract class Command {
		public abstract String getName();

		public abstract String getSyntax();

		public abstract String getDescription();

		public abstract boolean processCommand(String args);

		public String[] getAliases() {
			return new String[]{};
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			return null;
		}
    }

	private static class setCommandCommand extends Command {
		@Override
		public String getName() {
			return "command";
		}

		@Override
		public String[] getAliases() { return new String[] {"cmd"}; }

		@Override
		public String getSyntax() {
			return "command <get, reset, set, toggle> <survival, creative, adventure, spectator, playnote, displayprogress>";
		}

		@Override
		public String getDescription() {
			return "Changes what commands to run while playing. Use §9" + SongPlayer.prefix + "command get <command name>§3 to check what it is set to. You can run §9" + SongPlayer.prefix + "command toggle <command name>§6 to toggle weather the command can run.\n[There are variables like: {type} = instrument, {volume} = 0.0 - 1.0, {pitch} = pitch of note, {currentTime} = time the song has progressed, {songTime} = duration of song, {MIDI} = name of the current song playing]";
		}

		@Override
		public boolean processCommand(String args) {
			String[] theArgs = args.split(" ");
			if (theArgs.length == 0) {
				return false;
			}
			String[] Commands = {"survival", "creative", "adventure", "spectator", "playnote", "displayprogress"};
			String[] configName = {"survivalCommand", "creativeCommand", "adventureCommand", "spectatorCommand", "playSoundCommand", "showProgressCommand"};
			String[] Values = {SongPlayer.survivalCommand, SongPlayer.creativeCommand, SongPlayer.adventureCommand, SongPlayer.spectatorCommand, SongPlayer.playSoundCommand, SongPlayer.showProgressCommand};
			/*
			$command <get, reset, toggle> <command>
			$command set <command> /<command>
			 */
			switch(theArgs[0].toLowerCase()) {
				case "get": {
					if (theArgs.length != 2) {
						return false;
					}
					for (int i = 0; i < Commands.length; i++) {
						if (theArgs[1].equalsIgnoreCase(Commands[i])) {
							SongPlayer.addChatMessage("§6The command §9" + Commands[i] + "§6 is currently set to: §3/" + Values[i]);
						}
					}
					return true;
				}
				case "set": {
					if (theArgs.length < 3) {
						return false;
					}
					for (int i = 0; i < Commands.length; i++) {
						if (theArgs[1].equalsIgnoreCase(Commands[i])) {
							StringBuilder ncmd = new StringBuilder();
							String newCommand = "";
							for (int s = 2; s < theArgs.length; s++) {
								ncmd.append(theArgs[s] + " ");
							}
							newCommand = ncmd.toString().trim();
							if (newCommand.startsWith("/")) {
								newCommand = newCommand.substring(1);
							}
							if (Values[i].equals(newCommand)) {
								SongPlayer.addChatMessage("§6Nothing changed; Command is already set to §3/" + Values[i]);
								return true;
							}
							SongPlayer.addChatMessage("§6The command §9" + Commands[i] + "§6 has been updated\nfrom: §3/" + Values[i] + "\n§6to: §3/" + newCommand);

							ModProperties.getInstance().updateValue(configName[i], newCommand);
							Util.updateValuesToConfig();
							return true;
						}
					}
					return false;
				}
				case "reset": {
					if (theArgs.length != 2) {
						return false;
					}
					for (int i = 0; i < Commands.length; i++) {
						if (theArgs[1].equalsIgnoreCase(Commands[i])) {
							ModProperties.getInstance().getConfig().remove(configName[i]);
							ModProperties.getInstance().save();
							Util.updateValuesToConfig();
							SongPlayer.addChatMessage("§6The command §9" + Commands[i] + "§6 has been reset to its default value");
							return true;
						}
					}
					return false;
				}
				case "toggle": {
					if (theArgs.length != 2) {
						return false;
					}
					for (int i = 0; i < Commands.length; i++) {
						if (theArgs[1].equalsIgnoreCase(Commands[i])) {
							String configname = Commands[i];
							boolean switchto = !Boolean.parseBoolean((String) ModProperties.getInstance().getConfig().get("disablecommand" + configname));
							ModProperties.getInstance().updateValue("disablecommand" + configname, String.valueOf(switchto));
							ModProperties.getInstance().save();
							Util.updateValuesToConfig();
							SongPlayer.addChatMessage("§6Set §9use " + configname + " command§6 to §3" + !switchto);
							return true;
						}
					}
					return false;
				}
				default: {
					return false;
				}
			}
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] theArgs = args.split(" ");
			int argumentint = theArgs.length;
			if (args.endsWith(" ")) argumentint += 1;


			if (argumentint < 2) {
				possibleArguments.addAll(List.of(new String[]{"get", "set", "reset", "toggle"}));
			} else if (argumentint < 3) {
				possibleArguments.addAll(List.of(new String[]{"survival", "creative", "adventure", "spectator", "playnote", "displayprogress"}));
			} else {
				if (theArgs[0].equals("set")) {
					StringBuilder ncmd = new StringBuilder();
					String newCommand = "";
					for (int s = 2; s < theArgs.length; s++) {
						ncmd.append(theArgs[s] + " ");
					}
					newCommand = ncmd.toString().trim();
					if (newCommand.startsWith("/")) {
						newCommand = newCommand.substring(1);
					}
					possibleArguments.add(newCommand);
				} else {
					return null;
				}
			}
			return CommandSource.suggestMatching(possibleArguments, suggestionsBuilder);
		}
	}

	private static class playlistCommand extends Command {
		@Override
		public String getName() {
			return "playlist";
		}

		@Override
		public String[] getAliases() {
			return new String[]{"plist"};
		}

		@Override
		public String getSyntax() {
			return "playlist edit <playlist> <add, remove> <song>\n§6OR: §c" + SongPlayer.prefix + "playlist <create, delete, play> <playlist>\n§6OR: §c" + SongPlayer.prefix + "playlist sort <addedfirst, alphabetically, shuffle>";
		}

		@Override
		public String getDescription() {
			return "automatically load a set list of songs or manage your playlists";
		}

		@Override
		public boolean processCommand(String args) {
			String[] theArgs = args.split(" ");
			if (theArgs.length < 2) {
				return false;
			}
			File playlist = new File("SongPlayer/playlists/" + theArgs[1]);;
			File songsfolder = SongPlayer.SONG_DIR;
			switch(theArgs[0].toLowerCase()) {
				case "create": {
					if (playlist.exists()) {
						SongPlayer.addChatMessage("§6Playlist §3" + theArgs[1] + "§6 already exists!");
						return true;
					}
					playlist.mkdir();
					SongPlayer.addChatMessage("§6Added playlist named §3" + theArgs[1]);
					return true;
				}
				case "delete": {
					if (!playlist.exists()) {
						SongPlayer.addChatMessage("§6Playlist §3" + theArgs[1] + "§6 doesn't exist!");
						return true;
					}
					if (Util.currentPlaylist.equals(theArgs[1])) {
						SongPlayer.addChatMessage("§6You cannot modify playlists that are playing.");
						return true;
					}
					int songs = playlist.listFiles().length;
					if (Calendar.getInstance().getTime().getTime() > Util.confirmCommand) { //
						if (songs > 9) { //just in case
							SongPlayer.addChatMessage("§6Are you sure you want to delete the playlist §3" + theArgs[1] + "§6 along with the §9" + songs + "§6 songs it contains?");
							SongPlayer.addChatMessage("§cPlease run the command again in the next 10 seconds if you are sure you want to do this!");
							Util.confirmCommand = Calendar.getInstance().getTime().getTime() + 10000;
							return true;
						}
					}
					Util.confirmCommand = Calendar.getInstance().getTime().getTime();
					try {
						FileUtils.deleteDirectory(playlist);
						SongPlayer.addChatMessage("§6Deleted playlist §3" + theArgs[1]);
					} catch(IOException e) {
						SongPlayer.addChatMessage("§cThere was an internal error attempting to delete this playlist. Check your logs for more details.");
						e.printStackTrace();
						System.out.println("Crud... This isn't what my mod is supposed to do!");
					}
					return true;
				}
				case "edit": {
					//general
					if (theArgs.length < 4) {
						return false;
					}
					if (!playlist.exists()) {
						SongPlayer.addChatMessage("§6Playlist §3" + theArgs[1] + "§6 doesn't exist!");
						return true;
					}
					if (Util.currentPlaylist.equals(theArgs[1])) {
						SongPlayer.addChatMessage("§6You cannot modify playlists that are playing.");
						return true;
					}
					StringBuilder miditogetbuilder = new StringBuilder();
					for (int i = 3; i < theArgs.length; i++) {
						miditogetbuilder.append(theArgs[i] + " ");
					}
					String miditoget = miditogetbuilder.toString().substring(0, miditogetbuilder.length() - 1);
					if (theArgs[2].equalsIgnoreCase("add")) {
						File toget = new File("songs/" + miditoget);
						for (String file : playlist.list()) {
							if (file.equals(miditoget)) {
								SongPlayer.addChatMessage("§3" + miditoget + "§6 already exists in this playlist!");
								return true;
							}
						}
						for (String file : songsfolder.list()) {
							if (file.equals(miditoget)) {
								try {
									Files.copy(toget, new File("SongPlayer/playlists/" + theArgs[1] + "/" + miditoget));
									SongPlayer.addChatMessage("§6Added §3" + miditoget + "§6 to the playlist §9" + theArgs[1]);
								} catch(IOException e) {
									e.printStackTrace();
									SongPlayer.addChatMessage("§cThere was an error copying one or more files.");
								}
								return true;
							}
						}
						SongPlayer.addChatMessage("§cThat file doesn't exist");
						return true;
					} else if (theArgs[2].equalsIgnoreCase("remove")) {
						File toget = new File("SongPlayer/playlists/" + theArgs[1] + "/" + miditoget);
						for (String file : songsfolder.list()) {
							if (file.equals(miditoget)) {
								toget.delete();
								SongPlayer.addChatMessage("§6Removed §3" + miditoget + "§6 from the playlist §3" + theArgs[1]);
								return true;
							}
						}
						SongPlayer.addChatMessage("§cThat file doesn't exist");
						return true;
					}
					return false;
				}
				case "play": {
					if (!Util.currentPlaylist.isEmpty()) {
						SongPlayer.addChatMessage("§6A playlist is already running!");
						return true;
					}
					if (!playlist.exists()) {
						SongPlayer.addChatMessage("§6Playlist §3" + theArgs[1] + "§6 doesn't exist!");
						return true;
					}
					File[] songs = playlist.listFiles();
					String[] songnames = playlist.list();
					if (songs.length == 0) {
						SongPlayer.addChatMessage("§6No songs in playlist!");
						return true;
					}
					if (SongHandler.getInstance().stage != null) {
						SongHandler.getInstance().stage.movePlayerToStagePosition();
					}
					Util.playlistSongs.clear();
					SongHandler.getInstance().cleanup(true);
					//Util.loadSongs(songs, 0, playlist);
					switch(Util.playlistOrder) {
						case "addedfirst": {
							break;
						}
						case "alphabetically": {
							Arrays.sort(songnames, java.text.Collator.getInstance());
							break;
						}
						case "shuffle": {
							List<String> shuffle = Arrays.asList(songnames);
							Collections.shuffle(shuffle);
							songnames = shuffle.toArray(new String[shuffle.size()]);
							break;
						}
						default: {
							SongPlayer.addChatMessage("§cUnable to parse sorting method §4" + Util.playlistOrder + "§c. Using defaults. (addedfirst)");
						}
					}

					Util.playlistSongs.addAll(Arrays.asList(songnames));

					//SongHandler.getInstance().loadSong(Util.playlistSongs.get(0).getName(), playlist);
					if (Util.playlistSongs.size() < songs.length) {
						SongPlayer.addChatMessage("§cFailed to load some songs from playlist");
					}
					Util.currentPlaylist = theArgs[1];
					SongPlayer.addChatMessage("§6Loaded §9" + Util.playlistSongs.size() + "§6 songs from the playlist §3" + theArgs[1]);
					SongHandler.getInstance().loadSong(Util.playlistSongs.get(0), playlist);
					return true;
				}
				case "sort": {
					if (theArgs.length != 2) {
						return false;
					}
					switch(theArgs[1].toLowerCase()) {
						case "addedfirst":
						case "alphabetically":
						case "shuffle": {
							Util.playlistOrder = theArgs[1].toLowerCase();
							SongPlayer.addChatMessage("§6Playlist order now set to §3" + Util.playlistOrder);
							if (!Util.currentPlaylist.isEmpty()) {
								SongPlayer.addChatMessage("§9Changes will apply on the next playlist");
							}
							return true;
						}
						default: {
							SongPlayer.addChatMessage("§cUnknown order §4" + theArgs[1]);
							return false;
						}
					}
				}
			}
			return false;
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] theArgs = args.split(" ");
			int argumentint = theArgs.length;

			if (args.endsWith(" ")) argumentint += 1;

			switch(argumentint) {
				case 0:
				case 1: {
					possibleArguments.add("create");
					possibleArguments.add("delete");
					possibleArguments.add("edit");
					possibleArguments.add("play");
					possibleArguments.add("sort");
					break;
				}

				//list playlists here
				case 2: {
					if (theArgs[0].equalsIgnoreCase("create")) {
						possibleArguments.add("<playlist name>");
						break;
					} else if (theArgs[0].equalsIgnoreCase("sort")) {
						possibleArguments.add("addedfirst");
						possibleArguments.add("alphabetically");
						possibleArguments.add("shuffle");
						break;
					}
					try {
						List<String> filenames = Arrays.stream(Objects.requireNonNull(SongPlayer.PLAYLISTS_DIR.list())).toList();
						return CommandSource.suggestMatching(filenames, suggestionsBuilder);
					} catch(NullPointerException e) {
						if (!SongPlayer.SONG_DIR.exists()) {
							SongPlayer.SONG_DIR.mkdir();
						}
						return null;
					}
				}
				//if the beginning argument is "edit" do a different branch. else don't return anything
				case 3: {
					if (!theArgs[0].equalsIgnoreCase("edit")) {
						return null;
					}
					possibleArguments.add("add");
					possibleArguments.add("remove");
					break;
				}
				//if the beginning argument is "edit" and if 3rd argument is valid, list all the songs here. Otherwise return null.
				//sidenote: this is default because songs may have spaces in them.
				default: {
					if (!theArgs[0].equalsIgnoreCase("edit")) {
						return null;
					}
					boolean hasFile = false;
					for (File file : SongPlayer.PLAYLISTS_DIR.listFiles()) {
						if (file.getName().equals(theArgs[1])) {
							hasFile = true;
							break;
						}
					}
					if (!hasFile) {
						return null;
					}
					File playlistdir = new File("SongPlayer/playlists/" + theArgs[1] + "/");
					List<String> filenames = new ArrayList<>();
					if (theArgs[2].equalsIgnoreCase("add")) {
						filenames = Arrays.stream(SongPlayer.SONG_DIR.listFiles())
								.filter(File::isFile)
								.map(File::getName)
								.collect(Collectors.toList());
						for (String filenamesl : playlistdir.list()) {
							if (filenames.contains(filenamesl)) {
								filenames.remove(filenamesl);
							}
						}
					} else if (theArgs[2].equalsIgnoreCase("remove")) {
						filenames = Arrays.stream(playlistdir.listFiles())
								.filter(File::isFile)
								.map(File::getName)
								.collect(Collectors.toList());
					} else {
						return null;
					}
					return CommandSource.suggestMatching(filenames, suggestionsBuilder);
				}
			}

			return CommandSource.suggestMatching(possibleArguments, suggestionsBuilder);
		}
	}

	private static class setStage extends Command {
		@Override
		public String getName() {
			return "setStage";
		}

		@Override
		public String[] getAliases() {
			return new String[]{"stage", "updateStage"};
		}

		@Override
		public String getSyntax() {
			return "setStage <default, legacy, compact>";
		}

		@Override
		public String getDescription() {
			return "Changes how the stage will be built when using gamemode method for playing songs. default places 353 noteblocks, legacy places 300, and compact places 400.\n Huge thanks to Lizard16 for the compact stage design!";
		}

		@Override
		public boolean processCommand(String args) {
			String[] arguments = args.toLowerCase().split(" ");
			if (arguments.length != 1) {
				return false;
			}
			switch(arguments[0].toLowerCase()) {
				case "default": {
					ModProperties.getInstance().updateValue("stageType", "default");
					Util.updateValuesToConfig();
					break;
				}
				case "legacy": {
					ModProperties.getInstance().updateValue("stageType", "legacy");
					Util.updateValuesToConfig();
					break;
				}
				case "compact": {
					ModProperties.getInstance().updateValue("stageType", "compact");
					Util.updateValuesToConfig();
					break;
				}
				default: {
					return false;
				}
			}
			SongPlayer.addChatMessage("§6Stage type is set to §3" + arguments[0].toLowerCase());
			return true;
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] theArgs = args.split(" ");
			int argumentint = theArgs.length;

			if (args.endsWith(" ")) {
				argumentint += 1;
			}
			switch(argumentint) {
				case 0:
				case 1: {
					possibleArguments.add("default");
					possibleArguments.add("legacy");
					possibleArguments.add("compact");
					break;
				}
				default: {
					return null;
				}
			}
			return CommandSource.suggestMatching(possibleArguments, suggestionsBuilder);
		}
	}

	private static class RecordCommand extends Command {

		@Override
		public String getName() {
			return "record";
		}

		@Override
		public String getSyntax() {
			return "record <stop, cancel>\nOR: " + SongPlayer.prefix + "record start <name>";
		}

		@Override
		public String getDescription() {
			return "records noteblocks other players play that you hear and saves it as a .txt file that you can play back later.";
		}

		@Override
		public boolean processCommand(String args) {
			String[] arguments = args.toLowerCase().split(" ");
			switch(arguments[0].toLowerCase()) {
				case "start": {
					if (arguments.length < 2) {
						return false;
					}
					if (SongHandler.getInstance().currentSong != null) {
						SongPlayer.addChatMessage("§cCan't record a song while playing one");
						return true;
					}
					if (SongPlayer.recording) {
						SongPlayer.addChatMessage("§cYou are already recording");
						return true;
					}
					Util.recordName = args.substring(arguments[0].length() + 1);
					if (new File(SongPlayer.SONG_DIR + "/" + Util.recordName + ".txt").exists()) {
						SongPlayer.addChatMessage("§cFile already exists");
						return true;
					}
					SongPlayer.recordingtick = 0;
					SongPlayer.recording = true;
					SongPlayer.addChatMessage("§6recording ready. Recording will start as soon as you hear noteblocks being played.");
					return true;
				}
				case "stop": {
					if (arguments.length != 1) {
						return false;
					}
					if (!SongPlayer.recording) {
						SongPlayer.addChatMessage("§crecording never started");
						return true;
					}
					Util.recordednotesfailed = 0;
					if (new File(SongPlayer.SONG_DIR + "/" + Util.recordName + ".txt").exists()) {
						SongPlayer.addChatMessage("§cFailed to save recording: §4" + Util.recordName + ".txt§c file already exists");
						SongPlayer.recording = false;
						SongPlayer.recordingActive = false;
						SongPlayer.recordingtick = 0;
						Util.recordedNotes.clear();
						return true;
					}
					SongPlayer.recording = false;
					SongPlayer.addChatMessage("§6saving song, please wait...");
					try {
						FileWriter write = new FileWriter(SongPlayer.SONG_DIR + "/" + Util.recordName + ".txt");
						for (String s : Util.recordedNotes) {
							write.write(s + "\n");
						}
						write.close();
						SongPlayer.addChatMessage("§6Saved recording as §3" + Util.recordName + ".txt");
					} catch(IOException e) {
						e.printStackTrace();
						SongPlayer.addChatMessage("§cThere was an error when attempting to save your song. Please check the minecraft logs for more information.");
					} finally {
						SongPlayer.recording = false;
						SongPlayer.recordingActive = false;
						SongPlayer.recordingtick = 0;
						Util.recordedNotes.clear();
					}
					return true;
				}
				case "cancel": {
					if (arguments.length != 1) {
						return false;
					}
					if (!SongPlayer.recording) {
						SongPlayer.addChatMessage("§crecording never started");
						return true;
					}
					Util.recordednotesfailed = 0;
					SongPlayer.recording = false;
					SongPlayer.recordingActive = false;
					SongPlayer.recordingtick = 0;
					Util.recordedNotes.clear();
					SongPlayer.addChatMessage("§6recording cancelled");
					return true;
				}
				default: {
					return false;
				}
			}
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] theArgs = args.split(" ");
			int argumentint = theArgs.length;

			if (args.endsWith(" ")) {
				argumentint += 1;
			}
			switch(argumentint) {
				case 0:
				case 1: {
					possibleArguments.add("start");
					possibleArguments.add("stop");
					possibleArguments.add("cancel");
					break;
				}
				default: {
					if (!theArgs[0].equalsIgnoreCase("start")) {
						return null;
					}
					possibleArguments.add(args.substring(6));
				}
			}
			return CommandSource.suggestMatching(possibleArguments, suggestionsBuilder);
		}
	}

	private static class playmodeCommand extends Command {

		@Override
		public String getName() {
			return "setPlayMode";
		}

		@Override
		public String[] getAliases() {
			return new String[]{"pMode", "playMode", "updatePlayMode"};
		}

		@Override
		public String getSyntax() {
			return "setPlayMode <client, commands, gamemode, survival>";
		}

		@Override
		public String getDescription() {
			return "Change what method to use when playing songs.\n " +
					"gamemode - will switch from creative to build noteblocks, and switch to survival to play them.\n " +
					"survival - will scan for noteblocks in reach distance around the player. If all needed noteblocks are present, SongPlayer will start playing.\n" +
					"client - plays noteblocks client-side. Can be used to test out new songs you get before playing them for everyone else.\n " +
					"commands - will use only commands and no noteblocks. This should only be used if you have operator. (you can get kicked for spam)" +
					"commandblocks - uses command blocks to run commands rather than sending the commands yourself. Can be used to get around servers that ratelimit commands like kaboom and minehut.";
		}

		@Override
		public boolean processCommand(String args) {
			String[] arguments = args.toLowerCase().split(" ");
			if (arguments.length != 1) {
				return false;
			}

			switch (arguments[0].toLowerCase()) {
				case "commands": {
					if (SongPlayer.useNoteblocksWhilePlaying) {
						if (SongPlayer.switchGamemode) {
							SongHandler.getInstance().restoreGamemode();
						}
						if (SongHandler.getInstance().stage != null) {
							SongHandler.getInstance().stage.movePlayerToStagePosition(true, false, false);
							Util.disableFlightIfNeeded();
						}
					}
					if (SongPlayer.useCommandsForPlaying && !SongPlayer.includeCommandBlocks) {
						SongPlayer.addChatMessage("§6You are already using this method of playing songs");
						return true;
					}
					if (SongPlayer.showFakePlayer && SongPlayer.fakePlayer != null) { //fake player is showing
						SongPlayer.removeFakePlayer();
					}
					if (!SongPlayer.MC.player.hasPermissionLevel(2)) {
						SongPlayer.addChatMessage("§4WARNING\n§cYou don't seem to have permissions from the server to use this method of playing command blocks. Please consider a different playing mode.");
					}
					ModProperties.getInstance().updateValue("useCommandsForPlaying", String.valueOf(true));
					ModProperties.getInstance().updateValue("useCommandBlocks", String.valueOf(false));
					ModProperties.getInstance().updateValue("switchGamemode", String.valueOf(false));
					ModProperties.getInstance().updateValue("useNoteblocks", String.valueOf(false));
					Util.updateValuesToConfig();
					SongPlayer.addChatMessage("§6Changed method to §3using commands");
					return true;
				}
				case "commandblocks": {
					//THIS IS BUGGY. You can uncomment this and rebuild it if you want, but just be warned it isn't complete and probably will never be
					if (SongPlayer.useNoteblocksWhilePlaying) {
						if (SongPlayer.switchGamemode) {
							SongHandler.getInstance().restoreGamemode();
						}
						if (SongHandler.getInstance().stage != null) {
							SongHandler.getInstance().stage.movePlayerToStagePosition(true, false, false);
							Util.disableFlightIfNeeded();
						}
					}
					if (SongPlayer.useCommandsForPlaying && SongPlayer.includeCommandBlocks) {
						SongPlayer.addChatMessage("§6You are already using this method of playing songs");
						return true;
					}
					if (SongPlayer.showFakePlayer && SongPlayer.fakePlayer != null) { //fake player is showing
						SongPlayer.removeFakePlayer();
					}
					if (!SongPlayer.MC.player.hasPermissionLevel(2)) {
						SongPlayer.addChatMessage("§4WARNING\n§cYou don't seem to have permissions from the server to use this method of playing command blocks. Please consider a different playing mode.");
					}
					ModProperties.getInstance().updateValue("useCommandsForPlaying", String.valueOf(true));
					ModProperties.getInstance().updateValue("useCommandBlocks", String.valueOf(true));
					ModProperties.getInstance().updateValue("switchGamemode", String.valueOf(false));
					ModProperties.getInstance().updateValue("useNoteblocks", String.valueOf(false));
					Util.updateValuesToConfig();
					SongPlayer.addChatMessage("§6Changed method to §3use command blocks");
					Util.assignCommandBlocks(true);
					return true;
				}
				case "survival": {
					//hhhzzzsss pls add
					if (SongPlayer.useNoteblocksWhilePlaying && !SongPlayer.switchGamemode) {
						SongPlayer.addChatMessage("§6You are already using this method of playing songs");
						return true;
					}
					if (SongPlayer.switchGamemode) {
						SongHandler.getInstance().restoreGamemode();
					}
					if (SongHandler.getInstance().stage != null && SongHandler.getInstance().currentSong != null && !SongPlayer.useNoteblocksWhilePlaying) {
						Util.updateStageLocationToPlayer();
						if (SongPlayer.showFakePlayer) { //check if fakeplayer is enabled. if it is, spawn the fakeplayer to the stage
							if (SongPlayer.fakePlayer == null) {
								SongPlayer.fakePlayer = new FakePlayerEntity();
							}
							SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
						}
					}

					ModProperties.getInstance().updateValue("useCommandsForPlaying", String.valueOf(false));
					ModProperties.getInstance().updateValue("useCommandBlocks", String.valueOf(false));
					ModProperties.getInstance().updateValue("switchGamemode", String.valueOf(false));
					ModProperties.getInstance().updateValue("useNoteblocks", String.valueOf(true));
					Util.updateValuesToConfig();
					if (SongPlayer.showFakePlayer && SongPlayer.fakePlayer != null) { //fake player is showing
						SongPlayer.removeFakePlayer();
					}
					if (SongHandler.getInstance().currentSong != null && !SongHandler.getInstance().paused) { //player is in the middle of playing a song
						Util.enableFlightIfNeeded();
						SongHandler.getInstance().stage.checkBuildStatus(SongHandler.getInstance().currentSong);
						if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
							SongPlayer.MC.getNetworkHandler().sendCommand(SongPlayer.survivalCommand);
						}
					}
					SongPlayer.addChatMessage("§6Changed method to §3survival only");
					return true;
				}
				case "gamemode": {
					if (SongPlayer.switchGamemode) {
						SongPlayer.addChatMessage("§6You are already using this method of playing songs");
						return true;
					}
					if (SongHandler.getInstance().stage != null ) {
						Util.disableFlightIfNeeded();
						if (SongHandler.getInstance().currentSong != null && !SongPlayer.useNoteblocksWhilePlaying) {
							Util.updateStageLocationToPlayer();
							if (SongPlayer.showFakePlayer) { //check if fakeplayer is enabled. if it is, spawn the fakeplayer to the stage
								if (SongPlayer.fakePlayer == null) {
									SongPlayer.fakePlayer = new FakePlayerEntity();
								}
								SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
							}
						}
					}

					ModProperties.getInstance().updateValue("useCommandsForPlaying", String.valueOf(false));
					ModProperties.getInstance().updateValue("useCommandBlocks", String.valueOf(false));
					ModProperties.getInstance().updateValue("switchGamemode", String.valueOf(true));
					ModProperties.getInstance().updateValue("useNoteblocks", String.valueOf(true));
					Util.updateValuesToConfig();

					if (SongPlayer.showFakePlayer && SongPlayer.fakePlayer != null) { //fake player is showing
						SongPlayer.removeFakePlayer();
					}
					if (SongHandler.getInstance().currentSong != null && !SongHandler.getInstance().paused) { //player is in the middle of playing a song
						Util.enableFlightIfNeeded();
						SongHandler.getInstance().stage.checkBuildStatus(SongHandler.getInstance().currentSong);
						if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
							SongPlayer.MC.getNetworkHandler().sendCommand(SongPlayer.survivalCommand);
						}
					}
					SongPlayer.addChatMessage("§6Changed method to §3switching gamemode");
					return true;
				}
				case "client": {
					if (SongPlayer.useNoteblocksWhilePlaying) {
						if (SongPlayer.switchGamemode) {
							SongHandler.getInstance().restoreGamemode();
						}
						if (SongHandler.getInstance().stage != null) {
							SongHandler.getInstance().stage.movePlayerToStagePosition(true, false, false);
							Util.disableFlightIfNeeded();
						}
					}
					if (!SongPlayer.useCommandsForPlaying && !SongPlayer.useNoteblocksWhilePlaying) {
						SongPlayer.addChatMessage("§6You are already using this method of playing songs");
						return true;
					}
					ModProperties.getInstance().updateValue("useCommandsForPlaying", String.valueOf(false));
					ModProperties.getInstance().updateValue("useCommandBlocks", String.valueOf(false));
					ModProperties.getInstance().updateValue("switchGamemode", String.valueOf(false));
					ModProperties.getInstance().updateValue("useNoteblocks", String.valueOf(false));
					if (SongPlayer.showFakePlayer && SongPlayer.fakePlayer != null) { //fake player is showing
						SongPlayer.removeFakePlayer();
					}
					Util.updateValuesToConfig();
					SongPlayer.addChatMessage("§6Changed method to §3client-side");
					return true;
				}
				default: {
					return false;
				}
			}
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] theArgs = args.split(" ");
			int argumentint = theArgs.length;

			if (args.endsWith(" ")) {
				argumentint += 1;
			}
			switch(argumentint) {
				case 0:
				case 1: {
					possibleArguments.add("survival");
					possibleArguments.add("gamemode");
					possibleArguments.add("commands");
					possibleArguments.add("commandblocks"); //unfinished
					possibleArguments.add("client");
					break;
				}
				default: {
					return null;
				}
			}
			return CommandSource.suggestMatching(possibleArguments, suggestionsBuilder);
		}
	}

	private static class prefixCommand extends Command {

		@Override
		public String getName() {
			return "prefix";
		}

		@Override
		public String getSyntax() {
			return "prefix <command prefix>";
		}

		@Override
		public String getDescription() {
			return "Change the prefix used to use these commands.";
		}

		@Override
		public boolean processCommand(String args) {
			String[] arguments = args.toLowerCase().split(" ");
			if (!(arguments.length == 1)) {
				return false;
			}

			String newprefix = arguments[0].toLowerCase();

			if (newprefix.length() > 20) {
				SongPlayer.addChatMessage("§cPrefix can only be up to 20 characters long!");
				return true;
			} else if (newprefix.isEmpty()) {
				return false;
			} else if (newprefix.startsWith("/")) {
				SongPlayer.addChatMessage("§cPrefix can not start with §4/");
				return true;
			} else if (newprefix.contains(" ")) {
				SongPlayer.addChatMessage("§cPrefix can not contain spaces");
				return true;
			}
			ModProperties.getInstance().updateValue("prefix", newprefix);
			Util.updateValuesToConfig();
			SongPlayer.addChatMessage("§6Prefix is set to: §3" + newprefix);
			return true;
		}
	}

	private static class toggleCommand extends Command {

		@Override
		public String getName() {
			return "toggle";
		}

		@Override
		public String getSyntax() {
			return "toggle <allMovements, cleanup, ignoreWarnings, rotate, swing, useExactInstrumentsOnly, usePacketsOnly, volume> <true, false>";
		}

		@Override
		public String getDescription() {
			return "Allows you to toggle certain features on and off. [Some features may increase the chances of you exceeding packet limit or help to work around crashes with other mods]";
		}

		@Override
		public boolean processCommand(String args) {
			String[] arguments = args.toLowerCase().split(" ");

			if (!(arguments.length == 2)) {
				return false;
			}

			boolean toggleto;

			switch(arguments[1].toLowerCase()) {
				case "true":
				case "false": {
					toggleto = Boolean.parseBoolean(arguments[1].toLowerCase());
					break;
				}
				default: {
					return false;
				}
			}

			switch(arguments[0].toLowerCase()) {
				case "allmovements": {
					ModProperties.getInstance().updateValue("swing", String.valueOf(toggleto));
					ModProperties.getInstance().updateValue("rotate", String.valueOf(toggleto));
					SongPlayer.addChatMessage("§6Set §9swing§6 and §9rotate§6 to §3" + toggleto);
					break;
				}
				case "cleanup": {
					ModProperties.getInstance().updateValue("cleanupstage", String.valueOf(toggleto));
					SongPlayer.addChatMessage("§6Set §9clean up stage when done§6 to §3" + toggleto);
					break;
				}
				case "ignorewarnings": {
					ModProperties.getInstance().updateValue("stopiferror", String.valueOf(toggleto));
					SongPlayer.addChatMessage("§6set §9ignore warnings§6 to §3" + toggleto);
					break;
				}
				case "rotate": {
					ModProperties.getInstance().updateValue("rotate", String.valueOf(toggleto));
					SongPlayer.addChatMessage("§6Set §9rotate§6 to §3" + toggleto);
					break;
				}
				case "swing": {
					ModProperties.getInstance().updateValue("swing", String.valueOf(toggleto));
					SongPlayer.addChatMessage("§6Set §9swing§6 to §3" + toggleto);
					break;
				}
				case "useexactinstrumentsonly": {
					ModProperties.getInstance().updateValue("useAlternateInstruments", String.valueOf(toggleto));
					SongPlayer.addChatMessage("§6Set §9always use exact instruments§6 to §3" + toggleto);
					break;
				}
				case "usepacketsonly": {
					ModProperties.getInstance().updateValue("packetsWhilePlaying", String.valueOf(toggleto));
					SongPlayer.addChatMessage("§6Set §9packets only§6 to §3" + toggleto);
					break;
				}
				case "volume": {
					ModProperties.getInstance().updateValue("useVolume", String.valueOf(toggleto));
					SongPlayer.addChatMessage("§6Set §9use volume§6 to §3" + toggleto);
					break;
				}
				default: {
					return false;
				}
			}
			Util.updateValuesToConfig();
			return true;
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] theArgs = args.split(" ");
			int argumentint = theArgs.length;

			if (args.endsWith(" ")) {
				argumentint += 1;
			}

			switch(argumentint) {
				case 0:
				case 1: {
					possibleArguments.add("allMovements");
					possibleArguments.add("cleanup");
					possibleArguments.add("ignoreWarnings");
					possibleArguments.add("rotate");
					possibleArguments.add("swing");
					possibleArguments.add("useExactInstrumentsOnly");
					possibleArguments.add("usePacketsOnly");
					possibleArguments.add("volume");
					break;
				}
				case 2: {
					possibleArguments.add("true");
					possibleArguments.add("false");
					break;
				}
				default: {
					return null;
				}
			}

			return CommandSource.suggestMatching(possibleArguments, suggestionsBuilder);
		}
	}

	private static class helpCommand extends Command {
		public String getName() {
			return "help";
		}

		public String getSyntax() {
			return "help [command]";
		}

		public String getDescription() {
			return "Lists commands or explains command";
		}

		public boolean processCommand(String args) {
			if (args.length() == 0) {
				StringBuilder helpMessage = new StringBuilder("§6Commands -");
				for (Command c : commands) {
					helpMessage.append(" " + SongPlayer.prefix + c.getName());
				}
				SongPlayer.addChatMessage(helpMessage.toString());
			} else {
				if (args.contains(" ")) {
					return false;
				}
				if (!commandMap.containsKey(args.toLowerCase())) {
					SongPlayer.addChatMessage("§cCommand not recognized: " + args);
					return true;
				}
				Command c = commandMap.get(args.toLowerCase());
				SongPlayer.addChatMessage("§6------------------------------");
				SongPlayer.addChatMessage("§6Help: §3" + c.getName());
				SongPlayer.addChatMessage("§6Description: §3" + c.getDescription());
				SongPlayer.addChatMessage("§6Usage: §3" + c.getSyntax());
				if (c.getAliases().length > 0) {
					SongPlayer.addChatMessage("§6Aliases: §3" + String.join(", ", c.getAliases()));
				}
				SongPlayer.addChatMessage("§6------------------------------");
			}
			return true;
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] theArgs = args.split(" ");
			int argumentint = theArgs.length;

			if (args.endsWith(" ")) {
				argumentint += 1;
			}
			if (argumentint > 1) {
				return null;
			}
			return CommandSource.suggestMatching(commandCompletions, suggestionsBuilder);
		}
	}

	public static class noteLoudnessThresholdCommand extends Command {

		public String getName() {
			return "minimumVolume";
		}

		public String[] getAliases() {
			return new String[]{"mv"};
		}

		public String getSyntax() {
			return "minimumVolume <max volume>";
		}

		public String getDescription() {
			return "Set the limit to how loud notes can be in order to be played. If the volume is lower than the threshold, the note won't be played. This won't have any effect if you have volume toggled off.";
		}

		public boolean processCommand(String args) {
			String[] arguments = args.toLowerCase().split(" ");
			if (arguments.length != 1 || args.isBlank()) {
				return false;
			}
			if (!SongPlayer.parseVolume) {
				SongPlayer.addChatMessage("§cThis command will not have any effect unless you run §4" + SongPlayer.prefix + "toggle volume true");
			}
			byte newLimit;
			try {
				newLimit = Byte.parseByte(args);
			} catch (NumberFormatException e) {
				SongPlayer.addChatMessage("§4" + args + "§c is not a valid number or is not between 0 - 127.");
				return true;
			}
			if (newLimit < 0) {
				SongPlayer.addChatMessage("§cNumber must be between 0 and 127");
				return true;
			}
			ModProperties.getInstance().updateValue("noteVolumeThreshold", String.valueOf(newLimit));
			SongPlayer.addChatMessage("§6Notes at volume level §3" + newLimit + "§6 and higher will play");
			Util.updateValuesToConfig();
			return true;
		}
	}

	public static class convertCommand extends Command {
		public String getName() {return "convert";}

		public String getSyntax() {return "convert <input file> <.mid .nbs .txt>";}

		public String getDescription() {return "converts .txt .nbs & .mid files to different formats";}

		public boolean processCommand(String args) {
			if (args.length() < 2) {
				return false;
			}
			String[] theargs = args.split(" ");
			String[] formats = {".mid", ".midi", ".nbs", ".txt"};
			String filename = args.substring(0, args.length() - (theargs[theargs.length - 1].length() + 1));
			boolean validformat = false;
			for (String a : formats) {
				if (theargs[theargs.length - 1].equalsIgnoreCase(a) && !filename.endsWith(a)) {
					validformat = true;
					break;
				}
			}

			if (!validformat) return false;

			SongPlayer.addChatMessage("converting " + filename + " to " + theargs[theargs.length - 1]);
			SongHandler.getInstance().convertintto = theargs[theargs.length - 1];

			try {
				SongHandler.getInstance().loaderThread = new SongLoaderThread(filename, SongPlayer.SONG_DIR);
				SongHandler.getInstance().converting = true;
				SongHandler.getInstance().loaderThread.start();
			} catch (IOException e) {
				SongPlayer.addChatMessage("§cFailed to load song: §4" + e.getMessage());
				if (SongPlayer.kiosk) {
					Util.broadcastMessage("[{\"color\":\"red\",\"text\":\"Failed to load song: \"}, {\"color\":\"dark_red\",\"text\":\"" + e.getMessage() + "\"}]", null);
				}
			}
			return true;
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] theArgs = args.split(" ");
			String a = theArgs[theArgs.length - 1];
			if (a.endsWith(".mid") || a.endsWith(".midi") || a.endsWith(".nbs") || a.endsWith(".txt")) {
				possibleArguments.add(".mid");
				possibleArguments.add(".midi");
				possibleArguments.add(".nbs");
				possibleArguments.add(".txt");
				return CommandSource.suggestMatching(possibleArguments, suggestionsBuilder);
			}

			try {
				List<String> filenames = Arrays.stream(SongPlayer.SONG_DIR.listFiles())
						.filter(File::isFile)
						.map(File::getName)
						.collect(Collectors.toList());
				return CommandSource.suggestMatching(filenames, suggestionsBuilder);
			} catch(NullPointerException e) {
				if (!SongPlayer.SONG_DIR.exists()) {
					SongPlayer.SONG_DIR.mkdir();
				}
				return null;
			}
		}
	}

	private static class playCommand extends Command {
		public String getName() {
			return "play";
		}

		public String getSyntax() {
			return  "play <song or url>";
		}

		public String getDescription() {
			return "Plays a song";
		}

		public boolean processCommand(String args) {
			if (!Util.playlistSongs.isEmpty() || !Util.currentPlaylist.isEmpty()) {
				SongPlayer.addChatMessage("§cYou cannot use this command when a playlist is running! If you want to run an individual song, please type §4" + SongPlayer.prefix + "stop§c and try again.");
				return true;
			}
			if (args.isEmpty()) {
				return false;
			}
			if (SongHandler.getInstance().paused && SongHandler.getInstance().currentSong != null) {
				if (args.equals(SongHandler.getInstance().currentSong.name)) {
					Util.resumeSongIfNeeded();
					return true;
				}
			}
			SongHandler.getInstance().loadSong(args, SongPlayer.SONG_DIR);
			return true;
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			if (args.endsWith(".mid") || args.endsWith(".midi") || args.endsWith(".nbs") || args.endsWith(".txt")) {
				return null;
			}
			try {
				List<String> filenames = Arrays.stream(SongPlayer.SONG_DIR.listFiles())
						.filter(File::isFile)
						.map(File::getName)
						.collect(Collectors.toList());
				return CommandSource.suggestMatching(filenames, suggestionsBuilder);
			} catch(NullPointerException e) {
				if (!SongPlayer.SONG_DIR.exists()) {
					SongPlayer.SONG_DIR.mkdir();
				}
				return null;
			}
		}
    }

	private static class stopCommand extends Command {
		public String getName() {
			return "stop";
		}

		public String getSyntax() {
			return "stop";
		}

		public String getDescription() {
			return "Stops playing";
		}

		public boolean processCommand(String args) {
			if (args.length() != 0) {
				return false;
			}
			if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				Util.currentPlaylist = "";
				Util.playlistSongs.clear();
				return true;
			}
			if (SongPlayer.cleanupstage && SongPlayer.switchGamemode) {
				System.out.println("cleanup by $stop");
				Util.cleanupstage();
				return true;
			}
			if (SongHandler.getInstance().stage != null && SongPlayer.useNoteblocksWhilePlaying && !SongHandler.getInstance().paused) {
				SongHandler.getInstance().stage.movePlayerToStagePosition();
			}
			SongHandler.getInstance().paused = false;
			SongHandler.getInstance().cleanup(true);
			Util.disableFlightIfNeeded();
			if (SongHandler.oldItemHeld != null) {
				PlayerInventory inventory = SongPlayer.MC.player.getInventory();
				inventory.setStack(inventory.selectedSlot, SongHandler.oldItemHeld);
				SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
				SongHandler.oldItemHeld = null;
			}
			if (Util.currentPlaylist.isEmpty()) {
				SongPlayer.addChatMessage("§6Stopped playing");
			} else {
				SongPlayer.addChatMessage("§6Stopped playlist §3" + Util.currentPlaylist);
				Util.currentPlaylist = "";
			}
			return true;
		}
    }

	private static class pauseCommand extends Command {
		public String getName() {
			return "pause";
		}

		public String getSyntax() {
			return "pause";
		}

		public String getDescription() {
			return "Pauses a song";
		}

		public boolean processCommand(String args) {
			if (!SongHandler.getInstance().paused) {
				if (SongPlayer.cleanupstage && SongPlayer.switchGamemode) {
					SongPlayer.addChatMessage("cleanup by $pause");
					SongHandler.getInstance().paused = true;
					Util.cleanupstage();
					return true;
				}
				Util.pauseSongIfNeeded();
			} else {
				Util.resumeSongIfNeeded();
			}
			return true;
		}
	}

	private static class resumeCommand extends Command {
		public String getName() {
			return "resume";
		}

		public String[] getAliases() {
			return new String[]{"unpause"};
		}

		public String getSyntax() {
			return "resume";
		}

		public String getDescription() {
			return "resumes a song when paused";
		}

		public boolean processCommand(String args) {
			Util.resumeSongIfNeeded();
			return true;
		}
	}

	private static class skipCommand extends Command {
		public String getName() {
			return "skip";
		}

		public String getSyntax() {
			return "skip";
		}

		public String getDescription() {
			return "Skips current song";
		}

		public boolean processCommand(String args) {
			if (args.length() > 0) {
				return false;
			}
			if (SongHandler.getInstance().currentSong == null) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}
			Util.playcooldown = Calendar.getInstance().getTime().getTime() + 1500;
			SongHandler.getInstance().currentSong = null;
			Util.advancePlaylist();
			return true;
		}
	}

	private static class gotoCommand extends Command {
		public String getName() {
			return "goto";
		}

		public String getSyntax() {
			return "goto <mm:ss>";
		}

		public String getDescription() {
			return "Goes to a specific time in the song";
		}

		public boolean processCommand(String args) {
			if (SongHandler.getInstance().currentSong == null) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}
			if (args.length() == 0) {
				return false;
			}

			try {
				long time = Util.parseTime(args);
				SongHandler.getInstance().currentSong.setTime(time);
				SongPlayer.addChatMessage("§6Set song time to §3" + Util.formatTime(time));
				return true;
			} catch (IOException e) {
				SongPlayer.addChatMessage("§cNot a valid time stamp");
				return false;
			}
		}
    }

	private static class loopCommand extends Command {
		public String getName() {
			return "loop";
		}

		public String getSyntax() {
			return "loop";
		}

		public String getDescription() {
			return "Toggles song looping";
		}

		public boolean processCommand(String args) {
			if (SongHandler.getInstance().currentSong == null) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}
			boolean toggledto;
			if (Util.currentPlaylist.isEmpty()) { //toggle looping induvidual song
				SongHandler.getInstance().currentSong.looping = !SongHandler.getInstance().currentSong.looping;
				SongHandler.getInstance().currentSong.loopCount = 0;
				toggledto = SongHandler.getInstance().currentSong.looping;
			} else { //toggle looping playlist
				Util.loopPlaylist = !Util.loopPlaylist;
				toggledto = Util.loopPlaylist;
			}
			SongPlayer.addChatMessage((toggledto ? "§6Enabled" : "§6Disabled") + " looping");
			return true;
		}
    }

	private static class statusCommand extends Command {
		public String getName() {
			return "status";
		}

		public String[] getAliases() {
			return new String[]{"current"};
		}

		public String getSyntax() {
			return "status";
		}

		public String getDescription() {
			return "Gets the status of the song or playlist that is currently playing";
		}

		public boolean processCommand(String args) {
			if (args.length() != 0) {
				return false;
			}
			if (SongHandler.getInstance().currentSong == null) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}
			Song currentSong = SongHandler.getInstance().currentSong;
			long currentTime = Math.min(currentSong.time, currentSong.length);
			long totalTime = currentSong.length;
			if (Util.currentPlaylist.isEmpty()) {
				SongPlayer.addChatMessage(String.format("§6Currently playing §3%s §9(%s/%s) §6| looping: §3%s §6| paused: §3%s", currentSong.name, Util.formatTime(currentTime), Util.formatTime(totalTime), SongHandler.getInstance().currentSong.looping, SongHandler.getInstance().paused));
			} else {
				SongPlayer.addChatMessage(String.format("§6Currently playing playlist §3%s §9(%s/%s) §6| looping: §3%s §6| paused: §3%s", Util.currentPlaylist, Util.playlistIndex, Util.playlistSongs.size(), Util.loopPlaylist, SongHandler.getInstance().paused));
			}
			return true;
		}
    }

	private static class queueCommand extends Command {
		public String getName() {
			return "queue";
		}

		public String[] getAliases() {
			return new String[]{"showQueue"};
		}

		public String getSyntax() {
			return "queue";
		}

		public String getDescription() {
			return "Shows the current song queue";
		}

		public boolean processCommand(String args) {
			if (args.length() > 0) {
				return false;
			}
			int index = 0;
			if (!Util.currentPlaylist.isEmpty()) { //status on playlist
				SongPlayer.addChatMessage("§6------------------------------");
				SongPlayer.addChatMessage("§6Current playlist: §3" + Util.currentPlaylist);
				for (String song : Util.playlistSongs) {
					index++;
					if (SongHandler.getInstance().currentSong.name.equals(song)) {
						SongPlayer.addChatMessage("  §6" + index + ". §3" + song + "§9 (playing)");
					} else {
						SongPlayer.addChatMessage("  §6" + index + ". §3" + song);
					}
				}
				SongPlayer.addChatMessage("§6------------------------------");
				return true;
			}
			if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}

			SongPlayer.addChatMessage("§6------------------------------");
			if (SongHandler.getInstance().currentSong != null) {
				SongPlayer.addChatMessage("§6Current song: §3" + SongHandler.getInstance().currentSong.name);
			}
			for (Song song : SongHandler.getInstance().songQueue) {
				index++;
				SongPlayer.addChatMessage(String.format("§6%d. §3%s", index, song.name));
			}
			SongPlayer.addChatMessage("§6------------------------------");
			return true;
		}
	}

	private static class songsCommand extends Command {
		public String getName() {
			return "songs";
		}

		public String[] getAliases() {
			return new String[]{"list", "listSongs"};
		}

		public String getSyntax() {
			return "songs";
		}

		public String getDescription() {
			return "Lists available songs";
		}

		public boolean processCommand(String args) {
			StringBuilder sb = new StringBuilder("§6");
			boolean firstItem = true;
			for (File songFile : SongPlayer.SONG_DIR.listFiles()) {
				String fileName = songFile.getName();
				if (firstItem) {
					firstItem = false;
				} else {
					sb.append("§7, ");
				}
				sb.append(fileName);
			}
			SongPlayer.addChatMessage(sb.toString());
			return true;
		}
    }

	private static class setCreativeCommandCommand extends Command {
		public String getName() {
			return "setCreativeCommand";
		}

		public String[] getAliases() {
			return new String[]{"sc", "creativeCommand"};
		}

		public String getSyntax() {
			return "setCreativeCommand <command>";
		}

		public String getDescription() {
			return "Sets the command used to go into creative mode";
		}

		public boolean processCommand(String args) {
			if (args.length() == 0) {
				return false;
			}
			if (args.startsWith("/")) {
				args = args.substring(1);
			}
			String oldCommand = SongPlayer.creativeCommand;
			if (oldCommand.equals(args)) {
				if (args.isEmpty()) {
					SongPlayer.addChatMessage("§6Nothing changed; command is already disabled");
				} else {
					SongPlayer.addChatMessage("§6Nothing changed; command is already set to §3/" + oldCommand);
				}
				return true;
			}
			if (args.isEmpty()) {
				SongPlayer.addChatMessage("§6Disabled §9creative§6 command from running");
			} else {
				SongPlayer.addChatMessage("§6The command §9creative§6 has been updated\nfrom: §3/" + oldCommand + "\n§6to: §3/" + args);
			}
			ModProperties.getInstance().updateValue("creativeCommand", args);
			Util.updateValuesToConfig();
			return true;
		}
    }

	private static class setSurvivalCommandCommand extends Command {
		public String getName() {
			return "setSurvivalCommand";
		}

		public String[] getAliases() {
			return new String[]{"ss", "survivalCommand"};
		}

		public String getSyntax() {
			return "setSurvivalCommand <command>";
		}

		public String getDescription() {
			return "Sets the command used to go into survival mode";
		}

		public boolean processCommand(String args) {
			if (args.length() == 0) {
				return false;
			}
			if (args.startsWith("/")) {
				args = args.substring(1);
			}
			String oldCommand = SongPlayer.survivalCommand;
			if (oldCommand.equals(args)) {
				if (args.isEmpty()) {
					SongPlayer.addChatMessage("§6Nothing changed; command is already disabled");
				} else {
					SongPlayer.addChatMessage("§6Nothing changed; command is already set to §3/" + oldCommand);
				}
				return true;
			}
			if (args.isEmpty()) {
				SongPlayer.addChatMessage("§6Disabled §9survival§6 command from running");
			} else {
				SongPlayer.addChatMessage("§6The command §9survival§6 has been updated\nfrom: §3/" + oldCommand + "\n§6to: §3/" + args);
			}
			ModProperties.getInstance().updateValue("survivalCommand", args);
			Util.updateValuesToConfig();
			return true;
		}
    }

	private static class useEssentialsCommandsCommand extends Command {
		public String getName() {
			return "useEssentialsCommands";
		}

		public String[] getAliases() {
			return new String[]{"essentials", "useEssentials", "essentialsCommands"};
		}

		public String getSyntax() {
			return "useEssentialsCommands";
		}

		public String getDescription() {
			return "Switches to using essentials gamemode commands";
		}

		public boolean processCommand(String args) {
			if (args.length() == 0) {
				ModProperties.getInstance().updateValue("creativeCommand", "gmc");
				ModProperties.getInstance().updateValue("survivalCommand", "gms");
				ModProperties.getInstance().updateValue("adventureCommand", "gma");
				ModProperties.getInstance().updateValue("spectatorCommand", "gmsp");
				Util.updateValuesToConfig();
				SongPlayer.addChatMessage("§6Now using essentials gamemode commands");
				return true;
			} else {
				return false;
			}
		}
	}

	private static class useVanillaCommandsCommand extends Command {
		public String getName() {
			return "useVanillaCommands";
		}

		public String[] getAliases() {
			return new String[]{"vanilla", "useVanilla", "vanillaCommands"};
		}

		public String getSyntax() {
			return "useVanillaCommands";
		}

		public String getDescription() {
			return "Switches to using vanilla gamemode commands";
		}

		public boolean processCommand(String args) {
			if (args.length() == 0) {
				ModProperties.getInstance().updateValue("creativeCommand", "gamemode creative");
				ModProperties.getInstance().updateValue("survivalCommand", "gamemode survival");
				ModProperties.getInstance().updateValue("adventureCommand", "gamemode adventure");
				ModProperties.getInstance().updateValue("spectatorCommand", "gamemode spectator");
				Util.updateValuesToConfig();
				SongPlayer.addChatMessage("§6Now using vanilla gamemode commands");
				return true;
			} else {
				return false;
			}
		}
	}

	private static class toggleFakePlayerCommand extends Command {
		public String getName() {
			return "toggleFakePlayer";
		}

		public String[] getAliases() {
			return new String[]{"fakePlayer", "fp"};
		}

		public String getSyntax() {
			return "toggleFakePlayer";
		}

		public String getDescription() {
			return "Shows a fake player representing your true position when playing songs";
		}

		public boolean processCommand(String args) {
			if (args.length() != 0) {
				return false;
			}
			SongPlayer.showFakePlayer = !SongPlayer.showFakePlayer;
			if (!SongPlayer.showFakePlayer) {
				SongPlayer.addChatMessage("§6Disabled fake player");
				return true;
			}
			if (SongPlayer.useNoteblocksWhilePlaying) {
				if (SongHandler.getInstance().stage != null && !SongHandler.getInstance().paused) {
					SongPlayer.fakePlayer = new FakePlayerEntity();
					SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
				}
			}
			SongPlayer.addChatMessage("§6Enabled fake player");
			return true;

		}
	}

	private static class toggleKioskCommand extends Command {

		@Override
		public String getName() {
			return "togglekiosk";
		}

		@Override
		public String getSyntax() {
			return "togglekiosk";
		}

		@Override
		public String getDescription() {
			return "toggles kiosk mode, where others can control what songs you play";
		}

		public String[] getAliases() {
			return new String[] {"kiosk", "tkiosk", "tk"};
		}

		@Override
		public boolean processCommand(String args) {
			SongPlayer.kiosk = !SongPlayer.kiosk;
			if (SongPlayer.kiosk) {
				SongPlayer.useNoteblocksWhilePlaying = false;
				SongPlayer.useCommandsForPlaying = true;
				SongPlayer.switchGamemode = false;
				SongPlayer.includeCommandBlocks = true;
				Util.assignCommandBlocks(true);
				Util.sendCommandWithCommandblocks("bossbar add songplayer {\"text\":\"\"}");
				Util.sendCommandWithCommandblocks("bossbar set minecraft:songplayer color red");
			} else {
				Util.updateValuesToConfig();
			}
			SongPlayer.addChatMessage("§6Kiosk mode set to §3" + SongPlayer.kiosk);
			return true;
		}
	}

	private static class testSongCommand extends Command {
		public String getName() {
			return "testSong";
		}

		public String getSyntax() {
			return "testSong";
		}

		public String getDescription() {
			return "Creates a song for testing";
		}

		public boolean processCommand(String args) {
			if (args.length() != 0) {
				return false;
			}
			if (!Util.playlistSongs.isEmpty() || !Util.currentPlaylist.isEmpty()) {
				SongPlayer.addChatMessage("§cYou cannot use this command when a playlist is running! If you want to run an individual song, please type §4" + SongPlayer.prefix + "stop§c and try again.");
				return true;
			}
			Song song = new Song("test_song");
			for (int i=0; i<400; i++) {
				song.add(new Note(i, i*50, (byte) 127, (short) 0));
			}
			song.length = 400*50;
			SongHandler.getInstance().setSong(song);
			return true;
		}
	}

	private static class buildDelayCommand extends Command {
		public String getName() {
			return "buildDelay";
		}

		public String[] getAliases() {
			return new String[]{"setBuildDelay", "updateBuildDelay", "bd"};
		}

		public String getSyntax() {
			return "buildDelay <amount> <frames, ticks>";
		}

		public String getDescription() {
			return "Change the delay of building a noteblock / tuning a noteblock.\n specifying frames will wait every X amount of frames before building / tuning a noteblock.\n ticks will wait X amount of in-game ticks (1/20 of second) before building / tuning a noteblock.";
		}

		public boolean processCommand(String args) {
			String[] arguments = args.toLowerCase().split(" ");
			if (arguments.length != 2 || args.isBlank()) {
				return false;
			}
			int changeto;
			try {
				changeto = Integer.parseInt(arguments[0]);
				if (changeto < 0) {
					SongPlayer.addChatMessage("§4" + changeto + "§c cannot be below 0");
					return true;
				}
			} catch(NumberFormatException e) {
				SongPlayer.addChatMessage("§4" + arguments[0] + "§c isn't a valid number");
				return true;
			}
			boolean useFrames;
			switch (arguments[1].toLowerCase()) {
				case "frames": {
					useFrames = true;
					break;
				}
				case "ticks": {
					useFrames = false;
					break;
				}
				default: {
					return false;
				}
			}

			ModProperties.getInstance().updateValue("countByFrames", String.valueOf(useFrames));
			ModProperties.getInstance().updateValue("buildDelay", String.valueOf(changeto));
			Util.updateValuesToConfig();
			if (changeto == 0) {
				changeto = 1;
			}
			if (changeto == 1) {
				SongPlayer.addChatMessage("§6Building / tuning notes will now occur every §3" + StringUtils.chop(arguments[1]));
			} else {
				SongPlayer.addChatMessage("§6Building / tuning notes will now occur every §3" + changeto + " " + arguments[1].toLowerCase());
			}

			return true;
		}

		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] theArgs = args.split(" ");
			int argumentint = theArgs.length;

			if (args.endsWith(" ")) {
				argumentint += 1;
			}

			switch(argumentint) {
				case 0:
				case 1: {
					possibleArguments.add("<amount>");
					break;
				}
				case 2: {
					possibleArguments.add("frames");
					possibleArguments.add("ticks");
					break;
				}
				default: {
					return null;
				}
			}

			return CommandSource.suggestMatching(possibleArguments, suggestionsBuilder);
		}
	}


	// $ prefix included in command string
	public static CompletableFuture<Suggestions> handleSuggestions(String text, SuggestionsBuilder suggestionsBuilder) {
		if (!text.contains(" ")) {
			List<String> names = commandCompletions
					.stream()
					.map((commandName) -> SongPlayer.prefix + commandName)
					.collect(Collectors.toList());
			return CommandSource.suggestMatching(names, suggestionsBuilder);
		}

		String[] split = text.split(" ");
		if (!split[0].startsWith(SongPlayer.prefix)) {
			return null;
		}

		String commandName = split[0].substring(SongPlayer.prefix.length()).toLowerCase();
		if (!commandMap.containsKey(commandName)) {
			return null;
		}

		//split.length == 1 ? "" : split[1]
		//String.join(" ", Arrays.copyOfRange(split, 1, split.length))
		String[] cmdargs = text.split(" ", 2);
		possibleArguments.clear();
		return commandMap.get(commandName).getSuggestions(cmdargs[1], suggestionsBuilder);
	}


}
