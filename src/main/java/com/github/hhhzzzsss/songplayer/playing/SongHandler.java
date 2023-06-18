package com.github.hhhzzzsss.songplayer.playing;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.FakePlayerEntity;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.mixin.ClientPlayerInteractionManagerAccessor;
import com.github.hhhzzzsss.songplayer.song.*;
import net.minecraft.block.Block;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
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
    public Playlist currentPlaylist = null;
    public Stage stage = null;
    public boolean building = false;

    boolean playlistChecked = false;

    public void onUpdate(boolean tick) {
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
            }
            else if (nextSong == null) {
                SongPlayer.addChatMessage("§6Playlist has finished playing");
                currentPlaylist = null;
            }
            else {
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

        // Check if no song is playing and, if necessary, handle cleanup
        if (currentSong == null) {
            if (stage != null || SongPlayer.fakePlayer != null) {
                if (stage != null) {
                    stage.movePlayerToStagePosition();
                }
                cleanup();
            }
        }
        // Otherwise, handle song playing
        else {
            if (stage == null) {
                stage = new Stage();
                stage.movePlayerToStagePosition();
            }
            if (Config.getConfig().showFakePlayer && SongPlayer.fakePlayer == null) {
                SongPlayer.fakePlayer = new FakePlayerEntity();
                SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
            }
            if (!Config.getConfig().showFakePlayer && SongPlayer.fakePlayer != null) {
                SongPlayer.removeFakePlayer();
            }

            checkCommandCache();

            SongPlayer.MC.player.getAbilities().allowFlying = true;
            if (building) {
                if (tick) {
                    handleBuilding();
                }
            } else {
                handlePlaying(tick);
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
                SongPlayer.addChatMessage("§6Loading §3" + location + "");
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

    public void setSong(Song song) {
        currentSong = song;
        building = true;
        setCreativeIfNeeded();
        if (stage != null) {
            stage.movePlayerToStagePosition();
        }
        getAndSaveBuildSlot();
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

    // Runs every tick
    private int buildStartDelay = 0;
    private int buildEndDelay = 0;
    private int buildCooldown = 0;
    private int buildSlot = 0;
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
            buildEndDelay = 20;
        } else if (!stage.missingNotes.isEmpty()) {
            int desiredNoteId = stage.missingNotes.pollFirst();
            BlockPos bp = stage.noteblockPositions.get(desiredNoteId);
            if (bp == null) {
                return;
            }
            int blockId = Block.getRawIdFromState(world.getBlockState(bp));
            int currentNoteId = (blockId-SongPlayer.NOTEBLOCK_BASE_ID)/2;
            if (currentNoteId != desiredNoteId) {
                holdNoteblock(desiredNoteId, buildSlot);
                if (blockId != 0) {
                    attackBlock(bp);
                }
                placeBlock(bp);
            }
            buildCooldown = 0; // No cooldown, so it places a block every tick
            buildEndDelay = 20;
        } else { // Switch to playing
            restoreBuildSlot();
            building = false;
            setSurvivalIfNeeded();
            stage.movePlayerToStagePosition();
            SongPlayer.addChatMessage("§6Now playing §3" + currentSong.name);
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
                stage.checkBuildStatus(currentSong);
            }
            if (!stage.nothingToBuild()) { // Switch to building
                building = true;
                setCreativeIfNeeded();
                stage.movePlayerToStagePosition();
                currentSong.pause();
                buildStartDelay = 20;
                System.out.println("Total missing notes: " + stage.missingNotes.size());
                for (int note : stage.missingNotes) {
                    int pitch = note % 25;
                    int instrumentId = note / 25;
                    System.out.println("Missing note: " + Instrument.getInstrumentFromId(instrumentId).name() + ":" + pitch);
                }
                getAndSaveBuildSlot();
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

    public void cleanup() {
        currentSong = null;
        currentPlaylist = null;
        songQueue.clear();
        stage = null;
        SongPlayer.removeFakePlayer();
    }

    public void onNotIngame() {
        currentSong = null;
        currentPlaylist = null;
        songQueue.clear();
    }

    private long lastCommandTime = System.currentTimeMillis();
    private String cachedCommand = null;
    private void sendGamemodeCommand(String command) {
        cachedCommand = command;
    }
    private void checkCommandCache() {
        if (cachedCommand != null && System.currentTimeMillis() >= lastCommandTime + 1500) {
            SongPlayer.MC.getNetworkHandler().sendCommand(cachedCommand);
            cachedCommand = null;
            lastCommandTime = System.currentTimeMillis();
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
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", "minecraft:note_block");
        nbt.putByte("Count", (byte) 1);
        NbtCompound tag = new NbtCompound();
        NbtCompound bsTag = new NbtCompound();
        bsTag.putString("instrument", instrumentNames[instrument]);
        bsTag.putString("note", Integer.toString(note));
        tag.put("BlockStateTag", bsTag);
        nbt.put("tag", tag);
        ItemStack noteblockStack = ItemStack.fromNbt(nbt);
        inventory.main.set(slot, noteblockStack);
        SongPlayer.MC.interactionManager.clickCreativeStack(noteblockStack, 36 + slot);
    }
    private void placeBlock(BlockPos bp) {
        double fx = Math.max(0.0, Math.min(1.0, (stage.position.getX() + 0.5 - bp.getX())));
        double fy = Math.max(0.0, Math.min(1.0, (stage.position.getY() + 0.0 - bp.getY())));
        double fz = Math.max(0.0, Math.min(1.0, (stage.position.getZ() + 0.5 - bp.getZ())));
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

    private void doMovements(double lookX, double lookY, double lookZ) {
        if (Config.getConfig().swing) {
            SongPlayer.MC.player.swingHand(Hand.MAIN_HAND);
            if (SongPlayer.fakePlayer != null) {
                SongPlayer.fakePlayer.swingHand(Hand.MAIN_HAND);
            }
        }
        if (Config.getConfig().rotate) {
            double d = lookX - (stage.position.getX() + 0.5);
            double e = lookY - (stage.position.getY() + SongPlayer.MC.player.getStandingEyeHeight());
            double f = lookZ - (stage.position.getZ() + 0.5);
            double g = Math.sqrt(d * d + f * f);
            float pitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(e, g) * 57.2957763671875)));
            float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(f, d) * 57.2957763671875) - 90.0f);
            if (SongPlayer.fakePlayer != null) {
                SongPlayer.fakePlayer.setPitch(pitch);
                SongPlayer.fakePlayer.setYaw(yaw);
                SongPlayer.fakePlayer.setHeadYaw(yaw);
            }
            SongPlayer.MC.player.networkHandler.getConnection().send(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, true));
        }
    }

    private void getAndSaveBuildSlot() {
        buildSlot = SongPlayer.MC.player.getInventory().getSwappableHotbarSlot();
        prevHeldItem = SongPlayer.MC.player.getInventory().getStack(buildSlot);
        System.out.println(buildSlot);
        System.out.println(prevHeldItem.toString());
    }
    private void restoreBuildSlot() {
        SongPlayer.MC.player.getInventory().setStack(buildSlot, prevHeldItem);
        SongPlayer.MC.interactionManager.clickCreativeStack(prevHeldItem, 36 + buildSlot);
    }
}