package com.github.hhhzzzsss.songplayer;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.github.hhhzzzsss.songplayer.playing.Stage;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.SkinTextures;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class FakePlayerEntity extends OtherClientPlayerEntity {
    public static final UUID FAKE_PLAYER_UUID =
            UUID.nameUUIDFromBytes("songplayer:fake-player".getBytes(StandardCharsets.UTF_8));

    ClientPlayerEntity player = SongPlayer.MC.player;
    ClientWorld world = SongPlayer.MC.world;

    public FakePlayerEntity() {
        super(Objects.requireNonNull(SongPlayer.MC.world), getProfile());

        copyStagePosAndPlayerLook();

        getInventory().clone(player.getInventory());

        headYaw = player.headYaw;
        bodyYaw = player.bodyYaw;

        if (player.isSneaking()) {
            setSneaking(true);
            setPose(EntityPose.CROUCHING);
        }
        world.addEntity(this);
    }

    private static GameProfile getProfile() {
        MinecraftClient mc = MinecraftClient.getInstance();
        GameProfile sessionProfile = null;
        String name = "FakePlayer";
        try {
            sessionProfile = mc.getSession() != null ? mc.getGameProfile() : null;
            if (sessionProfile != null && sessionProfile.name() != null) {
                name = sessionProfile.name();
            }
        } catch (Throwable ignored) {
        }

        GameProfile fakeProfile = new GameProfile(FAKE_PLAYER_UUID, name);

        try {
            if (sessionProfile != null) {
                var textures = sessionProfile.properties().get("textures");
                if (!textures.isEmpty()) {
                    for (var p : textures) {
                        assert p != null;
                        fakeProfile.properties().put("textures", new Property(p.name(), p.value(), p.signature()));
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return fakeProfile;
    }

    public void resetPlayerPosition() {
        player.refreshPositionAndAngles(getX(), getY(), getZ(), getYaw(), getPitch());
    }

    public void copyStagePosAndPlayerLook() {
        Stage lastStage = SongHandler.getInstance().lastStage;
        if (lastStage != null) {
            refreshPositionAndAngles(lastStage.position.getX() + 0.5, lastStage.position.getY(), lastStage.position.getZ() + 0.5, player.getYaw(), player.getPitch());
            headYaw = player.headYaw;
        }
        else {
            copyPositionAndRotation(player);
        }
    }

    @Override
    public SkinTextures getSkin() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            return mc.player.getSkin();
        }
        return super.getSkin();
    }
}
