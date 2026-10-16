import os
import shutil

# Patch arm64-v8a
path_arm64 = r"C:\Users\jehue\.gemini\antigravity\scratch\videokey\app\src\main\jniLibs\arm64-v8a\libMewtwo.so"
backup_arm64 = path_arm64 + ".orig"
if not os.path.exists(backup_arm64):
    shutil.copyfile(path_arm64, backup_arm64)

with open(path_arm64, "rb") as f:
    data64 = bytearray(f.read())

# Check offset 0x2adc: should be cbz w8, 0x2d54 (0x340013c8)
import struct
w = struct.unpack_from("<I", data64, 0x2adc)[0]
print(f"arm64 at 0x2adc: 0x{w:08x}")
assert w == 0x340013c8, f"Expected 0x340013c8 but got 0x{w:08x}"

# Patch 0x2adc with NOP (0xd503201f)
struct.pack_into("<I", data64, 0x2adc, 0xd503201f)

# Patch isActivated at 0x35a8: mov w0, #1 (0x52800020), ret (0xd65f03c0)
struct.pack_into("<I", data64, 0x35a8, 0x52800020) # mov w0, #1
struct.pack_into("<I", data64, 0x35ac, 0xd65f03c0) # ret

with open(path_arm64, "wb") as f:
    f.write(data64)
print("arm64-v8a libMewtwo.so successfully patched!")

# Patch armeabi-v7a
path_arm32 = r"C:\Users\jehue\.gemini\antigravity\scratch\videokey\app\src\main\jniLibs\armeabi-v7a\libMewtwo.so"
backup_arm32 = path_arm32 + ".orig"
if not os.path.exists(backup_arm32):
    shutil.copyfile(path_arm32, backup_arm32)

with open(path_arm32, "rb") as f:
    data32 = bytearray(f.read())

# In 32-bit:
# 0x1c78: f000 80d6 (beq) -> patch with NOP NOP (0xbf00 0xbf00)
hw1, hw2 = struct.unpack_from("<HH", data32, 0x1c78)
print(f"arm32 at 0x1c78: 0x{hw1:04x} 0x{hw2:04x}")
struct.pack_into("<HH", data32, 0x1c78, 0xbf00, 0xbf00)

# 0x24bc: isActivated -> patch with movs r0, #1 (0x2001), bx lr (0x4770)
struct.pack_into("<HH", data32, 0x24bc, 0x2001, 0x4770)

with open(path_arm32, "wb") as f:
    f.write(data32)
print("armeabi-v7a libMewtwo.so successfully patched!")
