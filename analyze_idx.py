import re
import struct

path = r'C:\Users\jehue\.gemini\antigravity\brain\fed25705-cdc9-445e-89b6-52869d039478\shubidx'
with open(path, 'rb') as f:
    data = f.read()

print('Length:', len(data))

# Search for printable ASCII strings of length >= 4
matches = list(re.finditer(b'[A-Za-z0-9 ,.\'-]{4,}', data))
print('Total matches:', len(matches))
for m in matches[:30]:
    start = m.start()
    text = m.group().decode('latin1', errors='ignore')
    prefix = data[max(0, start-16):start]
    suffix = data[m.end():min(len(data), m.end()+16)]
    print(f'0x{start:06X}: "{text}" | pre: {prefix.hex()} | suf: {suffix.hex()}')
