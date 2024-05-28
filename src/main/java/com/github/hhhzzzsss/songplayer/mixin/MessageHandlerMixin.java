package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import net.minecraft.client.network.message.MessageHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MessageHandler.class)
public class MessageHandlerMixin {
    @Inject(at = @At("HEAD"), method = "onGameMessage", cancellable = true)
    public void onMessage(Text message, boolean overlay, CallbackInfo ci) {
        String msg = message.getString();
        if ((SongPlayer.includeCommandBlocks && SongHandler.getInstance().currentSong != null && !SongHandler.getInstance().paused) || SongPlayer.kiosk) {
            if (msg.startsWith("Command set: ") && SongPlayer.includeCommandBlocks) {
                ci.cancel();
            } else if (msg.startsWith("Command blocks are not enabled on this server")) {
                ci.cancel();
                SongPlayer.addChatMessage("Cannot run commands; Command blocks are disabled on this server.");
                SongHandler.getInstance().paused = true;
            } else if (msg.startsWith("Must be an opped player in creative mode")) {
                ci.cancel();
                SongPlayer.addChatMessage("Cannot run commands; You are not an operator or you aren't in creative mode.");
                SongHandler.getInstance().paused = true;
            }
        }
    }
}
