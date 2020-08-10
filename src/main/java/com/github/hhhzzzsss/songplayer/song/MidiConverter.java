package com.github.hhhzzzsss.songplayer.song;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class MidiConverter {
	public static final int SET_INSTRUMENT = 0xC0;
	public static final int SET_TEMPO = 0x51;
	public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    
    public static int[] instrument_offsets = new int[] {
    	54, //harp
    	0, //basedrum
    	0, //snare
    	0, //hat
    	30, //bass
    	66, //flute
    	78, //bell
    	42, //guitar
    	78, //chime
    	78, //xylophone
    	54, //iron xylophone
    	66, //cow bell
    	30, //didgeridoo
    	54, //bit
    	54, //banjo
    	54, //electric piano
    };

    public static String fileName = "moskau";
	public static TreeMap<Long, ArrayList<Integer>> noteMap;
    
	public static TreeMap<Long, ArrayList<Integer>> getMidi(String url) throws Exception {
		noteMap  = new TreeMap<>();
		
		//SocketAddress addr = new InetSocketAddress("34.94.90.93", 3128);
		//Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
		URLConnection conn = (new URL(url)).openConnection();
		conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0");
		BufferedInputStream downloadStream = new BufferedInputStream(conn.getInputStream());
		boolean fileTooLarge = true;
		byte tempbuf[] = new byte[10240];
		for (int i=0; i<1024; i++) {
			if (downloadStream.read(tempbuf, 0, 1024) == -1) {
				fileTooLarge = false;
				break;
			}
		}
		if (fileTooLarge) {
			throw new IOException("File too large");
		}
		downloadStream.close();
		downloadStream = new BufferedInputStream(new URL(url).openStream());

		Sequence sequence = MidiSystem.getSequence(downloadStream);
		
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
		
		Collections.sort(tempoEvents, new Comparator<MidiEvent>() {
		    @Override
		    public int compare(MidiEvent a, MidiEvent b) {
		        return (new Long(a.getTick())).compareTo(b.getTick());
		    }
		});
		
		for (Track track : sequence.getTracks()) {

			long microTime = 0;
			int[] instrumentIds = new int[16];
			//int apparent_mpq = (int) (sequence.getMicrosecondLength()/sequence.getTickLength()*tpq);
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
						int key = sm.getData1();
						long deltaTick = event.getTick() - prevTick;
						prevTick = event.getTick();
						microTime += (mpq/tpq) * deltaTick;
						if (sm.getChannel() == 9) {
							processMidiNote(128, sm.getData1(), microTime);
						}
						else {
							processMidiNote(instrumentIds[sm.getChannel()], sm.getData1(), microTime);
						}
					}
					else {
					}
				}
			}
		}
		
		downloadStream.close();
		
		return noteMap;
	}
	
	public static void processMidiNote(int midiInstrument, int midiPitch, long microTime) {
		int minecraftInstrument = -1;
		if ((midiInstrument >= 0 && midiInstrument <= 7) || (midiInstrument >= 24 && midiInstrument <= 31)) { //normal
			if (midiPitch >= 54 && midiPitch <= 78) {
				minecraftInstrument = 0; //piano
			}
			else if (midiPitch >= 30 && midiPitch <= 54) {
				minecraftInstrument = 4; //bass
			}
			else if (midiPitch >= 78 && midiPitch <= 102) {
				minecraftInstrument = 6; //bells
			}
		}
		else if (midiInstrument >= 8 && midiInstrument <= 15) { //chromatic percussion
			if (midiPitch >= 54 && midiPitch <= 78) {
				minecraftInstrument = 10; //iron xylophone
			}
			else if (midiPitch >= 78 && midiPitch <= 102) {
				minecraftInstrument = 9; //xylophone
			}
			else if (midiPitch >= 30 && midiPitch <= 54) {
				minecraftInstrument = 4; //bass
			}
		}
		else if ((midiInstrument >= 16 && midiInstrument <= 23) || (midiInstrument >= 32 && midiInstrument <= 71) || (midiInstrument >= 80 && midiInstrument <= 111)) { //synth
			if (midiPitch >= 54 && midiPitch <= 78) {
				minecraftInstrument = 13; //bit
			}
			else if (midiPitch >= 30 && midiPitch <= 54) { //didgeridoo
				minecraftInstrument = 12;
			}
			else if (midiPitch >= 78 && midiPitch <= 102) { //bells
				minecraftInstrument = 6;
			}
		}
		else if ((midiInstrument >= 72 && midiInstrument <= 79)) { //woodwind
			if (midiPitch >= 66 && midiPitch <= 90) {
				minecraftInstrument = 5; //flute
			}
			else if (midiPitch >= 30 && midiPitch <= 54) { //didgeridoo
				minecraftInstrument = 12;
			}
			else if (midiPitch >= 54 && midiPitch <= 78) {
				minecraftInstrument = 13; //bit
			}
			else if (midiPitch >= 78 && midiPitch <= 102) { //bells
				minecraftInstrument = 6;
			}
		}
		else if (midiInstrument == 128) {
			if (midiPitch == 35 || midiPitch == 36 || midiPitch == 41 || midiPitch == 43 || midiPitch == 45 || midiPitch == 57) {
				minecraftInstrument = 1; //bass drum
			}
			else if (midiPitch == 38 || midiPitch == 39 || midiPitch == 40 || midiPitch == 54 || midiPitch == 69 || midiPitch == 70 || midiPitch == 73 || midiPitch == 74 || midiPitch == 78 || midiPitch == 79) {
				minecraftInstrument = 2; //snare
			}
			else if (midiPitch == 37 || midiPitch == 42 || midiPitch == 44 || midiPitch == 46 || midiPitch == 49 || midiPitch == 51 || midiPitch == 52 || midiPitch == 55 || midiPitch == 57 || midiPitch == 59) {
				minecraftInstrument = 3; //hat
			}
			midiPitch = 0;
		}
		
		long milliTime = microTime / 1000;
		if (minecraftInstrument >= 0) {
			int noteId = (midiPitch-instrument_offsets[minecraftInstrument]) + minecraftInstrument*25;
			
			if (!noteMap.containsKey(milliTime)) {
				noteMap.put(milliTime, new ArrayList<Integer>());
			}
			if (!noteMap.get(milliTime).contains(noteId)) {
				noteMap.get(milliTime).add(noteId);
			}
		}
	}
}
