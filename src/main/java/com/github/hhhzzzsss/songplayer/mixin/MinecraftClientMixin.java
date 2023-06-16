package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.item.SongItemConfirmationScreen;
import com.github.hhhzzzsss.songplayer.item.SongItemUtils;
import com.github.hhhzzzsss.songplayer.playing.ProgressDisplay;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Inject(at = @At("HEAD"), method = "render(Z)V")
	public void onRender(boolean tick, CallbackInfo ci) {
		if (SongPlayer.MC.world != null && SongPlayer.MC.player != null && SongPlayer.MC.interactionManager != null) {
			SongHandler.getInstance().onUpdate(false);
		} else {
			SongHandler.getInstance().onNotIngame();
		}
	}

	@Inject(at = @At("HEAD"), method = "tick()V")
	public void onTick(CallbackInfo ci) {
		if (SongPlayer.MC.world != null && SongPlayer.MC.player != null && SongPlayer.MC.interactionManager != null) {
			SongHandler.getInstance().onUpdate(true);
		}
		ProgressDisplay.getInstance().onTick();
	}

	@Inject(at = @At("HEAD"), method = "doItemUse()V", cancellable = true)
	private void onDoItemUse(CallbackInfo ci) {
		ItemStack stack = SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND);
		if (SongItemUtils.isSongItem(stack)) {
			try {
				SongPlayer.MC.setScreen(new SongItemConfirmationScreen(stack));
			} catch (IOException e) {
				SongPlayer.addChatMessage("§cFailed to load song item: §4" + e.getMessage());
			}
		}
	}
}
