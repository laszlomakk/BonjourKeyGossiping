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
                getClass().getSimpleName() + "-" + handshakeId);
        JPAKEClient jpakeClient = msgClient.jpakeManager.findJPAKEClient(handshakeId);
        if (null == jpakeClient) {
            FLogger.e(TAG, strHandshakeId + "onReceive(). couldn't find jpakeClient for this handshake.");
            return;
        }
        boolean round2Success = jpakeClient.round2Receive(msgClient, this);
        if (round2Success) {
            onSuccessfulRound2(msgClient, jpakeClient);
        } else {
            FLogger.i(TAG, strHandshakeId + "round 2 failed.");
        }
    }

    private void onSuccessfulRound2(MsgClient msgClient, JPAKEClient jpakeClient) throws IOException {
        FLogger.i(TAG, strHandshakeId + "round 2 succeeded.");

        FLogger.d(TAG, strHandshakeId + "msgClient.iAmTheInitiator == " + msgClient.iAmTheInitiator);
        if (!msgClient.iAmTheInitiator) {
            byte[] sessionKeyBytes = jpakeClient.getSessionKey();
            SessionKey sessionKey = null;
            try {
                sessionKey = new SessionKey(sessionKeyBytes);
            } catch (SessionKey.InvalidSessionKeySizeException e) {
                FLogger.e(TAG, strHandshakeId + "InvalidSessionKeySizeException: " + e.getMessage());
                FLogger.d(TAG, HelperMethods.formatStackTraceAsString(e));
                FLogger.e(TAG, strHandshakeId + "stopping JPAKE handshake.");
                return;
            }
            InetAddress socketAddress = msgClient.getSocketAddress();
            FLogger.i(TAG, strHandshakeId + "saving sessionKey for socketAddress: " + socketAddress.getHostAddress());
            MsgServerManager.getInstance().getMsgServerEncrypted().inetAddressToSessionKeyMap
                    .put(socketAddress, sessionKey);
        }

        jpakeClient.round3Send(msgClient);
    }

}
