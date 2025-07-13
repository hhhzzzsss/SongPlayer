package com.github.hhhzzzsss.songplayer.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public interface ClientPlayNetworkHandlerAccessor {
	@Accessor
	Map<UUID, PlayerListEntry> getPlayerListEntries();
}
