package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.Config;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {
    @Shadow
    private final ClientConnection connection;

    public ClientCommonNetworkHandlerMixin() {
        connection = null;
    }

    @Inject(at = @At("HEAD"), method = "sendPacket(Lnet/minecraft/network/packet/Packet;)V", cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        SongHandler songHandler = SongHandler.getInstance();
        Stage lastStage = songHandler.lastStage;

        if (!songHandler.isIdle() && packet instanceof PlayerMoveC2SPacket) {
            if (lastStage != null) {
                if (!Config.getConfig().rotate) { // Only copy player rotation if rotate is not enabled
                    connection.send(new PlayerMoveC2SPacket.Full(
                            lastStage.position.getX() + 0.5, lastStage.position.getY(), lastStage.position.getZ() + 0.5,
                            SongPlayer.MC.player.getYaw(), SongPlayer.MC.player.getPitch(),
                            true, false));
                    if (songHandler.fakePlayer != null) {
                        songHandler.fakePlayer.copyStagePosAndPlayerLook();
                    }
                }
            }
            ci.cancel(); // Default movement packet is always cancelled if song is playing
        }
        else if (packet instanceof PlayerInputC2SPacket) {
            // Update fakePlayer crouching
            PlayerInput input = ((PlayerInputC2SPacket) packet).input();
            if (songHandler.fakePlayer != null) {
                if (input.sneak()) {
                    songHandler.fakePlayer.setSneaking(true);
                    songHandler.fakePlayer.setPose(EntityPose.CROUCHING);
                }
                else {
                    songHandler.fakePlayer.setSneaking(false);
                    songHandler.fakePlayer.setPose(EntityPose.STANDING);
                }
            }
        }
    }
}
