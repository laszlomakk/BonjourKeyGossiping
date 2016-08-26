package uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages;

import android.support.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public abstract class Message {

    protected final int type;
    private static final String TAG = "Message";

    private static final byte[] endMarker = new byte[] {0x11, 0x22, 0x33};

    protected Message() {
        this.type = MessageTypes.msgClassToMsgNumMap.get(getClass());
    }

    public int getType() {
        return type;
    }

    protected abstract void serialiseBodyToStream(DataOutputStream outStream) throws IOException;

    public final void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        serialiseBodyToStream(outStream);

        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    public void send(MsgClient msgClient) throws IOException {
        DataOutputStream outStream = msgClient.getOutStream();
        serialiseToStream(outStream);
    }

    public abstract void onReceive(MsgClient msgClient) throws IOException;

    @Nullable
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
                        "tried to skip endMarker on the stream. expected %d bytes to be discarded, it was instead %d bytes",
                        endMarker.length, nBytesDiscarded));
            }
            return (Message) obj;
        } catch (NoSuchMethodException e) {
            FLogger.e(TAG, "createFromStream(). caught NoSuchMethodException : " + e);
            FLogger.d(TAG, e);
        } catch (InvocationTargetException e) {
            FLogger.e(TAG, "createFromStream(). caught InvocationTargetException : " + e);
            FLogger.d(TAG, e);
        } catch (IllegalAccessException e) {
            FLogger.e(TAG, "createFromStream(). caught IllegalAccessException : " + e);
            FLogger.d(TAG, e);
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
    private static int discardBytesUntilNextMessage(DataInputStream inStream) throws IOException {
        int nBytesDiscarded = endMarker.length;
        byte[] bytes = new byte[nBytesDiscarded];
        inStream.readFully(bytes);
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
