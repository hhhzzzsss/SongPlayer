package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.song.Playlist;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class PlaylistCommand extends Command {
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
                        if (playlists.isEmpty()) {
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
                        SongPlayer.addChatMessage("§cIndex must be an integer");
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
        return switch (split[0].toLowerCase(Locale.ROOT)) {
            default -> null;
            case "play", "list", "delete" -> {
                if (split.length == 2) {
                    yield Util.givePlaylistSuggestions(suggestionsBuilder);
                }
                yield null;
            }
            case "addsong" -> {
                if (split.length == 2) {
                    yield Util.givePlaylistSuggestions(suggestionsBuilder);
                } else {
                    String location = String.join(" ", Arrays.copyOfRange(split, 2, split.length));
                    yield Util.giveSongSuggestions(location, suggestionsBuilder);
                }
            }
            case "removesong" -> {
                if (split.length == 2) {
                    yield Util.givePlaylistSuggestions(suggestionsBuilder);
                } else if (split.length == 3) {
                    Path playlistDir = SongPlayer.PLAYLISTS_DIR.resolve(split[1]);
                    Stream<Path> playlistFiles = Playlist.getSongFiles(playlistDir);
                    if (playlistFiles == null) {
                        yield null;
                    }
                    yield CommandSource.suggestMatching(
                            playlistFiles.map(Path::getFileName)
                                    .map(Path::toString),
                            suggestionsBuilder);
                }
                yield null;
            }
            case "renamesong" -> {
                if (split.length == 2) {
                    yield Util.givePlaylistSuggestions(suggestionsBuilder);
                } else if (split.length == 3) {
                    Path playlistDir = SongPlayer.PLAYLISTS_DIR.resolve(split[1]);
                    Stream<Path> playlistFiles = Playlist.getSongFiles(playlistDir);
                    if (playlistFiles == null) {
                        yield null;
                    }
                    int max = playlistFiles.toList().size();
                    Stream<String> suggestions = IntStream.range(1, max + 1).mapToObj(Integer::toString);
                    yield CommandSource.suggestMatching(suggestions, suggestionsBuilder);
                }
                yield null;
            }
        };
    }
}
