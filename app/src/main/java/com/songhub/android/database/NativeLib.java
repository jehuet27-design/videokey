package com.songhub.android.database;

import android.util.Log;

public final class NativeLib {

    public static boolean isLoaded = false;

    static {
        try {
            System.loadLibrary("c++_shared");
            System.loadLibrary("Mewtwo");
            isLoaded = true;
            Log.d("NativeLib", "libMewtwo successfully loaded!");
        } catch (Throwable t) {
            Log.e("NativeLib", "Failed to load libMewtwo", t);
            isLoaded = false;
        }
    }

    public NativeLib() {}

    public static native byte[] processMidiData(byte[] bArr, boolean z);

    public static native boolean verifyDiagnosticPassword(String str);

    public static boolean isActivated() {
        return true;
    }

    public static String getDeviceIdHash(String str) {
        return "ACTIVATED";
    }

    public static boolean verifyLicenseKey(String str, String str2) {
        return true;
    }
}
