package com.github.hhhzzzsss.songplayer.song;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import javax.sound.midi.*;

public class MidiConverter {
	public static final int SET_INSTRUMENT = 0xC0;
	public static final int SET_TEMPO = 0x51;
	public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;

	public static Song getSongFromUrl(URL url) throws IOException, InvalidMidiDataException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
		Sequence sequence = MidiSystem.getSequence(DownloadUtils.DownloadToInputStream(url, 5*1024*1024));
		return getSong(sequence, Paths.get(url.toURI().getPath()).getFileName().toString());
	}

	public static Song getSongFromFile(File file) throws InvalidMidiDataException, IOException {
		Sequence sequence = MidiSystem.getSequence(file);
		return getSong(sequence, file.getName());
	}

	public static Song getSongFromBytes(byte[] bytes, String name) throws InvalidMidiDataException, IOException {
		Sequence sequence = MidiSystem.getSequence(new ByteArrayInputStream(bytes));
		return getSong(sequence, name);
	}
    
	public static Song getSong(Sequence sequence, String name) {
		Song song  = new Song(name);
		
		long tpq = sequence.getResolution();
		
		ArrayList<MidiEvent> tempoEvents = new ArrayList<>();
		for (Track track : sequence.getTracks()) {
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				MidiMessage message = event.getMessage();
				if (message instanceof MetaMessage) {
					MetaMessage mm = (MetaMessage) message;
					if (mm.getType() == SET_TEMPO) {
						tempoEvents.add(event);
					}
				}
			}
		}
		
		Collections.sort(tempoEvents, (a, b) -> Long.compare(a.getTick(), b.getTick()));
		
		for (Track track : sequence.getTracks()) {

			long microTime = 0;
			int[] instrumentIds = new int[16];
			int mpq = 500000;
			int tempoEventIdx = 0;
			long prevTick = 0;
			
			for (int i = 0; i < track.size(); i++) {
				MidiEvent event = track.get(i);
				MidiMessage message = event.getMessage();
				
				while (tempoEventIdx < tempoEvents.size() && event.getTick() > tempoEvents.get(tempoEventIdx).getTick()) {
					long deltaTick = tempoEvents.get(tempoEventIdx).getTick() - prevTick;
					prevTick = tempoEvents.get(tempoEventIdx).getTick();
					microTime += (mpq/tpq) * deltaTick;
					
					MetaMessage mm = (MetaMessage) tempoEvents.get(tempoEventIdx).getMessage();
					byte[] data = mm.getData();
					int new_mpq = (data[2]&0xFF) | ((data[1]&0xFF)<<8) | ((data[0]&0xFF)<<16);
					if (new_mpq != 0) mpq = new_mpq;
					tempoEventIdx++;
				}
				
				if (message instanceof ShortMessage) {
					ShortMessage sm = (ShortMessage) message;
					if (sm.getCommand() == SET_INSTRUMENT) {
						instrumentIds[sm.getChannel()] = sm.getData1();
					}
					else if (sm.getCommand() == NOTE_ON) {
						if (sm.getData2() == 0) continue;
						int pitch = sm.getData1();
						long deltaTick = event.getTick() - prevTick;
						prevTick = event.getTick();
						microTime += (mpq/tpq) * deltaTick;

						Note note;
						if (sm.getChannel() == 9) {
							note = getMidiPercussionNote(pitch, microTime);
						}
						else {
							note = getMidiInstrumentNote(instrumentIds[sm.getChannel()], pitch, microTime);
						}
						if (note != null) {
							song.add(note);
						}

						long time = microTime / 1000L;
						if (time > song.length) {
							song.length = time;
						}
					}
					else if (sm.getCommand() == NOTE_OFF) {
						long deltaTick = event.getTick() - prevTick;
						prevTick = event.getTick();
						microTime += (mpq/tpq) * deltaTick;
						long time = microTime / 1000L;
						if (time > song.length) {
							song.length = time;
						}
					}
				}
			}
		}

		song.sort();
		
		return song;
	}

	public static Note getMidiInstrumentNote(int midiInstrument, int midiPitch, long microTime) {
		Instrument instrument = null;
		if ((midiInstrument >= 0 && midiInstrument <= 7) || (midiInstrument >= 24 && midiInstrument <= 31)) { //normal
			if (midiPitch >= 54 && midiPitch <= 78) {
				instrument = Instrument.HARP;
			}
			else if (midiPitch >= 30 && midiPitch <= 54) {
				instrument = Instrument.BASS;
			}
			else if (midiPitch >= 78 && midiPitch <= 102) {
				instrument = Instrument.BELL;
			}
		}
		else if (midiInstrument >= 8 && midiInstrument <= 15) { //chromatic percussion
			if (midiPitch >= 54 && midiPitch <= 78) {
				instrument = Instrument.IRON_XYLOPHONE;
			}
			else if (midiPitch >= 78 && midiPitch <= 102) {
				instrument = Instrument.XYLOPHONE;
			}
			else if (midiPitch >= 30 && midiPitch <= 54) {
				instrument = Instrument.BASS;
			}
		}
		else if ((midiInstrument >= 16 && midiInstrument <= 23) || (midiInstrument >= 32 && midiInstrument <= 71) || (midiInstrument >= 80 && midiInstrument <= 111)) { //synth
			if (midiPitch >= 54 && midiPitch <= 78) {
				instrument = Instrument.BIT;
			}
			else if (midiPitch >= 30 && midiPitch <= 54) {
				instrument = Instrument.DIDGERIDOO;
			}
			else if (midiPitch >= 78 && midiPitch <= 102) {
				instrument = Instrument.BELL;
			}
		}
		else if ((midiInstrument >= 72 && midiInstrument <= 79)) { //woodwind
			if (midiPitch >= 66 && midiPitch <= 90) {
				instrument = Instrument.FLUTE;
			}
			else if (midiPitch >= 30 && midiPitch <= 54) {
				instrument = Instrument.DIDGERIDOO;
			}
			else if (midiPitch >= 54 && midiPitch <= 78) {
				instrument = Instrument.BIT;
			}
			else if (midiPitch >= 78 && midiPitch <= 102) {
				instrument = Instrument.BELL;
			}
		}

		if (instrument == null) {
			return null;
		}

		int pitch = midiPitch-instrument.offset;
		int noteId = pitch + instrument.instrumentId*25;
		long time = microTime / 1000L;

		return new Note(noteId, time);
	}

	// 0 4 7 12 16 19 24
	public static HashMap<Integer, Integer> percussionMap = new HashMap<>();
	static {
		percussionMap.put(35, 0  + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(36, 4  + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(37, 7  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(38, 7  + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(39, 0 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(40, 12 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(41, 7  + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(42, 7  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(43, 12 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(44, 7  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(45, 16 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(46, 7  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(47, 19 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(48, 23 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(49, 12 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(50, 24 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(51, 13 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(52, 0  + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(53, 0  + 25*Instrument.COW_BELL.instrumentId);
		percussionMap.put(54, 12 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(55, 12 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(56, 13 + 25*Instrument.COW_BELL.instrumentId);
		percussionMap.put(57, 12 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(58, 0  + 25*Instrument.BASEDRUM.instrumentId); // ??
		percussionMap.put(59, 12 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(60, 24 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(61, 19 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(62, 24 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(63, 24 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(64, 20 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(65, 24 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(66, 20 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(67, 24 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(68, 22 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(69, 16 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(70, 16 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(71, 24 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(72, 24 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(73, 19 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(74, 19 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(75, 12 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(76, 24 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(77, 22 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(78, 22 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(79, 19 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(80, 16 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(81, 16 + 25*Instrument.HAT.instrumentId);
	}
	private static Note getMidiPercussionNote(int midiPitch, long microTime) {
		if (percussionMap.containsKey(midiPitch)) {
			int noteId = percussionMap.get(midiPitch);
			long time = microTime / 1000L;

			return new Note(noteId, time);
		}
		return null;
	}
}
