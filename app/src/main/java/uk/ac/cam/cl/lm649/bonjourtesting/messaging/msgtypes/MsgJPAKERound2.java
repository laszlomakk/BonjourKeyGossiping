package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServerManager;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.SessionKey;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MsgJPAKERound2 extends Message {

    private static final String TAG = "MsgJPAKERound2";

    public final UUID handshakeId;
    public final BigInteger a;
    public final BigInteger[] knowledgeProofForX2s;

    private final String strHandshakeId;

    public MsgJPAKERound2(UUID handshakeId, BigInteger a, BigInteger[] knowledgeProofForX2s) {
        super();

        this.handshakeId = handshakeId;
        this.a = a;
        this.knowledgeProofForX2s = knowledgeProofForX2s;

        this.strHandshakeId = JPAKEClient.createHandshakeIdLogString(handshakeId);
    }

    public static MsgJPAKERound2 createFromStream(DataInputStream inStream) throws IOException {
        String strHandshakeId = inStream.readUTF();
        UUID handshakeId = HelperMethods.uuidFromStringDefensively(strHandshakeId);
        if (null == handshakeId) return null;

        int radix = inStream.readInt();
        BigInteger a = new BigInteger(inStream.readUTF(), radix);
        BigInteger[] knowledgeProofForX2s = new BigInteger[] {
                new BigInteger(inStream.readUTF(), radix),
                new BigInteger(inStream.readUTF(), radix)
        };
        return new MsgJPAKERound2(handshakeId, a, knowledgeProofForX2s);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        outStream.writeUTF(handshakeId.toString());
        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(a.toString(radix));
        outStream.writeUTF(knowledgeProofForX2s[0].toString(radix));
        outStream.writeUTF(knowledgeProofForX2s[1].toString(radix));

        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, msgClient.strFromAddress + "received " +
                getClass().getSimpleName() + strHandshakeId);
        JPAKEClient jpakeClient = msgClient.jpakeManager.findJPAKEClient(handshakeId);
        if (null == jpakeClient) {
            FLogger.e(TAG, "onReceive(). couldn't find jpakeClient for this handshake." + strHandshakeId);
            return;
        }
        boolean round2Success = jpakeClient.round2Receive(msgClient, this);
        if (round2Success) {
            FLogger.i(TAG, "round 2 succeeded." + strHandshakeId);
            jpakeClient.round3Send(msgClient);
        } else {
            FLogger.i(TAG, "round 2 failed." + strHandshakeId);
        }
    }

}
