package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.content.Context;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServerManager;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.SessionKey;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class MsgJPAKERound3 extends Message {

    private static final String TAG = "MsgJPAKERound3";

    public final BigInteger macTag;
    public final int portForEncryptedComms;

    public MsgJPAKERound3(BigInteger macTag, int portForEncryptedComms) {
        super();

        this.macTag = macTag;
        this.portForEncryptedComms = portForEncryptedComms;
    }

    public static MsgJPAKERound3 createFromStream(DataInputStream inStream) throws IOException {
        int radix = inStream.readInt();
        BigInteger macTag = new BigInteger(inStream.readUTF(), radix);
        int portForEncryptedComms = inStream.readInt();
        return new MsgJPAKERound3(macTag, portForEncryptedComms);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

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
                getClass().getSimpleName());
        JPAKEClient jpakeClient = msgClient.jpakeClient;
        if (null == jpakeClient) {
            FLogger.e(TAG, "onReceive(). jpakeClient is null.");
            return;
        }
        boolean round3Success = jpakeClient.round3Receive(msgClient, this);
        if (round3Success) {
            BigInteger sessionKeySecret = jpakeClient.getSessionKey();
            FLogger.i(TAG, "round 3 succeeded! key: " + sessionKeySecret.toString(Character.MAX_RADIX));

            FLogger.d(TAG, "msgClient.iAmTheInitiator == " + msgClient.iAmTheInitiator);
            if (msgClient.iAmTheInitiator) {
                SessionKey sessionKey = null;
                try {
                    sessionKey = new SessionKey(sessionKeySecret.toByteArray());
                    startSettingUpAnEncryptedConnection(msgClient, portForEncryptedComms, sessionKey);
                } catch (SessionKey.InvalidSessionKeySizeException e) {
                    FLogger.e(TAG, "InvalidSessionKeySizeException: " + e.getMessage());
                }
            }
        } else {
            FLogger.i(TAG, "round 3 failed.");
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
