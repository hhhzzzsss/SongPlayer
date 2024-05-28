package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.KioskCommandProcessor;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.ProgressDisplay;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.Uuids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import net.minecraft.client.MinecraftClient;

import java.util.UUID;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Inject(at = @At("HEAD"), method = "render(Z)V")
	public void onRender(boolean tick, CallbackInfo ci) {
		if (SongPlayer.MC.world != null && SongPlayer.MC.player != null && SongPlayer.MC.interactionManager != null) {
			SongHandler.getInstance().onUpdate(false);
			//updates inventory and status of fakeplayer
			if (SongPlayer.fakePlayer != null) {
				SongPlayer.fakePlayer.updateFakePlayer();
			}
		} else {
			SongHandler.getInstance().onNotIngame();
		}
	}

	@Inject(at = @At("HEAD"), method = "tick()V")
	public void onTick(CallbackInfo ci) {
		if (SongPlayer.MC.interactionManager != null && SongPlayer.MC.world != null && SongPlayer.MC.player != null) {
			SongHandler.getInstance().onUpdate(true);
			Util.lastSwingPacket++;
			if (SongPlayer.recording && SongPlayer.recordingActive) {
				SongPlayer.recordingtick++;
			}
			if (SongPlayer.kiosk) {
				for (UUID e : SongPlayer.MC.getNetworkHandler().getPlayerUuids()) {
					KioskCommandProcessor.commandCooldown.merge(e, 1, Integer::sum);
				}
			}
		} else {
			SongPlayer.kiosk = false;
		}

		ProgressDisplay.getInstance().onTick();
	}
}