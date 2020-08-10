package com.github.hhhzzzsss.songplayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.hhhzzzsss.songplayer.SongPlayer.Mode;
import com.github.hhhzzzsss.songplayer.noteblocks.BuildingThread;
import com.github.hhhzzzsss.songplayer.noteblocks.Stage;
import com.github.hhhzzzsss.songplayer.song.DownloadingThread;
import com.github.hhhzzzsss.songplayer.song.Song;

public class CommandProcessor {
	public static ArrayList<Command> commands = new ArrayList<>();
	
	public static void initCommands() {
		commands.add(new helpCommand());
		commands.add(new playCommand());
		commands.add(new playurlCommand());
		commands.add(new stopCommand());
		commands.add(new gotoCommand());
		commands.add(new loopCommand());
		commands.add(new currentCommand());
		commands.add(new songsCommand());
		commands.add(new setCreativeCommandCommand());
		commands.add(new setSurvivalCommandCommand());
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
    		return "$play <song>";
    	}
    	public String getDescription() {
    		return "Plays a song";
    	}
    	public boolean processCommand(String args) {
			if (SongPlayer.mode != Mode.IDLE) {
				SongPlayer.addChatMessage("§cCannot do that while building or playing");
				return true;
			}
    		if (args.length() > 0) {
    			try {
    				SongPlayer.song = Song.getSongFromFile(args);
    			}
    			catch (IOException e) {
    				SongPlayer.addChatMessage("§cCould not find song §4" + args);
    				return true;
    			}
    			
    			SongPlayer.stage = new Stage();
    			SongPlayer.stage.movePlayerToStagePosition();
    			
    			SongPlayer.mode = Mode.BUILDING;
    			SongPlayer.addChatMessage("§6Starting building.");
    			SongPlayer.song.position = 0;
    			(new BuildingThread()).start();
    			return true;
    		}
    		else {
    			return false;
    		}
    	}
    }
	
	private static class playurlCommand extends Command {
    	public String getName() {
    		return "playurl";
    	}
    	public String getSyntax() {
    		return "$playurl <midi url>";
    	}
    	public String getDescription() {
    		return "Plays a song from a direct link to the midi";
    	}
    	public boolean processCommand(String args) {
			if (SongPlayer.mode != Mode.IDLE) {
				SongPlayer.addChatMessage("§cCannot do that while building or playing");
				return true;
			}
    		if (args.length() > 0) {
    			SongPlayer.stage = new Stage();
    			SongPlayer.stage.movePlayerToStagePosition();
    			
    			SongPlayer.addChatMessage("§6Downloading song from url");
    			SongPlayer.mode = Mode.DOWNLOADING;
    			(new DownloadingThread(args)).start();
    			return true;
    		}
    		else {
    			return false;
    		}
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
    		if (SongPlayer.mode != Mode.PLAYING && SongPlayer.mode != Mode.BUILDING) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}
    		if (args.length() == 0) {
    			SongPlayer.stage.movePlayerToStagePosition();
    			SongPlayer.mode = Mode.IDLE;
    			SongPlayer.song.loop = false;
    			SongPlayer.addChatMessage("§6Stopped playing");
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
			if (SongPlayer.mode != Mode.PLAYING) {
				SongPlayer.addChatMessage("§cNo song is currently playing");
				return true;
			}
			
    		if (args.length() > 0) {
    			Pattern timestamp_pattern = Pattern.compile("(\\d+):(\\d+)");
                Matcher timestamp_matcher = timestamp_pattern.matcher(args);
                
                if (timestamp_matcher.matches()) {
                	String minutes = timestamp_matcher.group(1);
                	String seconds = timestamp_matcher.group(2);
                    SongPlayer.song.gotoTime = Integer.parseInt(minutes)*60*1000 + Integer.parseInt(seconds)*1000;
                    System.out.println("set time to " + SongPlayer.song.gotoTime);
                    return true;
                }
                else {
					SongPlayer.addChatMessage("§cNot a valid time stamp");
                	return true;
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
    		if (SongPlayer.mode != Mode.PLAYING) {
				SongPlayer.addChatMessage("§cNo song is currently playing");
				return true;
			}
    		
    		SongPlayer.song.loop = !SongPlayer.song.loop;
			if (SongPlayer.song.loop) {
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
			if (SongPlayer.mode != Mode.PLAYING) {
				SongPlayer.addChatMessage("§6No song is currently playing");
				return true;
			}
    		if (args.length() == 0) {
    			int currTime = (int) (SongPlayer.song.get(SongPlayer.song.position).time/1000);
    			int totTime = (int) (SongPlayer.song.get(SongPlayer.song.size()-1).time/1000);
    			int currTimeSeconds = currTime % 60;
    			int totTimeSeconds = totTime % 60;
    			int currTimeMinutes = currTime / 60;
    			int totTimeMinutes = totTime / 60;
    			SongPlayer.addChatMessage(String.format("§6Currently playing %s §3(%d:%02d/%d:%02d)", SongPlayer.song.name, currTimeMinutes, currTimeSeconds, totTimeMinutes, totTimeSeconds));
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
    			SongPlayer.addChatMessage("Set creative command to " + SongPlayer.creativeCommand);
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
    			SongPlayer.addChatMessage("Set survival command to " + SongPlayer.survivalCommand);
				return true;
    		}
    		else {
    			return false;
    		}
    	}
    }
}
