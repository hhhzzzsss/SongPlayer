package com.github.hhhzzzsss.songplayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.hhhzzzsss.songplayer.SongPlayer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Inject(at = @At("HEAD"), method = "doItemUse()V")
	public void onDoItemUse(CallbackInfo ci) {
		Type type = SongPlayer.MC.crosshairTarget.getType();
		if (type == Type.BLOCK) {
			BlockHitResult blockHitResult = (BlockHitResult) SongPlayer.MC.crosshairTarget;
			BlockPos pos = blockHitResult.getBlockPos();
			System.out.println(blockHitResult.getSide() + ": " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
		}
	}
}
