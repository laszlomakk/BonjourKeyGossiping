package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgWhoAreYouQuestion extends Message {

    public static final int TYPE_NUM = 1;

    public MsgWhoAreYouQuestion() {
        super(TYPE_NUM);
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
    public void receive(MsgClient msgClient) throws IOException {
        FLogger.i(MsgClient.TAG, msgClient.sFromAddress + "received " + getClass().getSimpleName());
        Message msgThisIsMyId = new MsgThisIsMyIdentity(BadgeStatus.constructMyCurrentBadgeStatus());
        msgClient.sendMessage(msgThisIsMyId);
    }

}
