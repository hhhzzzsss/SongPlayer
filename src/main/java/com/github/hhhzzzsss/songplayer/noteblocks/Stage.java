package com.github.hhhzzzsss.songplayer.noteblocks;

import java.util.*;
import java.util.stream.Collectors;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import com.github.hhhzzzsss.songplayer.song.Song;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Stage {
	private final ClientPlayerEntity player = SongPlayer.MC.player;
	
	public BlockPos position;
//	public BlockPos[] tunedNoteblocks = new BlockPos[400];
	public HashMap<Integer, BlockPos> noteblockPositions = new HashMap<>();
	public boolean rebuild = false;

	public LinkedList<BlockPos> requiredBreaks = new LinkedList<>();
	public TreeSet<Integer> missingNotes = new TreeSet<>();
	
	public Stage() {
		position = player.getBlockPos();
	}
	
	public void movePlayerToStagePosition() {
		player.getAbilities().allowFlying = true;
		player.getAbilities().flying = true;
		player.refreshPositionAndAngles(position.getX() + 0.5, position.getY() + 0.0, position.getZ() + 0.5, player.getYaw(), player.getPitch());
		player.setVelocity(Vec3d.ZERO);
	}

	public void checkBuildStatus(Song song) {
		noteblockPositions.clear();
		missingNotes.clear();

		// Add all required notes to missingNotes
		for (int i=0; i<400; i++) {
			if (song.requiredNotes[i]) {
				missingNotes.add(i);
			}
		}

		ArrayList<BlockPos> noteblockLocations = new ArrayList<>();
		ArrayList<BlockPos> breakLocations = new ArrayList<>();
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				if (Math.abs(dx) == 4 && Math.abs(dz) == 4)  {
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
					breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
				} else {
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 1, position.getZ() + dz));
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
					breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
					breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
					breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 3, position.getZ() + dz));
				}
			}
		}

		// Sorting noteblock and break locations
		noteblockLocations.sort((a, b) -> {
			// First sort by y
			if (a.getY() < b.getY()) {
				return -1;
			} else if (a.getY() > b.getY()) {
				return 1;
			}
			// Then sort by horizontal distance
			int a_dx = a.getX() - position.getX();
			int a_dz = a.getZ() - position.getZ();
			int b_dx = b.getX() - position.getX();
			int b_dz = b.getZ() - position.getZ();
			int a_dist = a_dx*a_dx + a_dz*a_dz;
			int b_dist = b_dx*b_dx + b_dz*b_dz;
			if (a_dist < b_dist) {
				return -1;
			} else if (a_dist > b_dist) {
				return 1;
			}
			// Finally sort by angle
			double a_angle = Math.atan2(a_dz, a_dx);
			double b_angle = Math.atan2(b_dz, b_dx);
			if (a_angle < b_angle) {
				return -1;
			} else if (a_angle > b_angle) {
				return 1;
			} else {
				return 0;
			}
		});
		requiredBreaks = breakLocations
				.stream()
				.filter((bp) -> Block.getRawIdFromState(SongPlayer.MC.world.getBlockState(bp)) != 0)
				.sorted((a, b) -> {
					// First sort by y
					if (a.getY() < b.getY()) {
						return -1;
					} else if (a.getY() > b.getY()) {
						return 1;
					}
					// Then sort by horizontal distance
					int a_dx = a.getX() - position.getX();
					int a_dz = a.getZ() - position.getZ();
					int b_dx = b.getX() - position.getX();
					int b_dz = b.getZ() - position.getZ();
					int a_dist = a_dx*a_dx + a_dz*a_dz;
					int b_dist = b_dx*b_dx + b_dz*b_dz;
					if (a_dist < b_dist) {
						return -1;
					} else if (a_dist > b_dist) {
						return 1;
					}
					// Finally sort by angle
					double a_angle = Math.atan2(a_dz, a_dx);
					double b_angle = Math.atan2(b_dz, b_dx);
					if (a_angle < b_angle) {
						return -1;
					} else if (a_angle > b_angle) {
						return 1;
					} else {
						return 0;
					}
				})
				.collect(Collectors.toCollection(LinkedList::new));

		// Remove already-existing notes from missingNotes, adding their positions to noteblockPositions, and create a list of unused noteblock locations
		ArrayList<BlockPos> unusedNoteblockLocations = new ArrayList<>();
		for (BlockPos nbPos : noteblockLocations) {
			BlockState bs = SongPlayer.MC.world.getBlockState(nbPos);
			int blockId = Block.getRawIdFromState(bs);
			if (blockId >= SongPlayer.NOTEBLOCK_BASE_ID && blockId < SongPlayer.NOTEBLOCK_BASE_ID+800) {
				int noteId = (blockId-SongPlayer.NOTEBLOCK_BASE_ID)/2;
				if (missingNotes.contains(noteId)) {
//					stage.tunedNoteblocks[noteId] = pos;
					missingNotes.remove(noteId);
					noteblockPositions.put(noteId, nbPos);
				}
				else {
					unusedNoteblockLocations.add(nbPos);
				}
			}
			else {
				unusedNoteblockLocations.add(nbPos);
			}
		}

		// Populate missing noteblocks into the unused noteblock locations
		int idx = 0;
		for (int noteId : missingNotes) {
			if (idx >= unusedNoteblockLocations.size()) {
				System.out.println("Too many noteblocks!");
				break;
			}
			noteblockPositions.put(noteId, unusedNoteblockLocations.get(idx++));
		}
	}

	public boolean nothingToBuild() {
		return requiredBreaks.isEmpty() && missingNotes.isEmpty();
	}
}
