package com.github.hhhzzzsss.songplayer.playing;

import com.github.hhhzzzsss.songplayer.FakePlayerEntity;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.song.*;
import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.util.LinkedList;

public class SongHandler {
    private static SongHandler instance = null;
    public static SongHandler getInstance() {
        if (instance == null) {
            instance = new SongHandler();
        }
        return instance;
    }
    private SongHandler() {}

    public SongLoaderThread loaderThread = null;
    public LinkedList<Song> songQueue = new LinkedList<>();
    public Song currentSong = null;
    public Stage stage = null;
    public boolean building = false;

    public void onRenderIngame(boolean tick) {
        if (currentSong == null && songQueue.size() > 0) {
            setSong(songQueue.poll());
        }
        if (loaderThread != null && !loaderThread.isAlive()) {
            if (loaderThread.exception != null) {
                SongPlayer.addChatMessage("§cFailed to load song: §4" + loaderThread.exception.getMessage());
            } else {
                if (currentSong == null) {
                    setSong(loaderThread.song);
                } else {
                    queueSong(loaderThread.song);
                }
            }
            loaderThread = null;
        }

        if (currentSong == null) {
            if (stage != null || SongPlayer.fakePlayer != null) {
                if (stage != null) {
                    stage.movePlayerToStagePosition();
                }
                cleanup();
            }
            return;
        }

        if (stage == null) {
            stage = new Stage();
            stage.movePlayerToStagePosition();
        }
        if (SongPlayer.showFakePlayer && SongPlayer.fakePlayer == null) {
            SongPlayer.fakePlayer = new FakePlayerEntity();
            SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
        }
        if (!SongPlayer.showFakePlayer && SongPlayer.fakePlayer != null) {
            SongPlayer.removeFakePlayer();
        }

        checkCommandCache();

        SongPlayer.MC.player.getAbilities().allowFlying = true;
        if (building) {
            if (tick) {
                handleBuilding();
            }
        } else {
            // Check if stage was broken
            handlePlaying(tick);
        }
    }

    public void loadSong(String location) {
        if (loaderThread != null) {
            SongPlayer.addChatMessage("§cAlready loading a song, cannot load another");
        } else {
            try {
                loaderThread = new SongLoaderThread(location);
                SongPlayer.addChatMessage("§6Loading §3" + location + "");
                loaderThread.start();
            } catch (IOException e) {
                SongPlayer.addChatMessage("§cFailed to load song: §4" + e.getMessage());
            }
        }
    }

    public void setSong(Song song) {
        currentSong = song;
        building = true;
        setCreativeIfNeeded();
        if (stage != null) {
            stage.movePlayerToStagePosition();
        }
        SongPlayer.addChatMessage("§6Building noteblocks");
    }

    private void queueSong(Song song) {
        songQueue.add(song);
        SongPlayer.addChatMessage("§6Added song to queue: §3" + song.name);
    }

    // Runs every tick
    private int buildStartDelay = 0;
    private int buildEndDelay = 0;
    private int buildCooldown = 0;
    private void handleBuilding() {
        setBuildProgressDisplay();
        if (buildStartDelay > 0) {
            buildStartDelay--;
            return;
        }
        if (buildCooldown > 0) {
            buildCooldown--;
            return;
        }
        ClientWorld world = SongPlayer.MC.world;
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            return;
        }

        if (stage.nothingToBuild()) {
            if (buildEndDelay > 0) {
                buildEndDelay--;
                return;
            } else {
                stage.checkBuildStatus(currentSong);
            }
        }

        if (!stage.requiredBreaks.isEmpty()) {
            for (int i=0; i<5; i++) {
                if (stage.requiredBreaks.isEmpty()) break;
                BlockPos bp = stage.requiredBreaks.poll();
                SongPlayer.MC.interactionManager.attackBlock(bp, Direction.UP);
            }
            buildEndDelay = 40;
            return;
        } else if (!stage.missingNotes.isEmpty()) {
            int desiredNoteId = stage.missingNotes.pollFirst();
            BlockPos bp = stage.noteblockPositions.get(desiredNoteId);
            if (bp == null) {
                return;
            }
            int blockId = Block.getRawIdFromState(world.getBlockState(bp));
            int currentNoteId = (blockId-SongPlayer.NOTEBLOCK_BASE_ID)/2;
            if (currentNoteId != desiredNoteId) {
                holdNoteblock(desiredNoteId);
                if (blockId != 0) {
                    attackBlock(bp);
                }
                placeBlock(bp);
            }
            buildCooldown = 4;
            buildEndDelay = 40;
        } else {
            building = false;
            setSurvivalIfNeeded();
            stage.movePlayerToStagePosition();
            SongPlayer.addChatMessage("§6Now playing §3" + currentSong.name);
            return;
        }
    }
    private void setBuildProgressDisplay() {
        MutableText text = Text.empty()
                .append(Text.literal("Building noteblocks | " ).formatted(Formatting.GOLD))
                .append(Text.literal((stage.totalMissingNotes - stage.missingNotes.size()) + "/" + stage.totalMissingNotes).formatted(Formatting.DARK_AQUA));
        ProgressDisplay.getInstance().setText(text);
    }

    // Runs every frame
    private void handlePlaying(boolean tick) {
        if (tick) {
            setPlayProgressDisplay();
        }

        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
            currentSong.pause();
            return;
        }

        if (tick) {
            if (stage.hasBreakingModification()) {
                stage.checkBuildStatus(currentSong);
            }
            if (!stage.nothingToBuild()) {
                building = true;
                setCreativeIfNeeded();
                stage.movePlayerToStagePosition();
                currentSong.pause();
                buildStartDelay = 40;
                System.out.println("Total missing notes: " + stage.missingNotes.size());
                for (int note : stage.missingNotes) {
                    int pitch = note % 25;
                    int instrumentId = note / 25;
                    System.out.println("Missing note: " + Instrument.getInstrumentFromId(instrumentId).name() + ":" + pitch);
                }
                SongPlayer.addChatMessage("§6Stage was altered. Rebuilding!");
                return;
            }
        }

        currentSong.play();

        boolean somethingPlayed = false;
        currentSong.advanceTime();
        while (currentSong.reachedNextNote()) {
            Note note = currentSong.getNextNote();
            BlockPos bp = stage.noteblockPositions.get(note.noteId);
            if (bp != null) {
                attackBlock(bp);
                somethingPlayed = true;
            }
        }
        if (somethingPlayed) {
            stopAttack();
        }

        if (currentSong.finished()) {
            SongPlayer.addChatMessage("§6Done playing §3" + currentSong.name);
            currentSong = null;
        }
    }

    public void setPlayProgressDisplay() {
        long currentTime = Math.min(currentSong.time, currentSong.length);
        long totalTime = currentSong.length;
        MutableText text = Text.empty()
                .append(Text.literal("Now playing ").formatted(Formatting.GOLD))
                .append(Text.literal(currentSong.name).formatted(Formatting.BLUE))
                .append(Text.literal(" | ").formatted(Formatting.GOLD))
                .append(Text.literal(String.format("%s/%s", Util.formatTime(currentTime), Util.formatTime(totalTime))).formatted(Formatting.DARK_AQUA));
        if (currentSong.looping) {
            if (currentSong.loopCount > 0) {
                text.append(Text.literal(String.format(" | Loop (%d/%d)", currentSong.currentLoop, currentSong.loopCount)).formatted(Formatting.GOLD));
            } else {
                text.append(Text.literal(" | Looping enabled").formatted(Formatting.GOLD));
            }
        }
        ProgressDisplay.getInstance().setText(text);
    }

    public void cleanup() {
        currentSong = null;
        songQueue.clear();
        stage = null;
        SongPlayer.removeFakePlayer();
    }

    public void onNotIngame() {
        currentSong = null;
        songQueue.clear();
    }

    private long lastCommandTime = System.currentTimeMillis();
    private String cachedCommand = null;
    private void sendGamemodeCommand(String command) {
        cachedCommand = command;
    }
    private void checkCommandCache() {
        if (cachedCommand != null && System.currentTimeMillis() >= lastCommandTime + 1500) {
            SongPlayer.MC.player.sendCommand(cachedCommand);
            cachedCommand = null;
            lastCommandTime = System.currentTimeMillis();
        }
    }
    private void setCreativeIfNeeded() {
        cachedCommand = null;
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            sendGamemodeCommand(SongPlayer.creativeCommand);
        }
    }
    private void setSurvivalIfNeeded() {
        cachedCommand = null;
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
            sendGamemodeCommand(SongPlayer.survivalCommand);
        }
    }

    private final String[] instrumentNames = {"harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling"};
    private void holdNoteblock(int id) {
        PlayerInventory inventory = SongPlayer.MC.player.getInventory();
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
        SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
    }
    private void placeBlock(BlockPos bp) {
        double fx = Math.max(0.0, Math.min(1.0, (stage.position.getX() + 0.5 - bp.getX())));
        double fy = Math.max(0.0, Math.min(1.0, (stage.position.getY() + 0.0 - bp.getY())));
        double fz = Math.max(0.0, Math.min(1.0, (stage.position.getZ() + 0.5 - bp.getZ())));
        fx += bp.getX();
        fy += bp.getY();
        fz += bp.getZ();
        SongPlayer.MC.interactionManager.interactBlock(SongPlayer.MC.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(fx, fy, fz), Direction.UP, bp, false));
    }
    private void attackBlock(BlockPos bp) {
        SongPlayer.MC.interactionManager.attackBlock(bp, Direction.UP);
    }
    private void stopAttack() {
        SongPlayer.MC.interactionManager.cancelBlockBreaking();
    }
}