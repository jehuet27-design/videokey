import struct

with open(r"C:\Users\jehue\.gemini\antigravity\scratch\videokey\app\src\main\jniLibs\arm64-v8a\libMewtwo.so", "rb") as f:
    data = f.read()

# Let's decode 0x4f1c to 0x4f3c
for addr in range(0x4f1c, 0x4f40, 4):
    w = struct.unpack_from("<I", data, addr)[0]
    print(f"0x{addr:04x}: {w:08x}")
