import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.Inflater;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DumpUnknownSongs {
    static final int[] SECRET_OBF = {
        0x0C, 0x11, 0x79, 0x68, 0x6A, 0x68, 0x6C, 0x7B, 0x0A, 0x28, 0x35, 0x11, 0x3B, 0x28, 0x3B,
        0x35, 0x31, 0x3F, 0x1A, 0x10, 0x3F, 0x32, 0x2F, 0x3F, 0x24, 0x0A, 0x3B, 0x39, 0x31
    };

    static byte[] secret() {
        byte[] b = new byte[SECRET_OBF.length];
        for (int i = 0; i < b.length; i++) b[i] = (byte) (SECRET_OBF[i] ^ 0x5A);
        return b;
    }

    static byte[] ivFor(byte[] nonce, long id) {
        byte[] iv = new byte[16];
        long v = id + 1;
        for (int i = 0; i < 8; i++) iv[i] = (byte) (nonce[i] ^ (v >>> (56 - 8 * i)));
        System.arraycopy(nonce, 8, iv, 8, 4);
        return iv;
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
        return out;
    }

    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(args[1]), "UTF-8"));
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] header = new byte[48];
            raf.readFully(header);
            DataInputStream h = new DataInputStream(new ByteArrayInputStream(header));
            byte[] magic = new byte[4];
            h.readFully(magic);
            h.readUnsignedByte();
            h.readUnsignedByte();
            h.readShort();
            int count = h.readInt();
            long indexOffset = h.readLong();
            int idxComp = h.readInt();
            int idxOrig = h.readInt();
            byte[] nonce = new byte[16];
            h.readFully(nonce);

            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update(secret());
            sha.update(nonce);
            byte[] key = Arrays.copyOf(sha.digest(), 16);

            byte[] idxEnc = new byte[idxComp];
            raf.seek(indexOffset);
            raf.readFully(idxEnc);

            Cipher c = Cipher.getInstance("AES/CTR/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(ivFor(nonce, -1)));
            byte[] dec = c.doFinal(idxEnc);
            byte[] idxRaw = inflate(dec, idxOrig);

            DataInputStream d = new DataInputStream(new ByteArrayInputStream(idxRaw));
            int unknownCount = 0;
            for (int i = 0; i < count; i++) {
                String code = d.readUTF();
                String title = d.readUTF();
                String artist = d.readUTF();
                long offset = d.readLong();
                int compLen = d.readInt();
                int origLen = d.readInt();
                int songCrc = d.readInt();

                if (artist.equalsIgnoreCase("Unknown") || artist.trim().isEmpty()) {
                    unknownCount++;
                    pw.println(code + "\t" + title + "\t" + artist);
                }
            }
            pw.flush();
            System.out.println("Exported " + unknownCount + " unknown songs to " + args[1]);
        }
    }
}
