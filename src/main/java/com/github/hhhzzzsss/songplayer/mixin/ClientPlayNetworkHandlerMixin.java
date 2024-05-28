package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.*;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.command.PlaySoundCommand;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands;
import org.lwjgl.system.CallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.network.ClientPlayNetworkHandler;

import javax.security.auth.callback.Callback;
import java.util.Arrays;
import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {
	@Inject(at = @At("HEAD"), method = "sendChatMessage", cancellable=true)
	private void onSendChatMessage(String content, CallbackInfo ci) {
		boolean isCommand = CommandProcessor.processChatMessage(content);
		if (isCommand) {
			ci.cancel();
			if (content.startsWith(SongPlayer.prefix + "author")) { // lol watermark moment
				SongPlayer.MC.getNetworkHandler().sendChatMessage("SongPlayer made by hhhzzzsss, modified by Sk8kman, and tested by Lizard16");
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "onGameJoin(Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;)V")
	public void onOnGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
		//fixes fakeplayer not rendering the first time
		SongPlayer.fakePlayer = new FakePlayerEntity();
		SongPlayer.removeFakePlayer();
		if (SongHandler.getInstance().paused) {
			return;
		}
		if (!SongPlayer.useNoteblocksWhilePlaying) {
			return;
		}
		SongHandler.getInstance().cleanup(true);
	}

	@Inject(at = @At("TAIL"), method = "onPlayerRespawn(Lnet/minecraft/network/packet/s2c/play/PlayerRespawnS2CPacket;)V")
	public void onOnPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo ci) {
		//fixes fakeplayer not rendering the first time
		if (SongPlayer.fakePlayer == null) {
			SongPlayer.fakePlayer = new FakePlayerEntity();
			SongPlayer.removeFakePlayer();
		}
		if (SongHandler.getInstance().paused) {
			return;
		}
		if (!SongPlayer.useNoteblocksWhilePlaying) {
			return;
		}
		if (SongHandler.getInstance().currentSong != null && !SongHandler.getInstance().paused) {
			Util.pauseSongIfNeeded();
			SongPlayer.addChatMessage("§6Your song has been paused because you died.\n§6Go back to your stage or find a new location and type §3" + SongPlayer.prefix + "resume§6 to resume playing.");
			return;
		}
		//this shouldn't run but if it does at least stuff won't break
		SongHandler.getInstance().cleanup(true);
	}

	@Inject(at = @At("HEAD"), method = "onPlayerPositionLook(Lnet/minecraft/network/packet/s2c/play/PlayerPositionLookS2CPacket;)V")
	public void onPlayerTeleport(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
		if (!SongPlayer.useNoteblocksWhilePlaying) {
			return;
		}
		if (SongHandler.getInstance().paused || SongHandler.getInstance().currentSong == null || SongHandler.getInstance().stage == null) {
			Util.lagBackCounter = 0;
			return;
		}
		Util.lagBackCounter++;
		if (Util.lagBackCounter > 3) {
			Util.pauseSongIfNeeded();
			SongPlayer.addChatMessage("§6Your song has been paused because you are getting moved away from your stage.\nGo back to your stage and type §3" + SongPlayer.prefix + "resume§6 to resume playing.");
			Util.lagBackCounter = 0;
		}
	}

	@Inject(at = @At("HEAD"), method = "onEntityPassengersSet(Lnet/minecraft/network/packet/s2c/play/EntityPassengersSetS2CPacket;)V")
	public void onEntityMount(EntityPassengersSetS2CPacket packet, CallbackInfo ci) {
		if (SongHandler.getInstance().currentSong != null && !SongHandler.getInstance().paused && SongPlayer.useNoteblocksWhilePlaying) {
			for (int i : packet.getPassengerIds()) {
				if (i == SongPlayer.MC.player.getId()) {
					SongPlayer.addChatMessage("§6Your song has been paused because you got moved away from your stage.\nGo back to your stage and type §3" + SongPlayer.prefix + "resume§6 to resume playing.");
					Util.lagBackCounter = 0;
					Util.pauseSongIfNeeded();
				}
			}
		}
	}

	@Inject(at = @At("HEAD"), method = "onEntityVelocityUpdate(Lnet/minecraft/network/packet/s2c/play/EntityVelocityUpdateS2CPacket;)V", cancellable = true)
	public void onPlayerVelocityUpdate(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
		if (SongHandler.getInstance().currentSong != null && !SongHandler.getInstance().paused && SongPlayer.useNoteblocksWhilePlaying) {
			ci.cancel();
		}
	}

	@Inject(at = @At("TAIL"), method = "onPlaySound(Lnet/minecraft/network/packet/s2c/play/PlaySoundS2CPacket;)V")
	public void onPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
		if (!SongPlayer.recording) {
			return;
		}
		if (packet.getSound().value().getId().getPath().startsWith("block.note_block.")) {
			if (!SongPlayer.recordingActive) {
				SongPlayer.recordingActive = true;
			}
			Util.addRecordedNote(packet.getSound().value().getId().getPath(), packet.getPitch());
		}
	}

	@Inject(at = @At("RETURN"), method = "onPlayerAbilities")
	public void onPlayerAbilities(PlayerAbilitiesS2CPacket packet, CallbackInfo ci) {
		if (SongHandler.getInstance().currentSong != null || !SongHandler.getInstance().songQueue.isEmpty() || SongHandler.getInstance().building || !Util.playlistSongs.isEmpty()) {
			if (SongPlayer.switchGamemode && !SongHandler.getInstance().paused) {
				SongPlayer.MC.player.getAbilities().flying = SongHandler.getInstance().isflying;
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "onChatMessage(Lnet/minecraft/network/packet/s2c/play/ChatMessageS2CPacket;)V")
	public void onPlayerChat(ChatMessageS2CPacket packet, CallbackInfo ci) {
		if (!SongPlayer.kiosk) {
			return;
		}
		String content = packet.body().content();
		UUID uuid = packet.sender();
		String player = String.valueOf(SongPlayer.MC.getNetworkHandler().getPlayerListEntry(uuid).getProfile().getName());
		if (packet.sender().toString().equals(SongPlayer.MC.player.getUuidAsString())) {
			return;
		}
		String[] split = content.split(" ");
		if (split[0].endsWith("prefix")) {
			SongPlayer.MC.getNetworkHandler().sendCommand("tellraw " + player + " \"SongPlayer prefix: " + SongPlayer.prefix + "\"");
			return;
		}
		if (content.startsWith(SongPlayer.prefix)) {
			KioskCommandProcessor.runCommand(SongPlayer.MC.getNetworkHandler().getPlayerListEntry(uuid).getProfile(), content);
		}
	}
}
