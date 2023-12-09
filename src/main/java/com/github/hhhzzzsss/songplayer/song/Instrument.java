package com.github.hhhzzzsss.songplayer.song;

public enum Instrument {
	HARP(0, 54),
	BASEDRUM(1, 0),
	SNARE(2, 0),
	HAT(3, 0),
	BASS(4, 30),
	FLUTE(5, 66),
	BELL(6, 78),
	GUITAR(7, 42),
	CHIME(8, 78),
	XYLOPHONE(9, 78),
	IRON_XYLOPHONE(10, 54),
	COW_BELL(11, 66),
	DIDGERIDOO(12, 30),
	BIT(13, 54),
	BANJO(14, 54),
	PLING(15, 54);
	
	public final int instrumentId;
	public final int offset;

	Instrument(int instrumentId, int offset) {
		this.instrumentId = instrumentId;
		this.offset = offset;
	}

	private static final Instrument[] values = values();
	public static Instrument getInstrumentFromId(int instrumentId) {
		return values[instrumentId];
	}
}
