package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Shadow
	private final ClientConnection connection;
	
	public ClientPlayNetworkHandlerMixin() {
		connection = null;
	}
	
	@Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/Packet;)V", cancellable = true)
	private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
		Stage stage = SongHandler.getInstance().stage;
		if (stage != null && packet instanceof PlayerMoveC2SPacket) {
			connection.send(new PlayerMoveC2SPacket.Full(stage.position.getX()+0.5, stage.position.getY(), stage.position.getZ()+0.5, SongPlayer.MC.player.getYaw(), SongPlayer.MC.player.getPitch(), true));
			if (SongPlayer.fakePlayer != null) {
				SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
			}
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
}
