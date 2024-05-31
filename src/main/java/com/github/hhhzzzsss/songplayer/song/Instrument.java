package com.github.hhhzzzsss.songplayer.song;

public enum Instrument {
	HARP(0, 54, "Dirt/Other"),
	BASEDRUM(1, 0, "Any Stone"),
	SNARE(2, 0, "Sand/Gravel"),
	HAT(3, 0, "Glass"),
	BASS(4, 30, "Any Wood"),
	FLUTE(5, 66, "Clay"),
	BELL(6, 78, "Block of Gold"),
	GUITAR(7, 42, "Wool"),
	CHIME(8, 78, "Packed Ice"),
	XYLOPHONE(9, 78, "Bone Block"),
	IRON_XYLOPHONE(10, 54, "Block of Iron"),
	COW_BELL(11, 66, "Soul Sand"),
	DIDGERIDOO(12, 30, "Pumpkin"),
	BIT(13, 54, "Block of Emerald"),
	BANJO(14, 54, "Hay Bale"),
	PLING(15, 54, "Glowstone");
	
	public final int instrumentId;
	public final int offset;
	public final String material;

	Instrument(int instrumentId, int offset, String material) {
		this.instrumentId = instrumentId;
		this.offset = offset;
		this.material = material;
	}

	private static Instrument[] values = values();
	public static Instrument getInstrumentFromId(int instrumentId) {
		return values[instrumentId];
	}
}
