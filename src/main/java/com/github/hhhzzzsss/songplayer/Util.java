package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.config.ModProperties;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import com.github.hhhzzzsss.songplayer.song.Note;
import com.github.hhhzzzsss.songplayer.song.Song;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.CommandBlockBlockEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateCommandBlockC2SPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.*;

public class Util {
    public static String currentPlaylist = "";
    public static int playlistIndex = 0;
    public static int lastSwingPacket = 0; //there's probably a better way to do this
    public static int lagBackCounter = 0;
    public static boolean loopPlaylist = false;
    public static boolean cleaningup = false;
    public static String playlistOrder = "addedfirst";
    public static ArrayList<String> playlistSongs = new ArrayList<>();
    public static ArrayList<BlockPos> availableCommandBlocks = new ArrayList<>();
    private static int commandBlockIndex = 0;
    public static long playcooldown = Calendar.getInstance().getTime().getTime();
    public static long confirmCommand = 0;
    public static String lastTimeExecuted = "";
    public static float pitch = 0;
    public static float yaw = 0;
    public static float lastPitch = 0;
    public static float lastYaw = 0;
    public static double playerPosX = 0;
    public static double playerPosZ = 0;
    private static byte catchTry = 0;
    public static int recordednotesfailed = 0;
    public static String recordName = "";
    public static ArrayList<String> recordedNotes = new ArrayList<>();
    public static HashMap<BlockPos, BlockState> cleanupstage = new HashMap<>();

    public static void advancePlaylist() {
        if (Util.currentPlaylist.isEmpty()) {
            return;
        }
        Util.playlistIndex += 1;
        if (Util.playlistIndex >= Util.playlistSongs.size()) {
            if (Util.loopPlaylist) {
                Util.playlistIndex = 0;
            } else {
                SongPlayer.addChatMessage("§6Done playing playlist §3" + Util.currentPlaylist);
                SongHandler.getInstance().cleanup(true);
                return;
            }
        }
        int bruh = Util.playlistIndex + 1;
        SongPlayer.addChatMessage("§6Loading §3" + Util.playlistSongs.get(Util.playlistIndex) + "§6 from playlist §3" + Util.currentPlaylist + "§6 | §9" + bruh + "/" + Util.playlistSongs.size());
        SongHandler.getInstance().loadSong(Util.playlistSongs.get(Util.playlistIndex), new File("SongPlayer/playlists/" + Util.currentPlaylist));
    }
    public static void swingHand() {
        if (SongPlayer.usePacketsOnlyWhilePlaying) {
            if (lastSwingPacket < 2) {
                return;
            }
            lastSwingPacket = 0;
            SongPlayer.MC.getNetworkHandler().sendPacket(new HandSwingC2SPacket(SongPlayer.MC.player.getActiveHand()));
        } else {
            SongPlayer.MC.player.swingHand(SongPlayer.MC.player.getActiveHand());
        }
    }
    public static final float[] getAngleAtBlock(BlockPos to) {
        BlockPos playerPos = Stage.position;
        double dx = to.getX() - playerPos.getX();
        double dy = to.getY() - (playerPos.getY() + 1);
        double dz = to.getZ() - playerPos.getZ();
        //double distance = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) + Math.pow(dz, 2));
        float yaw = (float) ((Math.atan2(dz, dx) * 180 / PI) - 90);//Math.atan2(dz, dx);
        float pitch = (float) (Math.atan2(dy, Math.sqrt(Math.pow(dx, 2) + Math.pow(dz, 2))) * 180 / PI);//Math.atan2(Math.sqrt(dz * dz + dx * dx), dy) + Math.PI;
        Util.pitch = -pitch;
        Util.yaw = yaw;
        return (new float[] {-pitch, yaw});
    }
    public static void updateStageLocationToPlayer() {
        if (SongHandler.getInstance().stage == null) {
            return;
        }
        Stage.position = SongPlayer.MC.player.getBlockPos();
        SongHandler.getInstance().stage.checkBuildStatus(SongHandler.getInstance().currentSong);
    }
    public static String formatTime(long milliseconds) {
        long temp = abs(milliseconds);
        temp /= 1000;
        long seconds = temp % 60;
        temp /= 60;
        long minutes = temp % 60;
        temp /= 60;
        long hours = temp;
        StringBuilder sb = new StringBuilder();
        if (milliseconds < 0) {
            sb.append("-");
        }
        if (hours > 0) {
            sb.append(String.format("%d:", hours));
            sb.append(String.format("%02d:", minutes));
        } else {
            sb.append(String.format("%d:", minutes));
        }
        sb.append(String.format("%02d", seconds));
        return sb.toString();
    }
    public static Pattern timePattern = Pattern.compile("(?:(\\d+):)?(\\d+):(\\d+)");
    public static long parseTime(String timeStr) throws IOException {
        Matcher matcher = timePattern.matcher(timeStr);
        if (matcher.matches()) {
            long time = 0;
            String hourString = matcher.group(1);
            String minuteString = matcher.group(2);
            String secondString = matcher.group(3);
            if (hourString != null) {
                time += Integer.parseInt(hourString) * 60 * 60 * 1000;
            }
            time += Integer.parseInt(minuteString) * 60 * 1000;
            time += Double.parseDouble(secondString) * 1000.0;
            return time;
        } else {
            throw new IOException("Invalid time pattern");
        }
    }
    public static void disableFlightIfNeeded() {
        if (SongPlayer.MC.interactionManager.getCurrentGameMode() == GameMode.CREATIVE || SongPlayer.MC.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR || !SongPlayer.useNoteblocksWhilePlaying) {
            return;
        }
        ClientPlayerEntity player = SongPlayer.MC.player;
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
    }
    public static void enableFlightIfNeeded() {
        if (!SongPlayer.useNoteblocksWhilePlaying) {
            return;
        }
        if (SongHandler.getInstance().currentSong == null) {
            return;
        }
        ClientPlayerEntity player = SongPlayer.MC.player;
        player.getAbilities().allowFlying = true;
        player.getAbilities().flying = true;
    }
    public static void pauseSongIfNeeded() {
        if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
            SongPlayer.addChatMessage("§6No song is currently playing");
            Util.currentPlaylist = "";
            Util.playlistSongs.clear();
            return;
        }
        if (SongHandler.getInstance().paused) {
            return;
        }
        if (SongPlayer.showFakePlayer || SongPlayer.fakePlayer != null) { //fake player is showing
            SongPlayer.removeFakePlayer();
        }
        SongHandler.getInstance().stage.movePlayerToStagePosition();
        disableFlightIfNeeded();

        SongHandler.getInstance().paused = true;
        SongPlayer.addChatMessage("§6Song has been paused");
        //pause the song
    }
    public static void resumeSongIfNeeded() {
        if (SongHandler.getInstance().currentSong == null && SongHandler.getInstance().songQueue.isEmpty()) {
            SongPlayer.addChatMessage("§6No song is currently playing");
            currentPlaylist = "";
            playlistSongs.clear();
            return;
        }
        if (!SongHandler.getInstance().paused) {
            SongPlayer.addChatMessage("§6Song was never paused");
            return;
        }
        Stage stage = SongHandler.getInstance().stage;
        if (stage == null) {
            stage = new Stage();
        }

        if (stage != null && SongPlayer.useNoteblocksWhilePlaying) {
            updateStageLocationToPlayer();
            stage.movePlayerToStagePosition();
            enableFlightIfNeeded();
            if (!hasEnoughNoteblocks()) {
                return;
            }
            if (SongPlayer.showFakePlayer) { //check if fakeplayer is enabled. if it is, spawn the fakeplayer to the stage
                if (SongPlayer.fakePlayer == null) {
                    SongPlayer.fakePlayer = new FakePlayerEntity();
                }
                SongPlayer.fakePlayer.copyStagePosAndPlayerLook();
            }

            stage.checkBuildStatus(SongHandler.getInstance().currentSong);
            if (SongPlayer.switchGamemode) {
                if (stage.nothingToBuild()) {
                    if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.SURVIVAL) {
                        SongPlayer.MC.getNetworkHandler().sendCommand(SongPlayer.survivalCommand);
                    }
                } else {
                    SongPlayer.addChatMessage("§6Building noteblocks");
                    SongHandler.getInstance().building = true;
                    if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
                        SongPlayer.MC.getNetworkHandler().sendCommand(SongPlayer.creativeCommand);
                    }
                }
            } else {
                if (stage.hasPitchModification() > 0) {
                    SongPlayer.addChatMessage("§6Tuning noteblocks");
                    SongHandler.getInstance().building = true;
                }
            }
        }
        SongHandler.getInstance().paused = false;
        SongPlayer.addChatMessage("§6Resumed playing");
    }
    public static boolean hasEnoughNoteblocks() {
        if (SongPlayer.switchGamemode || SongHandler.getInstance().stage.totalMissingNotes == 0) {
            return true;
        }
        if (SongPlayer.showFakePlayer && SongPlayer.fakePlayer != null) { //fake player is showing
            SongPlayer.removeFakePlayer();
        }
        if (SongPlayer.ignoreWarnings) {
            return true;
        }
        SongHandler.getInstance().stage.movePlayerToStagePosition();
        SongHandler.getInstance().paused = true;
        MutableText hasenough = Text.empty();
        MutableText needsmore = Text.empty();
        SongPlayer.addChatMessage("§cYou do not have enough noteblocks around to play this song.");
        disableFlightIfNeeded();
        //%s string : %f float : %b Object : %o int : %tDate
        int have;
        int need;
        for (int i = 0; i < SongPlayer.instrumentList.length; i++) {
            have = 0;
            need = 0;

            for (int v : SongHandler.getInstance().stage.noteblockPositions.keySet()) {
                if (v / 25 == i) {
                    have++;
                }
            }

            for (int in = 25 * i; in < 25 * (i + 1); in++) {
                if (SongHandler.getInstance().currentSong.requiredNotes[in]) {
                    need++;
                }
            }

            if (need > 0) {
                if (have >= need) {
                    hasenough.append(String.format("\n§6%s: §a%d§6 / §3%d", SongPlayer.instrumentList[i], have, need));
                } else {
                    needsmore.append(String.format("\n§6%s: §c%d§6 / §3%d", SongPlayer.instrumentList[i], have, need));
                }
            }
        }
        if (SongPlayer.requireExactInstruments) {
            needsmore.append("\n§6consider running §3" + SongPlayer.prefix + "toggle useExactInstrumentsOnly false");
        }
        SongPlayer.addChatMessage(hasenough.getString());
        SongPlayer.addChatMessage(needsmore.getString());
        return false;
    }
    public static void sendCommandWithCommandblocks(String command) {
        if (!SongPlayer.includeCommandBlocks && !SongPlayer.kiosk) {
            return;
        }
        ClientPlayNetworkHandler nh = SongPlayer.MC.getNetworkHandler();
        BlockPos from;
        BlockPos to;
        if (commandBlockIndex == 127) {
            from = availableCommandBlocks.get(127);
            to = availableCommandBlocks.get(0);
            int x = from.getX();
            int y = from.getY();
            int z = from.getZ();
            int dx = to.getX();
            int dy = to.getY();
            int dz = to.getZ();
            nh.sendPacket(new UpdateCommandBlockC2SPacket(from, "", CommandBlockBlockEntity.Type.REDSTONE, true, false, true));
            nh.sendPacket(new UpdateCommandBlockC2SPacket(from, String.format("fill %d %d %d %d %d %d minecraft:repeating_command_block{Command:\"say bruv this should not run\"}", x, y, z, dx, dy, dz), CommandBlockBlockEntity.Type.REDSTONE, true, false, true));
            commandBlockIndex += 1;
        }
        if (commandBlockIndex > 254) {
            commandBlockIndex = 0;
            from = availableCommandBlocks.get(128);
            to = availableCommandBlocks.get(availableCommandBlocks.size() - 1);
            int x = from.getX();
            int y = from.getY();
            int z = from.getZ();
            int dx = to.getX();
            int dy = to.getY();
            int dz = to.getZ();
            nh.sendPacket(new UpdateCommandBlockC2SPacket(to, "", CommandBlockBlockEntity.Type.REDSTONE, true, false, true));
            nh.sendPacket(new UpdateCommandBlockC2SPacket(to, String.format("fill %d %d %d %d %d %d minecraft:repeating_command_block{Command:\"say bruv this should not run\"}", x, y, z, dx, dy, dz), CommandBlockBlockEntity.Type.REDSTONE, true, false, true));
        }
        BlockPos pos = availableCommandBlocks.get(commandBlockIndex);
        commandBlockIndex += 1;
        nh.sendPacket(new UpdateCommandBlockC2SPacket(pos, command, CommandBlockBlockEntity.Type.REDSTONE, true, false, true));
    }
    public static void refillCommandBlocks() {
        if (!SongPlayer.includeCommandBlocks || !SongPlayer.kiosk) {
            return;
        }
        if (availableCommandBlocks.size() < 2) {
            return;
        }
        BlockPos from = availableCommandBlocks.get(0);
        BlockPos to = availableCommandBlocks.get(availableCommandBlocks.size() - 1);

        int x = from.getX();
        int y = from.getY();
        int z = from.getZ();
        int dx = to.getX();
        int dy = to.getY();
        int dz = to.getZ();
        SongPlayer.MC.getNetworkHandler().sendCommand(String.format("fill %d %d %d %d %d %d minecraft:repeating_command_block destroy", x, y, z, dx, dy, dz));
    }
    public static void relocateCommandBlocks() {
        if (!SongPlayer.includeCommandBlocks && !SongPlayer.kiosk) {
            return;
        }
    }
    public static void assignCommandBlocks(boolean reassign) {
        if (!SongPlayer.includeCommandBlocks && !SongPlayer.kiosk) {
            return;
        }
        if (!availableCommandBlocks.isEmpty() && !reassign) {
            return;
        }
        commandBlockIndex = 0;
        BlockPos playerpos = SongPlayer.MC.player.getBlockPos();
        int x = (playerpos.getX() / 16) * 16;
        int y = 255; //remember this shouldn't be hardcoded u noob
        int z = (playerpos.getZ() / 16) * 16;

        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                availableCommandBlocks.add(new BlockPos(x + dx, y, z + dz));
            }
        }

        SongPlayer.MC.getNetworkHandler().sendCommand(String.format("fill %d %d %d %d %d %d minecraft:repeating_command_block destroy", x, y, z, x + 15, y, z + 15));
    }
    public static void broadcastMessage(String message, @Nullable String player) {
        if (!SongPlayer.kiosk) return;
        sendCommandWithCommandblocks("tellraw " + ((player == null) ? ("@a[name=!" + SongPlayer.MC.getGameProfile().getName() + "]") : player) + " " + message);
    }
    public static void cleanupstage() {
        System.out.println("cleanup called");
        if (!SongPlayer.switchGamemode || cleaningup) {
            return;
        }
        if (!SongPlayer.disablecommandcreative) {
            if (SongPlayer.MC.interactionManager.getCurrentGameMode() != GameMode.CREATIVE) {
                SongPlayer.MC.getNetworkHandler().sendCommand(SongPlayer.creativeCommand);
            }
        }
        System.out.println("cleanup running");
        cleaningup = true;
        SongHandler.getInstance().building = true;
    }
    public static void addRecordedNote(String instrument, float pitch) {
        //there has GOT to be a better and more efficient way of doing this lol
        //Maybe I do need to learn more advanced java lol this is a mess
        int indexI = 0;
        int indexS = -1;
        for (String s : SongPlayer.instrumentList) {
            if (instrument.equalsIgnoreCase("block.note_block." + s.replace(' ', '_'))) {
                break;
            }
            indexI++;
        }
        float lowest = 2f;
        for (int i = 0; i < SongPlayer.pitchGlobal.length; i++) {
            if (Math.abs(SongPlayer.pitchGlobal[i] - pitch) < lowest) {
                indexS = i;
                lowest = Math.abs(SongPlayer.pitchGlobal[i] - pitch);
            }
        }
        if (indexI >= SongPlayer.instrumentList.length || indexS < 0) {
            System.out.println("failed to save note: instrument = " + instrument + " - pitch = " + pitch);
            recordednotesfailed++;
            return;
        }
        recordedNotes.add(SongPlayer.recordingtick + ":" + indexS + ":" + indexI);
    }
    public static void updateValuesToConfig() {
        try {
            SongPlayer.creativeCommand = ModProperties.getInstance().getConfig().getProperty("creativeCommand", "gamemode creative");
            SongPlayer.survivalCommand = ModProperties.getInstance().getConfig().getProperty("survivalCommand", "gamemode survival");
            SongPlayer.adventureCommand = ModProperties.getInstance().getConfig().getProperty("adventureCommand", "gamemode adventure");
            SongPlayer.spectatorCommand = ModProperties.getInstance().getConfig().getProperty("spectatorCommand", "gamemode spectator");
            SongPlayer.playSoundCommand = ModProperties.getInstance().getConfig().getProperty("playSoundCommand", "execute as @a[tag=!nomusic] at @s run playsound minecraft:block.note_block.{type} record @s ~ ~ ~ {volume} {pitch} 1");
            SongPlayer.stageType = ModProperties.getInstance().getConfig().getProperty("stageType", "default");
            SongPlayer.showProgressCommand = ModProperties.getInstance().getConfig().getProperty("showProgressCommand",
                    "title @a[tag=!nomusic] actionbar [" +
                            "{\"color\":\"gold\",\"text\":\"Now playing \"}," +
                            "{\"color\":\"blue\",\"text\":\"{MIDI}\"}," +
                            "{\"color\":\"gold\",\"text\":\" | \"}," +
                            "{\"color\":\"dark_aqua\",\"text\":\"{CurrentTime}/{SongTime}\"}]");
            SongPlayer.prefix = ModProperties.getInstance().getConfig().getProperty("prefix", "$");

            SongPlayer.rotate = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("rotate", String.valueOf(false)));
            SongPlayer.swing = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("swing", String.valueOf(false)));
            SongPlayer.parseVolume = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("useVolume", String.valueOf(true)));
            SongPlayer.useCommandsForPlaying = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("useCommandsForPlaying", String.valueOf(false)));
            SongPlayer.includeCommandBlocks = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("useCommandBlocks", String.valueOf(false)));
            SongPlayer.switchGamemode = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("switchGamemode", String.valueOf(true)));
            SongPlayer.useNoteblocksWhilePlaying = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("useNoteblocks", String.valueOf(true)));
            SongPlayer.usePacketsOnlyWhilePlaying = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("packetsWhilePlaying", String.valueOf(false)));
            SongPlayer.useFramesInsteadOfTicks = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("countByFrames", String.valueOf(false)));
            SongPlayer.requireExactInstruments = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("useAlternateInstruments", String.valueOf(true)));
            SongPlayer.disablecommandcreative = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("disablecommandcreative", String.valueOf(false)));
            SongPlayer.disablecommandsurvival = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("disablecommandsurvival", String.valueOf(false)));
            SongPlayer.disablecommandadventure = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("disablecommandadventure", String.valueOf(false)));
            SongPlayer.disablecommandspectator = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("disablecommandspectator", String.valueOf(false)));
            SongPlayer.disablecommandplaynote = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("disablecommandplaynote", String.valueOf(false)));
            SongPlayer.disablecommanddisplayprogress = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("disablecommanddisplayprogress", String.valueOf(false)));
            SongPlayer.cleanupstage = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("cleanupstage", String.valueOf(false)));
            SongPlayer.ignoreWarnings = Boolean.parseBoolean(ModProperties.getInstance().getConfig().getProperty("stopiferror", String.valueOf(false)));

            SongPlayer.ignoreNoteThreshold = Byte.parseByte(ModProperties.getInstance().getConfig().getProperty("noteVolumeThreshold", String.valueOf(0)));
            SongPlayer.buildDelay = Integer.parseInt(ModProperties.getInstance().getConfig().getProperty("buildDelay", String.valueOf(1)));

            if (SongPlayer.prefix.startsWith("/")) { //prevent prefix from starting with '/'
                ModProperties.getInstance().updateValue("prefix", "$");
                SongPlayer.prefix = "$";
            }
        } catch (NumberFormatException e) {
            ModProperties.getInstance().updateValue("noteVolumeThreshold", String.valueOf(0));
            SongPlayer.buildDelay = Integer.parseInt(ModProperties.getInstance().getConfig().getProperty("buildDelay", String.valueOf(1)));
            if (catchTry < 3) {
                catchTry++;
                updateValuesToConfig();
            } else {
                SongPlayer.addChatMessage("§cThere was an error when attempting to save your settings.");
                catchTry = 0;
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
            ModProperties.getInstance().reset();
            catchTry = 0;
            SongPlayer.addChatMessage("§cSongPlayer config file is corrupted, so everything got reset to defaults.");
        }
    }
}
