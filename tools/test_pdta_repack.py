import struct
import io
from sf2utils.sf2parse import Sf2File

def test_pdta_repack():
    src_path = r'C:\Users\jehue\Music\SoundFont\Dream SAM2634 (ver. 2.1).sf2'
    with open(src_path, 'rb') as f:
        data = f.read()

    pos = 12
    pdta_chunk = None
    while pos < len(data):
        ch_id = data[pos:pos+4]
        ch_len = struct.unpack('<I', data[pos+4:pos+8])[0]
        ch_type = data[pos+8:pos+12] if ch_id == b'LIST' else b''
        if ch_id == b'LIST' and ch_type == b'pdta':
            pdta_chunk = data[pos+8:pos+8+ch_len]
            break
        pos += 8 + ch_len + (ch_len % 2)

    assert pdta_chunk is not None, 'pdta not found'
    print(f'pdta total len: {len(pdta_chunk)}')

    # Parse pdta subchunks
    pos = 4 # after b'pdta'
    subchunks = []
    order = ['phdr', 'pbag', 'pmod', 'pgen', 'inst', 'ibag', 'imod', 'igen', 'shdr']
    while pos < len(pdta_chunk):
        sub_id = pdta_chunk[pos:pos+4].decode('latin1', errors='ignore')
        sub_len = struct.unpack('<I', pdta_chunk[pos+4:pos+8])[0]
        sub_data = pdta_chunk[pos+8:pos+8+sub_len]
        subchunks.append((sub_id, sub_data))
        print(f'  subchunk {sub_id}: len={sub_len}')
        pos += 8 + sub_len + (sub_len % 2)

    # Re-pack pdta
    out_pdta = io.BytesIO()
    out_pdta.write(b'pdta')
    for sub_id, sub_data in subchunks:
        out_pdta.write(sub_id.encode('latin1'))
        out_pdta.write(struct.pack('<I', len(sub_data)))
        out_pdta.write(sub_data)
        if len(sub_data) % 2 == 1:
            out_pdta.write(b'\x00')

    repacked_pdta = out_pdta.getvalue()
    assert len(repacked_pdta) == len(pdta_chunk), f'pdta lens differ: {len(repacked_pdta)} vs {len(pdta_chunk)}'
    assert repacked_pdta == pdta_chunk, 'pdta bytes differ!'
    print('PASS: pdta subchunk round-trip succeeded!')

if __name__ == '__main__':
    test_pdta_repack()
