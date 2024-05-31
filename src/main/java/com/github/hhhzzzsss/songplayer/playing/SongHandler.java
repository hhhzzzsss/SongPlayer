package com.github.hhhzzzsss.songplayer.playing;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.FakePlayerEntity;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.mixin.ClientPlayerInteractionManagerAccessor;
import com.github.hhhzzzsss.songplayer.song.*;
import net.minecraft.block.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlockStateComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.state.property.Property;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

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
    public Playlist currentPlaylist = null;
    public Stage stage = null; // Only exists when playing
    public Stage lastStage = null; // Stays around even after playing
    public HashMap<BlockPos, BlockState> originalBlocks = new HashMap<>();
    public boolean building = false;
    public boolean cleaningUp = false;
    public boolean dirty = false;

    public boolean wasFlying = false;
    public GameMode originalGamemode = GameMode.CREATIVE;

    boolean playlistChecked = false;

    public void onUpdate(boolean tick) {
        if (!cleaningUp) {
            // Check current playlist and load song from it if necessary
            if (currentSong == null && currentPlaylist != null && currentPlaylist.loaded) {
                if (!playlistChecked) {
                    playlistChecked = true;
                    if (currentPlaylist.songsFailedToLoad.size() > 0) {
                        SongPlayer.addChatMessage("§cFailed to load the following songs from the playlist: §4" + String.join(" ", currentPlaylist.songsFailedToLoad));
                    }
                }
                Song nextSong = currentPlaylist.getNext();
                if (currentPlaylist.songs.size() == 0) {
                    SongPlayer.addChatMessage("§cPlaylist has no playable songs");
                    currentPlaylist = null;
                } else if (nextSong == null) {
                    SongPlayer.addChatMessage("§6Playlist has finished playing");
                    currentPlaylist = null;
                } else {
                    nextSong.reset();
                    setSong(nextSong);
                }
            }

            // Check queue and load song from it if necessary
            if (currentSong == null && currentPlaylist == null && songQueue.size() > 0) {
                setSong(songQueue.poll());
            }

            // Check if loader thread is finished and handle accordingly
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
        }

        // Run cached command if timeout reached
        checkCommandCache();

        // If either playing or doing cleanup
        if (cleaningUp || currentSong != null) {
            // Handle creating/removing fake player depending on settings
            if (Config.getConfig().showFakePlayer && SongPlayer.fakePlayer == null) {
                SongPlayer.fakePlayer = new FakePlayerEntity();
                SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
            }
            if (!Config.getConfig().showFakePlayer && SongPlayer.fakePlayer != null) {
                SongPlayer.removeFakePlayer();
            }
            if (SongPlayer.fakePlayer != null) {
                SongPlayer.fakePlayer.getInventory().clone(SongPlayer.MC.player.getInventory());
            }

            // Maintain flying status
            wasFlying = SongPlayer.MC.player.getAbilities().flying;
        }

        // Check if doing cleanup
        if (cleaningUp) {
            if (tick) {
                // Maintain flying status
                wasFlying = SongPlayer.MC.player.getAbilities().flying;

                handleCleanup();
            }
        }
        // Check if song is playing
        else if (currentSong != null) {
            // This should never happen, but I left this check in just in case.
            if (stage == null) {
                SongPlayer.addChatMessage("§cStage is null! This should not happen!");
                reset();
                return;
            }

            // Run building or playing tick depending on state
            if (building) {
                if (tick) {
                    handleBuilding();
                }
            } else {
                handlePlaying(tick);
            }
        }
        // Otherwise, handle cleanup if necessary
        else {
            if (dirty) {
                if (Config.getConfig().autoCleanup && originalBlocks.size() != 0 && !Config.getConfig().survivalOnly) {
                    partialResetAndCleanup();
                } else {
                    restoreStateAndReset();
                }
            }
            else {
                // When doing nothing else, record original gamemode
                originalGamemode = SongPlayer.MC.interactionManager.getCurrentGameMode();
            }
        }
    }

    public void loadSong(String location) {
        if (loaderThread != null) {
            SongPlayer.addChatMessage("§cAlready loading a song, cannot load another");
        }
        else if (currentPlaylist != null) {
            SongPlayer.addChatMessage("§cCannot load a song while a playlist is playing");
        }
        else {
            try {
                loaderThread = new SongLoaderThread(location);
                SongPlayer.addChatMessage("§6Loading §3" + location);
                loaderThread.start();
            } catch (IOException e) {
                SongPlayer.addChatMessage("§cFailed to load song: §4" + e.getMessage());
            }
        }
    }

    public void loadSong(SongLoaderThread thread) {
        if (loaderThread != null) {
            SongPlayer.addChatMessage("§cAlready loading a song, cannot load another");
        }
        else if (currentPlaylist != null) {
            SongPlayer.addChatMessage("§cCannot load a song while a playlist is playing");
        }
        else {
            loaderThread = thread;
        }
    }

    // Sets currentSong and sets everything up for building
    public void setSong(Song song) {
        dirty = true;
        currentSong = song;
        building = true;
        if (!Config.getConfig().survivalOnly) setCreativeIfNeeded();
        if (Config.getConfig().doAnnouncement) {
            sendMessage(Config.getConfig().announcementMessage.replaceAll("\\[name\\]", song.name));
        }
        prepareStage();
        if (!Config.getConfig().survivalOnly) getAndSaveBuildSlot();
        SongPlayer.addChatMessage("§6Building noteblocks");
    }

    private void queueSong(Song song) {
        songQueue.add(song);
        SongPlayer.addChatMessage("§6Added song to queue: §3" + song.name);
    }

    public void setPlaylist(Path playlist) {
        if (loaderThread != null || currentSong != null || !songQueue.isEmpty()) {
            SongPlayer.addChatMessage("§cCannot start playing a playlist while something else is playing");
        }
        else {
            currentPlaylist = new Playlist(playlist, Config.getConfig().loopPlaylists, Config.getConfig().shufflePlaylists);
            playlistChecked = false;
        }
    }

    public void setPlaylistLoop(boolean loop) {
        if (currentPlaylist != null) {
            currentPlaylist.setLoop(loop);
        }
    }

    public void setPlaylistShuffle(boolean shuffle) {
        if (currentPlaylist != null) {
            currentPlaylist.setShuffle(shuffle);
        }
    }

    public void startCleanup() {
        dirty = true;
        cleaningUp = true;
        setCreativeIfNeeded();
        getAndSaveBuildSlot();
        lastStage.sendMovementPacketToStagePosition();
    }

    // Runs every tick
    private int buildStartDelay = 0;
    private int buildEndDelay = 0;
    private int buildCooldown = 0;
    private int buildSlot = -1;
    private ItemStack prevHeldItem = null;
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
        if (!Config.getConfig().survivalOnly && SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            return;
        }

        if (stage.nothingToBuild()) { // If there's nothing to build, wait for end delay then check build status
            if (buildEndDelay > 0) { // Wait for end delay
                buildEndDelay--;
                return;
            } else { // Check build status when end delay is over
                if (!Config.getConfig().survivalOnly) {
                    stage.checkBuildStatus(currentSong);
                    recordStageBlocks();
                } else {
                    try {
                        stage.checkSurvivalBuildStatus(currentSong);
                    } catch (Stage.NotEnoughInstrumentsException e) {
                        e.giveInstrumentSummary();
                        reset();
                        return;
                    }
                }
                stage.sendMovementPacketToStagePosition();
            }
        }

        if (stage.nothingToBuild()) { // If there's still nothing to build after checking build status, switch to playing
            if (!Config.getConfig().survivalOnly) restoreBuildSlot();
            building = false;
            stage.sendMovementPacketToStagePosition();
            SongPlayer.addChatMessage("§6Now playing §3" + currentSong.name);
        }

        if (!Config.getConfig().survivalOnly) { // Regular mode
            if (!stage.requiredBreaks.isEmpty()) {
                for (int i = 0; i < 5; i++) {
                    if (stage.requiredBreaks.isEmpty()) break;
                    BlockPos bp = stage.requiredBreaks.poll();
                    attackBlock(bp);
                }
                buildEndDelay = 20;
            } else if (!stage.missingNotes.isEmpty()) {
                int desiredNoteId = stage.missingNotes.pollFirst();
                BlockPos bp = stage.noteblockPositions.get(desiredNoteId);
                if (bp == null) {
                    return;
                }
                int blockId = Block.getRawIdFromState(world.getBlockState(bp));
                int currentNoteId = (blockId - SongPlayer.NOTEBLOCK_BASE_ID) / 2;
                if (currentNoteId != desiredNoteId) {
                    holdNoteblock(desiredNoteId, buildSlot);
                    if (blockId != 0) {
                        attackBlock(bp);
                    }
                    placeBlock(bp);
                }
                buildCooldown = 0; // No cooldown, so it places a block every tick
                buildEndDelay = 20;
            }
        } else { // Survival only mode
            if (!stage.requiredClicks.isEmpty()) {
                BlockPos bp = stage.requiredClicks.pollFirst();
                if (SongPlayer.MC.world.getBlockState(bp).getBlock() == Blocks.NOTE_BLOCK) {
                    placeBlock(bp);
                }
                buildEndDelay = 20;
            }
        }
    }
    private void setBuildProgressDisplay() {
        MutableText buildText = Text.empty()
                .append(Text.literal("Building noteblocks | " ).formatted(Formatting.GOLD))
                .append(Text.literal((stage.totalMissingNotes - stage.missingNotes.size()) + "/" + stage.totalMissingNotes).formatted(Formatting.DARK_AQUA));
        MutableText playlistText = Text.empty();
        if (currentPlaylist != null && currentPlaylist.loaded) {
            playlistText = playlistText.append(Text.literal("Playlist: ").formatted(Formatting.GOLD))
                    .append(Text.literal(currentPlaylist.name).formatted(Formatting.BLUE))
                    .append(Text.literal(" | ").formatted(Formatting.GOLD))
                    .append(Text.literal(String.format(" (%s/%s)", currentPlaylist.songNumber, currentPlaylist.songs.size())).formatted(Formatting.DARK_AQUA));
            if (currentPlaylist.loop) {
                playlistText.append(Text.literal(" | Looping").formatted(Formatting.GOLD));
            }
            if (currentPlaylist.shuffle) {
                playlistText.append(Text.literal(" | Shuffled").formatted(Formatting.GOLD));
            }
        }
        ProgressDisplay.getInstance().setText(buildText, playlistText);
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
                if (!Config.getConfig().survivalOnly) {
                    stage.checkBuildStatus(currentSong);
                    recordStageBlocks();
                } else {
                    try {
                        stage.checkSurvivalBuildStatus(currentSong);
                    } catch (Stage.NotEnoughInstrumentsException e) {
                        SongPlayer.addChatMessage("§6Stopped because stage is missing instruments required for song.");
                        reset();
                        return;
                    }
                }
            }
            if (!stage.nothingToBuild()) { // Switch to building
                building = true;
                if (!Config.getConfig().survivalOnly) setCreativeIfNeeded();
                stage.sendMovementPacketToStagePosition();
                currentSong.pause();
                buildStartDelay = 20;
                System.out.println("Total missing notes: " + stage.missingNotes.size());
                for (int note : stage.missingNotes) {
                    int pitch = note % 25;
                    int instrumentId = note / 25;
                    System.out.println("Missing note: " + Instrument.getInstrumentFromId(instrumentId).name() + ":" + pitch);
                }
                if (!Config.getConfig().survivalOnly) getAndSaveBuildSlot();
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
    private void setPlayProgressDisplay() {
        long currentTime = Math.min(currentSong.time, currentSong.length);
        long totalTime = currentSong.length;
        MutableText songText = Text.empty()
                .append(Text.literal("Now playing: ").formatted(Formatting.GOLD))
                .append(Text.literal(currentSong.name).formatted(Formatting.BLUE))
                .append(Text.literal(" | ").formatted(Formatting.GOLD))
                .append(Text.literal(String.format("%s/%s", Util.formatTime(currentTime), Util.formatTime(totalTime))).formatted(Formatting.DARK_AQUA));
        if (currentSong.looping) {
            if (currentSong.loopCount > 0) {
                songText.append(Text.literal(String.format(" | Loop (%d/%d)", currentSong.currentLoop, currentSong.loopCount)).formatted(Formatting.GOLD));
            } else {
                songText.append(Text.literal(" | Looping enabled").formatted(Formatting.GOLD));
            }
        }
        MutableText playlistText = Text.empty();
        if (currentPlaylist != null && currentPlaylist.loaded) {
            playlistText = playlistText.append(Text.literal("Playlist: ").formatted(Formatting.GOLD))
                    .append(Text.literal(currentPlaylist.name).formatted(Formatting.BLUE))
                    .append(Text.literal(" | ").formatted(Formatting.GOLD))
                    .append(Text.literal(String.format(" (%s/%s)", currentPlaylist.songNumber, currentPlaylist.songs.size())).formatted(Formatting.DARK_AQUA));
            if (currentPlaylist.loop) {
                playlistText.append(Text.literal(" | Looping").formatted(Formatting.GOLD));
            }
            if (currentPlaylist.shuffle) {
                playlistText.append(Text.literal(" | Shuffled").formatted(Formatting.GOLD));
            }
        }
        ProgressDisplay.getInstance().setText(songText, playlistText);
    }

    // Runs every tick
    private int cleanupTotalBlocksToPlace = 0;
    private LinkedList<BlockPos> cleanupBreakList = new LinkedList<>();
    private LinkedList<BlockPos> cleanupPlaceList = new LinkedList<>();
    private ArrayList<BlockPos> cleanupUnplaceableBlocks = new ArrayList<>();
    private void handleCleanup() {
        setCleanupProgressDisplay();

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

        if (cleanupBreakList.isEmpty() && cleanupPlaceList.isEmpty()) {
            if (buildEndDelay > 0) {
                buildEndDelay--;
                return;
            } else {
                checkCleanupStatus();
                lastStage.sendMovementPacketToStagePosition();
            }
        }

        if (!cleanupBreakList.isEmpty()) {
            for (int i=0; i<5; i++) {
                if (cleanupBreakList.isEmpty()) break;
                BlockPos bp = cleanupBreakList.poll();
                attackBlock(bp);
            }
            buildEndDelay = 20;
        } else if (!cleanupPlaceList.isEmpty()) {
            BlockPos bp = cleanupPlaceList.pollFirst();
            BlockState actualBlockState = world.getBlockState(bp);
            BlockState desiredBlockState = originalBlocks.get(bp);
            if (actualBlockState != desiredBlockState) {
                holdBlock(desiredBlockState, buildSlot);
                if (!actualBlockState.isAir() && !actualBlockState.isLiquid()) {
                    attackBlock(bp);
                }
                placeBlock(bp);
            }
            buildCooldown = 0; // No cooldown, so it places a block every tick
            buildEndDelay = 20;
        } else {
            originalBlocks.clear();
            cleaningUp = false;
            SongPlayer.addChatMessage("§6Finished restoring original blocks");
            if (!cleanupUnplaceableBlocks.isEmpty()) {
                SongPlayer.addChatMessage(String.format("§3%d §6blocks could not be restored", cleanupUnplaceableBlocks.size()));
            }
        }
    }
    private void checkCleanupStatus() {
        ClientWorld world = SongPlayer.MC.world;

        cleanupPlaceList.clear();
        cleanupBreakList.clear();
        cleanupUnplaceableBlocks.clear();

        for (BlockPos bp : originalBlocks.keySet()) {
            BlockState actualBlockState = world.getBlockState(bp);
            BlockState desiredBlockState = originalBlocks.get(bp);
            if (actualBlockState != desiredBlockState) {
                if (isPlaceable(desiredBlockState)) {
                    cleanupPlaceList.add(bp);
                }
                if (!actualBlockState.isAir() && !actualBlockState.isLiquid()) {
                    cleanupBreakList.add(bp);
                }
            }
        }

        cleanupBreakList = cleanupBreakList.stream()
                .sorted((a, b) -> {
                    // First sort by gravity
                    boolean a_grav = SongPlayer.MC.world.getBlockState(a).getBlock() instanceof FallingBlock;
                    boolean b_grav = SongPlayer.MC.world.getBlockState(b).getBlock() instanceof FallingBlock;
                    if (a_grav && !b_grav) {
                        return 1;
                    } else if (!a_grav && b_grav) {
                        return -1;
                    }
                    // If there's gravity, sort by y coordinate
                    if (a_grav && b_grav) {
                        if (a.getY() < b.getY()) {
                            return -1;
                        } else if (a.getY() > b.getY()) {
                            return 1;
                        }
                    }
                    // Then sort by distance
                    int a_dx = a.getX() - lastStage.position.getX();
                    int a_dy = a.getY() - lastStage.position.getY();
                    int a_dz = a.getZ() - lastStage.position.getZ();
                    int b_dx = b.getX() - lastStage.position.getX();
                    int b_dy = b.getY() - lastStage.position.getY();
                    int b_dz = b.getZ() - lastStage.position.getZ();
                    int a_dist = a_dx*a_dx + a_dy*a_dy + a_dz*a_dz;
                    int b_dist = b_dx*b_dx + b_dy*b_dy + b_dz*b_dz;
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

        cleanupPlaceList = cleanupPlaceList.stream()
                .sorted((a, b) -> {
                    // First sort by gravity
                    boolean a_grav = originalBlocks.get(a).getBlock() instanceof FallingBlock;
                    boolean b_grav = originalBlocks.get(b).getBlock() instanceof FallingBlock;
                    if (a_grav && !b_grav) {
                        return -1;
                    } else if (!a_grav && b_grav) {
                        return 1;
                    }
                    // If there's gravity, sort by y coordinate
                    if (a_grav && b_grav) {
                        if (a.getY() < b.getY()) {
                            return 1;
                        } else if (a.getY() > b.getY()) {
                            return -1;
                        }
                    }
                    // Then sort by distance
                    int a_dx = a.getX() - lastStage.position.getX();
                    int a_dy = a.getY() - lastStage.position.getY();
                    int a_dz = a.getZ() - lastStage.position.getZ();
                    int b_dx = b.getX() - lastStage.position.getX();
                    int b_dy = b.getY() - lastStage.position.getY();
                    int b_dz = b.getZ() - lastStage.position.getZ();
                    int a_dist = a_dx*a_dx + a_dy*a_dy + a_dz*a_dz;
                    int b_dist = b_dx*b_dx + b_dy*b_dy + b_dz*b_dz;
                    if (a_dist < b_dist) {
                        return -1;
                    } else if (a_dist > b_dist) {
                        return 1;
                    }
                    // Finally sort by angle
                    double a_angle = Math.atan2(a_dz, a_dx);
                    double b_angle = Math.atan2(b_dz, b_dx);
                    if (a_angle < b_angle) {
                        return 1;
                    } else if (a_angle > b_angle) {
                        return -1;
                    } else {
                        return 0;
                    }
                })
                .collect(Collectors.toCollection(LinkedList::new));

        cleanupPlaceList = cleanupPlaceList.reversed();
        cleanupTotalBlocksToPlace = cleanupPlaceList.size();

        boolean noNecessaryBreaks = cleanupBreakList.stream().allMatch(
                bp -> world.getBlockState(bp).getBlock().getDefaultState().equals(originalBlocks.get(bp).getBlock().getDefaultState())
        );
        boolean noNecessaryPlacements = cleanupPlaceList.stream().allMatch(
                bp -> bp.equals(lastStage.position)
                || bp.equals(lastStage.position.up())
                || world.getBlockState(bp).getBlock().getDefaultState().equals(originalBlocks.get(bp).getBlock().getDefaultState())
        );
        if (noNecessaryBreaks && noNecessaryPlacements) {
            cleanupUnplaceableBlocks.addAll(cleanupPlaceList);
            cleanupPlaceList.clear();
        }
    }
    private void setCleanupProgressDisplay() {
        MutableText buildText = Text.empty()
                .append(Text.literal("Rebuilding original blocks | " ).formatted(Formatting.GOLD))
                .append(Text.literal((cleanupTotalBlocksToPlace - cleanupPlaceList.size()) + "/" + cleanupTotalBlocksToPlace).formatted(Formatting.DARK_AQUA));
        ProgressDisplay.getInstance().setText(buildText, Text.empty());
    }

    // Resets all internal states like currentSong, and songQueue, which stops all actions
    public void reset() {
        currentSong = null;
        currentPlaylist = null;
        songQueue.clear();
        stage = null;
        buildSlot = -1;
        SongPlayer.removeFakePlayer();
        cleaningUp = false;
        dirty = false;
    }
    public void restoreStateAndReset() {
        if (lastStage != null) {
            lastStage.movePlayerToStagePosition();
        }
        if (originalGamemode != SongPlayer.MC.interactionManager.getCurrentGameMode() && !Config.getConfig().survivalOnly) {
            if (originalGamemode == GameMode.CREATIVE) {
                sendGamemodeCommand(Config.getConfig().creativeCommand);
            }
            else if (originalGamemode == GameMode.SURVIVAL) {
                sendGamemodeCommand(Config.getConfig().survivalCommand);
            }
        }
        if (SongPlayer.MC.player.getAbilities().allowFlying == false) {
            SongPlayer.MC.player.getAbilities().flying = false;
        }
        if (!Config.getConfig().survivalOnly) restoreBuildSlot();
        reset();
    }
    public void partialResetAndCleanup() {
        restoreBuildSlot();
        currentSong = null;
        currentPlaylist = null;
        songQueue.clear();
        stage = null;
        buildSlot = -1;
        startCleanup();
    }

    // Runs every frame when player is not ingame
    public void onNotIngame() {
        currentSong = null;
        currentPlaylist = null;
        songQueue.clear();
    }

    // Create stage if it doesn't exist and move the player to it
    private void prepareStage() {
        if (stage == null) {
            stage = new Stage();
            lastStage = stage;
            originalBlocks.clear();
            stage.movePlayerToStagePosition();
        }
        else {
            stage.sendMovementPacketToStagePosition();
        }
    }

    private long lastCommandTime = System.currentTimeMillis();
    private String cachedCommand = null;
    private String cachedMessage = null;
    private void sendGamemodeCommand(String command) {
        cachedCommand = command;
    }
    private void sendMessage(String message) {
        cachedMessage = message;
    }
    private void checkCommandCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= lastCommandTime + 1500 && cachedCommand != null) {
            SongPlayer.MC.getNetworkHandler().sendCommand(cachedCommand);
            cachedCommand = null;
            lastCommandTime = currentTime;
        }
        else if (currentTime >= lastCommandTime + 500 && cachedMessage != null) {
            if (cachedMessage.startsWith("/")) {
                SongPlayer.MC.getNetworkHandler().sendCommand(cachedMessage.substring(1));
            }
            else {
                SongPlayer.MC.getNetworkHandler().sendChatMessage(cachedMessage);
            }
            cachedMessage = null;
            lastCommandTime = currentTime;
        }
    }
    private void setCreativeIfNeeded() {
        cachedCommand = null;
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
            sendGamemodeCommand(Config.getConfig().creativeCommand);
        }
    }
    private void setSurvivalIfNeeded() {
        cachedCommand = null;
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
            sendGamemodeCommand(Config.getConfig().survivalCommand);
        }
    }

    private final String[] instrumentNames = {"harp", "basedrum", "snare", "hat", "bass", "flute", "bell", "guitar", "chime", "xylophone", "iron_xylophone", "cow_bell", "didgeridoo", "bit", "banjo", "pling"};
    private void holdNoteblock(int id, int slot) {
        PlayerInventory inventory = SongPlayer.MC.player.getInventory();
        inventory.selectedSlot = slot;
        ((ClientPlayerInteractionManagerAccessor) SongPlayer.MC.interactionManager).invokeSyncSelectedSlot();
        int instrument = id/25;
        int note = id%25;
        ItemStack noteblockStack = Items.NOTE_BLOCK.getDefaultStack();
        noteblockStack.set(DataComponentTypes.BLOCK_STATE, new BlockStateComponent(Map.of(
                "instrument", instrumentNames[instrument],
                "note", Integer.toString(note)
        )));
        inventory.main.set(slot, noteblockStack);
        SongPlayer.MC.interactionManager.clickCreativeStack(noteblockStack, 36 + slot);
    }
    private void holdBlock(BlockState bs, int slot) {
        PlayerInventory inventory = SongPlayer.MC.player.getInventory();
        inventory.selectedSlot = slot;
        ((ClientPlayerInteractionManagerAccessor) SongPlayer.MC.interactionManager).invokeSyncSelectedSlot();
        ItemStack stack = new ItemStack(bs.getBlock());
        Map<String, String> stateMap = new TreeMap<>();
        for (Map.Entry<Property<?>, Comparable<?>> entry : bs.getEntries().entrySet()) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();
            stateMap.put(property.getName(), net.minecraft.util.Util.getValueAsString(property, value));
        }
        stack.set(DataComponentTypes.BLOCK_STATE, new BlockStateComponent(stateMap));
        inventory.main.set(slot, stack);
        SongPlayer.MC.interactionManager.clickCreativeStack(stack, 36 + slot);
    }
    private void placeBlock(BlockPos bp) {
        double fx = Math.max(0.0, Math.min(1.0, (lastStage.position.getX() + 0.5 - bp.getX())));
        double fy = Math.max(0.0, Math.min(1.0, (lastStage.position.getY() + 0.0 - bp.getY())));
        double fz = Math.max(0.0, Math.min(1.0, (lastStage.position.getZ() + 0.5 - bp.getZ())));
        fx += bp.getX();
        fy += bp.getY();
        fz += bp.getZ();
        SongPlayer.MC.interactionManager.interactBlock(SongPlayer.MC.player, Hand.MAIN_HAND, new BlockHitResult(new Vec3d(fx, fy, fz), Direction.UP, bp, false));
        doMovements(fx, fy, fz);
    }
    private void attackBlock(BlockPos bp) {
        SongPlayer.MC.interactionManager.attackBlock(bp, Direction.UP);
        doMovements(bp.getX() + 0.5, bp.getY() + 0.5, bp.getZ() + 0.5);
    }
    private void stopAttack() {
        SongPlayer.MC.interactionManager.cancelBlockBreaking();
    }
    private void recordBlocks(Iterable<BlockPos> bpList) {
        for (BlockPos bp : bpList) {
            if (!originalBlocks.containsKey(bp)) {
                BlockState bs = SongPlayer.MC.world.getBlockState(bp);
                originalBlocks.put(bp, bs);
            }
        }
    }
    private void recordStageBlocks() {
        recordBlocks(stage.requiredBreaks);
        recordBlocks(stage.missingNotes
                .stream()
                .map(noteId -> stage.noteblockPositions.get(noteId))
                .filter(Objects::nonNull)
                .toList()
        );
    }
    private boolean isPlaceable(BlockState bs) {
        Map<Property<?>, Comparable<?>> entries = bs.getEntries();
        for (Map.Entry<Property<?>, Comparable<?>> entry : entries.entrySet()) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();
            String propertyName = property.getName();
            String valueName = net.minecraft.util.Util.getValueAsString(property, value);
            if (propertyName == "half" && valueName == "upper") {
                return false;
            }
        }
        Block block = bs.getBlock();
        if (bs.isAir() || bs.isLiquid()) {
            return false;
        } else if (block instanceof DoorBlock || block instanceof BedBlock) {
            return false;
        } else {
            return true;
        }
    }

    private void doMovements(double lookX, double lookY, double lookZ) {
        if (Config.getConfig().swing) {
            SongPlayer.MC.player.swingHand(Hand.MAIN_HAND);
            if (SongPlayer.fakePlayer != null) {
                SongPlayer.fakePlayer.swingHand(Hand.MAIN_HAND);
            }
        }
        if (Config.getConfig().rotate) {
            double d = lookX - (lastStage.position.getX() + 0.5);
            double e = lookY - (lastStage.position.getY() + SongPlayer.MC.player.getStandingEyeHeight());
            double f = lookZ - (lastStage.position.getZ() + 0.5);
            double g = Math.sqrt(d * d + f * f);
            float pitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875)));
            float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875) - 90.0f);
            if (SongPlayer.fakePlayer != null) {
                SongPlayer.fakePlayer.setPitch(pitch);
                SongPlayer.fakePlayer.setYaw(yaw);
                SongPlayer.fakePlayer.setHeadYaw(yaw);
            }
            SongPlayer.MC.player.networkHandler.getConnection().send(new PlayerMoveC2SPacket.Full(
                    lastStage.position.getX() + 0.5, lastStage.position.getY(), lastStage.position.getZ() + 0.5,
                    yaw, pitch,
                    true));
        }
    }

    private void getAndSaveBuildSlot() {
        buildSlot = SongPlayer.MC.player.getInventory().getSwappableHotbarSlot();
        prevHeldItem = SongPlayer.MC.player.getInventory().getStack(buildSlot);
    }
    private void restoreBuildSlot() {
        if (buildSlot != -1) {
            SongPlayer.MC.player.getInventory().setStack(buildSlot, prevHeldItem);
            SongPlayer.MC.interactionManager.clickCreativeStack(prevHeldItem, 36 + buildSlot);
            buildSlot = -1;
        }
    }

    public boolean isIdle() {
        return currentSong == null && currentPlaylist == null && songQueue.isEmpty() && cleaningUp == false;
    }
}