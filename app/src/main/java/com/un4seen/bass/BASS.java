package com.un4seen.bass;

import android.content.res.AssetManager;
import android.os.ParcelFileDescriptor;
import java.nio.ByteBuffer;

public class BASS {
    public static final int BASSVERSION = 516;
    public static final String BASSVERSIONTEXT = "2.4";

    public static final int BASS_ACTIVE_STOPPED = 0;
    public static final int BASS_ACTIVE_PLAYING = 1;
    public static final int BASS_ACTIVE_STALLED = 2;
    public static final int BASS_ACTIVE_PAUSED = 3;
    public static final int BASS_ACTIVE_PAUSED_DEVICE = 4;

    public static final int BASS_CONFIG_BUFFER = 0;
    public static final int BASS_CONFIG_UPDATEPERIOD = 1;
    public static final int BASS_CONFIG_DEV_PERIOD = 19;
    public static final int BASS_CONFIG_UPDATETHREADS = 24;
    public static final int BASS_CONFIG_DEV_BUFFER = 27;
    public static final int BASS_CONFIG_SRC_SAMPLE = 54;
    public static final int BASS_CONFIG_ANDROID_SESSIONID = 62;
    public static final int BASS_CONFIG_ANDROID_AAUDIO = 67;

    public static final int BASS_SAMPLE_8BITS = 1;
    public static final int BASS_SAMPLE_MONO = 2;
    public static final int BASS_SAMPLE_LOOP = 4;
    public static final int BASS_SAMPLE_3D = 8;
    public static final int BASS_SAMPLE_SOFTWARE = 16;
    public static final int BASS_SAMPLE_FX = 128;
    public static final int BASS_SAMPLE_FLOAT = 256;
    public static final int BASS_STREAM_PRESCAN = 0x20000;
    public static final int BASS_STREAM_AUTOFREE = 0x40000;
    public static final int BASS_STREAM_DECODE = 0x200000;
    public static final int BASS_ASYNCFILE = 0x40000000;

    public static final int BASS_POS_BYTE = 0;
    public static final int BASS_POS_MUSIC_ORDER = 1;

    public static final int BASS_ATTRIB_FREQ = 1;
    public static final int BASS_ATTRIB_VOL = 2;
    public static final int BASS_ATTRIB_PAN = 3;
    public static final int BASS_ATTRIB_EAXMIX = 4;
    public static final int BASS_ATTRIB_NOBUFFER = 5;
    public static final int BASS_ATTRIB_VBR = 6;
    public static final int BASS_ATTRIB_CPU = 7;
    public static final int BASS_ATTRIB_SRC = 8;
    public static final int BASS_ATTRIB_BUFFER = 13;

    public static final int BASS_SYNC_POS = 0;
    public static final int BASS_SYNC_END = 2;
    public static final int BASS_SYNC_MIXTIME = 0x40000000;
    public static final int BASS_SYNC_ONETIME = 0x80000000;

    public static class Asset {
        public AssetManager manager;
        public String file;
        public Asset() {}
        public Asset(AssetManager m, String f) { manager = m; file = f; }
    }

    public static class FloatValue {
        public float value;
    }

    public static class IntValue {
        public int value;
    }

    public static class BASS_DEVICEINFO {
        public String name;
        public String driver;
        public int flags;
    }

    public static class BASS_INFO {
        public int flags;
        public int dsver;
        public int minbuf;
        public int latency;
        public int initflags;
        public int speakers;
        public int freq;
    }

    public static class BASS_RECORDINFO {
        public int flags;
        public int formats;
        public int inputs;
        public boolean singlein;
        public int freq;
    }

    public static class BASS_CHANNELINFO {
        public int freq;
        public int chans;
        public int flags;
        public int ctype;
        public int origres;
        public int plugin;
        public int sample;
        public String filename;
    }

    public static class BASS_PLUGINFORM {
        public int ctype;
        public String name;
        public String exts;
    }

    public static class BASS_PLUGININFO {
        public int version;
        public int formatc;
        public BASS_PLUGINFORM[] formats;
    }

    public static class BASS_3DVECTOR {
        public float x;
        public float y;
        public float z;
        public BASS_3DVECTOR() {}
        public BASS_3DVECTOR(float _x, float _y, float _z) { x = _x; y = _y; z = _z; }
    }

    public static class BASS_SAMPLE {
        public int freq;
        public float volume;
        public float pan;
        public int flags;
        public int length;
        public int max;
        public int origres;
        public int chans;
        public int mingap;
        public int mode3d;
        public float mindist;
        public float maxdist;
        public int iangle;
        public int oangle;
        public float outvol;
    }

    public interface SYNCPROC {
        void SYNCPROC(int handle, int channel, int data, Object user);
    }

    public interface DSPPROC {
        void DSPPROC(int handle, int channel, ByteBuffer buffer, int length, Object user);
    }

    public interface RECORDPROC {
        boolean RECORDPROC(int handle, ByteBuffer buffer, int length, Object user);
    }

    public interface STREAMPROC {
        int STREAMPROC(int handle, ByteBuffer buffer, int length, Object user);
    }

    public interface DOWNLOADPROC {
        void DOWNLOADPROC(ByteBuffer buffer, int length, Object user);
    }

    public interface BASS_FILEPROCS {
        void FILECLOSEPROC(Object user);
        long FILELENPROC(Object user);
        int FILEREADPROC(ByteBuffer buffer, int length, Object user);
        boolean FILESEEKPROC(long offset, Object user);
    }

    public static native boolean BASS_Init(int device, int freq, int flags);
    public static native boolean BASS_Free();
    public static native boolean BASS_Start();
    public static native boolean BASS_Stop();
    public static native boolean BASS_Pause();
    public static native int BASS_IsStarted();
    public static native int BASS_ErrorGetCode();
    public static native int BASS_GetVersion();
    public static native boolean BASS_SetConfig(int option, int newvalue);
    public static native int BASS_GetConfig(int option);
    public static native boolean BASS_SetConfigPtr(int option, Object newvalue);
    public static native Object BASS_GetConfigPtr(int option);
    public static native int BASS_PluginLoad(String file, int flags);
    public static native boolean BASS_PluginFree(int handle);
    public static native boolean BASS_SetVolume(float volume);
    public static native float BASS_GetVolume();
    public static native float BASS_GetCPU();

    public static native int BASS_StreamCreate(int freq, int chans, int flags, STREAMPROC proc, Object user);
    public static native int BASS_StreamCreate(int freq, int chans, int flags, int proc, Object user);
    public static native int BASS_StreamCreateFile(String file, long offset, long length, int flags);
    public static native int BASS_StreamCreateFile(ByteBuffer memory, long offset, long length, int flags);
    public static native int BASS_StreamCreateFile(ParcelFileDescriptor pfd, long offset, long length, int flags);
    public static native int BASS_StreamCreateFile(Asset asset, long offset, long length, int flags);
    public static native boolean BASS_StreamFree(int handle);

    public static native boolean BASS_ChannelPlay(int handle, boolean restart);
    public static native boolean BASS_ChannelStart(int handle);
    public static native boolean BASS_ChannelPause(int handle);
    public static native boolean BASS_ChannelStop(int handle);
    public static native boolean BASS_ChannelFree(int handle);
    public static native int BASS_ChannelIsActive(int handle);
    public static native boolean BASS_ChannelGetInfo(int handle, BASS_CHANNELINFO info);
    public static native long BASS_ChannelFlags(int handle, int flags, int mask);
    public static native boolean BASS_ChannelUpdate(int handle, int length);
    public static native boolean BASS_ChannelLock(int handle, boolean lock);

    public static native long BASS_ChannelGetLength(int handle, int mode);
    public static native long BASS_ChannelGetPosition(int handle, int mode);
    public static native boolean BASS_ChannelSetPosition(int handle, long pos, int mode);
    public static native double BASS_ChannelBytes2Seconds(int handle, long pos);
    public static native long BASS_ChannelSeconds2Bytes(int handle, double pos);

    public static native boolean BASS_ChannelSetAttribute(int handle, int attrib, float value);
    public static native boolean BASS_ChannelGetAttribute(int handle, int attrib, FloatValue value);
    public static native boolean BASS_ChannelSlideAttribute(int handle, int attrib, float value, int time);
    public static native boolean BASS_ChannelIsSliding(int handle, int attrib);

    public static native int BASS_ChannelGetLevel(int handle);
    public static native boolean BASS_ChannelGetLevelEx(int handle, float[] levels, float length, int flags);
    public static native int BASS_ChannelGetData(int handle, ByteBuffer buffer, int length);

    public static native int BASS_ChannelSetSync(int handle, int type, long param, SYNCPROC proc, Object user);
    public static native boolean BASS_ChannelRemoveSync(int handle, int sync);
    public static native int BASS_ChannelSetDSP(int handle, DSPPROC proc, Object user, int priority);
    public static native boolean BASS_ChannelRemoveDSP(int handle, int dsp);

    public static native int BASS_ChannelSetFX(int handle, int type, int priority);
    public static native boolean BASS_ChannelRemoveFX(int handle, int fx);
    public static native boolean BASS_FXSetParameters(int handle, Object params);
    public static native boolean BASS_FXGetParameters(int handle, Object params);
    public static native boolean BASS_FXReset(int handle);
    public static native boolean BASS_FXSetBypass(int handle, boolean bypass);

    public static native boolean BASS_RecordInit(int device);
    public static native boolean BASS_RecordFree();
    public static native int BASS_RecordGetDevice();
    public static native int BASS_RecordStart(int freq, int chans, int flags, RECORDPROC proc, Object user);
    public static native int BASS_RecordStart(int freq, int chans, int flags, int proc, Object user);

    static {
        System.loadLibrary("bass");
    }
}
