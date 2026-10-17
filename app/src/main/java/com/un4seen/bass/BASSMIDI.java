package com.un4seen.bass;

import android.os.ParcelFileDescriptor;
import java.nio.ByteBuffer;

public class BASSMIDI {
    public static final int BASS_CONFIG_MIDI_COMPACT = 0x10400;
    public static final int BASS_CONFIG_MIDI_VOICES = 0x10401;
    public static final int BASS_CONFIG_MIDI_AUTOFONT = 0x10402;
    public static final int BASS_CONFIG_MIDI_DEFFONT = 0x10403;
    public static final int BASS_CONFIG_MIDI_SAMPLETHREADS = 0x10406;
    public static final int BASS_CONFIG_MIDI_SAMPLEMEM = 0x10407;
    public static final int BASS_CONFIG_MIDI_SAMPLEREAD = 0x10408;

    public static final int BASS_ATTRIB_MIDI_PPQN = 0x12000;
    public static final int BASS_ATTRIB_MIDI_CPU = 0x12001;
    public static final int BASS_ATTRIB_MIDI_CHANS = 0x12002;
    public static final int BASS_ATTRIB_MIDI_VOICES = 0x12003;
    public static final int BASS_ATTRIB_MIDI_VOICES_ACTIVE = 0x12004;
    public static final int BASS_ATTRIB_MIDI_STATE = 0x12005;
    public static final int BASS_ATTRIB_MIDI_SRC = 0x12006;
    public static final int BASS_ATTRIB_MIDI_KILL = 0x12007;
    public static final int BASS_ATTRIB_MIDI_SPEED = 0x12008;
    public static final int BASS_ATTRIB_MIDI_REVERB = 0x12009;
    public static final int BASS_ATTRIB_MIDI_VOL = 0x1200A;
    public static final int BASS_ATTRIB_MIDI_TRACK_VOL = 0x12100;

    public static final int BASS_MIDI_NODRUMPARAMUSER = 512;
    public static final int BASS_MIDI_NODRUMPARAM = 1024;
    public static final int BASS_MIDI_NOSYSRESET = 2048;
    public static final int BASS_MIDI_DECAYEND = 4096;
    public static final int BASS_MIDI_NOFX = 8192;
    public static final int BASS_MIDI_DECAYSEEK = 16384;
    public static final int BASS_MIDI_NOCROP = 32768;
    public static final int BASS_MIDI_NOTEOFF1 = 65536;
    public static final int BASS_MIDI_ASYNC = 0x400000;
    public static final int BASS_MIDI_SINCINTER = 0x800000;

    public static final int BASS_MIDI_FONT_MMAP = 0x20000;
    public static final int BASS_MIDI_FONT_XGDRUMS = 0x40000;
    public static final int BASS_MIDI_FONT_NOFX = 0x80000;
    public static final int BASS_MIDI_FONT_LINATTMOD = 0x100000;
    public static final int BASS_MIDI_FONT_LINDECVOL = 0x200000;
    public static final int BASS_MIDI_FONT_NORAMPIN = 0x400000;
    public static final int BASS_MIDI_FONT_NOLIMITS = 0x800000;
    public static final int BASS_MIDI_FONT_MINFX = 0x1000000;

    public static final int MIDI_EVENT_END = 0;
    public static final int MIDI_EVENT_NOTE = 1;
    public static final int MIDI_EVENT_PROGRAM = 2;
    public static final int MIDI_EVENT_CHANPRES = 3;
    public static final int MIDI_EVENT_PITCH = 4;
    public static final int MIDI_EVENT_PITCHRANGE = 5;
    public static final int MIDI_EVENT_DRUMS = 6;
    public static final int MIDI_EVENT_FINETUNE = 7;
    public static final int MIDI_EVENT_COARSETUNE = 8;
    public static final int MIDI_EVENT_MASTERVOL = 9;
    public static final int MIDI_EVENT_BANK = 10;
    public static final int MIDI_EVENT_MODULATION = 11;
    public static final int MIDI_EVENT_VOLUME = 12;
    public static final int MIDI_EVENT_PAN = 13;
    public static final int MIDI_EVENT_EXPRESSION = 14;
    public static final int MIDI_EVENT_SUSTAIN = 15;
    public static final int MIDI_EVENT_SOUNDOFF = 16;
    public static final int MIDI_EVENT_RESET = 17;
    public static final int MIDI_EVENT_NOTESOFF = 18;
    public static final int MIDI_EVENT_REVERB = 23;
    public static final int MIDI_EVENT_CHORUS = 24;
    public static final int MIDI_EVENT_CUTOFF = 25;
    public static final int MIDI_EVENT_RESONANCE = 26;
    public static final int MIDI_EVENT_RELEASE = 27;
    public static final int MIDI_EVENT_ATTACK = 28;
    public static final int MIDI_EVENT_DECAY = 29;
    public static final int MIDI_EVENT_REVERB_MACRO = 30;
    public static final int MIDI_EVENT_CHORUS_MACRO = 31;
    public static final int MIDI_EVENT_REVERB_TIME = 32;
    public static final int MIDI_EVENT_REVERB_DELAY = 33;
    public static final int MIDI_EVENT_REVERB_LOCUTOFF = 34;
    public static final int MIDI_EVENT_REVERB_HICUTOFF = 35;
    public static final int MIDI_EVENT_REVERB_LEVEL = 36;
    public static final int MIDI_EVENT_CHORUS_DELAY = 37;
    public static final int MIDI_EVENT_CHORUS_DEPTH = 38;
    public static final int MIDI_EVENT_CHORUS_RATE = 39;
    public static final int MIDI_EVENT_CHORUS_FEEDBACK = 40;
    public static final int MIDI_EVENT_CHORUS_LEVEL = 41;
    public static final int MIDI_EVENT_CHORUS_REVERB = 42;
    public static final int MIDI_EVENT_DRUM_FINETUNE = 50;
    public static final int MIDI_EVENT_DRUM_COARSETUNE = 51;
    public static final int MIDI_EVENT_DRUM_PAN = 52;
    public static final int MIDI_EVENT_DRUM_REVERB = 53;
    public static final int MIDI_EVENT_DRUM_CHORUS = 54;
    public static final int MIDI_EVENT_DRUM_CUTOFF = 55;
    public static final int MIDI_EVENT_DRUM_RESONANCE = 56;
    public static final int MIDI_EVENT_DRUM_LEVEL = 57;
    public static final int MIDI_EVENT_SOFT = 60;
    public static final int MIDI_EVENT_SYSTEM = 61;
    public static final int MIDI_EVENT_TEMPO = 62;
    public static final int MIDI_EVENT_SCALETUNING = 63;
    public static final int MIDI_EVENT_CONTROL = 64;
    public static final int MIDI_EVENT_BANK_LSB = 70;
    public static final int MIDI_EVENT_MIXLEVEL = 0x10000;
    public static final int MIDI_EVENT_TRANSPOSE = 0x10001;
    public static final int MIDI_EVENT_SYSTEMEX = 0x10002;
    public static final int MIDI_EVENT_END_TRACK = 0x10003;
    public static final int MIDI_EVENT_SPEED = 0x10004;
    public static final int MIDI_EVENT_DEFDRUMS = 0x10006;

    public static final int MIDI_SYSTEM_DEFAULT = 0;
    public static final int MIDI_SYSTEM_GM1 = 1;
    public static final int MIDI_SYSTEM_GM2 = 2;
    public static final int MIDI_SYSTEM_XG = 3;
    public static final int MIDI_SYSTEM_GS = 4;

    public static final int BASS_POS_MIDI_TICK = 2;

    public static class BASS_MIDI_FONT {
        public int font;
        public int preset;
        public int bank;
    }

    public static class BASS_MIDI_FONTEX {
        public int font;
        public int spreset;
        public int sbank;
        public int dpreset;
        public int dbank;
        public int dbanklsb;
    }

    public static class BASS_MIDI_FONTEX2 extends BASS_MIDI_FONTEX {
        public int minchan;
        public int numchan;
    }

    public static class BASS_MIDI_FONTINFO {
        public String name;
        public String copyright;
        public String comment;
        public int presets;
        public int samsize;
        public int samload;
        public int samtype;
    }

    public static class BASS_MIDI_MARK {
        public int track;
        public int pos;
        public String text;
    }

    public static class BASS_MIDI_MARKB {
        public int track;
        public int pos;
        public byte[] text;
    }

    public static class BASS_MIDI_EVENT {
        public int event;
        public int param;
        public int chan;
        public int tick;
        public int pos;
    }

    public static class BASS_MIDI_DEVICEINFO {
        public String name;
        public int id;
        public int flags;
    }

    public interface MIDIFILTERPROC {
        boolean MIDIFILTERPROC(int handle, int track, BASS_MIDI_EVENT event, boolean seeking, Object user);
    }

    public static native int BASS_MIDI_GetVersion();
    public static native int BASS_MIDI_StreamCreate(int channels, int flags, int freq);
    public static native int BASS_MIDI_StreamCreateFile(String file, long offset, long length, int flags, int freq);
    public static native int BASS_MIDI_StreamCreateFile(ByteBuffer memory, long offset, long length, int flags, int freq);
    public static native int BASS_MIDI_StreamCreateFile(ParcelFileDescriptor pfd, long offset, long length, int flags, int freq);
    public static native int BASS_MIDI_StreamCreateFile(BASS.Asset asset, long offset, long length, int flags, int freq);
    public static native int BASS_MIDI_StreamCreateEvents(BASS_MIDI_EVENT[] events, int ppqn, int flags, int freq);

    public static native boolean BASS_MIDI_StreamGetMark(int handle, int type, int index, BASS_MIDI_MARK mark);
    public static native boolean BASS_MIDI_StreamGetMark(int handle, int type, int index, BASS_MIDI_MARKB mark);
    public static native int BASS_MIDI_StreamGetMarks(int handle, int track, int type, BASS_MIDI_MARK[] marks);
    public static native int BASS_MIDI_StreamGetMarks(int handle, int track, int type, BASS_MIDI_MARKB[] marks);

    public static native boolean BASS_MIDI_StreamSetFonts(int handle, BASS_MIDI_FONT[] fonts, int count);
    public static native boolean BASS_MIDI_StreamSetFonts(int handle, BASS_MIDI_FONTEX[] fonts, int count);
    public static native boolean BASS_MIDI_StreamSetFonts(int handle, BASS_MIDI_FONTEX2[] fonts, int count);
    public static native int BASS_MIDI_StreamGetFonts(int handle, BASS_MIDI_FONT[] fonts, int count);
    public static native int BASS_MIDI_StreamGetFonts(int handle, BASS_MIDI_FONTEX[] fonts, int count);
    public static native int BASS_MIDI_StreamGetFonts(int handle, BASS_MIDI_FONTEX2[] fonts, int count);

    public static native boolean BASS_MIDI_StreamLoadSamples(int handle);
    public static native boolean BASS_MIDI_StreamEvent(int handle, int chan, int event, int param);
    public static native int BASS_MIDI_StreamEvents(int handle, int mode, BASS_MIDI_EVENT[] events, int length);
    public static native int BASS_MIDI_StreamEvents(int handle, int mode, ByteBuffer events, int length);
    public static native int BASS_MIDI_StreamGetEvent(int handle, int chan, int event);
    public static native int BASS_MIDI_StreamGetEvents(int handle, int track, int filter, BASS_MIDI_EVENT[] events);
    public static native int BASS_MIDI_StreamGetEventsEx(int handle, int track, int filter, BASS_MIDI_EVENT[] events, int start, int count);
    public static native boolean BASS_MIDI_StreamGetPreset(int handle, int chan, BASS_MIDI_FONT font);
    public static native int BASS_MIDI_StreamGetChannel(int handle, int chan);
    public static native boolean BASS_MIDI_StreamSetFilter(int handle, boolean seeking, MIDIFILTERPROC proc, Object user);

    public static native int BASS_MIDI_FontInit(String file, int flags);
    public static native int BASS_MIDI_FontInit(ByteBuffer memory, int flags);
    public static native int BASS_MIDI_FontInit(ParcelFileDescriptor pfd, int flags);
    public static native int BASS_MIDI_FontInit(BASS.Asset asset, int flags);
    public static native boolean BASS_MIDI_FontFree(int handle);
    public static native boolean BASS_MIDI_FontGetInfo(int handle, BASS_MIDI_FONTINFO info);
    public static native boolean BASS_MIDI_FontGetPresets(int handle, int[] presets);
    public static native String BASS_MIDI_FontGetPreset(int handle, int preset, int bank);
    public static native boolean BASS_MIDI_FontLoad(int handle, int preset, int bank);
    public static native boolean BASS_MIDI_FontLoadEx(int handle, int preset, int bank, int length, int flags);
    public static native boolean BASS_MIDI_FontUnload(int handle, int preset, int bank);
    public static native boolean BASS_MIDI_FontCompact(int handle);
    public static native boolean BASS_MIDI_FontUnpack(int handle, String outfile, int flags);
    public static native boolean BASS_MIDI_FontSetVolume(int handle, float volume);
    public static native float BASS_MIDI_FontGetVolume(int handle);
    public static native int BASS_MIDI_FontFlags(int handle, int flags, int mask);

    static {
        System.loadLibrary("bassmidi");
    }
}
