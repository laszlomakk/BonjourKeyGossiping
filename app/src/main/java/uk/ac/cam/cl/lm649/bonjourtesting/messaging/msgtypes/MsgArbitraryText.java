package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MsgArbitraryText extends Message {

    public final String text;

    public MsgArbitraryText(String text) {
        super();
        this.text = text;
    }

    public static MsgArbitraryText createFromStream(DataInputStream inStream) throws IOException {
        String text = inStream.readUTF();
        return new MsgArbitraryText(text);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);
        outStream.writeUTF(text);
        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    @Override
    public void send(MsgClient msgClient) throws IOException {
        super.send(msgClient);
        HelperMethods.displayMsgToUser(msgClient.context, "msg sent");
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, msgClient.strFromAddress + "received " +
                getClass().getSimpleName() + ": " + text);
        HelperMethods.displayMsgToUser(msgClient.context, text);
    }

}
