package com.github.hhhzzzsss.songplayer.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.hhhzzzsss.songplayer.Freecam;
import com.github.hhhzzzsss.songplayer.SongPlayer;

import net.minecraft.client.Keyboard;
import net.minecraft.client.util.InputUtil;

@Mixin(Keyboard.class)
public class KeyboardMixin {
	@Inject(at = @At("HEAD"), method = "onKey(JIIII)V")
	private void onOnKey(long window, int key, int scancode, int i, int j, CallbackInfo ci) {
		if (SongPlayer.MC.currentScreen == null && i == GLFW.GLFW_PRESS && InputUtil.fromKeyCode(key, scancode).getTranslationKey().equals("key.keyboard.p")) {
			Freecam.getInstance().toggle();
		}
	}
}
