package com.github.hhhzzzsss.songplayer.playing;

import java.util.*;
import java.util.stream.Collectors;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.config.ModProperties;
import com.github.hhhzzzsss.songplayer.song.Song;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class Stage {
	private final ClientPlayerEntity player = SongPlayer.MC.player;
	public static BlockPos position;
	public HashMap<Integer, BlockPos> noteblockPositions = new HashMap<>();
	public HashMap<BlockPos, Integer> outOfTuneBlocks = new HashMap<>();
	public LinkedList<BlockPos> requiredBreaks = new LinkedList<>();
	public TreeMap<Integer, Integer> missingNotes = new TreeMap<>();
	public HashMap<BlockPos, Integer> originalIdBlockPos = new HashMap<>();
	private HashMap<BlockPos, BlockState> originalState = new HashMap<>();
	public int totalMissingNotes = 0;
	public int totalOutOfTuneNotes = 0;
	// 									0				  1       2       3       4           5                 6                 7        8                 9                 10               11               12      13               14               15
	private int[][] substituteNotes = {{15, 10, 14, 13}, {3, 2}, {3, 1}, {2, 1}, {12, 7, 0}, {11, 6, 8, 9, 0}, {8, 9, 5, 11, 0}, {12, 0}, {6, 9, 5, 11, 0}, {6, 8, 11, 5, 0}, {13, 14, 15, 0}, {5, 9, 6, 8, 0}, {7, 0}, {14, 10, 0, 15}, {13, 10, 15, 0}, {0, 10, 13, 14}};

	public Stage() {
		position = player.getBlockPos();
	}

	public void movePlayerToStagePosition() {
		if (SongHandler.getInstance().stage == null || !SongPlayer.useNoteblocksWhilePlaying || SongHandler.getInstance().paused) {
			return;
		}
		SongPlayer.MC.player.refreshPositionAndAngles(position.getX() + 0.5, position.getY() + 0.0, position.getZ() + 0.5, SongPlayer.MC.player.getYaw(), SongPlayer.MC.player.getPitch());
		SongPlayer.MC.player.setVelocity(Vec3d.ZERO);
		PlayerMoveC2SPacket moveToStagePacket = new PlayerMoveC2SPacket.PositionAndOnGround(position.getX() + 0.5, position.getY() + 0.0, position.getZ() + 0.5, true);
		SongPlayer.MC.getNetworkHandler().sendPacket(moveToStagePacket);
	}

	public void movePlayerToStagePosition(Boolean force, Boolean enableFlight, Boolean onlyPacket) {
		if (!force) { //check if moving the player to the stage is needed unless strictly told otherwise by the force argument
			if (SongPlayer.useCommandsForPlaying || !SongPlayer.useNoteblocksWhilePlaying || SongHandler.getInstance().paused) {
				return;
			}
			if (SongHandler.getInstance().stage == null) {
				return;
			}
			if (!Util.currentPlaylist.isEmpty() && !SongHandler.getInstance().songQueue.isEmpty() && SongHandler.getInstance().currentSong == null) {
				return;
			}
		}
		//send packet to ensure player is forced at the center of the stage. will fail if there is a boat or block in the way.
		if (enableFlight) {
			Util.enableFlightIfNeeded();
		}
		if (!onlyPacket) {
			SongPlayer.MC.player.refreshPositionAndAngles(position.getX() + 0.5, position.getY() + 0.0, position.getZ() + 0.5, SongPlayer.MC.player.getYaw(), SongPlayer.MC.player.getPitch());
			SongPlayer.MC.player.setVelocity(Vec3d.ZERO);
		}
		PlayerMoveC2SPacket moveToStagePacket = new PlayerMoveC2SPacket.PositionAndOnGround(position.getX() + 0.5, position.getY() + 0.0, position.getZ() + 0.5, true);
		SongPlayer.MC.getNetworkHandler().sendPacket(moveToStagePacket);
	}

	public void checkBuildStatus(Song song) {
		if (!SongPlayer.useNoteblocksWhilePlaying) {
			return;
		}

		//clear lists for next scan
		noteblockPositions.clear();
		missingNotes.clear();
		outOfTuneBlocks.clear();
		originalIdBlockPos.clear();

		// Add all required notes to missingNotes
		for (int i=0; i<400; i++) {
			if (song.requiredNotes[i]) {
				missingNotes.put(i, i / 25);
			}
		}

		ArrayList<BlockPos> noteblockLocations = new ArrayList<>();
		ArrayList<BlockPos> breakLocations = new ArrayList<>();
		if (SongPlayer.switchGamemode) {
			switch (SongPlayer.stageType) {
				case "compact": {
					int[] yLayers = {-4, -2, -1, 0, 1, 2, 3, 4, 5, 6};
					//UGH
					for (int dx = -5; dx <= 5; dx++) {
						for (int dz = -5; dz <= 5; dz++) {
							for (int dy : yLayers) {
								int adx = Math.abs(dx);
								int adz = Math.abs(dz);
								switch (dy) {
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
									case -2: { //also takes care of -3
										if (adz == 0 && adx == 0) { //prevents placing int the center
											break;
										}
										if (adz * adx > 9) { //prevents building out too far
											break;
										}
										if (adz + adx == 5 && adx != 0 && adz != 0) {
											//add noteblocks above and below here
											noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy + 1, position.getZ() + dz));
											noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + dy - 1, position.getZ() + dz));
											break;
										}
										if (adz * adx == 3) {
											//add noteblocks above and below here
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
										if (adz * adx == 10) { //expecting one to be 2, and one to be 5.
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
										if (adx > 3 || adz > 3) { //don't allow any more checks passed 3 blocks out
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
					break;
				}
				case "default": {
					for (int dx = -5; dx <= 5; dx++) {
						for (int dz = -5; dz <= 5; dz++) {
							if (((Math.abs(dx) == 4 && Math.abs(dz) == 4) || (Math.abs(dx) == 5 && Math.abs(dz) == 3) || (Math.abs(dx) == 3) && Math.abs(dz) == 5)) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
							} else if (Math.abs(dx) >= 4 && Math.abs(dz) >= 4) { //don't add it
							} else {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 1, position.getZ() + dz));
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
								breakLocations.add(new BlockPos(position.getX() + dx, position.getY() + 1, position.getZ() + dz));
							}
						}
					}
					for (int dx = -4; dx <= 4; dx++) {
						for (int dz = -4; dz <= 4; dz++) {
							if (withinBreakingDist(dx, -3, dz)) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 3, position.getZ() + dz));
							}
							if (withinBreakingDist(dx, 4, dz) && Math.abs(dx) + Math.abs(dz) != 7) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 4, position.getZ() + dz));
							}
						}
					}
					for (int dx = -1; dx <= 1; dx++) {
						for (int dz = -1; dz <= 1; dz++) {
							if (Math.abs(dz) + Math.abs(dx) < 2) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 6, position.getZ() + dz));
							}
						}
					}
					break;
				}
				case "legacy": {
					for (int dx = -4; dx <= 4; dx++) {
						for (int dz = -4; dz <= 4; dz++) {
							if (Math.abs(dx) == 4 && Math.abs(dz) == 4) {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 0, position.getZ() + dz));
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
							} else {
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() - 1, position.getZ() + dz));
								noteblockLocations.add(new BlockPos(position.getX() + dx, position.getY() + 2, position.getZ() + dz));
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
					break;
				}
				default: {
					ModProperties.getInstance().updateValue("stageType", "default");
					Util.updateValuesToConfig();
				}
			}
		} else {
			//survival only mode - add usable noteblocks around the player for the stage
			//hhhzzzsss please add this mode to ur SongPlayer mod, and maybe make this code less spaghetti too ty :)
			double[] maxBlockDistThreshold = {25.0, 27.0, 30.0, 30.0, 34.0, 35.0, 38.0, 41.0, 42.0, 45.0, 46.0, 50.0};
			for (int y = -4; y < 8; y++) {
				for (int x = -5; x < 6; x++) {
					for (int z = -5; z < 6; z++) {
						BlockPos targetBlock = new BlockPos(position.getX() + x, position.getY() + y, position.getZ() + z);
						if (new BlockPos(position.getX(), position.getY(), position.getZ()).getSquaredDistance(targetBlock) <= maxBlockDistThreshold[(y+4)]) { //check if location is close enough to stage
							BlockState noteblock = SongPlayer.MC.world.getBlockState(targetBlock);
							int blockId = Block.getRawIdFromState(noteblock);
							if (blockId >= SongPlayer.NOTEBLOCK_BASE_ID && blockId < SongPlayer.NOTEBLOCK_BASE_ID+800 && SongPlayer.MC.world.getBlockState(targetBlock.up()).isAir()) { //make sure block is a noteblock and the block above it is air before adding
								noteblockLocations.add(targetBlock);
							}
						}
					}
				}
			}
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
			int a_dist = a_dx * a_dx + a_dz * a_dz;
			int b_dist = b_dx * b_dx + b_dz * b_dz;
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
		//remember what all the blocks are that we might have to cleanup later if enabled
		if (SongPlayer.switchGamemode && !Util.cleaningup) {
			for (BlockPos nbPos : noteblockLocations) {
				Util.cleanupstage.putIfAbsent(nbPos, SongPlayer.MC.world.getBlockState(nbPos));
				Util.cleanupstage.putIfAbsent(nbPos.add(0, 1, 0), SongPlayer.MC.world.getBlockState(nbPos.add(0, 1, 0)));
			}
			for (BlockPos nbPos : breakLocations) {
				Util.cleanupstage.putIfAbsent(nbPos, SongPlayer.MC.world.getBlockState(nbPos));
			}
		}

		for (BlockPos nbPos : noteblockLocations) {
			BlockState bs = SongPlayer.MC.world.getBlockState(nbPos);
			int blockId = Block.getRawIdFromState(bs);
			if (blockId % 2 == 0) { //fixes issue with powered=true blockstate
				blockId += 1;
			}
			if (blockId < SongPlayer.NOTEBLOCK_BASE_ID || blockId > SongPlayer.NOTEBLOCK_BASE_ID + 800) {
				unusedNoteblockLocations.add(nbPos);
				continue;
			}

			int noteId = (blockId - SongPlayer.NOTEBLOCK_BASE_ID) / 2;
			originalIdBlockPos.put(nbPos, noteId);
			if (SongPlayer.switchGamemode) {
				if (missingNotes.containsKey(noteId)) {
					missingNotes.remove(noteId);
					noteblockPositions.put(noteId, nbPos);
					//add breaklocations above all used noteblocks
					breakLocations.add(nbPos.up());
				} else {
					unusedNoteblockLocations.add(nbPos);
				}
			} else { //survival only mode
				if (missingNotes.containsKey(noteId)) { //add all exact noteblocks that won't need tuning and are the correct instrument.
					missingNotes.remove(noteId);
					noteblockPositions.put(noteId, nbPos);
				}
			}
		}

		if (!SongPlayer.switchGamemode) {
			for (int noteId : new ArrayList<>(missingNotes.keySet())) { //check for right instrument, wrong pitch
				if (missingNotes.isEmpty()) {
					break;
				}

				int instrumentId = noteId / 25; //the instrument of the note we're adding
				int pitch = noteId % 25; //the pitch that we need but don't have
				int pitchId = pitch; //
				for (int ignored = 0; ignored < 25; ignored++) {
					pitchId--;
					if (pitchId < 0) {
						pitchId = 24;
					}
					int testBlockId = ((25 * instrumentId) + pitchId);
					BlockPos nbPos = null;

					if (!originalIdBlockPos.containsValue(testBlockId)) {
						continue;
					}
					for (Map.Entry<BlockPos, Integer> e : originalIdBlockPos.entrySet()) {
						if (noteblockPositions.containsValue(e.getKey())) {
							continue;
						}
						if (e.getValue() == testBlockId) {
							nbPos = e.getKey();
							break;
						}
					}

					if (nbPos == null) {
						continue;
					}
					missingNotes.remove(noteId);
					noteblockPositions.put(noteId, nbPos);
					outOfTuneBlocks.put(nbPos, noteId);

					break;
				}
			}

			HashMap<Integer, BlockPos> assignLater = new HashMap<>();

			if (!SongPlayer.requireExactInstruments) { //check for alternative noteblocks
				for (int noteId : new ArrayList<>(missingNotes.keySet())) { //check for wrong instruments but correct pitch that can be used as substitutes
					if (missingNotes.isEmpty()) { //there's no missing noteblocks. break out of this loop.
						break;
					}

					int instrumentId = noteId / 25;
					int pitchId = noteId % 25;

					for (int tryInstrumentId : substituteNotes[instrumentId]) {
						//attempt to see if the substitute is there
						int tryNoteId = (tryInstrumentId * 25) + pitchId;

						BlockPos nbPos = null;
						if (noteblockPositions.containsKey(tryNoteId)) {
							nbPos = noteblockPositions.get(tryNoteId);
						} else if (originalIdBlockPos.containsValue(tryNoteId)) {
							for (Map.Entry<BlockPos, Integer> e : originalIdBlockPos.entrySet()) {
								if (noteblockPositions.containsValue(e.getKey())) {
									continue;
								}
								if (e.getValue() == tryNoteId) {
									nbPos = e.getKey();
									break;
								}
							}
						}

						if (nbPos == null) {
							continue;
						}
						//we found one, add it
						missingNotes.remove(noteId);
						assignLater.put(noteId, nbPos);
						//noteblockPositions.put(noteId, nbPos);
						break;
					}
				}

				noteblockPositions.putAll(assignLater);
				assignLater.clear();
				HashMap<BlockPos, Integer> tuneLater = new HashMap<>();

				for (int noteId : new ArrayList<>(missingNotes.keySet())) { //check for wrong instruments & wrong pitch that can be used as substitutes
					if (missingNotes.isEmpty()) {
						break;
					}
					int instrumentId = noteId / 25;
					int pitchId = noteId % 25;
					BlockPos nbPos = null;
					for (int tryInstrumentId : substituteNotes[instrumentId]) {
						if (!missingNotes.containsKey(noteId)) {
							break;
						}
						int pitch = pitchId;
						for (Map.Entry<Integer, BlockPos> e : assignLater.entrySet()) {
							//see if the original instrument + the assignLater's pitch match. if so, add it
							BlockState bs = SongPlayer.MC.world.getBlockState(e.getValue());
							int blockId = Block.getRawIdFromState(bs);
							int TempnoteId = (blockId - SongPlayer.NOTEBLOCK_BASE_ID) / 2;
							if ((tryInstrumentId * 25) + pitchId == ((TempnoteId / 25) + e.getKey() % 25)) {
								nbPos = e.getValue();
								break;
							}
						}
						if (nbPos != null) {
							missingNotes.remove(noteId);
							assignLater.put(noteId, nbPos);
							break;
						}
						for (int ignored = 0; ignored < 25; ignored++) {
							pitch--;
							if (pitch < 0) {
								pitch = 24;
							}
							int tryNoteId = (tryInstrumentId * 25) + pitch;

							if (originalIdBlockPos.containsValue(tryNoteId)) {
								for (Map.Entry<BlockPos, Integer> e : originalIdBlockPos.entrySet()) {
									if (noteblockPositions.containsValue(e.getKey()) || assignLater.containsValue(e.getKey())) {
										continue;
									}
									if (e.getValue() == tryNoteId) {
										nbPos = e.getKey();
										if (pitch != pitchId) {
											tuneLater.put(e.getKey(), noteId);
										}
										break;
									}
								}
							}
							if (nbPos == null) {
								continue;
							}
							missingNotes.remove(noteId);
							assignLater.put(noteId, nbPos);
							break;
						}
					}
					for (int tryInstrumentId : substituteNotes[instrumentId]) {
						if (!missingNotes.containsKey(noteId)) {
							break;
						}

					}
				}
				noteblockPositions.putAll(assignLater);
				outOfTuneBlocks.putAll(tuneLater);
			}
		}

		if (SongPlayer.switchGamemode) {
			// Cull noteblocks that won't fit in stage
			if (missingNotes.size() > unusedNoteblockLocations.size()) {
				while (missingNotes.size() > unusedNoteblockLocations.size()) {
					missingNotes.pollLastEntry();
				}
			}

			// Populate missing noteblocks into the unused noteblock locations
			int idx = 0;
			for (int noteId : missingNotes.keySet()) {
				BlockPos bp = unusedNoteblockLocations.get(idx++);
				noteblockPositions.put(noteId, bp);
				// add breaklocation above missing noteblocks
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
		}

		for (BlockPos bp : noteblockPositions.values()) {
			if (!originalState.containsKey(bp)) {
				originalState.put(bp, SongPlayer.MC.world.getBlockState(bp));
			}
		}
		for (BlockPos bp : breakLocations) {
			if (!originalState.containsKey(bp)) {
				originalState.put(bp, SongPlayer.MC.world.getBlockState(bp));
			}
		}

		// Set total missing notes
		totalOutOfTuneNotes = outOfTuneBlocks.size();
		totalMissingNotes = missingNotes.size();
	}

	boolean withinBreakingDist(int dx, int dy, int dz) {
		double dy1 = dy + 0.5 - 1.62; // Standing eye height
		double dy2 = dy + 0.5 - 1.27; // Crouching eye height
		return dx*dx + dy1*dy1 + dz*dz < 5.99*5.99 && dx*dx + dy2*dy2 + dz*dz < 5.99*5.99;
	}

	public boolean nothingToBuild() {
		return (requiredBreaks.isEmpty() && missingNotes.isEmpty() && outOfTuneBlocks.isEmpty());
	}

	//for survival: private static final int WRONG_INSTRUMENT_TOLERANCE = 3;
	//private static final int WRONG_INSTRUMENT_TOLERANCE = 0;
	public boolean hasBreakingModification() { //add logic for survival only mode
		for (Map.Entry<Integer, BlockPos> entry : noteblockPositions.entrySet()) {
			int blockId = Block.getRawIdFromState(SongPlayer.MC.world.getBlockState(entry.getValue()));
			if (blockId % 2 == 0) { //fixes issue with powered=true blockstate
				blockId += 1;
			}
			int actualNoteId = (blockId-SongPlayer.NOTEBLOCK_BASE_ID)/2;
			if (actualNoteId < 0 || actualNoteId >= 400) {
				return true;
			}
			int actualPtich = actualNoteId % 25;
			int targetPitch = entry.getKey() % 25;
			if (targetPitch != actualPtich) {
				return true;
			}
			BlockState aboveBs = SongPlayer.MC.world.getBlockState(entry.getValue().up());

			if (SongPlayer.switchGamemode) {
				int actualInstrument = actualNoteId / 25;
				int targetInstrument = entry.getKey() / 25;
				if (actualInstrument != targetInstrument) {
					return true;
				}
				if (!aboveBs.isAir() && !aboveBs.isLiquid()) {
					return true;
				}
			} else {
				if (!aboveBs.isAir()) {
					return true;
				}
			}
		}
		return false;
	}
	public int hasPitchModification() { //check if noteblocks need re-tuned
		if (SongPlayer.switchGamemode) {
			return 0;
		}
		int outOfTune = 0;
		HashSet<BlockPos> checked = new HashSet<>();
		for (Map.Entry<BlockPos, Integer> entry : outOfTuneBlocks.entrySet()) {
			if (checked.contains(entry.getValue())) {
				continue;
			}
			checked.add(entry.getKey());
			BlockState bs = SongPlayer.MC.world.getBlockState(entry.getKey());
			int blockId = Block.getRawIdFromState(bs);
			if (blockId % 2 == 0) { //fixes issue with powered=true blockstate
				blockId += 1;
			}
			int actualNoteId = (blockId - SongPlayer.NOTEBLOCK_BASE_ID) / 2;
			if (actualNoteId < 0 || actualNoteId >= 400) { //the target noteblock is not there, hasBreakingModifications should be called for this.
				continue;
			}
			int actualPtich = actualNoteId % 25;
			int targetPitch = entry.getValue() % 25;

			if (targetPitch != actualPtich) {
				outOfTune++;
			}
		}
		return outOfTune;
	}
}
