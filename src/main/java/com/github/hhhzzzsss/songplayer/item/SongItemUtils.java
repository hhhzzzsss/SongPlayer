package com.github.hhhzzzsss.songplayer.item;

import com.github.hhhzzzsss.songplayer.Util;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Base64;

public class SongItemUtils {
    public static final String SONG_ITEM_KEY = "SongItemData";
    public static final String SONG_DATA_KEY = "SongData";
    public static final String FILE_NAME_KEY = "FileName";
    public static final String DISPLAY_NAME_KEY = "DisplayName";

    public static ItemStack createSongItem(ItemStack stack, byte[] songData, String filename, String displayName) {
        NbtCompound songPlayerNbt = new NbtCompound();
        songPlayerNbt.putString(SONG_DATA_KEY, Base64.getEncoder().encodeToString(songData));
        songPlayerNbt.putString(FILE_NAME_KEY, filename);
        songPlayerNbt.putString(DISPLAY_NAME_KEY, displayName);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(SONG_ITEM_KEY, songPlayerNbt));
        addSongItemDisplay(stack);
        return stack;
    }

    public static void addSongItemDisplay(ItemStack stack) {
        if (!isSongItem(stack)) return;
        NbtCompound songPlayerNbt = getSongItemTag(stack);
        String name = songPlayerNbt.getString(DISPLAY_NAME_KEY);
        if (name == null || name.length() == 0) name = songPlayerNbt.getString(FILE_NAME_KEY);
        if (name == null || name.length() == 0) name = "unnamed";
        Text nameText = Util.getStyledText(name, Style.EMPTY.withColor(Formatting.DARK_AQUA).withItalic(false));
        Util.setItemName(stack, nameText);
        Util.setItemLore(stack,
                Util.getStyledText("Song Item", Style.EMPTY.withColor(Formatting.YELLOW).withItalic(false)),
                Util.getStyledText("Right click to play", Style.EMPTY.withColor(Formatting.AQUA).withItalic(false)),
                Util.getStyledText("Requires SongPlayer 3.0+", Style.EMPTY.withColor(Formatting.GOLD).withItalic(false)),
                Util.getStyledText("https://github.com/hhhzzzsss/SongPlayer", Style.EMPTY.withColor(Formatting.GRAY).withItalic(false))
        );
    }

    public static NbtCompound getSongItemTag(ItemStack stack) {
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (nbt.contains(SONG_ITEM_KEY, NbtElement.COMPOUND_TYPE)) {
            return (NbtCompound)nbt.get(SONG_ITEM_KEY);
        } else {
            return null;
        }
    }

    public static boolean isSongItem(ItemStack stack) {
        return getSongItemTag(stack) != null;
    }

    public static byte[] getSongData(ItemStack stack) throws IllegalArgumentException {
        NbtCompound songPlayerNbt = getSongItemTag(stack);
        if (songPlayerNbt == null || !songPlayerNbt.contains(SONG_DATA_KEY, NbtElement.STRING_TYPE)) {
            return null;
        }
        else {
            return Base64.getDecoder().decode(songPlayerNbt.getString(SONG_DATA_KEY));
        }
    }
}
