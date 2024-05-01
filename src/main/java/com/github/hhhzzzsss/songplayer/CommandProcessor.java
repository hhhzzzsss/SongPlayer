package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.item.SongItemCreatorThread;
import com.github.hhhzzzsss.songplayer.item.SongItemUtils;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import com.github.hhhzzzsss.songplayer.song.Note;
import com.github.hhhzzzsss.songplayer.song.Playlist;
import com.github.hhhzzzsss.songplayer.song.Song;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.github.hhhzzzsss.songplayer.SongPlayer.MC;

public class CommandProcessor {
	public static ArrayList<Command> commands = new ArrayList<>();
	public static HashMap<String, Command> commandMap = new HashMap<>();
	public static ArrayList<String> commandCompletions = new ArrayList<>();

	public static void initCommands() {
		commands.add(new helpCommand());
		commands.add(new setPrefixCommand());
		commands.add(new playCommand());
		commands.add(new stopCommand());
		commands.add(new skipCommand());
		commands.add(new gotoCommand());
		commands.add(new loopCommand());
		commands.add(new statusCommand());
		commands.add(new queueCommand());
		commands.add(new songsCommand());
		commands.add(new playlistCommand());
		commands.add(new setCreativeCommandCommand());
		commands.add(new setSurvivalCommandCommand());
		commands.add(new useEssentialsCommandsCommand());
		commands.add(new useVanillaCommandsCommand());
		commands.add(new toggleFakePlayerCommand());
		commands.add(new setStageTypeCommand());
		commands.add(new toggleMovementCommand());
		commands.add(new announcementCommand());
		commands.add(new songItemCommand());
		commands.add(new testSongCommand());

		for (Command command : commands) {
			commandMap.put(command.getName().toLowerCase(Locale.ROOT), command);
			commandCompletions.add(command.getName());
			for (String alias : command.getAliases()) {
				commandMap.put(alias.toLowerCase(Locale.ROOT), command);
				commandCompletions.add(alias);
			}
		}
	}

	// returns true if it is a command and should be cancelled
	public static boolean processChatMessage(String message) {
		if (message.startsWith(Config.getConfig().prefix)) {
			String[] parts = message.substring(Config.getConfig().prefix.length()).split(" ", 2);
			String name = parts.length>0 ? parts[0] : "";
			String args = parts.length>1 ? parts[1] : "";
			Command c = commandMap.get(name.toLowerCase(Locale.ROOT));
			if (c == null) {
				SongPlayer.addChatMessage("§cUnrecognized command");
			} else {
				try {
					boolean success = c.processCommand(args);
					if (!success) {
						if (c.getSyntax().length == 0) {
							SongPlayer.addChatMessage("§cSyntax: " + Config.getConfig().prefix + c.getName());
						}
						else if (c.getSyntax().length == 1) {
							SongPlayer.addChatMessage("§cSyntax: " + Config.getConfig().prefix + c.getName() + " " + c.getSyntax()[0]);
						}
						else {
							SongPlayer.addChatMessage("§cSyntax:");
							for (String syntax : c.getSyntax()) {
								SongPlayer.addChatMessage("§c    " + Config.getConfig().prefix + c.getName() + " " + syntax);
							}
						}
					}
				}
				catch (Throwable e) {
					e.printStackTrace();
					SongPlayer.addChatMessage("§cAn error occurred while running this command: §4" + e.getMessage());
				}
			}
			return true;
		} else {
			return false;
		}
	}

	private static abstract class Command {
		public abstract String getName();
		public abstract String[] getSyntax();
		public abstract String getDescription();
		public abstract boolean processCommand(String args);
		public String[] getAliases() {
			return new String[]{};
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			return null;
		}
	}

	private static class helpCommand extends Command {
		public String getName() {
			return "help";
		}
		public String[] getSyntax() {
			return new String[]{"[command]"};
		}
		public String getDescription() {
			return "Lists commands or explains command";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				StringBuilder helpMessage = new StringBuilder("§6Commands -");
				for (Command c : commands) {
					helpMessage.append(" " + Config.getConfig().prefix + c.getName());
				}
				SongPlayer.addChatMessage(helpMessage.toString());
			}
			else {
				if (commandMap.containsKey(args.toLowerCase(Locale.ROOT))) {
					Command c = commandMap.get(args.toLowerCase(Locale.ROOT));
					SongPlayer.addChatMessage("§6------------------------------");
					SongPlayer.addChatMessage("§6Help: §3" + c.getName());
					SongPlayer.addChatMessage("§6Description: §3" + c.getDescription());
					if (c.getSyntax().length == 0) {
						SongPlayer.addChatMessage("§6Usage: §3" + Config.getConfig().prefix + c.getName());
					}
					else if (c.getSyntax().length == 1) {
						SongPlayer.addChatMessage("§6Usage: §3" + Config.getConfig().prefix + c.getName() + " " + c.getSyntax()[0]);
					} else {
						SongPlayer.addChatMessage("§6Usage:");
						for (String syntax : c.getSyntax()) {
							SongPlayer.addChatMessage("    §3" + Config.getConfig().prefix + c.getName() + " " + syntax);
						}
					}
					if (c.getAliases().length > 0) {
						SongPlayer.addChatMessage("§6Aliases: §3" + String.join(", ", c.getAliases()));
					}
					SongPlayer.addChatMessage("§6------------------------------");
				} else {
					SongPlayer.addChatMessage("§cCommand not recognized: " + args);
				}
			}
			return true;
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			return CommandSource.suggestMatching(commandCompletions, suggestionsBuilder);
		}
	}

	private static class setPrefixCommand extends Command {
		public String getName() {
			return "setPrefix";
		}
		public String[] getAliases() {
			return new String[]{"prefix"};
		}
		public String[] getSyntax() {
			return new String[] {"<prefix>"};
		}
		public String getDescription() {
			return "Sets the command prefix used by SongPlayer";
		}
		public boolean processCommand(String args) {
			if (args.contains(" ") || args.startsWith("/")) {
				SongPlayer.addChatMessage(args.startsWith("/") ? "§cPrefix can not start with §4/" : "Prefix cannot contain a space");
				return true;
			}
			else if (args.length() > 0) {
				Config.getConfig().prefix = args;
				SongPlayer.addChatMessage("§6Set prefix to " + args);
				Config.saveConfigWithErrorHandling();
				return true;
			}
			else {
				return false;
			}
		}
	}

	private static class playCommand extends Command {
		public String getName() {
			return "play";
		}
		public String[] getSyntax() {
			return new String[] {"<song or url>"};
		}
		public String getDescription() {
			return "Plays a song";
		}
		public boolean processCommand(String args) {
			if (args.length() > 0) {
				SongHandler.getInstance().loadSong(args);
				return true;
			}
			else {
				return false;
			}
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			return Util.giveSongSuggestions(args, suggestionsBuilder);
		}
	}

	private static class stopCommand extends Command {
		public String getName() {
			return "stop";
		}
		public String[] getSyntax() {
			return new String[0];
		}
		public String getDescription() {
			return "Stops playing";
		}
		public boolean processCommand(String args) {
			if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}
			if (args.length() == 0) {
				if (SongHandler.getInstance().stage != null) {
					SongHandler.getInstance().stage.movePlayerToStagePosition();
				}
				SongHandler.getInstance().restoreStateAndCleanUp();
				SongPlayer.addChatMessage("§6Stopped playing");
				return true;
			}
			else {
				return false;
			}
		}
	}

	private static class skipCommand extends Command {
		public String getName() {
			return "skip";
		}
		public String[] getSyntax() {
			return new String[0];
		}
		public String getDescription() {
			return "Skips current song";
		}
		public boolean processCommand(String args) {
			if (SongHandler.getInstance().currentSong == null) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}
			if (args.length() == 0) {
				SongHandler.getInstance().currentSong = null;
				return true;
			}
			else {
				return false;
			}
		}
	}

	private static class gotoCommand extends Command {
		public String getName() {
			return "goto";
		}
		public String[] getSyntax() {
			return new String[] {"<mm:ss>"};
		}
		public String getDescription() {
			return "Goes to a specific time in the song";
		}
		public boolean processCommand(String args) {
			if (SongHandler.getInstance().currentSong == null) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}

			if (args.length() > 0) {
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
			else {
				return false;
			}
		}
	}

	private static class loopCommand extends Command {
		public String getName() {
			return "loop";
		}
		public String[] getSyntax() {
			return new String[0];
		}
		public String getDescription() {
			return "Toggles song looping";
		}
		public boolean processCommand(String args) {
			if (SongHandler.getInstance().currentSong == null) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}

			SongHandler.getInstance().currentSong.looping = !SongHandler.getInstance().currentSong.looping;
			SongHandler.getInstance().currentSong.loopCount = 0;
			if (SongHandler.getInstance().currentSong.looping) {
				SongPlayer.addChatMessage("§6Enabled looping");
			}
			else {
				SongPlayer.addChatMessage("§6Disabled looping");
			}
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
		public String[] getSyntax() {
			return new String[0];
		}
		public String getDescription() {
			return "Gets the status of the song that is currently playing";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				if (SongHandler.getInstance().currentSong == null) {
					SongPlayer.addChatMessage("§6No song is currently playing");
					return true;
				}
				Song currentSong = SongHandler.getInstance().currentSong;
				long currentTime = Math.min(currentSong.time, currentSong.length);
				long totalTime = currentSong.length;
				SongPlayer.addChatMessage(String.format("§6Currently playing %s §3(%s/%s)", currentSong.name, Util.formatTime(currentTime), Util.formatTime(totalTime)));
				return true;
			}
			else {
				return false;
			}
		}
	}

	private static class queueCommand extends Command {
		public String getName() {
			return "queue";
		}
		public String[] getAliases() {
			return new String[]{"showQueue"};
		}
		public String[] getSyntax() {
			return new String[0];
		}
		public String getDescription() {
			return "Shows the current song queue";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
					SongPlayer.addChatMessage("§6No song is currently playing");
					return true;
				}

				SongPlayer.addChatMessage("§6------------------------------");
				if (SongHandler.getInstance().currentSong != null) {
					SongPlayer.addChatMessage("§6Current song: §3" + SongHandler.getInstance().currentSong.name);
				}
				int index = 0;
				for (Song song : SongHandler.getInstance().songQueue) {
					index++;
					SongPlayer.addChatMessage(String.format("§6%d. §3%s", index, song.name));
				}
				SongPlayer.addChatMessage("§6------------------------------");
				return true;
			}
			else {
				return false;
			}
		}
	}

	private static class songsCommand extends Command {
		public String getName() {
			return "songs";
		}
		public String[] getAliases() {
			return new String[]{"list"};
		}
		public String[] getSyntax() {
			return new String[] {
					"",
					"<subdirectory>"};
		}
		public String getDescription() {
			return "Lists available songs. If an argument is provided, lists all songs in the subdirectory.";
		}
		public boolean processCommand(String args) {
			if (!args.contains(" ")) {
				Path dir;
				if (args.length() == 0) {
					dir = SongPlayer.SONG_DIR;
				}
				else {
					dir = SongPlayer.SONG_DIR.resolve(args);
					if (!Files.isDirectory(dir)) {
						SongPlayer.addChatMessage("§cDirectory not found");
						return true;
					}
				}

				List<String> subdirectories = null;
				List<String> songs = null;
				try {
					subdirectories = Files.list(dir)
							.filter(Files::isDirectory)
							.map(Path::getFileName)
							.map(Path::toString)
							.map(str -> str + "/")
							.collect(Collectors.toList());
					songs = Files.list(dir)
							.filter(Files::isRegularFile)
							.map(Path::getFileName)
							.map(Path::toString)
							.collect(Collectors.toList());
				}
				catch (IOException e) {
					SongPlayer.addChatMessage("§cError reading folder: §4" + e.getMessage());
					return true;
				}

				if (subdirectories.size() == 0 && songs.size() == 0) {
					SongPlayer.addChatMessage("§bNo songs found. You can put midi or nbs files in the §3.minecraft/songs §6folder.");
				}
				else {
					SongPlayer.addChatMessage("§6----------------------------------------");
					SongPlayer.addChatMessage("§eContents of .minecraft/songs/" + args);
					if (subdirectories.size() > 0) {
						SongPlayer.addChatMessage("§6Subdirectories: §3" + String.join(" ", subdirectories));
					}
					if (songs.size() > 0) {
						SongPlayer.addChatMessage("§6Songs: §7" + String.join(", ", songs));
					}
					SongPlayer.addChatMessage("§6----------------------------------------");
				}
				return true;
			}
			else {
				return false;
			}
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			return Util.giveSongDirectorySuggestions(args, suggestionsBuilder);
		}
	}

	private static class playlistCommand extends Command {
		public String getName() {
			return "playlist";
		}
		public String[] getSyntax() {
			return new String[] {
					"play <playlist>",
					"create <playlist>",
					"list [<playlist>]",
					"delete <playlist> <song>",
					"addSong <playlist> <song>",
					"removeSong <playlist> <song>",
					"renameSong <playlist> <index> <new name>",
					"loop",
					"shuffle",
			};
		}
		public String getDescription() {
			return "Configures playlists";
		}
		public boolean processCommand(String args) {
			String[] split = args.split(" ");

			if (split.length < 1) return false;

			try {
				Path playlistDir = null;
				if (split.length >= 2) {
					playlistDir = SongPlayer.PLAYLISTS_DIR.resolve(split[1]);
				}
				switch (split[0].toLowerCase(Locale.ROOT)) {
					case "play": {
						if (split.length != 2) return false;
						if (!Files.exists(playlistDir)) {
							SongPlayer.addChatMessage("§cPlaylist does not exist");
							return true;
						}
						SongHandler.getInstance().setPlaylist(playlistDir);
						return true;
					}
					case "create": {
						if (split.length > 2) {
							SongPlayer.addChatMessage("§cCannot have spaces in playlist name");
							return true;
						}
						if (split.length != 2) return false;
						Playlist.createPlaylist(split[1]);
						SongPlayer.addChatMessage(String.format("§6Created playlist §3%s", split[1]));
						return true;
					}
					case "delete": {
						if (split.length != 2) return false;
						Playlist.deletePlaylist(playlistDir);
						SongPlayer.addChatMessage(String.format("§6Deleted playlist §3%s", split[1]));
						return true;
					}
					case "list": {
						if (split.length == 1) {
							if (!Files.exists(SongPlayer.PLAYLISTS_DIR)) return true;
							List<String> playlists = Files.list(SongPlayer.PLAYLISTS_DIR)
									.filter(Files::isDirectory)
									.map(Path::getFileName)
									.map(Path::toString)
									.collect(Collectors.toList());
							if (playlists.size() == 0) {
								SongPlayer.addChatMessage("§6No playlists found");
							} else {
								SongPlayer.addChatMessage("§6Playlists: §3" + String.join(", ", playlists));
							}
							return true;
						}
						List<String> playlistIndex = Playlist.listSongs(playlistDir);
						SongPlayer.addChatMessage("§6------------------------------");
						int index = 0;
						for (String songName : playlistIndex) {
							index++;
							SongPlayer.addChatMessage(String.format("§6%d. §3%s", index, songName));
						}
						SongPlayer.addChatMessage("§6------------------------------");
						return true;
					}
					case "addsong": {
						if (split.length < 3) return false;
						String location = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
						Playlist.addSong(playlistDir, SongPlayer.SONG_DIR.resolve(location));
						SongPlayer.addChatMessage(String.format("§6Added §3%s §6to §3%s", location, split[1]));
						return true;
					}
					case "removesong": {
						if (split.length < 3) return false;
						String location = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
						Playlist.removeSong(playlistDir, location);
						SongPlayer.addChatMessage(String.format("§6Removed §3%s §6from §3%s", location, split[1]));
						return true;
					}
					case "renamesong": {
						if (split.length < 4) return false;
						String location = String.join(" ", Arrays.copyOfRange(split, 3, split.length));
						int index = 0;
						try {
							index = Integer.parseInt(split[2]);
						}
						catch (Exception e) {
							SongPlayer.addChatMessage(String.format("§cIndex must be an integer"));
							return true;
						}
						String oldName = Playlist.renameSong(playlistDir, index-1, location);
						SongPlayer.addChatMessage(String.format("§6Renamed §3%s §6to §3%s", oldName, location));
						return true;
					}
					case "loop": {
						if (split.length != 1) return false;
						Config.getConfig().loopPlaylists = !Config.getConfig().loopPlaylists;
						SongHandler.getInstance().setPlaylistLoop(Config.getConfig().loopPlaylists);
						if (Config.getConfig().loopPlaylists) {
							SongPlayer.addChatMessage("§6Enabled playlist looping");
						} else {
							SongPlayer.addChatMessage("§6Disabled playlist looping");
						}
						Config.saveConfigWithErrorHandling();
						return true;
					}
					case "shuffle": {
						if (split.length != 1) return false;
						Config.getConfig().shufflePlaylists = !Config.getConfig().shufflePlaylists;
						SongHandler.getInstance().setPlaylistShuffle(Config.getConfig().shufflePlaylists);
						if (Config.getConfig().shufflePlaylists) {
							SongPlayer.addChatMessage("§6Enabled playlist shuffling");
						} else {
							SongPlayer.addChatMessage("§6Disabled playlist shuffling");
						}
						Config.saveConfigWithErrorHandling();
						return true;
					}
					default: {
						return false;
					}
				}
			}
			catch (IOException e) {
				SongPlayer.addChatMessage("§c" + e.getMessage());
				return true;
			}
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] split = args.split(" ", -1);
			if (split.length <= 1) {
				return CommandSource.suggestMatching(new String[]{
						"play",
						"create",
						"delete",
						"list",
						"addSong",
						"removeSong",
						"renameSong",
						"loop",
						"shuffle",
				}, suggestionsBuilder);
			}
			switch (split[0].toLowerCase(Locale.ROOT)) {
				case "create":
				case "loop":
				case "shuffle":
				default: {
					return null;
				}
				case "play":
				case "list":
				case "delete": {
					if (split.length == 2) {
						return Util.givePlaylistSuggestions(suggestionsBuilder);
					}
					return null;
				}
				case "addsong": {
					if (split.length == 2) {
						return Util.givePlaylistSuggestions(suggestionsBuilder);
					} else if (split.length >= 3) {
						String location = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
						return Util.giveSongSuggestions(location, suggestionsBuilder);
					}
					return null;
				}
				case "removesong": {
					if (split.length == 2) {
						return Util.givePlaylistSuggestions(suggestionsBuilder);
					} else if (split.length == 3) {
						Path playlistDir = SongPlayer.PLAYLISTS_DIR.resolve(split[1]);
						Stream<Path> playlistFiles = Playlist.getSongFiles(playlistDir);
						if (playlistFiles == null) {
							return null;
						}
						return CommandSource.suggestMatching(
								playlistFiles.map(Path::getFileName)
										.map(Path::toString),
								suggestionsBuilder);
					}
					return null;
				}
				case "renamesong": {
					if (split.length == 2) {
						return Util.givePlaylistSuggestions(suggestionsBuilder);
					} else if (split.length == 3) {
						Path playlistDir = SongPlayer.PLAYLISTS_DIR.resolve(split[1]);
						Stream<Path> playlistFiles = Playlist.getSongFiles(playlistDir);
						if (playlistFiles == null) {
							return null;
						}
						int max = playlistFiles.collect(Collectors.toList()).size();
						Stream<String> suggestions = IntStream.range(1, max+1).mapToObj(Integer::toString);
						return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
					}
					return null;
				}
			}
		}
	}

	private static class setCreativeCommandCommand extends Command {
		public String getName() {
			return "setCreativeCommand";
		}
		public String[] getAliases() {
			return new String[]{"sc"};
		}
		public String[] getSyntax() {
			return new String[] {"<command>"};
		}
		public String getDescription() {
			return "Sets the command used to go into creative mode";
		}
		public boolean processCommand(String args) {
			if (args.length() > 0) {
				if (args.startsWith("/")) {
					Config.getConfig().creativeCommand = args.substring(1);
				} else {
					Config.getConfig().creativeCommand = args;
				}
				SongPlayer.addChatMessage("§6Set creative command to §3/" + Config.getConfig().creativeCommand);
				Config.saveConfigWithErrorHandling();
				return true;
			}
			else {
				return false;
			}
		}
	}

	private static class setSurvivalCommandCommand extends Command {
		public String getName() {
			return "setSurvivalCommand";
		}
		public String[] getAliases() {
			return new String[]{"ss"};
		}
		public String[] getSyntax() {
			return new String[] {"<command>"};
		}
		public String getDescription() {
			return "Sets the command used to go into survival mode";
		}
		public boolean processCommand(String args) {
			if (args.length() > 0) {
				if (args.startsWith("/")) {
					Config.getConfig().survivalCommand = args.substring(1);
				} else {
					Config.getConfig().survivalCommand = args;
				}
				SongPlayer.addChatMessage("§6Set survival command to §3/" + Config.getConfig().survivalCommand);
				Config.saveConfigWithErrorHandling();
				return true;
			}
			else {
				return false;
			}
		}
	}

	private static class useEssentialsCommandsCommand extends Command {
		public String getName() {
			return "useEssentialsCommands";
		}
		public String[] getAliases() {
			return new String[]{"essentials", "useEssentials", "essentialsCommands"};
		}
		public String[] getSyntax() {
			return new String[0];
		}
		public String getDescription() {
			return "Switches to using essentials gamemode commands";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				Config.getConfig().creativeCommand = "gmc";
				Config.getConfig().survivalCommand = "gms";
				SongPlayer.addChatMessage("§6Now using essentials gamemode commands");
				Config.saveConfigWithErrorHandling();
				return true;
			}
			else {
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
		public String[] getSyntax() {
			return new String[0];
		}
		public String getDescription() {
			return "Switches to using vanilla gamemode commands";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				Config.getConfig().creativeCommand = "gamemode creative";
				Config.getConfig().survivalCommand = "gamemode survival";
				SongPlayer.addChatMessage("§6Now using vanilla gamemode commands");
				Config.saveConfigWithErrorHandling();
				return true;
			}
			else {
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
		public String[] getSyntax() {
			return new String[0];
		}
		public String getDescription() {
			return "Shows a fake player representing your true position when playing songs";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				Config.getConfig().showFakePlayer = !Config.getConfig().showFakePlayer;
				if (Config.getConfig().showFakePlayer) {
					SongPlayer.addChatMessage("§6Enabled fake player");
				}
				else {
					SongPlayer.addChatMessage("§6Disabled fake player");
				}
				Config.saveConfigWithErrorHandling();
				return true;
			}
			else {
				return false;
			}
		}
	}

	private static class setStageTypeCommand extends Command {
		public String getName() {
			return "setStageType";
		}
		public String[] getAliases() {
			return new String[]{"setStage", "stageType"};
		}
		public String[] getSyntax() {
			return new String[] {"<DEFAULT | WIDE | SPHERICAL>"};
		}
		public String getDescription() {
			return "Sets the type of noteblock stage to build";
		}
		public boolean processCommand(String args) {
			if (args.length() > 0) {
				try {
					Stage.StageType stageType = Stage.StageType.valueOf(args.toUpperCase(Locale.ROOT));
					Config.getConfig().stageType = stageType;
					SongPlayer.addChatMessage("§6Set stage type to §3" + stageType.name());
					Config.saveConfigWithErrorHandling();
				}
				catch (IllegalArgumentException e) {
					SongPlayer.addChatMessage("§cInvalid stage type");
				}
				return true;
			}
			else {
				return false;
			}
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			if (!args.contains(" ")) {
				return CommandSource.suggestMatching(Arrays.stream(Stage.StageType.values()).map(Stage.StageType::name), suggestionsBuilder);
			}
			else {
				return null;
			}
		}
	}

	private static class toggleMovementCommand extends Command {
		public String getName() {
			return "toggleMovement";
		}
		public String[] getAliases() {
			return new String[]{"movement"};
		}
		public String[] getSyntax() {
			return new String[] {"<swing | rotate>"};
		}
		public String getDescription() {
			return "Toggles different types of movements";
		}
		public boolean processCommand(String args) {
			switch (args.toLowerCase(Locale.ROOT)) {
				case "swing":
					Config.getConfig().swing = !Config.getConfig().swing;
					if (Config.getConfig().swing) {
						SongPlayer.addChatMessage("§6Enabled arm swinging");
					}
					else {
						SongPlayer.addChatMessage("§6Disabled arm swinging");
					}
					Config.saveConfigWithErrorHandling();
					return true;
				case "rotate":
					Config.getConfig().rotate = !Config.getConfig().rotate;
					if (Config.getConfig().rotate) {
						SongPlayer.addChatMessage("§6Enabled player rotation");
					}
					else {
						SongPlayer.addChatMessage("§6Disabled player rotation");
					}
					Config.saveConfigWithErrorHandling();
					return true;
				default:
					return false;
			}
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			if (!args.contains(" ")) {
				return CommandSource.suggestMatching(new String[]{"swing", "rotate"}, suggestionsBuilder);
			}
			else {
				return null;
			}
		}
	}

	private static class announcementCommand extends Command {
		public String getName() {
			return "announcement";
		}
		public String[] getSyntax() {
			return new String[] {
					"enable",
					"disable",
					"getMessage",
					"setMessage <message>",
			};
		}
		public String getDescription() {
			return "Set an announcement message that is sent when you start playing a song. With setMessage, write [name] where the song name should go.";
		}
		public boolean processCommand(String args) {
			String[] split = args.split(" ", 2);
			switch (split[0].toLowerCase(Locale.ROOT)) {
				case "enable":
					if (split.length != 1) return false;
					Config.getConfig().doAnnouncement = true;
					SongPlayer.addChatMessage("§6Enabled song announcements");
					Config.saveConfigWithErrorHandling();
					return true;
				case "disable":
					if (split.length != 1) return false;
					Config.getConfig().doAnnouncement = false;
					SongPlayer.addChatMessage("§6Disabled song announcements");
					Config.saveConfigWithErrorHandling();
					return true;
				case "getmessage":
					if (split.length != 1) return false;
					SongPlayer.addChatMessage("§6Current announcement message is §r" + Config.getConfig().announcementMessage);
					return true;
				case "setmessage":
					if (split.length != 2) return false;
					Config.getConfig().announcementMessage = split[1];
					SongPlayer.addChatMessage("§6Set announcement message to §r" + split[1]);
					Config.saveConfigWithErrorHandling();
					return true;
				default:
					return false;
			}
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			if (!args.contains(" ")) {
				return CommandSource.suggestMatching(new String[]{"enable", "disable", "getMessage", "setMessage"}, suggestionsBuilder);
			}
			else {
				return null;
			}
		}
	}

	private static class songItemCommand extends Command {
		public String getName() {
			return "songItem";
		}
		public String[] getAliases() {
			return new String[]{"item"};
		}
		public String[] getSyntax() {
			return new String[] {
					"create <song or url>",
					"setSongName <name>",
			};
		}
		public String getDescription() {
			return "Assigns/edits song data for the item in your hand";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				return false;
			}

			if (MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
				SongPlayer.addChatMessage("§cYou must be in creative mode to use this command");
				return true;
			}

			ItemStack stack = MC.player.getMainHandStack();
			NbtCompound songPlayerNBT = SongItemUtils.getSongItemTag(stack);

			String[] split = args.split(" ");
			switch (split[0].toLowerCase(Locale.ROOT)) {
				case "create":
					if (split.length < 2) return false;
					String location = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
					try {
						(new SongItemCreatorThread(location)).start();
					} catch (IOException e) {
						SongPlayer.addChatMessage("§cError creating song item: §4" + e.getMessage());
					}
					return true;
				case "setsongname":
					if (split.length < 2) return false;
					if (songPlayerNBT == null) {
						SongPlayer.addChatMessage("§cYou must be holding a song item");
						return true;
					}
					String name = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
					songPlayerNBT.putString(SongItemUtils.DISPLAY_NAME_KEY, name);
					SongItemUtils.addSongItemDisplay(stack);
					MC.player.setStackInHand(Hand.MAIN_HAND, stack);
					MC.interactionManager.clickCreativeStack(MC.player.getStackInHand(Hand.MAIN_HAND), 36 + MC.player.getInventory().selectedSlot);
					SongPlayer.addChatMessage("§6Set song's display name to §3" + name);
					return true;
				default:
					return false;
			}
		}
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			String[] split = args.split(" ", -1);
			if (split.length <= 1) {
				return CommandSource.suggestMatching(new String[] {
						"create",
						"setSongName",
				}, suggestionsBuilder);
			}
			switch (split[0].toLowerCase(Locale.ROOT)) {
				case "create":
					if (split.length >= 2) {
						String location = String.join(" ", Arrays.copyOfRange(split, 1, split.length));
						return Util.giveSongSuggestions(location, suggestionsBuilder);
					}
				case "setsongname":
				default:
					return null;
			}
		}
	}

	private static class testSongCommand extends Command {
		public String getName() {
			return "testSong";
		}
		public String[] getSyntax() {
			return new String[0];
		}
		public String getDescription() {
			return "Creates a song for testing";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				Song song = new Song("test_song");
				for (int i=0; i<400; i++) {
					song.add(new Note(i, i*50));
				}
				song.length = 400*50;
				SongHandler.getInstance().setSong(song);
				return true;
			}
			else {
				return false;
			}
		}
	}

	public static CompletableFuture<Suggestions> handleSuggestions(String text, SuggestionsBuilder suggestionsBuilder) {
		if (!text.contains(" ")) {
			List<String> names = commandCompletions
					.stream()
					.map((commandName) -> Config.getConfig().prefix+commandName)
					.collect(Collectors.toList());
			return CommandSource.suggestMatching(names, suggestionsBuilder);
		} else {
			String[] split = text.split(" ", 2);
			if (split[0].startsWith(Config.getConfig().prefix)) {
				String commandName = split[0].substring(1).toLowerCase(Locale.ROOT);
				if (commandMap.containsKey(commandName)) {
					return commandMap.get(commandName).getSuggestions(split.length == 1 ? "" : split[1], suggestionsBuilder);
				}
			}
			return null;
		}
	}
}
