import struct
import io
import sys
from sf2utils.sf2parse import Sf2File

def test_repack():
    src_path = r'C:\Users\jehue\Music\SoundFont\Dream SAM2634 (ver. 2.1).sf2'
    with open(src_path, 'rb') as f:
        data = f.read()

    # Verify RIFF header
    riff_id = data[:4]
    riff_len = struct.unpack('<I', data[4:8])[0]
    sfbk_id = data[8:12]
    assert riff_id == b'RIFF' and sfbk_id == b'sfbk'

    pos = 12
    chunks = []
    while pos < len(data):
        ch_id = data[pos:pos+4]
        ch_len = struct.unpack('<I', data[pos+4:pos+8])[0]
        ch_type = data[pos+8:pos+12] if ch_id == b'LIST' else b''
        ch_data = data[pos+8:pos+8+ch_len]
        chunks.append((ch_id, ch_type, ch_len, ch_data))
        pos += 8 + ch_len + (ch_len % 2)

    print(f'Parsed {len(chunks)} top-level chunks:')
    for ch_id, ch_type, ch_len, _ in chunks:
        print(f'  {ch_id.decode()} ({ch_type.decode()}): len={ch_len}')

    # Repack into a buffer
    out = io.BytesIO()
    # Placeholder for RIFF header
    out.write(b'RIFF\x00\x00\x00\x00sfbk')

    for ch_id, ch_type, ch_len, ch_data in chunks:
        out.write(ch_id)
        out.write(struct.pack('<I', len(ch_data)))
        out.write(ch_data)
        if len(ch_data) % 2 == 1:
            out.write(b'\x00')

    total_size = out.tell()
    out.seek(4)
    out.write(struct.pack('<I', total_size - 8))

    repacked_bytes = out.getvalue()
    print(f'Original size: {len(data)}, Repacked size: {len(repacked_bytes)}')
    assert len(data) == len(repacked_bytes), 'Sizes do not match!'
    assert data == repacked_bytes, 'Bytes do not match exactly!'
    print('PASS: Exact byte-level round-trip succeeded!')

if __name__ == '__main__':
    test_repack()
