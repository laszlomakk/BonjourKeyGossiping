package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.support.annotation.NonNull;

import java.net.Socket;

public class MsgServerPlaintext extends MsgServer {

    protected static final String TAG = "MsgServerPlaintext";

    @Override
    @NonNull
    protected MsgClient createMsgClientForIncomingConnection(Socket socket) {
        return new MsgClient(socket, null);
    }

}
