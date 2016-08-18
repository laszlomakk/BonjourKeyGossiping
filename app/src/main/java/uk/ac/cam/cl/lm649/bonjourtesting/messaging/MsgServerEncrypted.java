package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.support.annotation.Nullable;

import java.net.InetAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgMyPublicKey;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgServerEncrypted extends MsgServer {

    protected static final String TAG = "MsgServerEncrypted";

    public final ConcurrentHashMap<InetAddress, SessionData> inetAddressToSessionDataMap = new ConcurrentHashMap<>();

    @Override
    @Nullable
    protected MsgClient createMsgClientForIncomingConnection(Socket socket) {
        InetAddress socketAddress = socket.getInetAddress();
        SessionData sessionData = inetAddressToSessionDataMap.remove(socketAddress);

        boolean sessionDataFound = (null != sessionData);

        FLogger.i(TAG, String.format(Locale.US,
                "incoming connection from %s, session data found: %b",
                socketAddress.getHostAddress(), sessionDataFound));

        if (sessionDataFound) {
            MsgClient msgClientEncrypted = new MsgClient(socket, sessionData);

            if (Constants.RESPONDER_ALSO_SENDS_PUBLIC_KEY) {
                Message msg = MsgMyPublicKey.createNewMsgWithMyCurrentData(context);
                msgClientEncrypted.sendMessage(msg);
            }

            return msgClientEncrypted;
        } else {
            return null;
        }
    }

}
