package com.github.hhhzzzsss.songplayer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.hhhzzzsss.songplayer.CommandProcessor;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import javax.annotation.Nullable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
	@Inject(at = @At("HEAD"), method = "sendChatMessage(Ljava/lang/String;Lnet/minecraft/text/Text;)V", cancellable=true)
	private void onSendChatMessage(String message, @Nullable Text preview, CallbackInfo ci) {
		boolean isCommand = CommandProcessor.processChatMessage(message);
		if (isCommand) {
			ci.cancel();
		}
	}
}