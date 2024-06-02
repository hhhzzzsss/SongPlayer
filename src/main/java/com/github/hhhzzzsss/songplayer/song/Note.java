package com.github.hhhzzzsss.songplayer.song;

public class Note implements Comparable<Note> {
	public int noteId;
	public long time;
	public int velocity;
	public Note(int note, long time) {
		this.noteId = note;
		this.time = time;
		this.velocity = 100;
	}

	public Note(int note, long time, int velocity) {
		this.noteId = note;
		this.time = time;
		this.velocity = velocity;
	}

	@Override
	public int compareTo(Note other) {
		if (time < other.time) {
			return -1;
		}
		else if (time > other.time) {
			return 1;
		}
		else if (noteId < other.noteId) {
			return -1;
		}
		else if (noteId > other.noteId) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
