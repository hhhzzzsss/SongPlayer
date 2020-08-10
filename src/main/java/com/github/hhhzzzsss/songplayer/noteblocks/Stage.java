package com.github.hhhzzzsss.songplayer.noteblocks;

import java.util.HashSet;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Stage {
	private final ClientPlayerEntity player = SongPlayer.MC.player;
	
	public BlockPos position;
	public BlockPos[] tunedNoteblocks = new BlockPos[400];
	public HashSet<BlockPos> noteblockPositions = new HashSet<>();
	public boolean rebuild = false;
	
	public Stage() {
		position = player.getBlockPos();
	}
	
	public void movePlayerToStagePosition() {
		player.refreshPositionAndAngles(position.getX() + 0.5, position.getY() + 0.0, position.getZ() + 0.5, player.yaw, player.pitch);
		player.setVelocity(Vec3d.ZERO);
	}
}
