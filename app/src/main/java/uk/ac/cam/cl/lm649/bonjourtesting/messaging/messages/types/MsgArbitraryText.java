package uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgArbitraryText extends Message {

    public final String text;

    public MsgArbitraryText(String text) {
        super();
        this.text = text;
    }

    @UsedViaReflection
    public static MsgArbitraryText createFromStream(DataInputStream inStream) throws IOException {
        String text = inStream.readUTF();
        return new MsgArbitraryText(text);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(text);
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
