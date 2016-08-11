package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import java.net.Socket;

import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Symmetric;

public class MsgServerEncrypted extends MsgServer {

    protected static final String TAG = "MsgServerEncrypted";

    @Override
    protected MsgClient createMsgClientForIncomingConnection(Socket socket) {
        byte[] secretKeyBytes = new byte[Symmetric.KEY_LENGTH_IN_BYTES];
        return new MsgClient(socket, secretKeyBytes);
    }

}
