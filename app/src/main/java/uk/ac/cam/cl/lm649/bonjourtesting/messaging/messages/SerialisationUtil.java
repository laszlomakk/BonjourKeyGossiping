package uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public final class SerialisationUtil {

    private SerialisationUtil() {}

    public static void serialiseToStream(DataOutputStream outStream, BigInteger bigInt) throws IOException {
        byte[] bytes = bigInt.toByteArray();
        serialiseToStream(outStream, bytes);
    }

    public static BigInteger createBigIntFromStream(DataInputStream inStream) throws IOException {
        byte[] bytes = createByteArrayFromStream(inStream);
        return new BigInteger(bytes);
    }

    public static void serialiseToStream(DataOutputStream outStream, byte[] bytes) throws IOException {
        int bytesLen = bytes.length;
        outStream.writeInt(bytesLen);
        outStream.write(bytes);
    }

    public static byte[] createByteArrayFromStream(DataInputStream inStream) throws IOException {
        int bytesLen = inStream.readInt();
        byte[] bytes = new byte[bytesLen];
        inStream.readFully(bytes);
        return bytes;
    }

}
