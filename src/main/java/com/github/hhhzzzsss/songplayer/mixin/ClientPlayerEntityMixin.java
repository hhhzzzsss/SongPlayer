package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerAbilities;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Redirect(method = "tickMovement()V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z", opcode = Opcodes.GETFIELD))
    private boolean getAllowFlying(PlayerAbilities playerAbilities) {
        if (!SongHandler.getInstance().isIdle()) {
            return true;
        } else {
            return playerAbilities.allowFlying;
        }
    }
}
