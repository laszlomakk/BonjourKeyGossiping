package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgJPAKERound1 extends Message {

    private static final String TAG = "MsgJPAKERound1";

    public final BigInteger gx1;
    public final BigInteger gx2;
    public final BigInteger[] knowledgeProofForX1;
    public final BigInteger[] knowledgeProofForX2;

    public MsgJPAKERound1(
            BigInteger gx1,
            BigInteger gx2,
            BigInteger[] knowledgeProofForX1,
            BigInteger[] knowledgeProofForX2)
    {
        super();

        this.gx1 = gx1;
        this.gx2 = gx2;
        this.knowledgeProofForX1 = knowledgeProofForX1;
        this.knowledgeProofForX2 = knowledgeProofForX2;
    }

    public static MsgJPAKERound1 createFromStream(DataInputStream inStream) throws IOException {
        int radix = inStream.readInt();
        BigInteger gx1 = new BigInteger(inStream.readUTF(), radix);
        BigInteger gx2 = new BigInteger(inStream.readUTF(), radix);
        BigInteger[] knowledgeProofForX1 = new BigInteger[] {
                new BigInteger(inStream.readUTF(), radix),
                new BigInteger(inStream.readUTF(), radix)
        };
        BigInteger[] knowledgeProofForX2 = new BigInteger[] {
                new BigInteger(inStream.readUTF(), radix),
                new BigInteger(inStream.readUTF(), radix)
        };
        return new MsgJPAKERound1(gx1, gx2, knowledgeProofForX1, knowledgeProofForX2);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(gx1.toString(radix));
        outStream.writeUTF(gx2.toString(radix));
        outStream.writeUTF(knowledgeProofForX1[0].toString(radix));
        outStream.writeUTF(knowledgeProofForX1[1].toString(radix));
        outStream.writeUTF(knowledgeProofForX2[0].toString(radix));
        outStream.writeUTF(knowledgeProofForX2[1].toString(radix));

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

        JPAKEClient jpakeClient;
        if (JPAKEClient.canJPAKEBeStartedUsingThisMsgClient(msgClient)) {
            FLogger.d(TAG, "onReceive(). msgClient can be used for new JPAKE.");
            jpakeClient = msgClient.jpakeClient = new JPAKEClient(false);
        } else {
            jpakeClient = msgClient.jpakeClient;
        }
        if (null == jpakeClient) {
            FLogger.e(TAG, "onReceive(). jpakeClient is null.");
            return;
        }
        boolean round1Success = jpakeClient.round1Receive(msgClient, this);
        if (round1Success) {
            FLogger.i(TAG, "round 1 succeeded.");
            jpakeClient.round2Send(msgClient);
        } else {
            FLogger.i(TAG, "round 1 failed.");
        }
    }

}
