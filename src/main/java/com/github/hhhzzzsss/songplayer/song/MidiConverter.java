package com.github.hhhzzzsss.songplayer.song;
import com.github.hhhzzzsss.songplayer.SongPlayer;
import com.github.hhhzzzsss.songplayer.Util;
import com.github.hhhzzzsss.songplayer.playing.SongHandler;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

import javax.sound.midi.*;
import javax.sound.midi.spi.MidiFileWriter;

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
		int startnotes = 0;
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

		long offset = -1;

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
					} else if (sm.getCommand() == NOTE_ON) {
						if (sm.getData2() == 0) continue;
						byte volume = (byte) sm.getData2();
						int pitch = sm.getData1();
						long deltaTick = event.getTick() - prevTick;
						prevTick = event.getTick();
						microTime += (mpq/tpq) * deltaTick;
						Note note;
						if (sm.getChannel() == 9) {
							note = getMidiPercussionNote(pitch, microTime, volume);
						} else {
							note = getMidiInstrumentNote(instrumentIds[sm.getChannel()], pitch, microTime, volume);
						}
						if (note == null) continue;
						song.add(note);
						startnotes++;
						//get the time when the last note is, and make it the song length
						long time = microTime / 1000L;
						if (time > song.length) {
							song.length = time;
						}

						//get the time when the first note is
						if (time < offset || offset == -1) {
							offset = time;
						}
					}
				}
			}
		}
		song.sort();
		if (SongPlayer.kiosk) {
			Util.broadcastMessage("[{\"color\":\"gold\",\"text\":\"Loaded \"}, {\"color\":\"dark_aqua\",\"text\":\"" + startnotes + "\"}, {\"color\":\"gold\",\"text\":\" notes\"}]", null);
		}

		//this code here just lets us start the song at the very first note, because some midis are sometimes silent at the start
		for (Note n : song.notes) {
			n.time -= offset;
		}
		song.length -= offset;

		return song;
	}

	public static Note getMidiInstrumentNote(int midiInstrument, int midiPitch, long microTime, byte volume) {
		Instrument instrument = null;
		Instrument[] instrumentList = instrumentMap.get(midiInstrument);
		if (instrumentList != null) {
			for (Instrument candidateInstrument : instrumentList) {
				if (midiPitch >= candidateInstrument.offset && midiPitch <= candidateInstrument.offset+24) {
					instrument = candidateInstrument;
					break;
				}
			}
		}

		if (instrument == null) {
			return null;
		}

		int pitch = midiPitch-instrument.offset;
		int noteId = pitch + instrument.instrumentId*25;
		long time = microTime / 1000L;

		return new Note(noteId, time, volume, (short) 0);
	}

	private static Note getMidiPercussionNote(int midiPitch, long microTime, byte volume) {
		if (percussionMap.containsKey(midiPitch)) {
			int noteId = percussionMap.get(midiPitch);
			long time = microTime / 1000L;

			return new Note(noteId, time, volume, (short) 0);
		}
		return null;
	}

	public static void outputSong(Song song) {
		File outputfile = new File(SongPlayer.SONG_DIR + "/" + song.name + ".mid");
		Sequence sequence;
		try {
			sequence = new Sequence(Sequence.PPQ, 500);
		} catch (InvalidMidiDataException e) {
			SongPlayer.addChatMessage("something went wrong converting to midi file lol");
			SongPlayer.addChatMessage(e.getMessage());
			return;
		}
		try {
			Track events = sequence.createTrack();
			ShortMessage event = new ShortMessage();
			event.setMessage(176, 0, 120, 0);
			events.add(new MidiEvent(event, 0L));
			ShortMessage tempo = new ShortMessage();
			tempo.setMessage(ShortMessage.TIMING_CLOCK, 0, 0);
			for (int i = 0; i < 13; i++) {

			}
			Track track = sequence.createTrack(); //BASS, FLUTE, BELL
			for (Note note : song.notes) {
				ShortMessage a = new ShortMessage();
				int instrument = note.noteId / 25;

				if (instrument > 0 && instrument < 4) {
					boolean foundinstrument = false;
					for (Map.Entry e : percussionMap.entrySet()) {
						if (note.noteId == (Integer) e.getValue()) {
							a.setMessage(144, 9, note.noteId, note.volume);
							track.add(new MidiEvent(a, note.time));
							foundinstrument = true;
							break;
						}
					}
					if (foundinstrument) {
						continue;
					}
				}
				int midiinstrument = instrumentConvertMap.get(instrument);

				ShortMessage setinstrument = new ShortMessage();
				setinstrument.setMessage(192, 0, midiinstrument, note.volume);
				track.add(new MidiEvent(setinstrument, note.time));
				int pitch = note.noteId % 25;
				a.setMessage(144, 0, 0 + pitch + Instrument.getInstrumentFromId(instrument).offset, note.volume);
				track.add(new MidiEvent(a, note.time));
			}

			SongPlayer.addChatMessage("§6wrote converted file as §3" + song.name + ".mid");
		} catch(Exception err) {
			err.printStackTrace();
			System.out.println("Failed to process a note :(");
			SongHandler.getInstance().converting = false;
			SongHandler.getInstance().convertintto = "";
			SongPlayer.addChatMessage("§cFailed to convert file: §4" + err.getMessage());
		}
		try {
			MidiSystem.write(sequence, 1, outputfile);
		} catch(IOException e) {
			e.printStackTrace();
			System.out.println("Failed to write to file :(");
			SongHandler.getInstance().converting = false;
			SongHandler.getInstance().convertintto = "";
			SongPlayer.addChatMessage("§cFailed to convert file: §4" + e.getMessage());
		}
	}

	private static HashMap<Integer, Integer> instrumentConvertMap = new HashMap<>();
	static {
		// good: 0, 8, 16
		// maybe good: 24, 40
		instrumentConvertMap.put(0, 0);
		instrumentConvertMap.put(1, 117);
		instrumentConvertMap.put(2, 119);
		instrumentConvertMap.put(3, 115);
		instrumentConvertMap.put(4, 0);
		instrumentConvertMap.put(5, 40);
		instrumentConvertMap.put(6, 0);
		instrumentConvertMap.put(7, 24);
		instrumentConvertMap.put(8, 80);
		instrumentConvertMap.put(9, 8);
		instrumentConvertMap.put(10, 8);
		instrumentConvertMap.put(11, 113);
		instrumentConvertMap.put(12, 8);
		instrumentConvertMap.put(13, 16);
		instrumentConvertMap.put(14, 104);
		instrumentConvertMap.put(15, 90);
	}

	public static HashMap<Integer, Instrument[]> instrumentMap = new HashMap<>();
	static {
		// Piano (HARP BASS BELL)
		instrumentMap.put(0, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Acoustic Grand Piano
		instrumentMap.put(1, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Bright Acoustic Piano
		instrumentMap.put(2, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Electric Grand Piano
		instrumentMap.put(3, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Honky-tonk Piano
		instrumentMap.put(4, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Electric Piano 1
		instrumentMap.put(5, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Electric Piano 2
		instrumentMap.put(6, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Harpsichord
		instrumentMap.put(7, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Clavinet

		// Chromatic Percussion (IRON_XYLOPHONE XYLOPHONE BASS)
		instrumentMap.put(8, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.DIDGERIDOO, Instrument.XYLOPHONE}); // Celesta
		instrumentMap.put(9, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.DIDGERIDOO, Instrument.XYLOPHONE}); // Glockenspiel
		instrumentMap.put(10, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.DIDGERIDOO, Instrument.XYLOPHONE}); // Music Box
		instrumentMap.put(11, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.DIDGERIDOO, Instrument.XYLOPHONE}); // Vibraphone
		instrumentMap.put(12, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.DIDGERIDOO, Instrument.CHIME}); // Marimba
		instrumentMap.put(13, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.DIDGERIDOO, Instrument.XYLOPHONE}); // Xylophone
		instrumentMap.put(14, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.DIDGERIDOO, Instrument.CHIME}); // Tubular Bells
		instrumentMap.put(15, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.DIDGERIDOO, Instrument.XYLOPHONE}); // Dulcimer

		// Organ (BIT DIDGERIDOO BELL)
		instrumentMap.put(16, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Drawbar Organ
		instrumentMap.put(17, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Percussive Organ
		instrumentMap.put(18, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Rock Organ
		instrumentMap.put(19, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Church Organ
		instrumentMap.put(20, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Reed Organ
		instrumentMap.put(21, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Accordian
		instrumentMap.put(22, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Harmonica
		instrumentMap.put(23, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Tango Accordian

		// Guitar (BIT DIDGERIDOO BELL)
		instrumentMap.put(24, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Acoustic Guitar (nylon)
		instrumentMap.put(25, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Acoustic Guitar (steel)
		instrumentMap.put(26, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Electric Guitar (jazz)
		instrumentMap.put(27, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Electric Guitar (clean)
		instrumentMap.put(28, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Electric Guitar (muted)
		instrumentMap.put(29, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Overdriven Guitar
		instrumentMap.put(30, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Distortion Guitar
		instrumentMap.put(31, new Instrument[]{Instrument.GUITAR, Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Guitar Harmonics

		// Bass
		instrumentMap.put(32, new Instrument[]{Instrument.BASS, Instrument.HARP, Instrument.BELL}); // Acoustic Bass
		instrumentMap.put(33, new Instrument[]{Instrument.BASS, Instrument.HARP, Instrument.BELL}); // Electric Bass (finger)
		instrumentMap.put(34, new Instrument[]{Instrument.BASS, Instrument.HARP, Instrument.BELL}); // Electric Bass (pick)
		instrumentMap.put(35, new Instrument[]{Instrument.BASS, Instrument.HARP, Instrument.BELL}); // Fretless Bass
		instrumentMap.put(36, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Slap Bass 1
		instrumentMap.put(37, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Slap Bass 2
		instrumentMap.put(38, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Synth Bass 1
		instrumentMap.put(39, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.XYLOPHONE}); // Synth Bass 2

		// Strings
		instrumentMap.put(40, new Instrument[]{Instrument.FLUTE, Instrument.GUITAR, Instrument.BASS, Instrument.BELL}); // Violin
		instrumentMap.put(41, new Instrument[]{Instrument.FLUTE, Instrument.GUITAR, Instrument.BASS, Instrument.BELL}); // Viola
		instrumentMap.put(42, new Instrument[]{Instrument.FLUTE, Instrument.GUITAR, Instrument.BASS, Instrument.BELL}); // Cello
		instrumentMap.put(43, new Instrument[]{Instrument.FLUTE, Instrument.GUITAR, Instrument.BASS, Instrument.BELL}); // Contrabass
		instrumentMap.put(44, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Tremolo Strings
		instrumentMap.put(45, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL}); // Pizzicato Strings
		instrumentMap.put(46, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL}); // Orchestral Harp
		instrumentMap.put(47, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL}); // Timpani

		// Ensenble
		instrumentMap.put(48, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // String Ensemble 1
		instrumentMap.put(49, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // String Ensemble 2
		instrumentMap.put(50, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Synth Strings 1
		instrumentMap.put(51, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Synth Strings 2
		instrumentMap.put(52, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Choir Aahs
		instrumentMap.put(53, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Voice Oohs
		instrumentMap.put(54, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Synth Choir
		instrumentMap.put(55, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL}); // Orchestra Hit
		// Brass
		instrumentMap.put(56, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Trumpet
		instrumentMap.put(57, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Trombone
		instrumentMap.put(58, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Tuba
		instrumentMap.put(59, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Muted Trumpet
		instrumentMap.put(60, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // French Horn
		instrumentMap.put(61, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Brass Section
		instrumentMap.put(62, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Synth Brass 1
		instrumentMap.put(63, new Instrument[]{Instrument.BIT, Instrument.DIDGERIDOO, Instrument.BELL}); // Synth Brass 2

		// Reed
		instrumentMap.put(64, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(65, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(66, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(67, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(68, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(69, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(70, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(71, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});

		// Pipe
		instrumentMap.put(72, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(73, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(74, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(75, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(76, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(77, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(78, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});
		instrumentMap.put(79, new Instrument[]{Instrument.FLUTE, Instrument.DIDGERIDOO, Instrument.IRON_XYLOPHONE, Instrument.BELL});

		// Synth Lead
		instrumentMap.put(80, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(81, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(82, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(83, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(84, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(85, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(86, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(87, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});

		// Synth Pad
		instrumentMap.put(88, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(89, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(90, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(91, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(92, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(93, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(94, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});
		instrumentMap.put(95, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.BELL});

		// Synth Effects
//		instrumentMap.put(96, new Instrument[]{});
//		instrumentMap.put(97, new Instrument[]{}); //I see why you didn't map these hhhzzzsss lol
		instrumentMap.put(98, new Instrument[]{Instrument.DIDGERIDOO, Instrument.FLUTE, Instrument.CHIME});
		instrumentMap.put(99, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
		instrumentMap.put(100, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
		instrumentMap.put(101, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
		instrumentMap.put(102, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});
		instrumentMap.put(103, new Instrument[]{Instrument.HARP, Instrument.BASS, Instrument.BELL});

		// Ethnic
		instrumentMap.put(104, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
		instrumentMap.put(105, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
		instrumentMap.put(106, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
		instrumentMap.put(107, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
		instrumentMap.put(108, new Instrument[]{Instrument.BANJO, Instrument.BASS, Instrument.BELL});
		instrumentMap.put(109, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.BELL});
		instrumentMap.put(110, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.BELL});
		instrumentMap.put(111, new Instrument[]{Instrument.PLING, Instrument.DIDGERIDOO, Instrument.BELL});

		// Percussive
		instrumentMap.put(112, new Instrument[]{Instrument.COW_BELL, Instrument.BASS, Instrument.XYLOPHONE});
		instrumentMap.put(113, new Instrument[]{Instrument.COW_BELL, Instrument.BASS, Instrument.XYLOPHONE});
		instrumentMap.put(114, new Instrument[]{Instrument.DIDGERIDOO, Instrument.BIT, Instrument.FLUTE, Instrument.XYLOPHONE});
		instrumentMap.put(115, new Instrument[]{Instrument.HAT});
		instrumentMap.put(116, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
		instrumentMap.put(117, new Instrument[]{Instrument.HAT, Instrument.BASEDRUM});
		instrumentMap.put(118, new Instrument[]{Instrument.IRON_XYLOPHONE, Instrument.BASS, Instrument.XYLOPHONE});
		instrumentMap.put(119, new Instrument[]{Instrument.SNARE});

		// Sound effects
//		instrumentMap.put(120, new Instrument[]{});
//		instrumentMap.put(121, new Instrument[]{});
		instrumentMap.put(122, new Instrument[]{Instrument.SNARE}); //Seashore (I mean it kinda sounds like snare right?)
//		instrumentMap.put(123, new Instrument[]{});
//		instrumentMap.put(124, new Instrument[]{});
//		instrumentMap.put(125, new Instrument[]{});
//		instrumentMap.put(126, new Instrument[]{});
//		instrumentMap.put(127, new Instrument[]{});
	}

	public static HashMap<Integer, Integer> percussionMap = new HashMap<>();
	static {
		percussionMap.put(35, 10 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(36, 6  + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(37, 6  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(38, 8  + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(39, 6  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(40, 4  + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(41, 6  + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(42, 22 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(43, 13 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(44, 22 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(45, 15 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(46, 18 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(47, 20 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(48, 23 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(49, 17 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(50, 23 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(51, 24 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(52, 8  + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(53, 13 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(54, 18 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(55, 18 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(56, 1  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(57, 13 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(58, 2  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(59, 13 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(60, 9  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(61, 2  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(62, 8  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(63, 22 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(64, 15 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(65, 13 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(66, 8  + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(67, 8  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(68, 3  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(69, 20 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(70, 23 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(71, 24 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(72, 24 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(73, 17 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(74, 11 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(75, 18 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(76, 9  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(77, 5  + 25*Instrument.HAT.instrumentId);
		percussionMap.put(78, 22 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(79, 19 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(80, 17 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(81, 22 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(82, 22 + 25*Instrument.SNARE.instrumentId);
		percussionMap.put(83, 24 + 25*Instrument.CHIME.instrumentId);
		percussionMap.put(84, 24 + 25*Instrument.CHIME.instrumentId);
		percussionMap.put(85, 21 + 25*Instrument.HAT.instrumentId);
		percussionMap.put(86, 14 + 25*Instrument.BASEDRUM.instrumentId);
		percussionMap.put(87, 7  + 25*Instrument.BASEDRUM.instrumentId);
	}
}
