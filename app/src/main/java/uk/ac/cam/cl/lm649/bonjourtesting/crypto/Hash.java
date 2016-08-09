package uk.ac.cam.cl.lm649.bonjourtesting.crypto;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.InputStream;

public class Hash {

    public static final int HASH_LENGTH = 256 / 8;
    private static final int BUFFER_SIZE = 16384;

    public static byte[] hashBytes(byte[] data) {
        SHA256Digest digest = new SHA256Digest();
        digest.update(data,0,data.length);
        byte[] dataOut = new byte[HASH_LENGTH];
        digest.doFinal(dataOut, 0);
        return dataOut;
    }

    public static byte[] hashStream(InputStream inStream) throws IOException {
        SHA256Digest digest = new SHA256Digest();
        byte[] buffer = new byte[BUFFER_SIZE];
        int curRead = 0;
        for (;;) {
            curRead = inStream.read(buffer);
            if (curRead == -1) { //end of stream
                break;
            }
            if (curRead > BUFFER_SIZE) throw new RuntimeException();
            digest.update(buffer,0,curRead);
        }
        byte[] dataOut = new byte[HASH_LENGTH];
        digest.doFinal(dataOut, 0);
        return dataOut;
    }

    public static byte[] hashString(String data) {
        return hashBytes(data.getBytes());
    }

    public static String hashBytesToString(byte[] data) {
        return Hex.toHexString(hashBytes(data));
    }

    public static String hashStringToString(String data) {
        return Hex.toHexString(hashString(data));
    }

}
