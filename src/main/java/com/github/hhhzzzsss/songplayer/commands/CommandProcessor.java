package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CommandProcessor {
	static ArrayList<Command> commands = new ArrayList<>();
	static HashMap<String, Command> commandMap = new HashMap<>();
	static ArrayList<String> commandCompletions = new ArrayList<>();

	public static void initCommands() {
		commands.add(new HelpCommand());
		commands.add(new SetPrefixCommand());
		commands.add(new PlayCommand());
		commands.add(new StopCommand());
		commands.add(new SkipCommand());
		commands.add(new GotoCommand());
		commands.add(new LoopCommand());
		commands.add(new StatusCommand());
		commands.add(new QueueCommand());
		commands.add(new SongsCommand());
		commands.add(new PlaylistCommand());
		commands.add(new ToggleFakePlayerCommand());
		commands.add(new SetStageTypeCommand());
		commands.add(new ToggleMovementCommand());
		commands.add(new AnnouncementCommand());
		commands.add(new SongItemCommand());
		commands.add(new TestSongCommand());
		commands.add(new SetCommandsCommand());

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
		if (!message.startsWith(Config.getConfig().prefix)) return false;

		String[] parts = message.substring(Config.getConfig().prefix.length()).split(" ", 2);
		String name = parts.length > 0 ? parts[0] : "";
		String args = parts.length > 1 ? parts[1] : "";

		Command c = commandMap.get(name.toLowerCase(Locale.ROOT));
		if (c == null) {
			SongPlayer.addChatMessage("§cUnrecognized command");
			return true;
		}

		try {
			boolean success = c.processCommand(args);
			if (success) return true;

			if (c.getSyntax().length == 0) {
				SongPlayer.addChatMessage("§cSyntax: " + Config.getConfig().prefix + c.getName());
			} else if (c.getSyntax().length == 1) {
				SongPlayer.addChatMessage("§cSyntax: " + Config.getConfig().prefix + c.getName() + " " + c.getSyntax()[0]);
			} else {
				SongPlayer.addChatMessage("§cSyntax:");
				for (String syntax : c.getSyntax()) {
					SongPlayer.addChatMessage("§c    " + Config.getConfig().prefix + c.getName() + " " + syntax);
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
			SongPlayer.addChatMessage("§cAn error occurred while running this command: §4" + e.getMessage());
		}

		return true;
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
