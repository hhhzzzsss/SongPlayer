package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.playing.SongHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.network.ClientPlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @ModifyExpressionValue(method = "tickMovement()V", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerAbilities;allowFlying:Z", opcode = Opcodes.GETFIELD))
    private boolean getAllowFlying(boolean allowFlying) {
        if (!SongHandler.getInstance().isIdle()) {
            return true;
        } else {
            return allowFlying;
        }
    }
}
