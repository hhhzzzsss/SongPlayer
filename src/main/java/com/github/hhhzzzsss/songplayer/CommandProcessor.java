package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.song.Note;
import com.github.hhhzzzsss.songplayer.song.Song;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandProcessor {
	public static ArrayList<Command> commands = new ArrayList<>();
	
	public static void initCommands() {
		commands.add(new helpCommand());
		commands.add(new playCommand());
		commands.add(new stopCommand());
		commands.add(new skipCommand());
		commands.add(new gotoCommand());
		commands.add(new loopCommand());
		commands.add(new currentCommand());
		commands.add(new queueCommand());
		commands.add(new songsCommand());
		commands.add(new setCreativeCommandCommand());
		commands.add(new setSurvivalCommandCommand());
		commands.add(new useEssentialsCommandsCommand());
		commands.add(new useVanillaCommandsCommand());
		commands.add(new toggleFakePlayerCommand());
		commands.add(new testSongCommand());
	}
	
	// returns true if it is a command and should be cancelled
	public static boolean processChatMessage(String message) {
		if (message.startsWith("$")) {
			String[] parts = message.substring(1).split(" ", 2);
		    String name = parts.length>0 ? parts[0] : "";
		    String args = parts.length>1 ? parts[1] : "";
		    for (Command c : commands) {
		    	if (c.getName().equalsIgnoreCase(name)) {
		    		boolean success = c.processCommand(args);
		    		if (!success) {
		    			SongPlayer.addChatMessage("§cSyntax - " + c.getSyntax());
		    		}
		    		return true;
		    	}
		    }
		}
		return false;
	}
	
	private static abstract class Command {
    	public abstract String getName();
    	public abstract String getSyntax();
    	public abstract String getDescription();
    	public abstract boolean processCommand(String args);
		public CompletableFuture<Suggestions> getSuggestions(String args, SuggestionsBuilder suggestionsBuilder) {
			return null;
		}
    }
	
	private static class helpCommand extends Command {
    	public String getName() {
    		return "help";
    	}
    	public String getSyntax() {
    		return "$help [command]";
    	}
    	public String getDescription() {
    		return "Lists commands or explains command";
    	}
    	public boolean processCommand(String args) {
    		if (args.length() == 0) {
    			StringBuilder helpMessage = new StringBuilder("§6Commands -");
    			for (Command c : commands) {
    				helpMessage.append(" $" + c.getName());
    			}
    			SongPlayer.addChatMessage(helpMessage.toString());
    			return true;
    		}
    		else {
    			for (Command c : commands) {
    				if (c.getName().equalsIgnoreCase(args)) {
    					SongPlayer.addChatMessage("§6" + c.getName() + ": " + c.getDescription() + " - " + c.getSyntax());
    					return true;
    				}
    			}
    			SongPlayer.addChatMessage("§cCommand not recognized: " + args);
    			return true;
    		}
    	}
    }
	
	private static class playCommand extends Command {
    	public String getName() {
    		return "play";
    	}
    	public String getSyntax() {
    		return "$play <song or url>";
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
			List<String> filenames = Arrays.stream(SongPlayer.SONG_DIR.listFiles())
					.filter(File::isFile)
					.map(File::getName)
					.collect(Collectors.toList());
			return CommandSource.suggestMatching(filenames, suggestionsBuilder);
		}
    }

	private static class stopCommand extends Command {
    	public String getName() {
    		return "stop";
    	}
    	public String getSyntax() {
    		return "$stop";
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
				SongHandler.getInstance().cleanup();
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
		public String getSyntax() {
			return "$skip";
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
    	public String getSyntax() {
    		return "$goto <mm:ss>";
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
    	public String getSyntax() {
    		return "$loop";
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
			if (SongHandler.getInstance().currentSong.looping) {
				SongPlayer.addChatMessage("§6Enabled looping");
			}
			else {
				SongPlayer.addChatMessage("§6Disabled looping");
			}
			return true;
    	}
    }
	
	private static class currentCommand extends Command {
    	public String getName() {
    		return "current";
    	}
    	public String getSyntax() {
    		return "$current";
    	}
    	public String getDescription() {
    		return "Gets the song that is currently playing";
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
    			SongPlayer.addChatMessage(String.format("§6Currently playing %s §3(%s/%s)", Util.formatTime(currentTime), Util.formatTime(totalTime)));
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
		public String getSyntax() {
			return "$queue";
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

				if (SongHandler.getInstance().currentSong != null) {
					SongPlayer.addChatMessage("§6Current song: §3" + SongHandler.getInstance().currentSong.name);
				}
				int index = 0;
				for (Song song : SongHandler.getInstance().songQueue) {
					index++;
					SongPlayer.addChatMessage(String.format("§6%d. §3%s", index, song.name));
				}
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
    	public String getSyntax() {
    		return "$songs";
    	}
    	public String getDescription() {
    		return "Lists available songs";
    	}
    	public boolean processCommand(String args) {
    		if (args.length() == 0) {
				StringBuilder sb = new StringBuilder("§6");
				boolean firstItem = true;
    			for (File songFile : SongPlayer.SONG_DIR.listFiles()) {
        			String fileName = songFile.getName();
        			if (firstItem) {
        				firstItem = false;
        			}
        			else {
        				sb.append(", ");
        			}
    				sb.append(fileName);
        		}
    			SongPlayer.addChatMessage(sb.toString());
    			return true;
    		}
    		else {
    			return false;
    		}
    	}
    }
	
	private static class setCreativeCommandCommand extends Command {
    	public String getName() {
    		return "setCreativeCommand";
    	}
    	public String getSyntax() {
    		return "$setCreativeCommand";
    	}
    	public String getDescription() {
    		return "Sets the command used to go into creative mode";
    	}
    	public boolean processCommand(String args) {
    		if (args.length() > 0) {
    			SongPlayer.creativeCommand = args;
				if (SongPlayer.creativeCommand.startsWith("/")) {
					SongPlayer.creativeCommand = SongPlayer.creativeCommand.substring(1);
				}
    			SongPlayer.addChatMessage("§6Set creative command to §3/" + SongPlayer.creativeCommand);
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
    	public String getSyntax() {
    		return "$setSurvivalCommand";
    	}
    	public String getDescription() {
    		return "Sets the command used to go into survival mode";
    	}
    	public boolean processCommand(String args) {
    		if (args.length() > 0) {
    			SongPlayer.survivalCommand = args;
				if (SongPlayer.survivalCommand.startsWith("/")) {
					SongPlayer.survivalCommand = SongPlayer.survivalCommand.substring(1);
				}
    			SongPlayer.addChatMessage("§6Set survival command to §3/" + SongPlayer.survivalCommand);
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
		public String getSyntax() {
			return "$useEssentialsCommands";
		}
		public String getDescription() {
			return "Switches to using essentials gamemode commands";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				SongPlayer.creativeCommand = "gmc";
				SongPlayer.survivalCommand = "gms";
				SongPlayer.addChatMessage("§6Now using essentials gamemode commands");
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
		public String getSyntax() {
			return "$useVanillaCommands";
		}
		public String getDescription() {
			return "Switches to using vanilla gamemode commands";
		}
		public boolean processCommand(String args) {
			if (args.length() == 0) {
				SongPlayer.creativeCommand = "gamemode creative";
				SongPlayer.survivalCommand = "gamemode survival";
				SongPlayer.addChatMessage("§6Now using vanilla gamemode commands");
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
    	public String getSyntax() {
    		return "$toggleFakePlayer";
    	}
    	public String getDescription() {
    		return "Shows a fake player representing your true position when playing songs";
    	}
    	public boolean processCommand(String args) {
    		if (args.length() == 0) {
    			SongPlayer.showFakePlayer = !SongPlayer.showFakePlayer;
    			if (SongPlayer.showFakePlayer) {
    				SongPlayer.addChatMessage("§6Enabled fake player");
    			}
    			else {
    				SongPlayer.addChatMessage("§6Disabled fake player");
    			}
				return true;
    		}
    		else {
    			return false;
    		}
    	}
	}

	private static class testSongCommand extends Command {
		public String getName() {
			return "testSong";
		}
		public String getSyntax() {
			return "$testSong";
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

	// $ prefix included in command string
	public static CompletableFuture<Suggestions> handleSuggestions(String text, SuggestionsBuilder suggestionsBuilder) {
		if (!text.contains(" ")) {
			List<String> names = commands
					.stream()
					.map((command) -> "$"+command.getName())
					.collect(Collectors.toList());
			return CommandSource.suggestMatching(names, suggestionsBuilder);
		} else {
			String[] split = text.split(" ");
			for (Command command : commands) {
				if (split[0].equalsIgnoreCase("$"+command.getName())) {
					return command.getSuggestions(split.length == 1 ? "" : split[1], suggestionsBuilder);
				}
			}
		}
		return null;
	}
}
