package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.content.Context;

import org.bouncycastle.util.encoders.Hex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.SessionKey;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class MsgJPAKERound3 extends Message {

    private static final String TAG = "MsgJPAKERound3";

    public final UUID handshakeId;
    public final BigInteger macTag;
    public final int portForEncryptedComms;

    private final String strHandshakeId;

    public MsgJPAKERound3(UUID handshakeId, BigInteger macTag, int portForEncryptedComms) {
        super();

        this.handshakeId = handshakeId;
        this.macTag = macTag;
        this.portForEncryptedComms = portForEncryptedComms;

        this.strHandshakeId = JPAKEClient.createHandshakeIdLogString(handshakeId);
    }

    public static MsgJPAKERound3 createFromStream(DataInputStream inStream) throws IOException {
        String strHandshakeId = inStream.readUTF();
        UUID handshakeId = HelperMethods.uuidFromStringDefensively(strHandshakeId);
        if (null == handshakeId) return null;

        int radix = inStream.readInt();
        BigInteger macTag = new BigInteger(inStream.readUTF(), radix);
        int portForEncryptedComms = inStream.readInt();
        return new MsgJPAKERound3(handshakeId, macTag, portForEncryptedComms);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        outStream.writeUTF(handshakeId.toString());
        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(macTag.toString(radix));
        outStream.writeInt(portForEncryptedComms);

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
            byte[] sessionKeyBytes = jpakeClient.getSessionKey();
            FLogger.i(TAG, "round 3 succeeded! key: " + Hex.toHexString(sessionKeyBytes) + strHandshakeId);

            FLogger.d(TAG, "msgClient.iAmTheInitiator == " + msgClient.iAmTheInitiator + strHandshakeId);
            if (msgClient.iAmTheInitiator) {
                SessionKey sessionKey = null;
                try {
                    sessionKey = new SessionKey(sessionKeyBytes);
                    startSettingUpAnEncryptedConnection(msgClient, portForEncryptedComms, sessionKey);
                } catch (SessionKey.InvalidSessionKeySizeException e) {
                    FLogger.e(TAG, "InvalidSessionKeySizeException: " + e.getMessage() + strHandshakeId);
                }
            }
        } else {
            FLogger.i(TAG, "round 3 failed." + strHandshakeId);
        }
    }

    private static void startSettingUpAnEncryptedConnection(MsgClient msgClient, int port, SessionKey sessionKey) {
        if (!NetworkUtil.isPortValid(port)) {
            FLogger.w(TAG, "startSettingUpAnEncryptedConnection(). invalid port(" + port + "). exiting.");
            return;
        }
        if (null == sessionKey) {
            FLogger.w(TAG, "startSettingUpAnEncryptedConnection(). sessionKey == null.");
            return;
        }
        MsgClient msgClientEncrypted = MsgClient.upgradeToEncryptedMsgClient(
                msgClient, port, sessionKey);

        Context context = msgClientEncrypted.context;
        Message msg = MsgMyPublicKey.createNewMsgWithMyCurrentData(context);
        msgClientEncrypted.sendMessage(msg);
    }

}
