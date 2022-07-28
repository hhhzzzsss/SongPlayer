package com.github.hhhzzzsss.songplayer.mixin;

import com.github.hhhzzzsss.songplayer.CommandProcessor;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(ChatInputSuggestor.class)
public class ChatInputSuggestorMixin {
    @Shadow
    CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    private static int getStartOfCurrentWord(String input) {
        return 0;
    }

    @Shadow
    public void show(boolean narrateFirstSuggestion) {}

    @Shadow
    final TextFieldWidget textField;

    public ChatInputSuggestorMixin() {
        textField = null;
    }

    @Inject(at = @At("TAIL"), method = "refresh()V")
    public void onRefresh(CallbackInfo ci) {
        String textStr = this.textField.getText();
        int cursorPos = this.textField.getCursor();
        String preStr = textStr.substring(0, cursorPos);
        if (!preStr.startsWith("$")) {
            return;
        }

        int wordStart = getStartOfCurrentWord(preStr);
        CompletableFuture<Suggestions> suggestions = CommandProcessor.handleSuggestions(preStr, new SuggestionsBuilder(preStr, wordStart));
        if (suggestions != null) {
            this.pendingSuggestions = suggestions;
            this.show(true);
        }
    }
}
