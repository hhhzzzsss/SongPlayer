package com.github.hhhzzzsss.songplayer;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;

public class FakePlayerEntity extends OtherClientPlayerEntity {
	ClientPlayerEntity player = SongPlayer.MC.player;
	ClientWorld world = SongPlayer.MC.world;
	
	public FakePlayerEntity() {
		super(SongPlayer.MC.world, SongPlayer.MC.player.getGameProfile());
		
		copyStagePosAndPlayerLook();
		
		getInventory().clone(player.getInventory());
		
		Byte playerModel = player.getDataTracker().get(PlayerEntity.PLAYER_MODEL_PARTS);
		getDataTracker().set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
		
		headYaw = player.headYaw;
		bodyYaw = player.bodyYaw;

		capeX = getX();
		capeY = getY();
		capeZ = getZ();
		
		world.addEntity(getId(), this);
	}
	
	public void resetPlayerPosition() {
		player.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());
	}
	
	public void copyStagePosAndPlayerLook() {
		if (SongPlayer.stage != null) {
			refreshPositionAndAngles(SongPlayer.stage.position.getX()+0.5, SongPlayer.stage.position.getY(), SongPlayer.stage.position.getZ()+0.5, player.getYaw(), player.getPitch());
			headYaw = player.headYaw;
			bodyYaw = player.bodyYaw;
		}
		else {
			copyPositionAndRotation(player);
		}
	}
}
