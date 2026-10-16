import struct

with open(r"C:\Users\jehue\.gemini\antigravity\brain\fed25705-cdc9-445e-89b6-52869d039478\shubidx", "rb") as f:
    data = f.read()

max_sector = 606828544 // 2048 # 296303 sectors in shubblob
print(f"Max sector in shubblob: {max_sector}")

playable_songs = []
pos = 0x4FAF0
while pos < len(data) - 12:
    start = pos
    while pos < len(data) - 12 and 32 <= data[pos] <= 126:
        pos += 1
    if pos < len(data) - 12 and data[pos] == 1:
        code = struct.unpack_from("<I", data, pos + 4)[0]
        sec = struct.unpack_from("<I", data, pos + 8)[0]
        scount = data[pos + 1]
        if code in range(1, 1000000):
            title = data[start:pos].decode('latin1', 'ignore').strip()
            if sec < max_sector:
                playable_songs.append((code, title, sec, scount))
        pos += 12
    else:
        pos += 1

print(f"Total songs parsed with valid code: {len(playable_songs)}")
print("First 20 playable songs:")
for code, title, sec, scount in playable_songs[:20]:
    print(f"  [{code:06d}] {title:35s} (Sector: {sec:6d}, SCount: {scount})")
