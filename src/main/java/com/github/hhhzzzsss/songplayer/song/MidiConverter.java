package com.github.hhhzzzsss.songplayer.song;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;

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

	private static Note getMidiPercussionNote(int midiPitch, long microTime) {
		Instrument instrument = null;
		if (midiPitch == 35 || midiPitch == 36 || midiPitch == 41 || midiPitch == 43 || midiPitch == 45 || midiPitch == 57) {
			instrument = Instrument.BASEDRUM;
		}
		else if (midiPitch == 38 || midiPitch == 39 || midiPitch == 40 || midiPitch == 54 || midiPitch == 69 || midiPitch == 70 || midiPitch == 73 || midiPitch == 74 || midiPitch == 78 || midiPitch == 79) {
			instrument = Instrument.SNARE;
		}
		else if (midiPitch == 37 || midiPitch == 42 || midiPitch == 44 || midiPitch == 46 || midiPitch == 49 || midiPitch == 51 || midiPitch == 52 || midiPitch == 55 || midiPitch == 57 || midiPitch == 59) {
			instrument = Instrument.HAT;
		}

		if (instrument == null) {
			return null;
		}

		int pitch = 0;
		int noteId = pitch + instrument.instrumentId*25;
		long time = microTime / 1000L;

		return new Note(noteId, time);
	}
}
