package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgWhoAreYouQuestion extends Message {

    public MsgWhoAreYouQuestion() {
        super();
    }

    public static MsgWhoAreYouQuestion createFromStream(DataInputStream inStream) throws IOException {
        return new MsgWhoAreYouQuestion();
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);
        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    @Override
    public void send(MsgClient msgClient) throws IOException {
        DataOutputStream outStream = msgClient.getOutStream();
        serialiseToStream(outStream);
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(MsgClient.TAG, msgClient.sFromAddress + "received " + getClass().getSimpleName());
        Message msgThisIsMyId = new MsgBadgeStatusUpdate(BadgeStatus.constructMyCurrentBadgeStatus());
        msgClient.sendMessage(msgThisIsMyId);
    }

}
