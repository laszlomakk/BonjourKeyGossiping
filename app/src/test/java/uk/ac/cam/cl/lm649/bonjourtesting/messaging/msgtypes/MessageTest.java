package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.SecureRandom;

import static org.junit.Assert.*;

public class MessageTest {

    @Test
    public void testDiscardBytesUntilNextMessage() throws Exception {
        Method method = Message.class.getDeclaredMethod("discardBytesUntilNextMessage", DataInputStream.class);
        method.setAccessible(true);

        Field endMarkerField = Message.class.getDeclaredField("endMarker");
        endMarkerField.setAccessible(true);
        byte[] endMarker = (byte[]) endMarkerField.get(null);

        testDiscardBytesUntilNextMessage1(method, endMarker.length);
        testDiscardBytesUntilNextMessage2(method, endMarker.length);
        testDiscardBytesUntilNextMessage3(method, endMarker.length);
    }

    private void testDiscardBytesUntilNextMessage1(Method discardBytesUntilNextMessage, int endMarkerLen) throws Exception {
        DataInputStream inStream = createDataInputStream(new StreamOperation() {
            @Override
            public void run(DataOutputStream outStream) throws IOException {
                Message.writeMessageEndMarker(outStream);
                Message.writeMessageEndMarker(outStream);
                Message.writeMessageEndMarker(outStream);
                outStream.flush();
            }
        });
        assertEquals(
                endMarkerLen,
                discardBytesUntilNextMessage.invoke(null, inStream)
        );
        assertEquals(
                endMarkerLen,
                discardBytesUntilNextMessage.invoke(null, inStream)
        );
        assertEquals(
                endMarkerLen,
                discardBytesUntilNextMessage.invoke(null, inStream)
        );
    }

    private void testDiscardBytesUntilNextMessage2(Method discardBytesUntilNextMessage, int endMarkerLen) throws Exception {
        DataInputStream inStream = createDataInputStream(new StreamOperation() {
            @Override
            public void run(DataOutputStream outStream) throws IOException {
                outStream.writeByte(200);
                outStream.writeByte(-145);
                outStream.writeByte(77);
                outStream.writeByte(0x11);
                outStream.writeByte(0x22);
                outStream.writeByte(0x11);
                outStream.writeByte(0x11);
                Message.writeMessageEndMarker(outStream);
                outStream.writeByte(0x11);
                outStream.writeByte(0x22);
                outStream.writeByte(-55);
                outStream.flush();
            }
        });
        assertEquals(
                7 + endMarkerLen,
                discardBytesUntilNextMessage.invoke(null, inStream)
        );
    }

    private void testDiscardBytesUntilNextMessage3(Method discardBytesUntilNextMessage, int endMarkerLen) throws Exception {
        final int firstSeq = 50;
        final int secondSeq = 30;
        final int thirdSeq = 70;
        final int fourthSeq = 20;
        DataInputStream inStream = createDataInputStream(new StreamOperation() {
            @Override
            public void run(DataOutputStream outStream) throws IOException {
                SecureRandom rand = new SecureRandom();
                byte[] bytes;

                bytes = new byte[firstSeq];
                rand.nextBytes(bytes);
                outStream.write(bytes);

                Message.writeMessageEndMarker(outStream);

                bytes = new byte[secondSeq];
                rand.nextBytes(bytes);
                outStream.write(bytes);

                Message.writeMessageEndMarker(outStream);

                bytes = new byte[thirdSeq];
                rand.nextBytes(bytes);
                outStream.write(bytes);

                Message.writeMessageEndMarker(outStream);

                bytes = new byte[fourthSeq];
                rand.nextBytes(bytes);
                outStream.write(bytes);

                outStream.flush();
            }
        });
        assertEquals(
                firstSeq + endMarkerLen,
                discardBytesUntilNextMessage.invoke(null, inStream)
        );
        assertEquals(
                secondSeq + endMarkerLen,
                discardBytesUntilNextMessage.invoke(null, inStream)
        );
        assertEquals(
                thirdSeq + endMarkerLen,
                discardBytesUntilNextMessage.invoke(null, inStream)
        );
        boolean detectedEOF = false;
        try {
            discardBytesUntilNextMessage.invoke(null, inStream);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof EOFException) {
                detectedEOF = true;
            } else {
                throw e;
            }
        }
        assertTrue(detectedEOF);
    }

    private static DataInputStream createDataInputStream(StreamOperation op) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(new BufferedOutputStream(bytesOut));

        op.run(outStream);

        byte[] bytes = bytesOut.toByteArray();
        return new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(bytes)));
    }

    private abstract class StreamOperation {
        public abstract void run(DataOutputStream outStream) throws IOException;
    }

}