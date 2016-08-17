package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import org.bouncycastle.util.encoders.Hex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServerManager;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.SessionKey;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgJPAKERound3 extends Message {

    private static final String TAG = "MsgJPAKERound3";

    public final UUID handshakeId;
    public final BigInteger macTag;

    private final String strHandshakeId;

    public MsgJPAKERound3(UUID handshakeId, BigInteger macTag) {
        super();

        this.handshakeId = handshakeId;
        this.macTag = macTag;

        this.strHandshakeId = JPAKEClient.createHandshakeIdLogString(handshakeId);
    }

    @UsedViaReflection
    public static MsgJPAKERound3 createFromStream(DataInputStream inStream) throws IOException {
        String strHandshakeId = inStream.readUTF();
        UUID handshakeId = HelperMethods.uuidFromStringDefensively(strHandshakeId);
        if (null == handshakeId) return null;

        BigInteger macTag = Util.createBigIntFromStream(inStream);
        return new MsgJPAKERound3(handshakeId, macTag);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        outStream.writeUTF(handshakeId.toString());
        Util.serialiseToStream(outStream, macTag);

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
        boolean round3Success = jpakeClient.round3Receive(msgClient, this);
        if (round3Success) {
            onRound3Success(msgClient, jpakeClient);
        } else {
            FLogger.i(TAG, "round 3 failed." + strHandshakeId);
        }

        sendRound3AckIfNeeded(msgClient);
    }

    private void onRound3Success(MsgClient msgClient, JPAKEClient jpakeClient) throws IOException {
        byte[] sessionKeyBytes = jpakeClient.getSessionKey();
        FLogger.i(TAG, "round 3 succeeded! key: " + Hex.toHexString(sessionKeyBytes) + strHandshakeId);

        FLogger.d(TAG, "msgClient.iAmTheInitiator == " + msgClient.iAmTheInitiator + strHandshakeId);
        if (!msgClient.iAmTheInitiator) {
            FLogger.d(TAG, "we need to prepare for incoming connection." + strHandshakeId);
            prepareForIncomingConnection(msgClient, jpakeClient);
        } else {
            FLogger.d(TAG, "waiting for round 3 ACK." + strHandshakeId);
        }
    }

    private void prepareForIncomingConnection(MsgClient msgClient, JPAKEClient jpakeClient) throws IOException {
        byte[] sessionKeyBytes = jpakeClient.getSessionKey();
        SessionKey sessionKey = null;
        try {
            sessionKey = new SessionKey(sessionKeyBytes);
        } catch (SessionKey.InvalidSessionKeySizeException e) {
            FLogger.e(TAG, "InvalidSessionKeySizeException: " + e.getMessage() + strHandshakeId);
            FLogger.d(TAG, e);
            FLogger.e(TAG, "stopping JPAKE handshake." + strHandshakeId);
            return;
        }
        InetAddress socketAddress = msgClient.getSocketAddress();
        FLogger.i(TAG, "saving sessionKey for socketAddress: " + socketAddress.getHostAddress() + strHandshakeId);
        MsgServerManager.getInstance().getMsgServerEncrypted().inetAddressToSessionKeyMap
                .put(socketAddress, sessionKey);
    }

    private void sendRound3AckIfNeeded(MsgClient msgClient) {
        if (!msgClient.iAmTheInitiator) {
            FLogger.d(TAG, "we need to send round 3 ACK." + strHandshakeId);
            int portForEncryptedComms = MsgServerManager.getInstance().getMsgServerEncrypted().getPort();
            Message msg = new MsgJPAKERound3Ack(handshakeId, portForEncryptedComms);
            msgClient.sendMessage(msg);
        }
    }

}
