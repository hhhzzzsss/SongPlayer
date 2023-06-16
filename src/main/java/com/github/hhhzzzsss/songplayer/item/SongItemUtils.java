package com.github.hhhzzzsss.songplayer.item;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.Base64;

public class SongItemUtils {
    public static final String SONG_ITEM_KEY = "SongItemData";
    public static final String SONG_DATA_KEY = "SongData";
    public static final String FILE_NAME_KEY = "FileName";
    public static final String DISPLAY_NAME_KEY = "DisplayName";

    public static ItemStack createSongItem(ItemStack stack, byte[] songData, String filename, String displayName) {
        NbtCompound songPlayerNbt = new NbtCompound();
        stack.setSubNbt(SONG_ITEM_KEY, songPlayerNbt);
        songPlayerNbt.putString(SONG_DATA_KEY, Base64.getEncoder().encodeToString(songData));
        songPlayerNbt.putString(FILE_NAME_KEY, filename);
        songPlayerNbt.putString(DISPLAY_NAME_KEY, displayName);
        return stack;
    }

    public static NbtCompound getSongItemTag(ItemStack stack) {
        return stack.getSubNbt(SONG_ITEM_KEY);
    }

    public static boolean isSongItem(ItemStack stack) {
        return getSongItemTag(stack) != null;
    }

    public static byte[] getSongData(ItemStack stack) {
        NbtCompound songPlayerNbt = getSongItemTag(stack);
        if (songPlayerNbt == null || !songPlayerNbt.contains(SONG_DATA_KEY, NbtElement.STRING_TYPE)) {
            return null;
        }
        else {
            return Base64.getDecoder().decode(songPlayerNbt.getString(SONG_DATA_KEY));
        }
    }
}
