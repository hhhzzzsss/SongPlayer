package com.github.hhhzzzsss.songplayer;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class Freecam {
	private static Freecam instance = null;
	public static Freecam getInstance() {
		if (instance == null) {
			instance = new Freecam();
		}
		return instance;
	}
	
	boolean enabled = false;
	
	private final ClientPlayerEntity player = SongPlayer.MC.player;
	private FakePlayerEntity fakePlayer;
	
	private Freecam() {
	}
	
	public void enable() {
		enabled = true;
		fakePlayer = new FakePlayerEntity();
		SongPlayer.addChatMessage("Freecam is enabled");
	}
	
	public void disable() {
		enabled = false;
		if (fakePlayer != null) {
			fakePlayer.resetPlayerPosition();
			fakePlayer.remove(Entity.RemovalReason.DISCARDED);
			fakePlayer = null;
			player.setVelocity(Vec3d.ZERO);
		}
		SongPlayer.addChatMessage("Freecam is disabled");
	}
	
	public void onGameJoin() {
		enabled = false;
		fakePlayer = null;
	}
	
	public void toggle() {
		if (enabled) {
			disable();
		}
		else {
			enable();
		}
	}
	
	public boolean isEnabled() {
		return enabled;
	}
}
