import zlib
import struct

with open(r"C:\Users\jehue\.gemini\antigravity\scratch\videokey\sector23.bin", "rb") as f:
    raw = f.read()

comp_size = struct.unpack_from("<I", raw, 9)[0]
data = raw[13:13+comp_size]

for wbits in [-15, 15, 31, -zlib.MAX_WBITS, zlib.MAX_WBITS]:
    try:
        decomp = zlib.decompress(data, wbits)
        print(f"Direct zlib SUCCESS with wbits={wbits}! Decompressed size={len(decomp)}")
        if decomp[:4] == b"MThd":
            print("*** VALID MIDI FOUND! ***")
    except Exception as e:
        pass
