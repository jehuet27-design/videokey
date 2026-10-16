import zlib
import struct

with open(r"C:\Users\jehue\.gemini\antigravity\scratch\videokey\sector23.bin", "rb") as f:
    raw = f.read()

print(f"Read {len(raw)} bytes from sector23.bin")
magic = raw[:4]
print("Magic:", magic)
ver = raw[4]
uncomp_size = struct.unpack_from("<I", raw, 5)[0]
comp_size = struct.unpack_from("<I", raw, 9)[0]
print(f"Format: ver={ver}, uncomp_size={uncomp_size}, comp_size={comp_size}")

comp_data = raw[13:13+comp_size]
print(f"Compressed data slice length: {len(comp_data)}")

# Let's inspect libMewtwo.so rodata around 0x1200-0x1400
with open(r"C:\Users\jehue\.gemini\antigravity\scratch\videokey\app\src\main\jniLibs\arm64-v8a\libMewtwo.so", "rb") as f:
    so_data = f.read()

# Let's test all possible 32-byte chunks in rodata (0x11f0 to 0x1500)
found_any = False
for ro_off in range(0x11f0, 0x1500, 1):
    k = so_data[ro_off:ro_off+32]
    if len(k) < 32: continue
    xored = bytearray(len(comp_data))
    for i in range(len(comp_data)):
        xored[i] = comp_data[i] ^ k[i % 32]
    for wbits in [-15, 15, -zlib.MAX_WBITS, zlib.MAX_WBITS]:
        try:
            decomp = zlib.decompress(bytes(xored), wbits)
            print(f"SUCCESS with rodata offset 0x{ro_off:x}, wbits={wbits}! Decompressed size={len(decomp)}")
            if decomp[:4] == b"MThd":
                print(f"*** VALID MIDI FOUND! Header: MThd ***")
                with open(r"C:\Users\jehue\.gemini\antigravity\scratch\videokey\pamela_one_test.mid", "wb") as mf:
                    mf.write(decomp)
            found_any = True
            break
        except Exception:
            pass
    if found_any: break

if not found_any:
    print("Direct 32-byte chunk scan in rodata did not match, trying other key combinations...")
