package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;

public class FakePlayerEntity extends OtherClientPlayerEntity {
	ClientWorld world = SongPlayer.MC.world;

	public FakePlayerEntity() {
		super(SongPlayer.MC.world, SongPlayer.MC.player.getGameProfile());
		copyStagePosAndPlayerLook();
		getInventory().clone(SongPlayer.MC.player.getInventory());
		Byte playerModel = SongPlayer.MC.player.getDataTracker().get(PlayerEntity.PLAYER_MODEL_PARTS);
		getDataTracker().set(PlayerEntity.PLAYER_MODEL_PARTS, playerModel);
		headYaw = SongPlayer.MC.player.headYaw;
		bodyYaw = SongPlayer.MC.player.bodyYaw;
		capeX = getX();
		capeY = getY();
		capeZ = getZ();
		world.addEntity(this);
		SongPlayer.fakePlayer = this;
	}

	public void updateFakePlayer() {
		if (SongPlayer.fakePlayer != null && SongPlayer.showFakePlayer) {
			ClientPlayerEntity player = SongPlayer.MC.player;
			this.getInventory().clone(player.getInventory());
			this.getAttributes().setFrom(player.getAttributes());
			this.getInventory().selectedSlot = player.getInventory().selectedSlot;
			this.setSneaking(player.isSneaking());
			this.setCurrentHand(SongPlayer.MC.player.getActiveHand());
			if (this.isSneaking()) {
				this.setPose(EntityPose.CROUCHING);
			} else {
				this.setPose(EntityPose.STANDING);
			}
		}
	}

	public void copyStagePosAndPlayerLook() {
		Stage stage = SongHandler.getInstance().stage;
		if (stage == null) {
			copyPositionAndRotation(SongPlayer.MC.player);
			return;
		}
		if (SongPlayer.rotate) {
			refreshPositionAndAngles(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, Util.yaw, Util.pitch);
			headYaw = Util.yaw;
			bodyYaw = Util.yaw;
		} else {
			refreshPositionAndAngles(stage.position.getX() + 0.5, stage.position.getY(), stage.position.getZ() + 0.5, SongPlayer.MC.player.getYaw(), SongPlayer.MC.player.getPitch());
			headYaw = SongPlayer.MC.player.headYaw;
			bodyYaw = SongPlayer.MC.player.bodyYaw;
		}
	}
}
