package com.github.hhhzzzsss.songplayer;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {
    public static void createDirectoriesSilently(Path path) {
        try {
            Files.createDirectories(path);
        }
        catch (IOException e) {}
    }

    public static Path resolveWithIOException(Path path, String other) throws IOException {
        try {
            return path.resolve(other);
        }
        catch (InvalidPathException e) {
            throw new IOException(e.getMessage());
        }
    }

    public static class LimitedSizeInputStream extends InputStream {
        private final InputStream original;
        private final long maxSize;
        private long total;

        public LimitedSizeInputStream(InputStream original, long maxSize) {
            this.original = original;
            this.maxSize = maxSize;
        }

        @Override
        public int read() throws IOException {
            int i = original.read();
            if (i>=0) incrementCounter(1);
            return i;
        }

        @Override
        public int read(byte b[]) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            int i = original.read(b, off, len);
            if (i>=0) incrementCounter(i);
            return i;
        }

        private void incrementCounter(int size) throws IOException {
            total += size;
            if (total>maxSize) throw new IOException("Input stream exceeded maximum size of " + maxSize + " bytes");
        }
    }

    public static String formatTime(long milliseconds) {
        long temp = Math.abs(milliseconds);
        temp /= 1000;
        long seconds = temp % 60;
        temp /= 60;
        long minutes = temp % 60;
        temp /= 60;
        long hours = temp;
        StringBuilder sb = new StringBuilder();
        if (milliseconds < 0) {
            sb.append("-");
        }
        if (hours > 0) {
            sb.append(String.format("%d:", hours));
            sb.append(String.format("%02d:", minutes));
        } else {
            sb.append(String.format("%d:", minutes));
        }
        sb.append(String.format("%02d", seconds));
        return sb.toString();
    }

    public static Pattern timePattern = Pattern.compile("(?:(\\d+):)?(\\d+):(\\d+)");
    public static long parseTime(String timeStr) throws IOException {
        Matcher matcher = timePattern.matcher(timeStr);
        if (matcher.matches()) {
            long time = 0;
            String hourString = matcher.group(1);
            String minuteString = matcher.group(2);
            String secondString = matcher.group(3);
            if (hourString != null) {
                time += Integer.parseInt(hourString) * 60 * 60 * 1000;
            }
            time += Integer.parseInt(minuteString) * 60 * 1000;
            time += Double.parseDouble(secondString) * 1000.0;
            return time;
        } else {
            throw new IOException("Invalid time pattern");
        }
    }

    public static CompletableFuture<Suggestions> giveSongSuggestions(String arg, SuggestionsBuilder suggestionsBuilder) {
        int lastSlash = arg.lastIndexOf("/");
        String dirString = "";
        Path dir = SongPlayer.SONG_DIR;
        if (lastSlash >= 0) {
            dirString = arg.substring(0, lastSlash+1);
            try {
                dir = resolveWithIOException(dir, dirString);
            }
            catch (IOException e) {
                return null;
            }
        }

        Stream<Path> songFiles;
        try {
            songFiles = Files.list(dir);
        } catch (IOException e) {
            return null;
        }

        int clipStart;
        if (arg.contains(" ")) {
            clipStart = arg.lastIndexOf(" ") + 1;
        }
        else {
            clipStart = 0;
        }

        ArrayList<String> suggestionsList = new ArrayList<>();
        for (Path path : songFiles.collect(Collectors.toList())) {
            if (Files.isRegularFile(path)) {
                suggestionsList.add(dirString + path.getFileName().toString());
            }
            else if (Files.isDirectory(path)) {
                suggestionsList.add(dirString + path.getFileName().toString() + "/");
            }
        }
        Stream<String> suggestions = suggestionsList.stream()
                .filter(str -> str.startsWith(arg))
                .map(str -> str.substring(clipStart));
        return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
    }

    public static CompletableFuture<Suggestions> givePlaylistSuggestions(SuggestionsBuilder suggestionsBuilder) {
        if (!Files.exists(SongPlayer.PLAYLISTS_DIR)) return null;
        try {
            return CommandSource.suggestMatching(
                    Files.list(SongPlayer.PLAYLISTS_DIR)
                            .filter(Files::isDirectory)
                            .map(Path::getFileName)
                            .map(Path::toString),
                    suggestionsBuilder);
        } catch (IOException e) {
            return null;
        }
    }

    public static CompletableFuture<Suggestions> giveSongDirectorySuggestions(String arg, SuggestionsBuilder suggestionsBuilder) {
        int lastSlash = arg.lastIndexOf("/");
        String dirString;
        Path dir = SongPlayer.SONG_DIR;
        if (lastSlash >= 0) {
            dirString = arg.substring(0, lastSlash+1);
            try {
                dir = resolveWithIOException(dir, dirString);
            }
            catch (IOException e) {
                return null;
            }
        }
        else {
            dirString = "";
        }

        Stream<Path> songFiles;
        try {
            songFiles = Files.list(dir);
        } catch (IOException e) {
            return null;
        }

        int clipStart;
        if (arg.contains(" ")) {
            clipStart = arg.lastIndexOf(" ") + 1;
        }
        else {
            clipStart = 0;
        }

        Stream<String> suggestions = songFiles
                .filter(Files::isDirectory)
                .map(path -> dirString + path.getFileName().toString() + "/")
                .filter(str -> str.startsWith(arg))
                .map(str -> str.substring(clipStart));
        return CommandSource.suggestMatching(suggestions, suggestionsBuilder);
    }

    public static MutableText getStyledText(String str, Style style) {
        MutableText text = MutableText.of(PlainTextContent.of(str));
        text.setStyle(style);
        return text;
    }

    public static void setItemName(ItemStack stack, Text text) {
        stack.set(DataComponentTypes.CUSTOM_NAME, text);
    }

    public static void setItemLore(ItemStack stack, Text... loreLines) {
        stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(loreLines)));
    }

    public static MutableText joinTexts(MutableText base, Text... children) {
        if (base == null) {
            base = Text.empty();
        }
        for (Text child : children) {
            base.append(child);
        }
        return base;
    }
}
