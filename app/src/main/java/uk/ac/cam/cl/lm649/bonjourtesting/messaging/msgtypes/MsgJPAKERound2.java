package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgJPAKERound2 extends Message {

    private static final String TAG = "MsgJPAKERound2";

    public final BigInteger a;
    public final BigInteger[] knowledgeProofForX2s;

    public MsgJPAKERound2(BigInteger a, BigInteger[] knowledgeProofForX2s) {
        super(MessageTypes.msgClassToMsgNumMap.get(MsgJPAKERound2.class));

        this.a = a;
        this.knowledgeProofForX2s = knowledgeProofForX2s;
    }

    public static MsgJPAKERound2 createFromStream(DataInputStream inStream) throws IOException {
        int radix = inStream.readInt();
        BigInteger a = new BigInteger(inStream.readUTF(), radix);
        BigInteger[] knowledgeProofForX2s = new BigInteger[] {
                new BigInteger(inStream.readUTF(), radix),
                new BigInteger(inStream.readUTF(), radix)
        };
        return new MsgJPAKERound2(a, knowledgeProofForX2s);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(a.toString(radix));
        outStream.writeUTF(knowledgeProofForX2s[0].toString(radix));
        outStream.writeUTF(knowledgeProofForX2s[1].toString(radix));

        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    @Override
    public void send(MsgClient msgClient) throws IOException {
        DataOutputStream outStream = msgClient.getOutStream();
        serialiseToStream(outStream);
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(MsgClient.TAG, msgClient.sFromAddress + "received " +
                getClass().getSimpleName());
        JPAKEClient jpakeClient = msgClient.getJpakeClient();
        if (null == jpakeClient) {
            FLogger.e(TAG, "onReceive(). jpakeClient is null.");
            return;
        }
        boolean round2Success = jpakeClient.round2Receive(msgClient, this);
        if (round2Success) {
            FLogger.i(TAG, "round 2 succeeded.");
            jpakeClient.round3Send(msgClient);
        } else {
            FLogger.i(TAG, "round 2 failed.");
        }
    }

}
