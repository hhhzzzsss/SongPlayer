package com.github.hhhzzzsss.songplayer.mixin;

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
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
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
		/*if (Freecam.getInstance().isEnabled() && packet instanceof PlayerMoveC2SPacket) {
			ci.cancel();
		}*/
		if (SongPlayer.mode != SongPlayer.Mode.IDLE && packet instanceof PlayerMoveC2SPacket) {
			connection.send(new PlayerMoveC2SPacket.Full(SongPlayer.stage.position.getX()+0.5, SongPlayer.stage.position.getY(), SongPlayer.stage.position.getZ()+0.5, SongPlayer.MC.player.getYaw(), SongPlayer.MC.player.getPitch(), true));
			if (SongPlayer.fakePlayer != null) {
				SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
			}
			ci.cancel();
		}
	}
	
	@Inject(at = @At("HEAD"), method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V")
	public void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
		//Freecam.getInstance().onGameJoin();
		SongPlayer.mode = SongPlayer.Mode.IDLE;
	}
	
	@Inject(at = @At("HEAD"), method = "onBlockUpdate(Lnet/minecraft/network/packet/s2c/play/BlockUpdateS2CPacket;)V")
	public void onOnBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
		if (SongPlayer.mode == SongPlayer.Mode.PLAYING && SongPlayer.stage.noteblockPositions.contains(packet.getPos())) {
			SongPlayer.stage.rebuild = true;
		}
	}
}
