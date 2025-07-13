package com.github.hhhzzzsss.songplayer.item;

import com.github.hhhzzzsss.songplayer.Util;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

import java.util.Base64;
import java.util.Optional;
import java.util.function.Consumer;

public class SongItemUtils {
    public static final String SONG_ITEM_KEY = "SongItemData";
    public static final String SONG_DATA_KEY = "SongData";
    public static final String FILE_NAME_KEY = "FileName";
    public static final String DISPLAY_NAME_KEY = "DisplayName";

    public static ItemStack createSongItem(ItemStack stack, byte[] songData, String filename, String displayName) {
        NbtCompound songItemTag = new NbtCompound();
        songItemTag.putString(SONG_DATA_KEY, Base64.getEncoder().encodeToString(songData));
        songItemTag.putString(FILE_NAME_KEY, filename);
        songItemTag.putString(DISPLAY_NAME_KEY, displayName);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(SONG_ITEM_KEY, songItemTag));
        addSongItemDisplay(stack);
        return stack;
    }

    public static void addSongItemDisplay(ItemStack stack) {
        getSongItemTag(stack).ifPresent((songItemTag) -> {
            String name = songItemTag.getString(DISPLAY_NAME_KEY)
                    .or(() -> songItemTag.getString(FILE_NAME_KEY))
                    .orElse("unnamed");
            Util.setItemName(stack,
                    Util.getStyledText(name, Style.EMPTY.withColor(Formatting.DARK_AQUA).withItalic(false))
            );
            Util.setItemLore(stack,
                    Util.getStyledText("Song Item", Style.EMPTY.withColor(Formatting.YELLOW).withItalic(false)),
                    Util.getStyledText("Right click to play", Style.EMPTY.withColor(Formatting.AQUA).withItalic(false)),
                    Util.getStyledText("Requires SongPlayer 3.0+", Style.EMPTY.withColor(Formatting.GOLD).withItalic(false)),
                    Util.getStyledText("https://github.com/hhhzzzsss/SongPlayer", Style.EMPTY.withColor(Formatting.GRAY).withItalic(false))
            );
        });
    }

    public static void updateSongItemTag(ItemStack stack, Consumer<NbtCompound> nbtSetter) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, (itemNbt) -> {
            itemNbt.getCompound(SONG_ITEM_KEY).ifPresent(nbtSetter);
        });
    }

    public static Optional<NbtCompound> getSongItemTag(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .copyNbt()
                .getCompound(SONG_ITEM_KEY);
    }

    public static boolean isSongItem(ItemStack stack) {
        return getSongItemTag(stack).isPresent();
    }

    public static byte[] getSongData(ItemStack stack) throws IllegalArgumentException {
        return getSongItemTag(stack)
                .flatMap((songItemTag) -> songItemTag.getString(SONG_DATA_KEY))
                .map((songData) -> Base64.getDecoder().decode(songData))
                .orElse(null);
    }
}
