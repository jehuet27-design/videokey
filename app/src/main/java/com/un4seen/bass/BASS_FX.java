package com.un4seen.bass;

public class BASS_FX {
    public static final int BASS_ATTRIB_TEMPO = 0x10000;
    public static final int BASS_ATTRIB_TEMPO_PITCH = 0x10001;
    public static final int BASS_ATTRIB_TEMPO_FREQ = 0x10002;

    public static final int BASS_FX_FREESOURCE = 0x10000;

    public static final int BASS_FX_BFX_ROTATE = 0x10000;
    public static final int BASS_FX_BFX_ECHO = 0x10001;
    public static final int BASS_FX_BFX_FLANGER = 0x10002;
    public static final int BASS_FX_BFX_VOLUME = 0x10003;
    public static final int BASS_FX_BFX_PEAKEQ = 0x10004;
    public static final int BASS_FX_BFX_REVERB = 0x10005;
    public static final int BASS_FX_BFX_LPF = 0x10006;
    public static final int BASS_FX_BFX_MIX = 0x10007;
    public static final int BASS_FX_BFX_DAMP = 0x10008;
    public static final int BASS_FX_BFX_AUTOWAH = 0x10009;
    public static final int BASS_FX_BFX_ECHO2 = 0x1000A;
    public static final int BASS_FX_BFX_PHASER = 0x1000B;
    public static final int BASS_FX_BFX_ECHO3 = 0x1000C;
    public static final int BASS_FX_BFX_CHORUS = 0x1000D;
    public static final int BASS_FX_BFX_APF = 0x1000E;
    public static final int BASS_FX_BFX_COMPRESSOR = 0x1000F;
    public static final int BASS_FX_BFX_DISTORTION = 0x10010;
    public static final int BASS_FX_BFX_COMPRESSOR2 = 0x10011;
    public static final int BASS_FX_BFX_VOLUME_ENV = 0x10012;
    public static final int BASS_FX_BFX_BQF = 0x10013;
    public static final int BASS_FX_BFX_ECHO4 = 0x10014;
    public static final int BASS_FX_BFX_PITCHSHIFT = 0x10015;
    public static final int BASS_FX_BFX_FREEVERB = 0x10016;

    public static final int BASS_BFX_CHANALL = -1;
    public static final int BASS_BFX_CHANNONE = 0;
    public static final int BASS_BFX_CHAN1 = 1;
    public static final int BASS_BFX_CHAN2 = 2;

    public static class BASS_BFX_PEAKEQ {
        public int lBand;
        public float fBandwidth;
        public float fQ;
        public float fCenter;
        public float fGain;
        public int lChannel;
    }

    public static class BASS_BFX_COMPRESSOR2 {
        public float fGain;
        public float fThreshold;
        public float fRatio;
        public float fAttack;
        public float fRelease;
        public int lChannel;
    }

    public static class BASS_BFX_FREEVERB {
        public float fDryMix;
        public float fWetMix;
        public float fRoomSize;
        public float fDamp;
        public float fWidth;
        public int lMode;
        public int lChannel;
    }

    public static class BASS_BFX_ECHO4 {
        public float fDryMix;
        public float fWetMix;
        public float fFeedback;
        public float fDelay;
        public boolean bStereo;
        public int lChannel;
    }

    public static class BASS_BFX_CHORUS {
        public float fDryMix;
        public float fWetMix;
        public float fFeedback;
        public float fMinSweep;
        public float fMaxSweep;
        public float fRate;
        public int lChannel;
    }

    public static native int BASS_FX_GetVersion();
    public static native int BASS_FX_TempoCreate(int chan, int flags);
    public static native int BASS_FX_TempoGetSource(int chan);
    public static native float BASS_FX_TempoGetRateRatio(int chan);

    static {
        System.loadLibrary("bass_fx");
    }
}
