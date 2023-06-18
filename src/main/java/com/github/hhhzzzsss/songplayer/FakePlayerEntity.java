package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public class FakePlayerEntity extends OtherClientPlayerEntity {
	ClientPlayerEntity player = SongPlayer.MC.player;
	ClientWorld world = SongPlayer.MC.world;
	
	public FakePlayerEntity() {
		super(SongPlayer.MC.world, new GameProfile(UUID.randomUUID(), SongPlayer.MC.player.getGameProfile().getName()));
		
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
}
