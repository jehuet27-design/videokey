import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;

/**
 * VkPackBuilder - packs a folder of karaoke MIDI files into ONE encrypted, compressed
 * VideoKey archive (.vkpack) that only the VideoKey player can read.
 *
 * Usage:
 *   java VkPackBuilder.java <midiFolder> <output.vkpack>          (build + full verify)
 *   java VkPackBuilder.java --verify <file.vkpack> <midiFolder>    (verify only)
 *
 * File layout (big-endian):
 *   [0..47]   Header: "VKPK", ver(1), flags(1), reserved(2), songCount(4),
 *             indexOffset(8), indexCompLen(4), indexOrigLen(4), nonce(16), headerCrc(4)
 *   [48..]    Song blobs: AES-128-CTR( Deflate( original .mid bytes ) )
 *   [index]   AES-128-CTR( Deflate( entries ) )
 *             entry = UTF code, UTF title, UTF artist, long offset, int compLen, int origLen, int crc32
 *
 * Compression is lossless: every byte of the original MIDI is restored exactly (verified by CRC32
 * and a full byte-by-byte comparison after building).
 *
 * IMPORTANT: The secret below MUST match VkPackReader.kt in the Android app.
 */
public class VkPackBuilder {

    static final byte[] MAGIC = {'V', 'K', 'P', 'K'};
    static final int VERSION = 1;
    static final int FLAGS = 0x03; // bit0 = deflate, bit1 = AES-CTR
    static final int HEADER_SIZE = 48;
    static final int BATCH = 1024;

    // Same obfuscated secret as the Android app (XOR 0x5A).
    static final int[] SECRET_OBF = {
        0x0C, 0x11, 0x79, 0x68, 0x6A, 0x68, 0x6C, 0x7B, 0x0A, 0x28, 0x35, 0x11, 0x3B, 0x28, 0x3B,
        0x35, 0x31, 0x3F, 0x1A, 0x10, 0x3F, 0x32, 0x2F, 0x3F, 0x24, 0x0A, 0x3B, 0x39, 0x31
    };

    static byte[] secret() {
        byte[] b = new byte[SECRET_OBF.length];
        for (int i = 0; i < b.length; i++) b[i] = (byte) (SECRET_OBF[i] ^ 0x5A);
        return b;
    }

    static byte[] deriveKey(byte[] nonce) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        sha.update(secret());
        sha.update(nonce);
        return Arrays.copyOf(sha.digest(), 16);
    }

    /** IV: high 8 bytes = nonce[0..8] XOR (id+1); low 8 bytes = nonce[8..12] + 4 zero bytes (counter space). */
    static byte[] ivFor(byte[] nonce, long id) {
        byte[] iv = new byte[16];
        long v = id + 1;
        for (int i = 0; i < 8; i++) iv[i] = (byte) (nonce[i] ^ (v >>> (56 - 8 * i)));
        System.arraycopy(nonce, 8, iv, 8, 4);
        return iv;
    }

    static byte[] ctr(byte[] key, byte[] iv, byte[] data) throws Exception {
        Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return c.doFinal(data);
    }

    static byte[] deflate(byte[] data) {
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION, true);
        d.setInput(data);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.max(64, data.length / 2));
        byte[] buf = new byte[65536];
        while (!d.finished()) out.write(buf, 0, d.deflate(buf));
        d.end();
        return out.toByteArray();
    }

    static byte[] inflate(byte[] data, int origLen) throws Exception {
        Inflater inf = new Inflater(true);
        inf.setInput(data);
        byte[] out = new byte[origLen];
        int n = 0;
        while (n < origLen && !inf.finished()) {
            int r = inf.inflate(out, n, origLen - n);
            if (r == 0 && (inf.needsInput() || inf.needsDictionary())) break;
            n += r;
        }
        inf.end();
        if (n != origLen) throw new IOException("Inflate size mismatch " + n + " != " + origLen);
        return out;
    }

    static int crc(byte[] data) {
        CRC32 c = new CRC32();
        c.update(data);
        return (int) c.getValue();
    }

    static final Pattern NAME = Pattern.compile("^(\\d{4,6})\\s*-\\s*(.+?)\\s*-\\s*(.+?)$");

    static String[] parseName(String fileName) {
        String base = fileName.replaceAll("(?i)\\.(mid|kar)$", "");
        String[] parts = base.split(" - ", 3);
        if (parts.length >= 3) return new String[]{parts[0].trim(), parts[1].trim(), parts[2].trim()};
        if (parts.length == 2) return new String[]{parts[0].trim(), parts[1].trim(), "Karaoke"};
        Matcher m = NAME.matcher(base);
        if (m.find()) return new String[]{m.group(1).trim(), m.group(2).trim(), m.group(3).trim()};
        return new String[]{base.length() > 6 ? base.substring(0, 6) : base, base, "Unknown"};
    }

    static class Entry {
        String code, title, artist;
        long offset;
        int compLen, origLen, crc;
        byte[] blob; // transient during build
        Path source;
    }

    static List<Path> listMidi(Path folder) throws IOException {
        try (Stream<Path> s = Files.list(folder)) {
            return s.filter(p -> {
                String n = p.getFileName().toString().toLowerCase();
                return Files.isRegularFile(p) && (n.endsWith(".mid") || n.endsWith(".kar"));
            }).sorted(Comparator.comparing(p -> p.getFileName().toString())).collect(Collectors.toList());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length >= 3 && args[0].equals("--add")) {
            add(Paths.get(args[1]), Paths.get(args[2]), args.length >= 4 && args[3].equals("--replace"));
            return;
        }
        if (args.length >= 3 && args[0].equals("--verify")) {
            verify(Paths.get(args[1]), Paths.get(args[2]));
            return;
        }
        if (args.length < 2) {
            System.out.println("Usage:\n"
                    + "  build : java VkPackBuilder.java <midiFolder> <output.vkpack>\n"
                    + "  add   : java VkPackBuilder.java --add <existing.vkpack> <newMidiFolder> [--replace]\n"
                    + "  verify: java VkPackBuilder.java --verify <file.vkpack> <midiFolder>");
            return;
        }
        Path folder = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        build(folder, out);
        verify(out, folder);
    }

    static void build(Path folder, Path outPath) throws Exception {
        long t0 = System.currentTimeMillis();
        List<Path> files = listMidi(folder);
        System.out.println("Found " + files.size() + " MIDI files in " + folder);

        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        byte[] key = deriveKey(nonce);

        List<Entry> entries = new ArrayList<>(files.size());
        long totalOrig = 0;
        Path tmp = outPath.resolveSibling(outPath.getFileName() + ".part");

        try (RandomAccessFile raf = new RandomAccessFile(tmp.toFile(), "rw")) {
            raf.setLength(0);
            raf.write(new byte[HEADER_SIZE]); // placeholder
            long pos = HEADER_SIZE;

            for (int start = 0; start < files.size(); start += BATCH) {
                int end = Math.min(files.size(), start + BATCH);
                final int base = start;
                List<Entry> batch = IntStream.range(start, end).parallel().mapToObj(i -> {
                    try {
                        Path p = files.get(i);
                        byte[] raw = Files.readAllBytes(p);
                        String[] meta = parseName(p.getFileName().toString());
                        Entry e = new Entry();
                        e.code = meta[0];
                        e.title = meta[1];
                        e.artist = meta[2];
                        e.origLen = raw.length;
                        e.crc = crc(raw);
                        e.blob = ctr(key, ivFor(nonce, i), deflate(raw));
                        e.compLen = e.blob.length;
                        e.source = p;
                        return e;
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed on " + files.get(i), ex);
                    }
                }).collect(Collectors.toList());

                for (Entry e : batch) {
                    e.offset = pos;
                    raf.write(e.blob);
                    pos += e.compLen;
                    totalOrig += e.origLen;
                    e.blob = null;
                    entries.add(e);
                }
                System.out.printf("  packed %d / %d  (%.1f MB -> %.1f MB)%n", end, files.size(),
                        totalOrig / 1048576.0, (pos - HEADER_SIZE) / 1048576.0);
            }

            // Index table
            ByteArrayOutputStream idxBytes = new ByteArrayOutputStream();
            try (DataOutputStream d = new DataOutputStream(idxBytes)) {
                for (Entry e : entries) {
                    d.writeUTF(e.code);
                    d.writeUTF(e.title);
                    d.writeUTF(e.artist);
                    d.writeLong(e.offset);
                    d.writeInt(e.compLen);
                    d.writeInt(e.origLen);
                    d.writeInt(e.crc);
                }
            }
            byte[] idxRaw = idxBytes.toByteArray();
            byte[] idxEnc = ctr(key, ivFor(nonce, -1), deflate(idxRaw));
            long indexOffset = pos;
            raf.write(idxEnc);

            // Header
            ByteArrayOutputStream hb = new ByteArrayOutputStream();
            DataOutputStream h = new DataOutputStream(hb);
            h.write(MAGIC);
            h.writeByte(VERSION);
            h.writeByte(FLAGS);
            h.writeShort(0);
            h.writeInt(entries.size());
            h.writeLong(indexOffset);
            h.writeInt(idxEnc.length);
            h.writeInt(idxRaw.length);
            h.write(nonce);
            h.flush();
            byte[] header44 = hb.toByteArray();
            h.writeInt(crc(header44));
            h.flush();
            raf.seek(0);
            raf.write(hb.toByteArray());
        }

        Files.move(tmp, outPath, StandardCopyOption.REPLACE_EXISTING);
        long size = Files.size(outPath);
        System.out.printf("%nDONE: %s%n  songs: %d%n  original: %.1f MB%n  packed:   %.1f MB (%.1f%% of original)%n  time: %.1fs%n",
                outPath, entries.size(), totalOrig / 1048576.0, size / 1048576.0,
                100.0 * size / Math.max(1, totalOrig), (System.currentTimeMillis() - t0) / 1000.0);
    }

    static class Item {
        String code, title, artist;
        int origLen, crc;
        java.util.concurrent.Callable<byte[]> deflated; // returns the DEFLATED (not encrypted) bytes
    }

    /** Opens an existing pack and returns its entries plus a thread-safe loader for deflated bytes. */
    static List<Item> readPackItems(Path pack, java.nio.channels.FileChannel[] chOut) throws Exception {
        java.nio.channels.FileChannel ch = java.nio.channels.FileChannel.open(pack, StandardOpenOption.READ);
        chOut[0] = ch;
        java.nio.ByteBuffer hb = java.nio.ByteBuffer.allocate(HEADER_SIZE);
        while (hb.hasRemaining()) if (ch.read(hb, hb.position()) < 0) throw new IOException("Truncated header");
        byte[] header = hb.array();
        DataInputStream h = new DataInputStream(new ByteArrayInputStream(header));
        byte[] magic = new byte[4];
        h.readFully(magic);
        if (!Arrays.equals(magic, MAGIC)) throw new IOException("Not a VKPK file");
        h.readUnsignedByte(); h.readUnsignedByte(); h.readShort();
        int count = h.readInt();
        long indexOffset = h.readLong();
        int idxComp = h.readInt();
        int idxOrig = h.readInt();
        byte[] nonce = new byte[16];
        h.readFully(nonce);
        if (h.readInt() != crc(Arrays.copyOf(header, 44))) throw new IOException("Header CRC mismatch");
        byte[] key = deriveKey(nonce);

        java.nio.ByteBuffer ib = java.nio.ByteBuffer.allocate(idxComp);
        while (ib.hasRemaining()) if (ch.read(ib, indexOffset + ib.position()) < 0) throw new IOException("Truncated index");
        byte[] idxRaw = inflate(ctr(key, ivFor(nonce, -1), ib.array()), idxOrig);
        DataInputStream d = new DataInputStream(new ByteArrayInputStream(idxRaw));

        List<Item> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final int id = i;
            Item it = new Item();
            it.code = d.readUTF(); it.title = d.readUTF(); it.artist = d.readUTF();
            final long off = d.readLong();
            final int compLen = d.readInt();
            it.origLen = d.readInt();
            it.crc = d.readInt();
            it.deflated = () -> {
                java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(compLen);
                while (b.hasRemaining()) if (ch.read(b, off + b.position()) < 0) throw new IOException("Truncated blob");
                return ctr(key, ivFor(nonce, id), b.array()); // CTR decrypt = deflated data
            };
            items.add(it);
        }
        return items;
    }

    /** Writes items to a new pack: fresh nonce, ids follow list order. */
    static void writePack(Path outPath, List<Item> items) throws Exception {
        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        byte[] key = deriveKey(nonce);
        Path tmp = outPath.resolveSibling(outPath.getFileName() + ".part");
        long[] offs = new long[items.size()];
        int[] lens = new int[items.size()];

        try (RandomAccessFile raf = new RandomAccessFile(tmp.toFile(), "rw")) {
            raf.setLength(0);
            raf.write(new byte[HEADER_SIZE]);
            long pos = HEADER_SIZE;
            for (int start = 0; start < items.size(); start += BATCH) {
                int end = Math.min(items.size(), start + BATCH);
                final int base = start;
                byte[][] blobs = new byte[end - start][];
                IntStream.range(start, end).parallel().forEach(i -> {
                    try {
                        blobs[i - base] = ctr(key, ivFor(nonce, i), items.get(i).deflated.call());
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed on " + items.get(i).code, ex);
                    }
                });
                for (int i = start; i < end; i++) {
                    offs[i] = pos;
                    lens[i] = blobs[i - start].length;
                    raf.write(blobs[i - start]);
                    pos += lens[i];
                }
            }
            ByteArrayOutputStream idxBytes = new ByteArrayOutputStream();
            try (DataOutputStream d = new DataOutputStream(idxBytes)) {
                for (int i = 0; i < items.size(); i++) {
                    Item e = items.get(i);
                    d.writeUTF(e.code); d.writeUTF(e.title); d.writeUTF(e.artist);
                    d.writeLong(offs[i]); d.writeInt(lens[i]); d.writeInt(e.origLen); d.writeInt(e.crc);
                }
            }
            byte[] idxRaw = idxBytes.toByteArray();
            byte[] idxEnc = ctr(key, ivFor(nonce, -1), deflate(idxRaw));
            long indexOffset = pos;
            raf.write(idxEnc);

            ByteArrayOutputStream hbo = new ByteArrayOutputStream();
            DataOutputStream h = new DataOutputStream(hbo);
            h.write(MAGIC);
            h.writeByte(VERSION);
            h.writeByte(FLAGS);
            h.writeShort(0);
            h.writeInt(items.size());
            h.writeLong(indexOffset);
            h.writeInt(idxEnc.length);
            h.writeInt(idxRaw.length);
            h.write(nonce);
            h.flush();
            byte[] header44 = hbo.toByteArray();
            h.writeInt(crc(header44));
            h.flush();
            raf.seek(0);
            raf.write(hbo.toByteArray());
        }
        Files.move(tmp, outPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /** --add: merge new MIDI files from a folder into an existing pack. Existing codes are skipped (or replaced with --replace). */
    static void add(Path pack, Path newFolder, boolean replace) throws Exception {
        long t0 = System.currentTimeMillis();
        java.nio.channels.FileChannel[] chBox = new java.nio.channels.FileChannel[1];
        List<Item> items = readPackItems(pack, chBox);
        int before = items.size();
        Map<String, Integer> byCode = new HashMap<>();
        for (int i = 0; i < items.size(); i++) byCode.put(items.get(i).code, i);

        // Keep a backup of the original pack until the new one is written successfully.
        Path backup = pack.resolveSibling(pack.getFileName() + ".bak");
        int added = 0, replaced = 0, skipped = 0;
        try {
            for (Path p : listMidi(newFolder)) {
                byte[] raw = Files.readAllBytes(p);
                String[] meta = parseName(p.getFileName().toString());
                Item it = new Item();
                it.code = meta[0]; it.title = meta[1]; it.artist = meta[2];
                it.origLen = raw.length;
                it.crc = crc(raw);
                final byte[] def = deflate(raw);
                it.deflated = () -> def;
                Integer existing = byCode.get(it.code);
                if (existing != null) {
                    if (replace) {
                        items.set(existing, it);
                        replaced++;
                        System.out.println("  replaced " + it.code + " - " + it.title);
                    } else {
                        skipped++;
                        System.out.println("  SKIPPED (code already exists) " + p.getFileName());
                    }
                } else {
                    byCode.put(it.code, items.size());
                    items.add(it);
                    added++;
                    System.out.println("  added   " + it.code + " - " + it.title + " - " + it.artist);
                }
            }
            if (added == 0 && replaced == 0) {
                System.out.println("\nWalang idinagdag. (skipped=" + skipped + ")");
                return;
            }
            Path tmpOut = pack.resolveSibling(pack.getFileName() + ".new");
            writePack(tmpOut, items);
            chBox[0].close();
            Files.copy(pack, backup, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tmpOut, pack, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            if (chBox[0].isOpen()) chBox[0].close();
        }

        // Self-check the new pack: every song must decrypt, inflate and match its CRC32.
        java.nio.channels.FileChannel[] chk = new java.nio.channels.FileChannel[1];
        List<Item> check = readPackItems(pack, chk);
        try {
            for (Item it : check) {
                if (crc(inflate(it.deflated.call(), it.origLen)) != it.crc) throw new IOException("CRC mismatch " + it.code);
            }
        } finally {
            chk[0].close();
        }
        System.out.printf("%nADD OK: %d -> %d songs (added %d, replaced %d, skipped %d). Self-check passed. Size %.1f MB, %.1fs%n",
                before, check.size(), added, replaced, skipped, Files.size(pack) / 1048576.0, (System.currentTimeMillis() - t0) / 1000.0);
        System.out.println("Backup of previous pack: " + backup + "  (pwede mong i-delete kapag okay na)");
    }

    /** Reads the pack exactly like the app does and compares EVERY song byte-for-byte to the originals. */
    static void verify(Path pack, Path folder) throws Exception {
        long t0 = System.currentTimeMillis();
        System.out.println("\nVerifying " + pack + " ...");
        try (RandomAccessFile raf = new RandomAccessFile(pack.toFile(), "r")) {
            byte[] header = new byte[HEADER_SIZE];
            raf.readFully(header);
            DataInputStream h = new DataInputStream(new ByteArrayInputStream(header));
            byte[] magic = new byte[4];
            h.readFully(magic);
            if (!Arrays.equals(magic, MAGIC)) throw new IOException("Bad magic");
            h.readUnsignedByte(); h.readUnsignedByte(); h.readShort();
            int count = h.readInt();
            long indexOffset = h.readLong();
            int idxComp = h.readInt();
            int idxOrig = h.readInt();
            byte[] nonce = new byte[16];
            h.readFully(nonce);
            int hcrc = h.readInt();
            if (hcrc != crc(Arrays.copyOf(header, 44))) throw new IOException("Header CRC mismatch");
            byte[] key = deriveKey(nonce);

            byte[] idxEnc = new byte[idxComp];
            raf.seek(indexOffset);
            raf.readFully(idxEnc);
            long tIdx = System.nanoTime();
            byte[] idxRaw = inflate(ctr(key, ivFor(nonce, -1), idxEnc), idxOrig);
            DataInputStream d = new DataInputStream(new ByteArrayInputStream(idxRaw));
            List<Entry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Entry e = new Entry();
                e.code = d.readUTF(); e.title = d.readUTF(); e.artist = d.readUTF();
                e.offset = d.readLong(); e.compLen = d.readInt(); e.origLen = d.readInt(); e.crc = d.readInt();
                entries.add(e);
            }
            System.out.printf("  index loaded: %d songs in %.1f ms%n", count, (System.nanoTime() - tIdx) / 1e6);

            List<Path> files = listMidi(folder);
            if (files.size() != count) throw new IOException("Song count mismatch: pack=" + count + " folder=" + files.size());

            int ok = 0;
            long maxNs = 0;
            for (int i = 0; i < count; i++) {
                Entry e = entries.get(i);
                long s = System.nanoTime();
                byte[] enc = new byte[e.compLen];
                raf.seek(e.offset);
                raf.readFully(enc);
                byte[] restored = inflate(ctr(key, ivFor(nonce, i), enc), e.origLen);
                maxNs = Math.max(maxNs, System.nanoTime() - s);
                if (crc(restored) != e.crc) throw new IOException("CRC mismatch for " + e.code);
                byte[] original = Files.readAllBytes(files.get(i));
                if (!Arrays.equals(original, restored)) throw new IOException("Byte mismatch for " + files.get(i));
                ok++;
                if (ok % 8000 == 0) System.out.println("  verified " + ok + " / " + count);
            }
            System.out.printf("VERIFY OK: %d / %d songs are byte-for-byte identical to the originals (slowest single-song decode: %.2f ms). Total %.1fs%n",
                    ok, count, maxNs / 1e6, (System.currentTimeMillis() - t0) / 1000.0);
        }
    }
}
