package com.github.hhhzzzsss.songplayer.playing;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.song.Instrument;
import com.github.hhhzzzsss.songplayer.song.Song;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.stream.Collectors;

public class Stage {
	private final MinecraftClient MC = SongPlayer.MC;

	public enum StageType {
		DEFAULT,
		WIDE,
		SPHERICAL,
	}

	public String worldName;
	public String serverIdentifier;
	public BlockPos position;
	public HashMap<Integer, BlockPos> noteblockPositions = new HashMap<>();

	// Not used in survival-only mode
	public LinkedList<BlockPos> requiredBreaks = new LinkedList<>();
	public TreeSet<Integer> missingNotes = new TreeSet<>();
	public int totalMissingNotes = 0;

	// Only used in survival-only mode
	public LinkedList<BlockPos> requiredClicks = new LinkedList<>();

	public Stage() {
		position = MC.player.getBlockPos();

		// Information tracked for checking cleanup conditions
		worldName = Util.getWorldName();
		serverIdentifier = Util.getServerIdentifier();
		System.out.println("Server identifier: " + serverIdentifier);
	}

	public void movePlayerToStagePosition() {
		MC.player.getAbilities().flying = true;
		MC.player.refreshPositionAndAngles(position.getX() + 0.5, position.getY() + 0.0, position.getZ() + 0.5, MC.player.getYaw(), MC.player.getPitch());
		MC.player.setVelocity(Vec3d.ZERO);
		sendMovementPacketToStagePosition();
	}

	public void sendMovementPacketToStagePosition() {
		if (SongPlayer.fakePlayer != null) {
			SongPlayer.MC.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
					position.getX() + 0.5, position.getY(), position.getZ() + 0.5,
					SongPlayer.fakePlayer.getYaw(), SongPlayer.fakePlayer.getPitch(),
					true, false));
		}
		else {
			SongPlayer.MC.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
					position.getX() + 0.5, position.getY(), position.getZ() + 0.5,
					SongPlayer.MC.player.getYaw(), SongPlayer.MC.player.getPitch(),
					true, false));
		}
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
		HashSet<BlockPos> breakLocations = new HashSet<>();
		switch (Config.getConfig().stageType) {
			case DEFAULT -> loadDefaultBlocks(noteblockLocations, breakLocations);
			case WIDE -> loadWideBlocks(noteblockLocations, breakLocations);
			case SPHERICAL -> loadSphericalBlocks(noteblockLocations, breakLocations);
		}

		// Sorting noteblock and break locations
		noteblockLocations.sort((a, b) -> {
			// First sort by y
			int a_dy = a.getY() - position.getY();
			int b_dy = b.getY() - position.getY();
			if (a_dy == -1) a_dy = 0; // same layer
			if (b_dy == -1) b_dy = 0; // same layer
			if (Math.abs(a_dy) < Math.abs(b_dy)) {
				return -1;
			} else if (Math.abs(a_dy) > Math.abs(b_dy)) {
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

		// Remove already-existing notes from missingNotes, adding their positions to noteblockPositions, and create a list of unused noteblock locations
		ArrayList<BlockPos> unusedNoteblockLocations = new ArrayList<>();
		for (BlockPos nbPos : noteblockLocations) {
			BlockState bs = SongPlayer.MC.world.getBlockState(nbPos);
			int blockId = Block.getRawIdFromState(bs);
			if (blockId >= SongPlayer.NOTEBLOCK_BASE_ID && blockId < SongPlayer.NOTEBLOCK_BASE_ID+800) {
				int noteId = (blockId-SongPlayer.NOTEBLOCK_BASE_ID)/2;
				if (missingNotes.contains(noteId)) {
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

		// Cull noteblocks that won't fit in stage
		if (missingNotes.size() > unusedNoteblockLocations.size()) {
			while (missingNotes.size() > unusedNoteblockLocations.size()) {
				missingNotes.pollLast();
			}
		}

		// Populate missing noteblocks into the unused noteblock locations
		int idx = 0;
		for (int noteId : missingNotes) {
			BlockPos bp = unusedNoteblockLocations.get(idx++);
			noteblockPositions.put(noteId, bp);
		}

		for (BlockPos bp : noteblockPositions.values()) { // Optional break locations
			breakLocations.add(bp.up());
		}

		requiredBreaks = breakLocations
				.stream()
				.filter((bp) -> {
					BlockState bs = SongPlayer.MC.world.getBlockState(bp);
					return !bs.isAir() && !bs.isLiquid();
				})
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

		if (requiredBreaks.stream().allMatch(bp -> !withinBreakingDist(bp.getX()-position.getX(), bp.getY()-position.getY(), bp.getZ()-position.getZ()))) {
			requiredBreaks.clear();
		}

		// Set total missing notes
		totalMissingNotes = missingNotes.size();
	}

	public void checkSurvivalBuildStatus(Song song) throws NotEnoughInstrumentsException {
		noteblockPositions.clear();

		Map<BlockPos, Integer>[] instrumentMap = loadSurvivalBlocks();

		int[] requiredInstruments = new int[16];
		boolean hasMissing = false;
		for (int instrumentId = 0; instrumentId < 16; instrumentId++) {
			for (int pitch = 0; pitch < 25; pitch++) {
				int noteId = instrumentId*25 + pitch;
				if (song.requiredNotes[noteId]) {
					requiredInstruments[instrumentId]++;
				}
			}
			if (requiredInstruments[instrumentId] > instrumentMap[instrumentId].size()) {
				hasMissing = true;
			}
		}

		if (hasMissing) {
			int[] foundInstruments = new int[16];
			for (int i = 0; i < 16; i++) {
				foundInstruments[i] = instrumentMap[i].size();
			}
			throw new NotEnoughInstrumentsException(requiredInstruments, foundInstruments);
		}

		for (int noteid=0; noteid<400; noteid++) {
			if (song.requiredNotes[noteid]) {
				int instrumentId = noteid / 25;
				int targetPitch = noteid % 25;
				Map.Entry<BlockPos, Integer> closest = instrumentMap[instrumentId].entrySet()
						.stream()
						.min((a, b) -> {
							int adist = (targetPitch - a.getValue() + 25) % 25;
							int bdist = (targetPitch - b.getValue() + 25) % 25;
							return Integer.compare(adist, bdist);
						})
						.get();
				BlockPos bp = closest.getKey();
				int closestPitch = closest.getValue();
				instrumentMap[instrumentId].remove(bp);
				noteblockPositions.put(noteid, bp);
				int repetitions = (targetPitch - closestPitch + 25) % 25;
				for (int i = 0; i < repetitions; i++) {
					requiredClicks.add(bp);
				}
			}
		}
	}

	public class NotEnoughInstrumentsException extends Exception {
		public int[] requiredInstruments;
		public int[] foundInstruments;
		public NotEnoughInstrumentsException(int[] requiredInstruments, int[] foundInstruments) {
			this.requiredInstruments = requiredInstruments;
			this.foundInstruments = foundInstruments;
		}
		public void giveInstrumentSummary() {
			SongPlayer.addChatMessage("§c------------------------------");
			SongPlayer.addChatMessage("§cMissing instruments required to play song:");
			for (int instrumentId = 0; instrumentId < 16; instrumentId++) {
				if (requiredInstruments[instrumentId] > 0) {
					Instrument instrument = Instrument.getInstrumentFromId(instrumentId);
					SongPlayer.addChatMessage(String.format(
							"    §3%s (%s): §%s%d/%d",
							instrument.name(), instrument.material,
							foundInstruments[instrumentId] < requiredInstruments[instrumentId] ? "c" : "a",
							foundInstruments[instrumentId], requiredInstruments[instrumentId]
					));
				}
			}
			SongPlayer.addChatMessage("§c------------------------------");
		}
	}

	void loadDefaultBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				if (Math.abs(dx) == 4 && Math.abs(dz) == 4)  {
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
					breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
				}
				else {
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 1, position.getZ() + dz));
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
					breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
					breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
				}
			}
		}
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				if (withinBreakingDist(dx, -3, dz)) {
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 3, position.getZ() + dz));
				}
				if (withinBreakingDist(dx, 4, dz)) {
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 4, position.getZ() + dz));
				}
			}
		}
	}

	void loadWideBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				if (withinBreakingDist(dx, 2, dz)) {
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
					if (withinBreakingDist(dx, -1, dz)) {
						noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 1, position.getZ() + dz));
						breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
						breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
					}
					else if (withinBreakingDist(dx, 0, dz)) {
						noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
						breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
					}
				}
				if (withinBreakingDist(dx, -3, dz)) {
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 3, position.getZ() + dz));
				}
				if (withinBreakingDist(dx, 4, dz)) {
					noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 4, position.getZ() + dz));
				}
			}
		}
	}

	// This code was taken from Sk8kman fork of SongPlayer
	// Thanks Sk8kman and Lizard16 for this spherical stage design!
	void loadSphericalBlocks(Collection<BlockPos> noteblockLocations, Collection<BlockPos> breakLocations) {
		int[] yLayers = {-4, -2, -1, 0, 1, 2, 3, 4, 5, 6};

		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				for (int dy : yLayers) {
					int adx = Math.abs(dx);
					int adz = Math.abs(dz);
					switch(dy) {
						case -4: {
							if (adx < 3 && adz < 3) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							if ((adx == 3 ^ adz == 3) && (adx == 0 ^ adz == 0)) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							break;
						}
						case -2: { // also takes care of -3
							if (adz == 0 && adx == 0) { // prevents placing int the center
								break;
							}
							if (adz * adx > 9) { // prevents building out too far
								break;
							}
							if (adz + adx == 5 && adx != 0 && adz != 0) {
								// add noteblocks above and below here
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 1, position.getZ() + dz));
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy - 1, position.getZ() + dz));
								break;
							}
							if (adz * adx == 3) {
								// add noteblocks above and below here
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 1, position.getZ() + dz));
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy - 1, position.getZ() + dz));
								break;
							}
							if (adx < 3 && adz < 3 && adx + adz > 0) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 2, position.getZ() + dz));
								break;
							}
							if (adz == 0 ^ adx == 0) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 2, position.getZ() + dz));
								break;
							}
							if (adz * adx == 10) { // expecting one to be 2, and one to be 5.
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 2, position.getZ() + dz));
								break;
							}
							if (adz + adx == 6) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								if (adx == 5 ^ adz == 5) {
									breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 2, position.getZ() + dz));
								}
								break;
							}
							break;
						}
						case -1: {
							if (adx + adz == 7 || adx + adz == 0) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							break;
						}
						case 0: {
							int check = adx + adz;
							if ((check == 8 || check == 6) && adx * adz > 5) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							break;
						}
						case 1: {
							int addl1 = adx + adz;
							if (addl1 == 7 || addl1 == 3 || addl1 == 2) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							if (adx == 5 ^ adz == 5 && addl1 < 7) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							if (addl1 == 4 && adx * adz != 0) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							if (adx + adz < 7) {
								breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							break;
						}
						case 2: {
							int addl2 = adx + adz;
							if (adx == 5 || adz == 5) {
								break;
							}
							if (addl2 == 8 || addl2 == 6 || addl2 == 5 || addl2 == 1) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							if ((addl2 == 4) && (adx == 0 ^ adz == 0)) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							if (addl2 == 0) {
								breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							break;
						}
						case 3: {
							if (adx * adz == 12 || adx + adz == 0) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							if ((adx == 5 ^ adz == 5) && (adx < 2 ^ adz < 2)) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							if (adx > 3 || adz > 3) { // don't allow any more checks past 3 blocks out
								break;
							}
							if (adx + adz > 1 && adx + adz < 5) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							break;
						}
						case 4: {
							if (adx == 5 || adz == 5) {
								break;
							}
							if (adx + adz == 4 && adx * adz == 0) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							int addl4 = adx + adz;
							if (addl4 == 1 || addl4 == 5 || addl4 == 6) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							break;
						}
						case 5: {
							if (adx > 3 || adz > 3) {
								break;
							}
							int addl5 = adx + adz;
							if (addl5 > 1 && addl5 < 5) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							break;
						}
						case 6: {
							if (adx + adz < 2) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy, position.getZ() + dz));
								break;
							}
							break;
						}
					}
					//all breaks lead here
				}
			}
		}
	}

	// Find available noteblocks in range for the player to use in survival only mode
	Map<BlockPos, Integer>[] loadSurvivalBlocks() {
		@SuppressWarnings("unchecked")
		Map<BlockPos, Integer>[] instrumentMap = new Map[16];
		for (int i = 0; i < 16; i++) {
			instrumentMap[i] = new TreeMap<>();
		}
		for (int dx = -5; dx <= 5; dx++) {
			for (int dz = -5; dz <= 5; dz++) {
				for (int dy : new int[]{-1, 0, 1, 2, -2, 3, -3, 4, -4, 5, 6}) {
					BlockPos bp = position.add(dx, dy, dz);
					BlockState bs = SongPlayer.MC.world.getBlockState(bp);
					BlockState aboveBs = SongPlayer.MC.world.getBlockState(bp.up());
					int blockId = Block.getRawIdFromState(bs);
					if (blockId >= SongPlayer.NOTEBLOCK_BASE_ID && blockId < SongPlayer.NOTEBLOCK_BASE_ID + 800 && aboveBs.isAir()) {
						int noteId = (blockId - SongPlayer.NOTEBLOCK_BASE_ID) / 2;
						int instrument = noteId / 25;
						int pitch = noteId % 25;
						instrumentMap[instrument].put(bp, pitch);
					}
				}
			}
		}
		return instrumentMap;
	}

	// This doesn't check for whether the block above the noteblock position is also reachable
	// Usually there is sky above you though so hopefully this doesn't cause a problem most of the time
	boolean withinBreakingDist(int dx, int dy, int dz) {
		double dy1 = dy + 0.5 - 1.62; // Standing eye height
		double dy2 = dy + 0.5 - 1.27; // Crouching eye height
		return dx*dx + dy1*dy1 + dz*dz < 5.99999*5.99999 && dx*dx + dy2*dy2 + dz*dz < 5.99999*5.99999;
	}

	public boolean nothingToBuild() {
		if (!Config.getConfig().survivalOnly) {
			return requiredBreaks.isEmpty() && missingNotes.isEmpty();
		} else {
			return requiredClicks.isEmpty();
		}
	}

	public boolean hasBreakingModification() {
		for (Map.Entry<Integer, BlockPos> entry : noteblockPositions.entrySet()) {
			BlockState bs = SongPlayer.MC.world.getBlockState(entry.getValue());
			int blockId = Block.getRawIdFromState(bs);
			int actualNoteId = (blockId-SongPlayer.NOTEBLOCK_BASE_ID)/2;
			if (actualNoteId < 0 || actualNoteId >= 400) {
				return true;
			}
			int actualInstrument = actualNoteId / 25;
			int actualPitch = actualNoteId % 25;
			int targetInstrument = entry.getKey() / 25;
			int targetPitch = entry.getKey() % 25;
			if (targetPitch != actualPitch) {
				return true;
			}
			if (targetInstrument != actualInstrument) {
				return true;
			}

			BlockState aboveBs = SongPlayer.MC.world.getBlockState(entry.getValue().up());
			if (!aboveBs.isAir() && !aboveBs.isLiquid()) {
				return true;
			}
		}
		return false;
	}

	public Vec3d getOriginBottomCenter() {
		return Vec3d.ofBottomCenter(position);
	}
}
