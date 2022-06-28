package com.github.hhhzzzsss.songplayer.playing;

import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.Objects;

public class ProgressDisplay {
    private static ProgressDisplay instance = null;
    public static ProgressDisplay getInstance() {
        if (instance == null) {
            instance = new ProgressDisplay();
        }
        return instance;
    }
    private ProgressDisplay() {}

    public MutableText displayText = Text.empty();
    public int fade = 0;

    public void setText(MutableText text) {
        displayText = text;
        fade = 100;
    }

    public void onRenderHUD(MatrixStack matrixStack, int scaledWidth, int scaledHeight, int heldItemTooltipFade) {
        if (fade <= 0) {
            return;
        }

        int textWidth = SongPlayer.MC.textRenderer.getWidth(displayText);
        int textX = (scaledWidth - textWidth) / 2;
        int textY = scaledHeight - 59;
        if (!SongPlayer.MC.interactionManager.hasStatusBars()) {
            textY += 14;
        }
        if (heldItemTooltipFade > 0) {
            textY -= 12;
        }

        int opacity = (int)((float)this.fade * 256.0F / 10.0F);
        if (opacity > 255) {
            opacity = 255;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Objects.requireNonNull(SongPlayer.MC.textRenderer);
        SongPlayer.MC.textRenderer.drawWithShadow(matrixStack, displayText, (float)textX, (float)textY, 16777215 + (opacity << 24));
        RenderSystem.disableBlend();
    }

    public void onTick() {
        if (fade > 0) {
            fade--;
        }
    }
}
