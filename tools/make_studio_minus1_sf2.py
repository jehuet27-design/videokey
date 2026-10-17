import struct
import io
from sf2utils.sf2parse import Sf2File

def build_minus1_soundfont(src_path, dst_path):
    print(f'Loading {src_path}...')
    with open(src_path, 'rb') as f:
        data = f.read()

    # Parse top-level chunks
    pos = 12
    chunks = {}
    chunk_order = []
    while pos < len(data):
        ch_id = data[pos:pos+4]
        ch_len = struct.unpack('<I', data[pos+4:pos+8])[0]
        ch_type = data[pos+8:pos+12] if ch_id == b'LIST' else b''
        ch_data = data[pos+8:pos+8+ch_len]
        chunks[(ch_id, ch_type)] = ch_data
        chunk_order.append((ch_id, ch_type))
        pos += 8 + ch_len + (ch_len % 2)

    # 1. Customize INFO chunk
    info_data = chunks[(b'LIST', b'INFO')]
    info_subchunks = []
    p = 4 # after b'INFO'
    while p < len(info_data):
        sub_id = info_data[p:p+4]
        sub_len = struct.unpack('<I', info_data[p+4:p+8])[0]
        sub_val = info_data[p+8:p+8+sub_len]
        info_subchunks.append((sub_id, sub_val))
        p += 8 + sub_len + (sub_len % 2)

    new_info_subchunks = []
    for sub_id, sub_val in info_subchunks:
        if sub_id == b'INAM':
            val = b'VideoKey Studio Minus-One\x00' # 26 bytes (even)
            new_info_subchunks.append((sub_id, val))
        elif sub_id == b'ISFT':
            val = b'VideoKey Audio Lab / Jerome Huet\x00\x00' # 34 bytes (even)
            new_info_subchunks.append((sub_id, val))
        elif sub_id == b'ICRD':
            val = b'2026-10-16 23:00:00\x00' # 20 bytes (even)
            new_info_subchunks.append((sub_id, val))
        else:
            new_info_subchunks.append((sub_id, sub_val))

    # Add IENG and ICMT (both even length)
    new_info_subchunks.append((b'IENG', b'Developed by Jerome Huet\x00\x00')) # 26 bytes
    new_info_subchunks.append((b'ICMT', b'Balanced Studio Minus-One SoundFont for Jerome Huet VideoKey\x00')) # 60 bytes

    out_info = io.BytesIO()
    out_info.write(b'INFO')
    for sub_id, sub_val in new_info_subchunks:
        # Pad string to even length if needed
        val = sub_val if len(sub_val) % 2 == 0 else sub_val + b'\x00'
        out_info.write(sub_id)
        out_info.write(struct.pack('<I', len(val)))
        out_info.write(val)
    chunks[(b'LIST', b'INFO')] = out_info.getvalue()

    # 2. Parse pdta subchunks
    pdta_data = chunks[(b'LIST', b'pdta')]
    p = 4 # after b'pdta'
    pdta_subs = {}
    pdta_order = []
    while p < len(pdta_data):
        sub_id = pdta_data[p:p+4].decode('latin1')
        sub_len = struct.unpack('<I', pdta_data[p+4:p+8])[0]
        sub_val = pdta_data[p+8:p+8+sub_len]
        pdta_subs[sub_id] = sub_val
        pdta_order.append(sub_id)
        p += 8 + sub_len + (sub_len % 2)

    phdr = pdta_subs['phdr']
    pbag = pdta_subs['pbag']
    pmod = pdta_subs['pmod']
    pgen = pdta_subs['pgen']

    num_presets = len(phdr) // 38
    presets = []
    for i in range(num_presets):
        rec = phdr[i*38:(i+1)*38]
        name = rec[:20].rstrip(b'\x00').decode('latin1', errors='ignore')
        preset, bank, bag_idx = struct.unpack('<HHH', rec[20:26])
        dwLib, dwGenre, dwMorph = struct.unpack('<III', rec[26:38])
        presets.append({
            'index': i,
            'name': name,
            'preset': preset,
            'bank': bank,
            'bag_idx': bag_idx,
            'raw': rec
        })

    # Tuning rules for Studio Minus-One balance:
    def get_preset_tuning(bank, prog, name):
        if bank == 128:
            # All drum kits: punchy kick and crisp snare with live room reverb
            return {
                'attenuation': -40, # +4.0 dB boost for kick/snare punch
                'reverb': 160,      # 16% room reverb
                'chorus': 0
            }

        # Melodic instruments (GM standard program 0-127)
        if 32 <= prog <= 39:
            # Bass (Acoustic, Fingered, Picked, Fretless, Slap 1 & 2, Synth 1 & 2)
            return {
                'attenuation': -45, # +4.5 dB boost for solid low-end foundation
                'reverb': 25,       # Tight, clean bass with minimal muddy reverb
                'chorus': 0
            }
        elif 0 <= prog <= 7:
            # Pianos: Concert Grand, Bright, Electric Grand, Rhodes EP, DX7 EP
            is_ep = prog in (4, 5)
            return {
                'attenuation': -15, # Warm, rich piano harmony
                'reverb': 300,      # Concert hall resonance
                'chorus': 250 if is_ep else 0
            }
        elif 24 <= prog <= 31:
            # Guitars: Nylon, Steel, Jazz, Clean, Muted, Overdrive, Distortion
            is_clean_elec = prog in (26, 27)
            return {
                'attenuation': -12, # Crisp rhythm strumming and acoustic resonance
                'reverb': 280,
                'chorus': 220 if is_clean_elec else 0
            }
        elif 40 <= prog <= 55:
            # Strings & Orchestra: Violin, Cello, Strings 1 & 2, Choir, Orchestra Hit
            return {
                'attenuation': -5,  # Lush, spacious pad behind singer
                'reverb': 380,      # Wide stereo hall reverb
                'chorus': 200
            }
        elif 56 <= prog <= 71:
            # Brass & Reed: Trumpet, Trombone, Saxophones, Oboe, Clarinet
            # Tame harsh brass so it doesn't pierce ears or drown singer!
            return {
                'attenuation': 35,  # 3.5 dB attenuation (warm & studio-balanced)
                'reverb': 220,
                'chorus': 0
            }
        elif 72 <= prog <= 79:
            # Flute & Pipes: Flute, Piccolo, Pan Flute, Recorder
            return {
                'attenuation': -15, # Sweet, clear melody guide tone
                'reverb': 180,
                'chorus': 0
            }
        elif 80 <= prog <= 87:
            # Synth Leads: Softened so synths blend smoothly
            return {
                'attenuation': 30,  # 3.0 dB attenuation
                'reverb': 250,
                'chorus': 150
            }
        elif 88 <= prog <= 95:
            # Synth Pads: Warm atmospheric background
            return {
                'attenuation': 0,
                'reverb': 350,
                'chorus': 200
            }
        elif 16 <= prog <= 23:
            # Organs: Smooth harmonic bed
            return {
                'attenuation': 10,
                'reverb': 200,
                'chorus': 100
            }
        elif 8 <= prog <= 15:
            # Chromatic Percussion: Glockenspiel, Music Box, Marimba
            return {
                'attenuation': -10,
                'reverb': 260,
                'chorus': 0
            }
        else:
            # Default GM balance
            return {
                'attenuation': 0,
                'reverb': 240,
                'chorus': 0
            }

    # Reconstruct pgen and pbag
    new_pgen = bytearray()
    new_pbag = bytearray()
    new_phdr = bytearray()

    current_bag_idx = 0
    current_gen_idx = 0

    for i in range(num_presets - 1):
        p_info = presets[i]
        p_next = presets[i + 1]
        bag_start = p_info['bag_idx']
        bag_end = p_next['bag_idx']

        tuning = get_preset_tuning(p_info['bank'], p_info['preset'], p_info['name'])

        # Store updated preset record with current_bag_idx
        # Preserve original 38-byte record but update wPresetBagNdx (bytes 24..26)
        rec = bytearray(p_info['raw'])
        rec[24:26] = struct.pack('<H', current_bag_idx)
        new_phdr.extend(rec)

        # Bag 0 of preset: Global Zone
        # Get original mod_idx for global bag
        orig_global_bag = pbag[bag_start*4:(bag_start+1)*4]
        orig_global_gstart = struct.unpack('<H', orig_global_bag[0:2])[0]
        orig_global_mstart = struct.unpack('<H', orig_global_bag[2:4])[0]

        # Write new global bag pointing to current_gen_idx
        new_pbag.extend(struct.pack('<HH', current_gen_idx, orig_global_mstart))
        current_bag_idx += 1

        # Emit tuned generators for global zone:
        # OPER_INITIAL_ATTENUATION = 48
        new_pgen.extend(struct.pack('<Hh', 48, tuning['attenuation']))
        current_gen_idx += 1

        # OPER_REVERB_EFFECTS_SEND = 16
        if tuning['reverb'] > 0:
            new_pgen.extend(struct.pack('<Hh', 16, tuning['reverb']))
            current_gen_idx += 1

        # OPER_CHORUS_EFFECTS_SEND = 15
        if tuning['chorus'] > 0:
            new_pgen.extend(struct.pack('<Hh', 15, tuning['chorus']))
            current_gen_idx += 1

        # Copy any remaining non-attenuation, non-reverb, non-chorus generators from original global bag
        orig_global_gend = struct.unpack('<H', pbag[(bag_start+1)*4:(bag_start+1)*4+2])[0]
        for g in range(orig_global_gstart, orig_global_gend):
            oper, amount = struct.unpack('<Hh', pgen[g*4:(g+1)*4])
            if oper not in (48, 16, 15):
                new_pgen.extend(struct.pack('<Hh', oper, amount))
                current_gen_idx += 1

        # Subsequent bags: Instrument Zones (bag_start + 1 to bag_end - 1)
        for b in range(bag_start + 1, bag_end):
            b_data = pbag[b*4:(b+1)*4]
            b_gstart = struct.unpack('<H', b_data[0:2])[0]
            b_mstart = struct.unpack('<H', b_data[2:4])[0]
            b_gend = struct.unpack('<H', pbag[(b+1)*4:(b+1)*4+2])[0]

            new_pbag.extend(struct.pack('<HH', current_gen_idx, b_mstart))
            current_bag_idx += 1

            for g in range(b_gstart, b_gend):
                oper, amount = struct.unpack('<Hh', pgen[g*4:(g+1)*4])
                new_pgen.extend(struct.pack('<Hh', oper, amount))
                current_gen_idx += 1

    # Terminal preset record (EOP)
    last_p = presets[-1]
    rec = bytearray(last_p['raw'])
    rec[24:26] = struct.pack('<H', current_bag_idx)
    new_phdr.extend(rec)

    # Terminal bag record
    last_bag = pbag[len(pbag)-4:len(pbag)]
    last_mstart = struct.unpack('<H', last_bag[2:4])[0]
    new_pbag.extend(struct.pack('<HH', current_gen_idx, last_mstart))

    # Terminal generator record (null gen)
    new_pgen.extend(struct.pack('<HH', 0, 0))

    # Update pdta subchunks
    pdta_subs['phdr'] = bytes(new_phdr)
    pdta_subs['pbag'] = bytes(new_pbag)
    pdta_subs['pgen'] = bytes(new_pgen)

    # Re-pack pdta
    out_pdta = io.BytesIO()
    out_pdta.write(b'pdta')
    for sub_id in pdta_order:
        sub_data = pdta_subs[sub_id]
        out_pdta.write(sub_id.encode('latin1'))
        out_pdta.write(struct.pack('<I', len(sub_data)))
        out_pdta.write(sub_data)
        if len(sub_data) % 2 == 1:
            out_pdta.write(b'\x00')
    chunks[(b'LIST', b'pdta')] = out_pdta.getvalue()

    # Write out the full SF2 file
    print(f'Writing to {dst_path}...')
    with open(dst_path, 'wb') as out_f:
        # Header placeholder
        out_f.write(b'RIFF\x00\x00\x00\x00sfbk')
        total_data = 4 # for 'sfbk'

        for ch_id, ch_type in chunk_order:
            ch_data = chunks[(ch_id, ch_type)]
            out_f.write(ch_id)
            out_f.write(struct.pack('<I', len(ch_data)))
            out_f.write(ch_data)
            total_data += 8 + len(ch_data)
            if len(ch_data) % 2 == 1:
                out_f.write(b'\x00')
                total_data += 1

        out_f.seek(4)
        out_f.write(struct.pack('<I', total_data))

    print(f'Done! Output size: {total_data + 8} bytes')

    # Verify with sf2utils
    print('Verifying with sf2utils...')
    sf = Sf2File(open(dst_path, 'rb'))
    print(f'Verified successfully! Bank name: {sf.raw.info.get(b"INAM")}')
    print(f'Total presets parsed: {len(sf.presets)}')
    for p in sf.presets[:5]:
        print(f'  Preset {p.bank}:{p.preset} {p.name}')
    return True

if __name__ == '__main__':
    src = r'C:\Users\jehue\Music\SoundFont\Dream SAM2634 (ver. 2.1).sf2'
    dst = r'C:\Users\jehue\Music\SoundFont\VideoKey_Studio_Minus1.sf2'
    build_minus1_soundfont(src, dst)
