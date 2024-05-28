package com.github.hhhzzzsss.songplayer.playing;

import com.github.hhhzzzsss.songplayer.FakePlayerEntity;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.song.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SongHandler {
    public static ItemStack oldItemHeld = null;
    private static SongHandler instance = null;
    public static SongHandler getInstance() {
        if (instance == null) {
            instance = new SongHandler();
        }
        return instance;
    }
    private SongHandler() {}
    public static SongLoaderThread loaderThread = null;
    public LinkedList<Song> songQueue = new LinkedList<>();
    public Song currentSong = null;
    public Stage stage = null;
    public boolean building = false;
    public boolean paused = false;
    public boolean isflying = false;
    public boolean converting = false;
    public String convertintto = "";
    int bandaidpatch;

    public void onUpdate(Boolean tick) {
        isflying = SongPlayer.MC.player.getAbilities().flying;
        if (currentSong == null && songQueue.size() > 0) {
            setSong(songQueue.poll());
        }
        if (loaderThread != null && !loaderThread.isAlive()) {
            if (loaderThread.exception != null) {
                SongPlayer.addChatMessage("§cFailed to load song: §4" + loaderThread.exception.getMessage());
                Util.advancePlaylist();
            } else {
                if (converting) {
                    switch(convertintto) {
                        case ".txt": {
                            TxtConverter.outputSong(loaderThread.song);
                            break;
                        }
                        case ".nbs": {
                            NBSConverter.outputSong(loaderThread.song);
                            break;
                        }
                        case ".midi":
                        case ".mid": {
                            MidiConverter.outputSong(loaderThread.song);
                            break;
                        }
                        default: {
                            SongPlayer.addChatMessage("unsupported file format");
                        }
                    }
                    loaderThread = null;
                    converting = false;
                    convertintto = "";
                    return;
                }
                if (currentSong == null) {
                    setSong(loaderThread.song);
                } else {
                    queueSong(loaderThread.song);
                }
            }
            loaderThread = null;
        }
        if (SongPlayer.recording) {
            setRecordingDisplay();
        }
        if (currentSong == null) {
            if (songQueue.isEmpty() && Util.playlistSongs.isEmpty()) {
                if (stage != null || SongPlayer.fakePlayer != null) {
                    if (stage != null) {
                        stage.movePlayerToStagePosition(false, false, false);
                        Util.disableFlightIfNeeded();
                    }
                    cleanup(false);
                } else {
                    SongPlayer.oldgamemode = SongPlayer.MC.interactionManager.getCurrentGameMode();
                }
            }
            return;
        }

        if (stage == null) {
            stage = new Stage();
            if (songQueue.isEmpty() && Util.playlistSongs.isEmpty()) {
                stage.movePlayerToStagePosition(false, false, false);
            }
        }
        if (SongPlayer.showFakePlayer && SongPlayer.fakePlayer == null && SongPlayer.useNoteblocksWhilePlaying && !paused) {
            SongPlayer.fakePlayer = new FakePlayerEntity();
            SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
        }
        if (!SongPlayer.showFakePlayer && SongPlayer.fakePlayer != null) {
            SongPlayer.removeFakePlayer();
        }
        checkCommandCache();
        if (SongPlayer.useNoteblocksWhilePlaying && !paused) {
            SongPlayer.MC.player.getAbilities().allowFlying = true;
        }
        if (building && SongPlayer.useNoteblocksWhilePlaying) {
            handleBuilding(tick);
        } else {
            handlePlaying(tick);
        }
    }

    public void loadSong(String location, File dir) {
        if (loaderThread != null) {
            SongPlayer.addChatMessage("§cAlready loading a song, cannot load another");
            return;
        }
        if (SongPlayer.recording) {
            SongPlayer.addChatMessage("§cCannot load song while recording noteblocks");
            return;
        }
        try {
            if (Util.currentPlaylist.isEmpty()) {
                SongPlayer.addChatMessage("§6Loading §3" + location + "");
                if (SongPlayer.kiosk) {
                    Util.broadcastMessage("[{\"color\":\"gold\",\"text\":\"Loading \"}, {\"color\":\"dark_aqua\",\"text\":\"" + location + "\"}]", null);
                }
            }
            loaderThread = new SongLoaderThread(location, dir);
            loaderThread.start();
        } catch (IOException e) {
            SongPlayer.addChatMessage("§cFailed to load song: §4" + e.getMessage());
            if (SongPlayer.kiosk) {
                Util.broadcastMessage("[{\"color\":\"red\",\"text\":\"Failed to load song: \"}, {\"color\":\"dark_red\",\"text\":\"" + e.getMessage() + "\"}]", null);
            }
        }
    }

    public void setSong(Song song) {
        currentSong = song;
        if (SongPlayer.kiosk) {
            Util.sendCommandWithCommandblocks("bossbar set minecraft:songplayer max " + this.currentSong.length);
            Util.sendCommandWithCommandblocks("bossbar set minecraft:songplayer visible true");
            Util.sendCommandWithCommandblocks("bossbar set minecraft:songplayer players @a");
            Util.broadcastMessage("[{\"color\":\"gold\",\"text\":\"Now playing \"}, {\"color\":\"dark_aqua\",\"text\":\"" + currentSong.name + "\"}]", null);
        }
        if (!SongPlayer.useNoteblocksWhilePlaying) {
            Util.assignCommandBlocks(false);
            building = false;
            return;
        }
        if (stage == null) {
            stage = new Stage();
            stage.movePlayerToStagePosition(true, true, false);
        } else {
            stage.movePlayerToStagePosition(true, false, true);
        }
        pitchesLeft = 0;
        modifyingblock = false;
        stage.checkBuildStatus(currentSong);
        if (!Util.hasEnoughNoteblocks()) {
            return;
        }
        if (stage.nothingToBuild()) { //nothing else needs to be built. Go in survival if needed and play the song
            setSurvivalIfNeeded();
            return;
        }
        if (SongPlayer.switchGamemode) {
            SongPlayer.addChatMessage("§6Building noteblocks");
        } else {
            SongPlayer.addChatMessage("§6Tuning noteblocks");
        }
        building = true;
        setCreativeIfNeeded();
    }

    private void queueSong(Song song) {
        if (!Util.playlistSongs.isEmpty() || !Util.currentPlaylist.isEmpty()) {
            SongPlayer.addChatMessage("§cUnable to add song to queue. Playlist is in progress.");
            return;
        }
        songQueue.add(song);
        if (Util.currentPlaylist.isEmpty()) {
            SongPlayer.addChatMessage("§6Added song to queue: §3" + song.name);
        }
    }

    // Runs every tick

    private int buildCooldown = 0;
    private int updatePlayerPosCooldown = 0;
    private int pitchesLeft = 0;
    private int outoftuneindex = 0;
    private long buildEndDelay = Calendar.getInstance().getTime().getTime();
    private boolean modifyingblock = false;


    private HashMap<BlockPos, Integer> outOfTune = new HashMap<>();
    private void handleBuilding(boolean tick) {
        if (paused) {
            building = false;
            return;
        }
        if (SongPlayer.MC.player.isDead()) {
            stage.outOfTuneBlocks.clear();
            Util.pauseSongIfNeeded();
            SongPlayer.addChatMessage("§6Your song has been paused because you died.\n§6Go back to your stage or find a new location and type §3" + SongPlayer.prefix + "resume§6 to resume playing.");
            return;
        }
        if (!SongPlayer.useNoteblocksWhilePlaying) {
            return;
        }
        //handle cooldown
        setBuildProgressDisplay();

        if (buildCooldown > 0) {
            if (tick || SongPlayer.useFramesInsteadOfTicks) {
                buildCooldown--;
            }
            return;
        }

        if (!SongPlayer.useNoteblocksWhilePlaying) {
            if (updatePlayerPosCooldown > 0 && (tick || SongPlayer.useFramesInsteadOfTicks)) {
                updatePlayerPosCooldown--;
            }
        }

        ClientWorld world = SongPlayer.MC.world;
        PlayerInventory inventory = SongPlayer.MC.player.getInventory();

        if (SongPlayer.switchGamemode) {
            //we're cleaning up if this is true
            if (Util.cleaningup && Util.cleanupstage.size() > 0) {
                HashMap<BlockPos, BlockState> backup = new HashMap<>(Util.cleanupstage);
                for (Map.Entry<BlockPos, BlockState> e : backup.entrySet()) {
                    BlockPos bPos = e.getKey();
                    BlockState block = e.getValue();
                    if (SongPlayer.MC.world.getBlockState(bPos).getBlock() == block.getBlock()) {
                        Util.cleanupstage.remove(bPos);
                    }
                }
                if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE || Util.cleanupstage.size() < 1) {
                    return;
                }
                Map.Entry<BlockPos,BlockState> entry = Util.cleanupstage.entrySet().iterator().next();
                BlockPos bPos = entry.getKey();
                BlockState block = entry.getValue();
                ItemStack item = new ItemStack(block.getBlock().asItem());
                holdBlock(item);
                if (SongPlayer.MC.world.getBlockState(bPos) != block && !SongPlayer.MC.world.getBlockState(bPos).isAir()) {
                    attackBlock(bPos);
                    buildCooldown = SongPlayer.buildDelay;
                }
                if (SongPlayer.MC.world.getBlockState(bPos) != block && !block.isAir()) {
                    placeBlock(bPos);
                    buildCooldown = SongPlayer.buildDelay;
                }
                Util.cleanupstage.remove(bPos);
                return;
            }

            //we're done cleaning up if this is true
            if (Util.cleaningup) {
                if (SongHandler.getInstance().stage != null && SongPlayer.useNoteblocksWhilePlaying) {
                    SongHandler.getInstance().stage.movePlayerToStagePosition();
                }
                if (!paused) {
                    SongHandler.getInstance().cleanup(true);
                }
                Util.disableFlightIfNeeded();
                if (SongHandler.oldItemHeld != null) {
                    inventory.setStack(inventory.selectedSlot, SongHandler.oldItemHeld);
                    SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
                    SongHandler.oldItemHeld = null;
                }
                SongPlayer.addChatMessage("done cleaning up");
                Util.cleaningup = false;
                building = false;
                return;
            }

            outoftuneindex = 0;

            if (stage.nothingToBuild()) {
                if (buildEndDelay > Calendar.getInstance().getTime().getTime()) {
                    stage.checkBuildStatus(currentSong);
                    return;
                }
                if (oldItemHeld != null) {
                    inventory.main.set(inventory.selectedSlot, oldItemHeld);
                    SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
                    oldItemHeld = null;
                }
                setSurvivalIfNeeded();
                if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
                    return;
                }
                building = false;
                stage.outOfTuneBlocks.clear();
                modifyingblock = false;
                pitchesLeft = 0;
                return;
            }

            if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
                return;
            }

            //break blocks that need to be broken if any exist
            if (!stage.requiredBreaks.isEmpty()) {
                BlockPos bp = stage.requiredBreaks.poll();
                attackBlock(bp);
                buildCooldown = SongPlayer.buildDelay;
                buildEndDelay = Calendar.getInstance().getTime().getTime() + 500;
                return;
            }

            //check if there needs to be any building
            if (!stage.missingNotes.isEmpty()) {
                int desiredNoteId = stage.missingNotes.pollFirstEntry().getKey();
                BlockPos bp = stage.noteblockPositions.get(desiredNoteId);

                int blockId = Block.getRawIdFromState(world.getBlockState(bp));
                if (blockId % 2 == 0) { //fixes issue with powered=true blockstate
                    blockId += 1;
                }
                int currentNoteId = (blockId - SongPlayer.NOTEBLOCK_BASE_ID) / 2;
                if (currentNoteId != desiredNoteId) {
                    holdNoteblock(desiredNoteId);
                    if (blockId != 0) {
                        attackBlock(bp);
                    }
                    placeBlock(bp);
                }
                buildCooldown = SongPlayer.buildDelay;
                buildEndDelay = Calendar.getInstance().getTime().getTime() + 500;
                return;
            }
        } else { //survival only mode
            if (outoftuneindex >= stage.outOfTuneBlocks.size() && stage.outOfTuneBlocks.size() > 0) {
                if (buildEndDelay > Calendar.getInstance().getTime().getTime()) {
                    return;
                }
                outoftuneindex = 0;
                outOfTune.clear();
                int notright = 0;
                for (BlockPos test : new ArrayList<>(stage.outOfTuneBlocks.keySet())) {
                    int blockId = Block.getRawIdFromState(world.getBlockState(test));
                    if (blockId % 2 == 0) { //fixes issue with powered=true blockstate
                        blockId += 1;
                    }
                    if (blockId < SongPlayer.NOTEBLOCK_BASE_ID || blockId > SongPlayer.NOTEBLOCK_BASE_ID + 800) {
                        continue;
                    }

                    int currentBlockId = (blockId - SongPlayer.NOTEBLOCK_BASE_ID) / 2;
                    int currentPitchId = currentBlockId % 25;
                    int wantedNoteId = stage.outOfTuneBlocks.get(test);
                    int wantedPitchId = wantedNoteId % 25;

                    if (currentPitchId != wantedPitchId) {
                        notright++;
                        outOfTune.put(test, wantedNoteId);
                    }
                }
                stage.outOfTuneBlocks.clear();
                if (notright == 0) {
                    stage.missingNotes.clear();
                    outOfTune.clear();
                    buildEndDelay = Calendar.getInstance().getTime().getTime() + 1000;
                    stage.checkBuildStatus(currentSong);
                    return;
                } else {
                    stage.outOfTuneBlocks.putAll(outOfTune);
                    buildEndDelay = Calendar.getInstance().getTime().getTime() + 1000;
                }
            }

            //check if anything needs to be done
            if (stage.hasPitchModification() == 0) {
                outoftuneindex = 0;
                if (buildEndDelay > Calendar.getInstance().getTime().getTime()) {
                    return;
                }
                stage.checkBuildStatus(currentSong);
                if (stage.outOfTuneBlocks.size() > 0) { //aaagh someone be messing with ur noteblocks, or your ping is over a full second behind.
                    return;
                }
                building = false;
                setSurvivalIfNeeded();
                stage.outOfTuneBlocks.clear();
                modifyingblock = false;
                pitchesLeft = 0;
                return;
            }

            if (stage.outOfTuneBlocks.isEmpty()) {
                buildCooldown = SongPlayer.buildDelay;
                stage.checkBuildStatus(currentSong);
                pitchesLeft = 0;
                outoftuneindex = 0;
                modifyingblock = false;
                return;
            }

            buildEndDelay = Calendar.getInstance().getTime().getTime() + 1000;
            //rebuilding
            Set<Map.Entry<BlockPos, Integer>> entrySet = stage.outOfTuneBlocks.entrySet();
            List<Map.Entry<BlockPos, Integer>> entryList = new ArrayList<>(entrySet);
            int desiredNoteId = entryList.get(outoftuneindex).getValue();
            BlockPos bp = entryList.get(outoftuneindex).getKey();
            if (bp == null) {
                pitchesLeft = 0;
                outoftuneindex++;
                modifyingblock = false;
                return;
            }

            int blockId = Block.getRawIdFromState(world.getBlockState(bp));
            if (blockId % 2 == 0) { //fixes issue with powered=true blockstate
                blockId += 1;
            }

            if (blockId < SongPlayer.NOTEBLOCK_BASE_ID || blockId > SongPlayer.NOTEBLOCK_BASE_ID + 800) { //target block is not a noteblock, skip it
                pitchesLeft = 0;
                outoftuneindex++;
                modifyingblock = false;
                return;
            }
            if (pitchesLeft > 0 && modifyingblock) {
                placeBlock(bp);
                pitchesLeft--;
            } else {
                if (modifyingblock) {
                    pitchesLeft = 0;
                    outoftuneindex++;
                    modifyingblock = false;
                    return;
                }

                int currentNoteId = ((blockId - SongPlayer.NOTEBLOCK_BASE_ID) / 2) % 25;
                int currentPitch = currentNoteId % 25;
                int neededPitch = desiredNoteId % 25;
                int amountOfPitches = neededPitch - currentPitch;
                if (currentPitch == neededPitch) {
                    modifyingblock = false;
                    outoftuneindex++;
                    return;
                }
                if (amountOfPitches < 0) {
                    amountOfPitches += 25;
                }
                pitchesLeft = amountOfPitches;
                modifyingblock = true;
            }
        }
        buildEndDelay = Calendar.getInstance().getTime().getTime() + 500;
        buildCooldown = SongPlayer.buildDelay;
    }
    private void setBuildProgressDisplay() {
        MutableText text = Text.empty();
        MutableText empty = Text.empty(); //why bother re-writing code when you can do a rubberband fix?
        if (SongPlayer.switchGamemode) {
            if (stage.totalMissingNotes == 0) {
                return;
            }
            text.append(Text.literal("Building noteblocks | ").formatted(Formatting.GOLD));
            text.append(Text.literal((stage.totalMissingNotes - stage.missingNotes.size() + "/" + stage.totalMissingNotes)).formatted(Formatting.DARK_AQUA));
        } else {
            int progress = stage.totalOutOfTuneNotes - stage.hasPitchModification(); // (stage.totalOutOfTuneNotes - (stage.outOfTuneBlocks.size() + stage.hasPitchModification()));
            if (stage.totalOutOfTuneNotes == 0) {
                return;
            }
            text.append(Text.literal("Tuning noteblocks | ").formatted(Formatting.GOLD));
            text.append(Text.literal(progress + "/" + stage.totalOutOfTuneNotes).formatted(Formatting.DARK_AQUA));
        }
        ProgressDisplay.getInstance().setText(text, empty);
    }

    private void setRecordingDisplay() {
        MutableText subtext = Text.empty();
        MutableText text = Text.empty();

        if (!SongPlayer.recordingActive) {
            text.append("§6Recording | §6waiting for a noteblock to be played");
        } else {
            text.append("§6Recording | §6" + Util.recordName + ".txt");
        }
        subtext.append("§6recorded notes: §3" + Util.recordedNotes.size() + (Util.recordednotesfailed > 0 ? "§6 | §4" + Util.recordednotesfailed + "§c failed to save, check logs" : "") + "§6 | time: §3" + Util.formatTime(SongPlayer.recordingtick * 50) + "§6");
        ProgressDisplay.getInstance().setText(text, subtext);
    }

    // Runs every frame
    private void handlePlaying(boolean tick) {
        setPlayProgressDisplay();
        if (paused) {
            currentSong.pause();
            building = false;
            return;
        }
        if (SongPlayer.useNoteblocksWhilePlaying) {
            if (SongPlayer.MC.player.isDead()) {
                Util.pauseSongIfNeeded();
                SongPlayer.addChatMessage("§6Your song has been paused because you died.\n §6Go back to your stage or find a new location and type §3" + SongPlayer.prefix + "resume§6 to resume playing.");
                return;
            }
            if (SongPlayer.MC.player.isRiding()) {
                Util.pauseSongIfNeeded();
                SongPlayer.addChatMessage("§6Your song has been paused because you were moved away from your stage.\n §6Go back to your stage or find a new location and type §3" + SongPlayer.prefix + "resume§6 to resume playing.");
                return;
            }
            if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL || SongPlayer.MC.player.isSleeping()) {
                currentSong.pause();
                return;
            }
            if (tick) {
                if (SongPlayer.switchGamemode || SongPlayer.requireExactInstruments) {
                    stage.checkBuildStatus(currentSong);
                } else {
                    if (stage.hasBreakingModification()) { //only refresh the stage if at least one of the noteblocks are not the correct pitch so your game doesn't turn into a slideshow when playing
                        stage.checkBuildStatus(currentSong);
                    }
                }
            }
            if (!Util.hasEnoughNoteblocks() && !SongPlayer.ignoreWarnings) {
                return;
            }
            if (stage.hasPitchModification() > 0) {
                building = true;
                currentSong.pause();
                SongPlayer.addChatMessage("§6Stage was altered. Retuning!");
                return;
            }
            if (!stage.nothingToBuild() && !SongPlayer.ignoreWarnings) {
                if (!SongPlayer.switchGamemode) {
                    if (!tick) {
                        return;
                    }
                    paused = true;
                    SongPlayer.addChatMessage("§6Stage was altered and too many noteblocks were removed or can't be played. Please repair the stage and then type §3" + SongPlayer.prefix + "resume");
                    return;
                }
                building = true;
                setCreativeIfNeeded();
                currentSong.pause();
                SongPlayer.addChatMessage("§6Stage was altered. Rebuilding!");
                return;
            }
        }

        if (Calendar.getInstance().getTime().getTime() < Util.playcooldown) {
            return;
        }

        //cooldown is over!

        if (!tick) {
            return;
        }
        currentSong.play();
        currentSong.advanceTime();
        ClientPlayerEntity player = SongPlayer.MC.player;
        if (player.isCreative() || player.isSpectator()) {
            setSurvivalIfNeeded();
        }
        if (updatePlayerPosCooldown < 1) {
            updatePlayerPosCooldown = 10;
            Util.playerPosX = player.getX();
            Util.playerPosZ = player.getZ();
        }
        SoundEvent[] soundlist = {SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), SoundEvents.BLOCK_NOTE_BLOCK_SNARE.value(), SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), SoundEvents.BLOCK_NOTE_BLOCK_FLUTE.value(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundEvents.BLOCK_NOTE_BLOCK_GUITAR.value(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundEvents.BLOCK_NOTE_BLOCK_XYLOPHONE.value(), SoundEvents.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE.value(), SoundEvents.BLOCK_NOTE_BLOCK_COW_BELL.value(), SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundEvents.BLOCK_NOTE_BLOCK_BANJO.value(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value()};
        World world = SongPlayer.MC.player.getWorld();
        HashMap<Integer, Note> playedNotes = new HashMap<>();
        while (currentSong.reachedNextNote()) {
            Note note = currentSong.getNextNote();
            if (playedNotes.containsKey(note.noteId)) {
                if (note.volume > playedNotes.get(note.noteId).volume) {
                    playedNotes.put(note.noteId, note);
                }
                continue;
            }
            playedNotes.put(note.noteId, note);
        }
        for (Note note : playedNotes.values()) {
            float volfloat = (float) (note.volume / 127.0);
            String volume = String.valueOf(volfloat);
            if (SongPlayer.parseVolume) {
                if (SongPlayer.ignoreNoteThreshold > note.volume) { //skip note - too quiet
                    continue;
                }
                if (volume.length() > 4) {
                    volume = volume.substring(0, 6);
                    if (volume.endsWith(".")) {
                        volume = volume + "0";
                    } else if (!volume.contains(".")) {
                        volume = volume + ".0";
                    }
                }
            }
            if (SongPlayer.useNoteblocksWhilePlaying) {
                BlockPos bp = stage.noteblockPositions.get(note.noteId);
                if (bp != null) {
                    attackBlock(bp);
                }
            } else if (SongPlayer.useCommandsForPlaying) {
                if (SongPlayer.disablecommandplaynote) {
                    break;
                }
                int instrument = note.noteId / 25;
                int pitchID = (note.noteId % 25);
                double pitch = Math.pow(2, (pitchID + note.pitchCorrection/100.0 - 12) / 12);
                if (pitch > 2.0) pitch = 2.0;
                if (pitch < 0.5) pitch = 0.5;
                String command;
                if (!SongPlayer.parseVolume) {
                    volume = "1.0";
                }
                command = SongPlayer.playSoundCommand.replace("{type}", instrumentNames[instrument]).replace("{volume}", volume).replace("{pitch}", Double.toString(pitch));
                if (SongPlayer.includeCommandBlocks) {
                    Util.sendCommandWithCommandblocks(command);
                } else {
                    SongPlayer.MC.getNetworkHandler().sendCommand(command);
                }
            } else { //play client-side
                if (SongPlayer.parseVolume) {
                    player.playSound(soundlist[note.noteId / 25], SoundCategory.RECORDS, volfloat, (float) Math.pow(2, (note.noteId % 25 + note.pitchCorrection/100.0 - 12) / 12));
                } else {
                    world.playSound(Util.playerPosX, player.getY() + 3000000, Util.playerPosZ, soundlist[note.noteId / 25], SoundCategory.RECORDS, 30000000, (float) Math.pow(2, (note.noteId % 25 + note.pitchCorrection/100.0 - 12) / 12), false);
                }
            }
        }

        if (!playedNotes.isEmpty()) {
            stopAttack();
        }

        if (currentSong.finished()) {
            if (SongPlayer.cleanupstage && songQueue.isEmpty() && !Util.cleanupstage.isEmpty()) {
                Util.cleanupstage();
                return;
            }
            Util.playcooldown = Calendar.getInstance().getTime().getTime() + 1500;
            if (Util.currentPlaylist.isEmpty()) {
                if (SongPlayer.kiosk) {
                    Util.broadcastMessage("[{\"color\":\"gold\",\"text\":\"Done playing \"}, {\"color\":\"dark_aqua\",\"text\":\"" + currentSong.name + "\"}]", null);
                    Util.sendCommandWithCommandblocks("bossbar set minecraft:songplayer visible false");
                }
                SongPlayer.addChatMessage("§6Done playing §3" + currentSong.name);
            }
            currentSong = null;
            Util.advancePlaylist();
        }
    }

    public void setPlayProgressDisplay() {
        long currentTime = Math.min(currentSong.time, currentSong.length);
        long totalTime = currentSong.length;
        MutableText text = Text.empty();
        MutableText empty = Text.empty(); //I should use this for something... Thanks hhhzzzsss!
        if (paused) {
            text.append(Text.literal("Paused ").formatted(Formatting.GOLD));
        } else {
            text.append(Text.literal("Now playing ").formatted(Formatting.GOLD));
        }
        text.append(Text.literal(currentSong.name).formatted(Formatting.BLUE))
        .append(Text.literal(" | ").formatted(Formatting.GOLD))
        .append(Text.literal(String.format("%s/%s", Util.formatTime(currentTime), Util.formatTime(totalTime))).formatted(Formatting.DARK_AQUA));
        if (currentSong.looping) {
            if (currentSong.loopCount > 0) {
                text.append(Text.literal(String.format(" | Looping (%d/%d)", currentSong.currentLoop, currentSong.loopCount)).formatted(Formatting.GOLD));
            } else {
                text.append(Text.literal(" | Looping enabled").formatted(Formatting.GOLD));
            }
        }
        ProgressDisplay.getInstance().setText(text, empty);
        if (SongPlayer.useCommandsForPlaying && !Util.lastTimeExecuted.equalsIgnoreCase(Util.formatTime(currentTime))) {
            Util.lastTimeExecuted = Util.formatTime(currentTime);
            if (SongPlayer.disablecommanddisplayprogress) {
                return;
            }
            if (SongPlayer.kiosk) {
                Util.sendCommandWithCommandblocks("bossbar set minecraft:songplayer name [{\"color\":\"gold\",\"text\":\"Now playing \"}, {\"color\":\"blue\",\"text\":\"" + this.currentSong.name + "\"}, {\"color\":\"gold\",\"text\":\" | \"}, {\"color\":\"dark_aqua\",\"text\":\"" + String.format("%s/%s", Util.formatTime(currentTime), Util.formatTime(totalTime)) + "\"}]");
                Util.sendCommandWithCommandblocks("bossbar set minecraft:songplayer value " + currentTime);
            }
            String midiname = currentSong.name;
            String rawcommand = SongPlayer.showProgressCommand;
            String command = rawcommand.replace("{MIDI}", midiname).replace("{CurrentTime}", Util.formatTime(currentTime)).replace("{SongTime}", Util.formatTime(totalTime));
            int cmdlength = command.length();
            if (cmdlength > 254) {
                while (cmdlength > 250) {
                    midiname = midiname.substring(0, midiname.length() - 1);
                    cmdlength -= 1;
                }
                midiname = midiname + "...";
                command = rawcommand.replace("{MIDI}", midiname).replace("{CurrentTime}", Util.formatTime(currentTime)).replace("{SongTime}", Util.formatTime(totalTime));
            }
            if (SongPlayer.useCommandsForPlaying) {
                if (SongPlayer.includeCommandBlocks) {
                    Util.sendCommandWithCommandblocks(command);
                } else {
                    SongPlayer.MC.getNetworkHandler().sendCommand(command);
                }
            }
        }
    }

    public void restoreGamemode() {
        if (SongPlayer.oldgamemode == SongPlayer.MC.interactionManager.getCurrentGameMode()) return;
        cachedCommand = null;
        switch(SongPlayer.oldgamemode) {
            case SURVIVAL: {
                SongPlayer.MC.getNetworkHandler().sendCommand(SongPlayer.survivalCommand);
                return;
            }
            case CREATIVE: {
                SongPlayer.MC.getNetworkHandler().sendCommand(SongPlayer.creativeCommand);
                return;
            }
            case ADVENTURE: {
                SongPlayer.MC.getNetworkHandler().sendCommand(SongPlayer.adventureCommand);
                return;
            }
            case SPECTATOR: {
                SongPlayer.MC.getNetworkHandler().sendCommand(SongPlayer.spectatorCommand);
                return;
            }
            default: {
                SongPlayer.addChatMessage("Attempted to switch to an unknown gamemode");
            }
        }
    }

    public void cleanup(boolean includePlaylist) {
        if (!includePlaylist && Util.playlistSongs.size() > 0) {
            return;
        }
        if (SongPlayer.switchGamemode && !SongPlayer.recording && (currentSong != null || !Util.playlistSongs.isEmpty())) {
            restoreGamemode();
        }
        currentSong = null;
        songQueue.clear();
        stage = null;
        paused = false;
        if (!SongPlayer.kiosk) {
            Util.availableCommandBlocks.clear();
        }
        Util.playlistSongs.clear();
        Util.currentPlaylist = "";
        Util.playlistIndex = 0;
        Util.loopPlaylist = false;
        SongPlayer.removeFakePlayer();
        ProgressDisplay.getInstance().setText(Text.literal(""), Text.literal(""));
        SongPlayer.recording = false;
        SongPlayer.recordingActive = false;
        SongPlayer.recordingtick = 0;
        Util.recordedNotes.clear();
        Util.cleanupstage.clear();
    }

    public void onNotIngame() {
        currentSong = null;
        songQueue.clear();
        Util.playlistSongs.clear();
        Util.currentPlaylist = "";
        Util.playlistIndex = 0;
    }

    private long lastCommandTime = System.currentTimeMillis();
    private String cachedCommand = null;
    private void sendGamemodeCommand(String command) {
        cachedCommand = command;
    }
    private void checkCommandCache() {
        //does not handle useCommandsForPlaying mode
        if (cachedCommand == null) return;
        if (System.currentTimeMillis() >= lastCommandTime + 1500 || (SongPlayer.MC.player.hasPermissionLevel(2) && System.currentTimeMillis() >= lastCommandTime + 250)) {
            SongPlayer.MC.getNetworkHandler().sendCommand(cachedCommand);
            cachedCommand = null;
            lastCommandTime = System.currentTimeMillis();
        }
    }
    private void setCreativeIfNeeded() {
        cachedCommand = null;
        if (!SongPlayer.switchGamemode) {
            return;
        }
        if (SongPlayer.disablecommandcreative) {
            return;
        }
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            sendGamemodeCommand(SongPlayer.creativeCommand);
        }
    }
    private void setSurvivalIfNeeded() {
        cachedCommand = null;
        if (!SongPlayer.switchGamemode) {
            return;
        }
        if (SongPlayer.disablecommandsurvival) {
            return;
        }
        if (oldItemHeld != null) {
            CreativeInventoryActionC2SPacket packet = new CreativeInventoryActionC2SPacket(SongPlayer.MC.player.getInventory().selectedSlot + 36, oldItemHeld);
            SongPlayer.MC.player.networkHandler.sendPacket(packet);
            PlayerInventory inventory = SongPlayer.MC.player.getInventory();
            SongPlayer.MC.player.getInventory().main.set(inventory.selectedSlot, oldItemHeld.copy());
            SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
            oldItemHeld = null;
        }
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
            sendGamemodeCommand(SongPlayer.survivalCommand);
        }
    }

    private final String[] instrumentNames = {"harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling"};
    private void holdNoteblock(int id) {
        PlayerInventory inventory = SongPlayer.MC.player.getInventory();
        if (oldItemHeld == null) {
            oldItemHeld = inventory.getMainHandStack();
        }
        bandaidpatch = id;
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
    private void holdBlock(ItemStack item) {
        PlayerInventory inventory = SongPlayer.MC.player.getInventory();
        if (oldItemHeld == null) {
            oldItemHeld = inventory.getMainHandStack();
        }
        inventory.main.set(inventory.selectedSlot, item);
        SongPlayer.MC.interactionManager.clickCreativeStack(SongPlayer.MC.player.getStackInHand(Hand.MAIN_HAND), 36 + inventory.selectedSlot);
    }

    private void placeBlock(BlockPos bp) {
        double fx = Math.max(0.0, Math.min(1.0, (stage.position.getX() - bp.getX())));
        double fy = Math.max(0.0, Math.min(1.0, (stage.position.getY() + 0.0 - bp.getY())));
        double fz = Math.max(0.0, Math.min(1.0, (stage.position.getZ() - bp.getZ())));
        fx += bp.getX();
        fy += bp.getY();
        fz += bp.getZ();

        if (SongPlayer.rotate) {
            float[] pitchandyaw = Util.getAngleAtBlock(bp);
            PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.LookAndOnGround(pitchandyaw[1], pitchandyaw[0], true);
            SongPlayer.MC.player.networkHandler.sendPacket(packet);
        }
        if (SongPlayer.usePacketsOnlyWhilePlaying) {
            if (SongPlayer.switchGamemode) {
                SongPlayer.MC.player.getWorld().playSound(bp.getX(), bp.getY(), bp.getZ(), Blocks.NOTE_BLOCK.getSoundGroup(Blocks.NOTE_BLOCK.getDefaultState()).getPlaceSound(), SoundCategory.BLOCKS, 1f, 0.8f, true);
                SongPlayer.MC.player.getWorld().setBlockState(bp, Block.getStateFromRawId(Block.getRawIdFromState(Blocks.NOTE_BLOCK.getDefaultState()) + bandaidpatch));
            }
            PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(SongPlayer.MC.player.getActiveHand(), new BlockHitResult(new Vec3d(fx, fy, fz), Direction.DOWN, bp, false), 0);
            SongPlayer.MC.getNetworkHandler().sendPacket(packet);
        } else {
            SongPlayer.MC.interactionManager.interactBlock(SongPlayer.MC.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(fx, fy, fz), Direction.DOWN, bp, false));
        }
        if (SongPlayer.swing) {
            Util.swingHand();
        }
    }
    private void attackBlock(BlockPos bp) {
        ClientPlayerEntity player = SongPlayer.MC.player;
        if (SongPlayer.MC.world.getBlockState(bp).getBlock() == Blocks.AIR) {
            return;
        }
        if (SongPlayer.rotate) {
            float[] pitchandyaw = Util.getAngleAtBlock(bp);
            PlayerMoveC2SPacket packet = new PlayerMoveC2SPacket.LookAndOnGround(pitchandyaw[1], pitchandyaw[0], true);
            player.networkHandler.sendPacket(packet);
        }
        if (SongPlayer.usePacketsOnlyWhilePlaying) {
            if (SongHandler.getInstance().building && SongPlayer.switchGamemode) {
                SongPlayer.MC.world.playSound(SongPlayer.MC.player, bp, SongPlayer.MC.world.getBlockState(bp).getBlock().getSoundGroup(SongPlayer.MC.world.getBlockState(bp)).getBreakSound(), SoundCategory.BLOCKS, 1f, 0.8f);
                SongPlayer.MC.world.setBlockState(bp, Blocks.AIR.getDefaultState());
            }
            PlayerActionC2SPacket attack = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, bp, Direction.DOWN);
            PlayerActionC2SPacket stopattack = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, bp, Direction.DOWN);
            SongPlayer.MC.getNetworkHandler().sendPacket(attack);
            SongPlayer.MC.getNetworkHandler().sendPacket(stopattack);
        } else {
            SongPlayer.MC.interactionManager.attackBlock(bp, Direction.DOWN);
        }
        if (SongPlayer.swing) {
            Util.swingHand();
        }
    }
    private void stopAttack() {
        if (!SongPlayer.usePacketsOnlyWhilePlaying) {
            SongPlayer.MC.interactionManager.cancelBlockBreaking();
        }
    }
}