package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.mixin.ClientPlayNetworkHandlerAccessor;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public class FakePlayerEntity extends OtherClientPlayerEntity {
	public static final UUID FAKE_PLAYER_UUID = UUID.randomUUID();

	ClientPlayerEntity player = SongPlayer.MC.player;
	ClientWorld world = SongPlayer.MC.world;
	
	public FakePlayerEntity() {
		super(SongPlayer.MC.world, getProfile());
		
		copyStagePosAndPlayerLook();
		
		getInventory().clone(player.getInventory());
		
		Byte playerModel = player.getDataTracker().get(PlayerEntity.PLAYER_MODEL_PARTS);
		getDataTracker().set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
		
		headYaw = player.headYaw;
		bodyYaw = player.bodyYaw;

		if (player.isSneaking()) {
			setSneaking(true);
			setPose(EntityPose.CROUCHING);
		}

		capeX = getX();
		capeY = getY();
		capeZ = getZ();
		
		world.addEntity(getId(), this);
	}
	
	public void resetPlayerPosition() {
		player.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());
	}
	
	public void copyStagePosAndPlayerLook() {
		Stage stage = SongHandler.getInstance().stage;
		if (stage != null) {
			refreshPositionAndAngles(stage.position.getX()+0.5, stage.position.getY(), stage.position.getZ()+0.5, player.getYaw(), player.getPitch());
			headYaw = player.headYaw;
		}
		else {
			copyPositionAndRotation(player);
		}
	}

	private static GameProfile getProfile() {
		GameProfile profile = new GameProfile(FAKE_PLAYER_UUID, SongPlayer.MC.player.getGameProfile().getName());
		profile.getProperties().putAll(SongPlayer.MC.player.getGameProfile().getProperties());
		PlayerListEntry playerListEntry = new PlayerListEntry(SongPlayer.MC.player.getGameProfile(), false);
		((ClientPlayNetworkHandlerAccessor)SongPlayer.MC.getNetworkHandler()).getPlayerListEntries().put(FAKE_PLAYER_UUID, playerListEntry);
		return profile;
	}
}
