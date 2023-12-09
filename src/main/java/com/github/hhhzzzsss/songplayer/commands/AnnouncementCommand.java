package com.github.hhhzzzsss.songplayer.commands;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

class AnnouncementCommand extends Command {
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

        return null;
    }
}
