package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.ProgressDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import net.minecraft.client.MinecraftClient;

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
}
