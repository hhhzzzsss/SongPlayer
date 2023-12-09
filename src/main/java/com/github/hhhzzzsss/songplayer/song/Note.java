package com.github.hhhzzzsss.songplayer.song;

public class Note implements Comparable<Note> {
	public int noteId;
	public long time;
	public Note(int note, long time) {
		this.noteId = note;
		this.time = time;
	}

	@Override
	public int compareTo(Note other) {
		if (time < other.time) {
			return -1;
		} else if (time > other.time) {
			return 1;
		} else return Integer.compare(noteId, other.noteId);
	}
}
