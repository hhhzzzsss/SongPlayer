package com.github.hhhzzzsss.songplayer.noteblocks;

import java.util.ArrayList;

import com.github.hhhzzzsss.songplayer.FakePlayerEntity;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.song.Song;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public class BuildingThread extends Thread {
	private final ClientPlayerEntity player = SongPlayer.MC.player;
	private final PlayerInventory inventory = SongPlayer.MC.player.getInventory();
	private final ClientWorld world = SongPlayer.MC.world;
	private final Stage stage = SongPlayer.stage;
	private final BlockPos stagePos = SongPlayer.stage.position;
	private final Song song = SongPlayer.song;
	private final int NOTEBLOCK_BASE_ID = Block.getRawIdFromState(Blocks.NOTE_BLOCK.getDefaultState());
	private final String[] instrumentNames = {"harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling"};
	private boolean[] missingNotes = new boolean[400];
	
	public void run() {
		for (int i=0; i<400; i++) {
    		missingNotes[i] = song.requiredNotes[i];
    	}
		stage.noteblockPositions.clear();
    	ArrayList<BlockPos> unusedNoteblockLocations = new ArrayList<>();
		for (int dy : new int[] {-1,2}) {
			for (int dx = -4; dx <= 4; dx++) {
				for (int dz = -4; dz <= 4; dz++) {
					if (Math.abs(dx) == 4 && Math.abs(dz) == 4) continue;
					BlockPos pos = new BlockPos(stagePos.getX()+dx, stagePos.getY()+dy, stagePos.getZ()+dz);
					BlockState bs = world.getBlockState(pos);
					int blockId = Block.getRawIdFromState(bs);
					if (blockId >= NOTEBLOCK_BASE_ID && blockId < NOTEBLOCK_BASE_ID+800) {
						int noteId = (blockId-NOTEBLOCK_BASE_ID)/2;
						if (missingNotes[noteId]) {
							stage.tunedNoteblocks[noteId] = pos;
							missingNotes[noteId] = false;
							stage.noteblockPositions.add(pos);
						}
						else {
							unusedNoteblockLocations.add(pos);
						}
					}
					else {
						unusedNoteblockLocations.add(pos);
					}
				}
			}
		}
		int idx = 0;
		for (int i=0; i<400; i++) {
			if (idx == unusedNoteblockLocations.size()) {
				System.out.println("Too many noteblocks!");
				break;
			}
			if (missingNotes[i]) {
				stage.tunedNoteblocks[i] = unusedNoteblockLocations.get(idx++);
				stage.noteblockPositions.add(stage.tunedNoteblocks[i]);
			}
		}
		
		player.sendChatMessage(SongPlayer.creativeCommand);
		try { //delay in case of block updates
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		while (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
			if (SongPlayer.mode != SongPlayer.Mode.BUILDING) {return;}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		}
		player.getAbilities().allowFlying = true;
		player.getAbilities().flying = true;
		SongPlayer.stage.movePlayerToStagePosition();
		if (SongPlayer.showFakePlayer) {
			if (SongPlayer.fakePlayer != null) {
				SongPlayer.fakePlayer.remove(Entity.RemovalReason.DISCARDED);
			}
			SongPlayer.fakePlayer = new FakePlayerEntity();
		}
		
    	for (int dy : new int[] {0,1,3}) {
    		for (int dx = -4; dx <= 4; dx++) {
    			for (int dz = -4; dz <= 4; dz++) {
					if (SongPlayer.mode != SongPlayer.Mode.BUILDING) {return;}
					
	    			if (Math.abs(dx) == 4 && Math.abs(dz) == 4) continue;
	    			int x = stagePos.getX() + dx;
	    			int y = stagePos.getY() + dy;
	    			int z = stagePos.getZ() + dz;
	    			if (Block.getRawIdFromState(world.getBlockState(new BlockPos(x, y, z))) != 0) {
	    				SongPlayer.MC.interactionManager.attackBlock(new BlockPos(x, y, z), Direction.UP);
        				try {
    						Thread.sleep(10);
    					} catch (InterruptedException e) {
    						e.printStackTrace();
    						return;
    					}
	    			}
	    		}
    		}
    	}
    	System.out.println("done clearing blocks");
		
		for (int i=0; i<400; i++) if (song.requiredNotes[i]) {
			if (SongPlayer.mode != SongPlayer.Mode.BUILDING) {return;}
			
			BlockPos p = stage.tunedNoteblocks[i];
			int blockId = Block.getRawIdFromState(world.getBlockState(p));
			int currentNoteId = (blockId-NOTEBLOCK_BASE_ID)/2;
			int desiredNoteId = i;
			
			if (currentNoteId != desiredNoteId) {
		    	holdNoteblock(desiredNoteId);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
				if (blockId != 0) {
					SongPlayer.MC.interactionManager.attackBlock(p, Direction.UP);
    				try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
						return;
					}
				}
				placeBlock(p);
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
		}
		System.out.println("done placing blocks");
		
		stage.rebuild = false;
		SongPlayer.mode = SongPlayer.Mode.PLAYING;
		SongPlayer.addChatMessage("§6Noteblocks are built. Now playing " + song.name + ".");
		(new PlayingThread()).start();
	}
	
	private void holdNoteblock(int id) {
		int instrument = id/25;
    	int note = id%25;
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", "minecraft:note_block");
		nbt.putByte("Count", (byte) 1);
		NbtCompound tag = new NbtCompound();
		NbtCompound bsTag = new NbtCompound();
		bsTag.putString("instrument", instrumentNames[instrument]);
		bsTag.putString("note", Integer.toString(note));
		tag.put("BlockStateTag", bsTag);
		nbt.put("tag", tag);
		inventory.main.set(inventory.selectedSlot, ItemStack.fromNbt(nbt));
		SongPlayer.MC.interactionManager.clickCreativeStack(player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
	}
	
	private void placeBlock(BlockPos p) {
		double fx = Math.max(0.0, Math.min(1.0, (stage.position.getX() + 0.5 - p.getX())));
		double fy = Math.max(0.0, Math.min(1.0, (stage.position.getY() + 0.0 - p.getY())));
		double fz = Math.max(0.0, Math.min(1.0, (stage.position.getZ() + 0.5 - p.getZ())));
    	fx += p.getX();
    	fy += p.getY();
    	fz += p.getZ();
    	SongPlayer.MC.interactionManager.interactBlock(player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(fx, fy, fz), Direction.UP, p, false));
	}
}
