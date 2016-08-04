package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
        outStream.flush();
    }

}
