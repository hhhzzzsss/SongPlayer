package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.CommandProcessor;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Inject(at = @At("HEAD"), method = "sendChatMessage(Ljava/lang/String;)V", cancellable=true)
	private void onSendChatMessage(String content, CallbackInfo ci) {
		boolean isCommand = CommandProcessor.processChatMessage(content);
		if (isCommand) {
			ci.cancel();
		}
	}
	
	@Inject(at = @At("TAIL"), method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V")
	public void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
		SongHandler.getInstance().cleanup();
	}

	@Inject(at = @At("TAIL"), method = "onPlayerRespawn(Lnet/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket;)V")
	public void onOnPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
		SongHandler.getInstance().cleanup();
	}

	@Inject(at = @At("TAIL"), method = "onPlayerPositionLook(Lnet/minecraft/network/packet/s2c/play/PlayerPositionLookS2CPacket;)V")
	public void onOnPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
		Stage stage = SongHandler.getInstance().stage;
		if (!SongHandler.getInstance().isIdle() && stage != null && Vec3d.ofBottomCenter(stage.position).squaredDistanceTo(SongPlayer.MC.player.getPos()) > 3*3) {
			SongPlayer.addChatMessage("§6Stopped playing because the server moved the player too far from the stage!");
			SongHandler.getInstance().cleanup();
		}
	}

	@Inject(at = @At("TAIL"), method = "onPlayerAbilities(Lnet/minecraft/network/packet/s2c/play/PlayerAbilitiesS2CPacket;)V")
	public void onOnPlayerAbilities(PlayerAbilitiesS2CPacket packet, CallbackInfo ci) {
		SongHandler handler = SongHandler.getInstance();
		if (handler.currentSong != null || handler.currentPlaylist != null || handler.songQueue.size() > 0) {
			SongPlayer.MC.player.getAbilities().flying = handler.wasFlying;
		}
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setVelocityClient(DDD)V"), method = "onEntityVelocityUpdate", cancellable = true)
	public void onOnEntityVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
		if (!SongHandler.getInstance().isIdle() && packet.getId() == SongPlayer.MC.player.getId()) {
			ci.cancel();
		}
	}
}
