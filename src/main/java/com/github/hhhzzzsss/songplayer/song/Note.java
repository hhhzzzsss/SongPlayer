package com.github.hhhzzzsss.songplayer.song;

public class Note implements Comparable<Note> {
	public int noteId;
	public long time;
	public byte volume;
	public short pitchCorrection;
	public Note(int note, long time, byte volume, short pitchCorrection) {
		this.noteId = note;
		this.time = time;
		this.volume = volume;
		this.pitchCorrection = pitchCorrection;
	}

	@Override
	public int compareTo(Note other) {
		if (time < other.time) {
			return -1;
		} else if (time > other.time) {
			return 1;
		} else {
			return 0;
		}
	}
}
