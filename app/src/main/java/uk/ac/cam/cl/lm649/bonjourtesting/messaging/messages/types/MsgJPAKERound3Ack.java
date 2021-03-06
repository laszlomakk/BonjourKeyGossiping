package uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types;

import android.content.Context;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.SessionKey;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.ratelimit.JPAKERateLimiter;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgJPAKERound3Ack extends Message {

    private static final String TAG = "MsgJPAKERound3Ack";

    public final UUID handshakeId;
    public final int portForEncryptedComms;

    private final String strHandshakeId;

    public MsgJPAKERound3Ack(UUID handshakeId, int portForEncryptedComms) {
        super();

        this.handshakeId = handshakeId;
        this.portForEncryptedComms = portForEncryptedComms;

        this.strHandshakeId = JPAKEClient.createHandshakeIdLogString(handshakeId);
    }

    @UsedViaReflection
    public static MsgJPAKERound3Ack createFromStream(DataInputStream inStream) throws IOException {
        String strHandshakeId = inStream.readUTF();
        UUID handshakeId = HelperMethods.uuidFromStringDefensively(strHandshakeId);
        if (null == handshakeId) return null;

        int portForEncryptedComms = inStream.readInt();
        return new MsgJPAKERound3Ack(handshakeId, portForEncryptedComms);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(handshakeId.toString());
        outStream.writeInt(portForEncryptedComms);
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
        if (!msgClient.iAmTheInitiator) {
            FLogger.e(TAG, "msgClient.iAmTheInitiator == false! we shouldn't have received an ACK." + strHandshakeId);
            return;
        }
        boolean jpakeSuccess = jpakeClient.hasHandshakeFinishedSuccessfully();
        if (jpakeSuccess) {
            onJpakeSuccess(msgClient, jpakeClient);
        } else {
            FLogger.i(TAG, "JPAKE failed." + strHandshakeId);
        }
    }

    private void onJpakeSuccess(MsgClient msgClient, JPAKEClient jpakeClient) {
        byte[] sessionKeyBytes = jpakeClient.getSessionKey();
        FLogger.i(TAG, "JPAKE succeeded." + strHandshakeId);
        SessionKey sessionKey = null;
        try {
            sessionKey = new SessionKey(sessionKeyBytes);
            startSettingUpAnEncryptedConnection(msgClient, portForEncryptedComms, sessionKey);
        } catch (SessionKey.InvalidSessionKeySizeException e) {
            FLogger.e(TAG, "InvalidSessionKeySizeException: " + e + strHandshakeId);
        }
    }

    private void startSettingUpAnEncryptedConnection(MsgClient msgClient, int port, SessionKey sessionKey) {
        if (!NetworkUtil.isPortValid(port)) {
            FLogger.w(TAG, "startSettingUpAnEncryptedConnection(). invalid port(" + port + "). exiting." + strHandshakeId);
            return;
        }
        if (null == sessionKey) {
            FLogger.w(TAG, "startSettingUpAnEncryptedConnection(). sessionKey == null." + strHandshakeId);
            return;
        }
        MsgClient msgClientEncrypted = MsgClient.upgradeToEncryptedMsgClient(
                msgClient, port, sessionKey);

        Context context = msgClientEncrypted.context;
        Message msg = MsgMyPublicKey.createNewMsgWithMyCurrentData(context);
        msgClientEncrypted.sendMessage(msg);

        Message msgContainingContacts = MsgBloomFilterOfContacts.createNewMsgWithMyCurrentData();
        msgClientEncrypted.sendMessage(msgContainingContacts);
    }

}
