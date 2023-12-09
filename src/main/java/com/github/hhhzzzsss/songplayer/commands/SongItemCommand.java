package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.item.SongItemCreatorThread;
import com.github.hhhzzzsss.songplayer.item.SongItemUtils;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.github.hhhzzzsss.songplayer.SongPlayer.MC;

class SongItemCommand extends Command {
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
        if (args.isEmpty()) return false;

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