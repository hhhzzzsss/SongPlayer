package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.noteblocks.SongHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Inject(at = @At("HEAD"), method = "render(Z)V")
	public void onRender(boolean tick, CallbackInfo ci) {
		if (SongPlayer.MC.world != null && SongPlayer.MC.player != null && SongPlayer.MC.interactionManager != null) {
			SongHandler.getInstance().onRenderIngame(tick);
		} else {
			SongHandler.getInstance().onNotIngame();
		}
	}
}
