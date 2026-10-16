with open(r"C:\Users\jehue\.gemini\antigravity\scratch\videokey\app\src\main\jniLibs\arm64-v8a\libMewtwo.so", "rb") as f:
    data = f.read()

print("__android_log_print in libMewtwo:", b"__android_log_print" in data)
