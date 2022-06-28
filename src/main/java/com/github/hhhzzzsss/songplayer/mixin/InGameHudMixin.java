package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.playing.ProgressDisplay;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Shadow
    private int scaledWidth;

    @Shadow
    private int scaledHeight;

    @Shadow
    private int heldItemTooltipFade;

    @Inject(method = "render(Lnet/minecraft/client/util/math/MatrixStack;F)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;enableBlend()V", ordinal = 4))
    private void onRender(MatrixStack matrixStack, float tickDelta, CallbackInfo ci) {
        if (SongPlayer.MC.options.debugEnabled) {
            return;
        }

        ProgressDisplay.getInstance().onRenderHUD(matrixStack, scaledWidth, scaledHeight, heldItemTooltipFade);
    }
}
