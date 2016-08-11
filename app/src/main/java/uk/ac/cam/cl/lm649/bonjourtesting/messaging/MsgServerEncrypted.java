package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.support.annotation.Nullable;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgMyPublicKey;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgServerEncrypted extends MsgServer {

    protected static final String TAG = "MsgServerEncrypted";

    public final ConcurrentHashMap<InetAddress, BigInteger> inetAddressToSessionKeyMap = new ConcurrentHashMap<>();

    @Override
    @Nullable
    protected MsgClient createMsgClientForIncomingConnection(Socket socket) {
        InetAddress socketAddress = socket.getInetAddress();
        BigInteger sessionKey = inetAddressToSessionKeyMap.remove(socketAddress);
        byte[] secretKeyBytes = new byte[0];

        boolean sessionKeyFound = (null != sessionKey);
        boolean secretKeyHasValidLength = false;

        if (sessionKeyFound) {
            secretKeyBytes = sessionKey.toByteArray();
            secretKeyHasValidLength = MsgClient.isSecretKeyLengthValid(secretKeyBytes);
        }

        FLogger.i(TAG, String.format(Locale.US,
                "incoming connection from %s, session key found: %b, has valid length: %s",
                socketAddress.getHostAddress(), sessionKeyFound, secretKeyHasValidLength));

        if (sessionKeyFound && secretKeyHasValidLength) {
            MsgClient msgClientEncrypted = new MsgClient(socket, secretKeyBytes);

            Message msg = MsgMyPublicKey.createNewMsgWithMyCurrentData(context);
            msgClientEncrypted.sendMessage(msg);

            return msgClientEncrypted;
        } else {
            return null;
        }
    }

}
