package com.github.hhhzzzsss.songplayer.song;

import java.util.ArrayList;
import java.util.Collections;

public class Song {
	public ArrayList<Note> notes = new ArrayList<>();
	public String name;
	public int position = 0; // Current note index
	public boolean[] requiredNotes = new boolean[400];
	public boolean looping = false;
	public boolean paused = true;
	public long startTime = 0; // Start time in millis since unix epoch
	public long length = 0; // Milliseconds in the song
	public long time = 0; // Time since start of song
	public long loopPosition = 0; // Milliseconds into the song to start looping
	public int loopCount = 0; // Number of times to loop
	public int currentLoop = 0; // Number of loops so far
	
	public Song(String name) {
		this.name = name;
	}
	
	public Note get(int i) {
		return notes.get(i);
	}
	
	public void add(Note e) {
		notes.add(e);
		requiredNotes[e.noteId] = true;
	}

	public void sort() {
		Collections.sort(notes);
	}

	/**
	 * Starts playing song (does nothing if already playing)
	 */
	public void play() {
		if (paused) {
			paused = false;
			startTime = System.currentTimeMillis() - time;
		}
	}

	/**
	 * Pauses song (does nothing if already paused)
	 */
	public void pause() {
		if (!paused) {
			paused = true;
			// Recalculates time so that the song will continue playing after the exact point it was paused
			advanceTime();
		}
	}

	public void setTime(long t) {
		time = t;
		startTime = System.currentTimeMillis() - time;
		position = 0;
		while (position < notes.size() && notes.get(position).time < t) {
			position++;
		}
	}

	public void advanceTime() {
		time = System.currentTimeMillis() - startTime;
	}

	public boolean reachedNextNote() {
		if (position < notes.size()) {
			return notes.get(position).time <= time;
		}
		if (time > length && shouldLoop()) {
			loop();
			if (position < notes.size()) {
				return notes.get(position).time <= time;
			}
		}
		return false;
	}

	public Note getNextNote() {
		if (position >= notes.size()) {
			if (shouldLoop()) {
				loop();
			} else {
				return null;
			}
		}
		return notes.get(position++);
	}

	public boolean finished() {
		return time > length && !shouldLoop();
	}

	private void loop() {
		position = 0;
		startTime += length - loopPosition;
		time -= length - loopPosition;
		while (position < notes.size() && notes.get(position).time < loopPosition) {
			position++;
		}
		currentLoop++;
	}

	private boolean shouldLoop() {
		if (looping) {
			if (loopCount == 0) {
				return true;
			} else {
				return currentLoop < loopCount;
			}
		} else {
			return false;
		}
	}

	public int size() {
		return notes.size();
	}
}
