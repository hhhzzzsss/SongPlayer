package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.CommandProcessor;
import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Shadow
	private final ClientConnection connection;
	
	public ClientPlayNetworkHandlerMixin() {
		connection = null;
	}
	
	@Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", cancellable = true)
	private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
		Stage stage = SongHandler.getInstance().stage;

		if (stage != null && packet instanceof PlayerMoveC2SPacket) {
			if (Config.getConfig().rotate) {
				connection.send(new PlayerMoveC2SPacket.PositionAndOnGround(stage.position.getX()+0.5, stage.position.getY(), stage.position.getZ()+0.5, true));
			} else {
				connection.send(new PlayerMoveC2SPacket.Full(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, SongPlayer.MC.player.getYaw(), SongPlayer.MC.player.getPitch(), true));
				if (SongPlayer.fakePlayer != null) {
					SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
				}
			}
			ci.cancel();
		}
		else if (packet instanceof ClientCommandC2SPacket) {
			ClientCommandC2SPacket.Mode mode = ((ClientCommandC2SPacket) packet).getMode();
			if (SongPlayer.fakePlayer != null) {
				if (mode == ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY) {
					SongPlayer.fakePlayer.setSneaking(true);
					SongPlayer.fakePlayer.setPose(EntityPose.CROUCHING);
				}
				else if (mode == ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY) {
					SongPlayer.fakePlayer.setSneaking(false);
					SongPlayer.fakePlayer.setPose(EntityPose.STANDING);
				}
			}
		}
	}

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
}
