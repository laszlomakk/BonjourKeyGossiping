package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.JPAKEManager;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MsgJPAKERound1 extends Message {

    private static final String TAG = "MsgJPAKERound1";

    public final UUID handshakeId;
    public final BigInteger gx1;
    public final BigInteger gx2;
    public final BigInteger[] knowledgeProofForX1;
    public final BigInteger[] knowledgeProofForX2;

    private final String strHandshakeId;

    public MsgJPAKERound1(
            UUID handshakeId,
            BigInteger gx1,
            BigInteger gx2,
            BigInteger[] knowledgeProofForX1,
            BigInteger[] knowledgeProofForX2)
    {
        super();

        this.handshakeId = handshakeId;
        this.gx1 = gx1;
        this.gx2 = gx2;
        this.knowledgeProofForX1 = knowledgeProofForX1;
        this.knowledgeProofForX2 = knowledgeProofForX2;

        this.strHandshakeId = JPAKEClient.createHandshakeIdLogString(handshakeId);
    }

    public static MsgJPAKERound1 createFromStream(DataInputStream inStream) throws IOException {
        String strHandshakeId = inStream.readUTF();
        UUID handshakeId = HelperMethods.uuidFromStringDefensively(strHandshakeId);
        if (null == handshakeId) return null;

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
        return new MsgJPAKERound1(handshakeId, gx1, gx2, knowledgeProofForX1, knowledgeProofForX2);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        outStream.writeUTF(handshakeId.toString());
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
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, msgClient.strFromAddress + "received " +
                getClass().getSimpleName() + strHandshakeId);

        JPAKEManager jpakeManager = msgClient.jpakeManager;
        JPAKEClient jpakeClient = jpakeManager.findJPAKEClient(handshakeId);
        if (null == jpakeClient) {
            FLogger.d(TAG, "onReceive(). this is a new handshakeId, the other end initiated." + strHandshakeId);
            jpakeClient = jpakeManager.createJPAKEClientDueToIncomingMessage(handshakeId);
        }
        if (null == jpakeClient) {
            FLogger.e(TAG, "onReceive(). wtf. jpakeClient is null." + strHandshakeId);
            return;
        }
        boolean round1Success = jpakeClient.round1Receive(msgClient, this);
        if (round1Success) {
            FLogger.i(TAG, "round 1 succeeded." + strHandshakeId);
            jpakeClient.round2Send(msgClient);
        } else {
            FLogger.i(TAG, "round 1 failed." + strHandshakeId);
        }
    }

}
