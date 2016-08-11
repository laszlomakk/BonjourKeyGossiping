package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import java.net.Socket;

public class MsgServerPlaintext extends MsgServer {

    protected static final String TAG = "MsgServerPlaintext";

    @Override
    protected MsgClient createMsgClientForIncomingConnection(Socket socket) {
        return new MsgClient(socket, null);
    }

}
