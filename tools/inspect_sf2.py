import struct

with open(r'C:\Users\jehue\Music\SoundFont\Dream SAM2634 (ver. 2.1).sf2', 'rb') as f:
    f.seek(141286200)
    data = f.read(168200)

pos = 12
subchunks = {}
while pos < len(data):
    ch_id = data[pos:pos+4].decode('latin1', errors='ignore')
    if len(ch_id) < 4:
        break
    ch_len = struct.unpack('<I', data[pos+4:pos+8])[0]
    subchunks[ch_id] = data[pos+8:pos+8+ch_len]
    pos += 8 + ch_len + (ch_len % 2)

inst = subchunks['inst']
ibag = subchunks['ibag']
igen = subchunks['igen']

def inspect_inst(inst_idx):
    name = inst[inst_idx*22:inst_idx*22+20].rstrip(b'\x00').decode('latin1', errors='ignore')
    b_start = struct.unpack('<H', inst[inst_idx*22+20:inst_idx*22+22])[0]
    b_end = struct.unpack('<H', inst[(inst_idx+1)*22+20:(inst_idx+1)*22+22])[0]
    print(f'Instrument {inst_idx} "{name}" (bags {b_start}..{b_end}):')
    for b in range(b_start, min(b_start + 4, b_end)):
        g_start = struct.unpack('<H', ibag[b*4:b*4+2])[0]
        g_end = struct.unpack('<H', ibag[(b+1)*4:(b+1)*4+2])[0]
        gens = [struct.unpack('<Hh', igen[g*4:(g+1)*4]) for g in range(g_start, g_end)]
        interesting = [g for g in gens if g[0] in (48, 16, 15, 8, 17)]
        print(f'  bag {b}: total_gens={len(gens)}, key={interesting}')

inspect_inst(299) # HQ STANDARD 1 drums
inspect_inst(300) # HQ STANDARD 1(K,S&T)
inspect_inst(64)  # SC55 Piano 1
inspect_inst(175) # Fingered Bass
