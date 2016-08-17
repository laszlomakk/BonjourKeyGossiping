package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

public final class Util {

    private Util() {}

    public static void serialiseToStream(DataOutputStream outStream, BigInteger bigInt) throws IOException {
        byte[] bytes = bigInt.toByteArray();
        int bytesLen = bytes.length;
        outStream.writeInt(bytesLen);
        outStream.write(bytes);
    }

    public static BigInteger createBigIntFromStream(DataInputStream inStream) throws IOException {
        int bytesLen = inStream.readInt();
        byte[] bytes = new byte[bytesLen];
        inStream.readFully(bytes);
        return new BigInteger(bytes);
    }

}
