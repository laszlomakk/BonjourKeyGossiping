package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public abstract class Message {

    protected final int type;
    private static final String TAG = "Message";

    private static final byte[] endMarker = new byte[] {0x11, 0x22, 0x33};

    protected Message(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public abstract void serialiseToStream(DataOutputStream outStream) throws IOException;

    public abstract void send(MsgClient msgClient) throws IOException;

    public abstract void receive(MsgClient msgClient) throws IOException;

    public static Message createFromStream(DataInputStream inStream) throws IOException, UnknownMessageTypeException {
        int typeNum = inStream.readInt();
        Class<? extends Message> msgClass = MessageTypes.msgNumToMsgClassMap.get(typeNum);
        if (null == msgClass) {
            long nBytesDiscarded = discardBytesUntilNextMessage(inStream);
            FLogger.d(TAG, "discarded " + nBytesDiscarded + " bytes from inputStream");
            throw new UnknownMessageTypeException("typeNum: " + typeNum);
        }
        try {
            Method method = msgClass.getMethod("createFromStream", DataInputStream.class);
            Object obj = method.invoke(null, inStream);
            long nBytesDiscarded = discardBytesUntilNextMessage(inStream); // discard end marker
            if (nBytesDiscarded != endMarker.length) {
                FLogger.e(TAG, String.format(Locale.US,
                        "tried to skip endMarker from the stream. expected %d bytes to be discarded, it was instead %d bytes",
                        nBytesDiscarded, endMarker.length));
            }
            return (Message) obj;
        } catch (NoSuchMethodException e) {
            FLogger.e(TAG, "createFromStream(). caught NoSuchMethodException : " + e.getMessage());
            FLogger.d(TAG, HelperMethods.formatStackTraceAsString(e));
        } catch (InvocationTargetException e) {
            FLogger.e(TAG, "createFromStream(). caught InvocationTargetException : " + e.getMessage());
            FLogger.d(TAG, HelperMethods.formatStackTraceAsString(e));
        } catch (IllegalAccessException e) {
            FLogger.e(TAG, "createFromStream(). caught IllegalAccessException : " + e.getMessage());
            FLogger.d(TAG, HelperMethods.formatStackTraceAsString(e));
        }
        return null;
    }

    protected static void writeMessageEndMarker(DataOutputStream outStream) throws IOException {
        outStream.write(endMarker);
    }

    private static boolean testForEndMarker(byte[] bytes) {
        return Arrays.equals(endMarker, bytes);
    }

    /**
     * Discards bytes from a stream until a message end marker is found. The end marker
     * is also discarded.
     *
     * @return the number of bytes discarded from the stream, including the end marker
     */
    private static long discardBytesUntilNextMessage(DataInputStream inStream) throws IOException {
        long nBytesDiscarded = 0;
        byte[] bytes = new byte[endMarker.length];
        int nBytesRead = inStream.read(bytes);
        if (nBytesRead == -1) return 0;
        nBytesDiscarded += nBytesRead;
        while (!testForEndMarker(bytes)) {
            // shift all elements to the left by one
            System.arraycopy(bytes, 1, bytes, 0, bytes.length-1);
            // read next byte into last index
            bytes[bytes.length-1] = inStream.readByte();
            nBytesDiscarded++;
        }
        return nBytesDiscarded;
    }

}
