package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MsgJPAKERound3 extends Message {

    private static final String TAG = "MsgJPAKERound3";

    public final BigInteger macTag;

    public MsgJPAKERound3(BigInteger macTag) {
        super(MessageTypes.msgClassToMsgNumMap.get(MsgJPAKERound3.class));

        this.macTag = macTag;
    }

    public static MsgJPAKERound3 createFromStream(DataInputStream inStream) throws IOException {
        int radix = inStream.readInt();
        BigInteger macTag = new BigInteger(inStream.readUTF(), radix);
        return new MsgJPAKERound3(macTag);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(macTag.toString(radix));

        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    @Override
    public void send(MsgClient msgClient) throws IOException {
        DataOutputStream outStream = msgClient.getOutStream();
        serialiseToStream(outStream);
    }

    @Override
    public void receive(MsgClient msgClient) throws IOException {
        FLogger.i(MsgClient.TAG, msgClient.sFromAddress + "received " +
                getClass().getSimpleName());
        JPAKEClient jpakeClient = msgClient.getJpakeClient();
        if (null == jpakeClient) {
            FLogger.e(TAG, "receive(). jpakeClient is null.");
            return;
        }
        boolean round3Success = jpakeClient.round3Receive(msgClient, this);
        if (round3Success) {
            String text = "round 3 succeeded! key: " + jpakeClient.getSessionKey().toString(Character.MAX_RADIX);
            FLogger.i(TAG, text);
            HelperMethods.displayMsgToUser(msgClient.context, text);
        } else {
            FLogger.i(TAG, "round 3 failed.");
        }
    }

}
