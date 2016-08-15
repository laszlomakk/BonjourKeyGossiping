package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.support.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgMyPhoneNumber extends Message {

    public final String customName;
    public final String phoneNumber;

    public MsgMyPhoneNumber(@NonNull String customName, @NonNull String phoneNumber) {
        super();
        this.customName = customName;
        this.phoneNumber = phoneNumber;
    }

    public static MsgMyPhoneNumber createFromStream(DataInputStream inStream) throws IOException {
        String customName = inStream.readUTF();
        String phoneNumber = inStream.readUTF();
        return new MsgMyPhoneNumber(customName, phoneNumber);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        outStream.writeUTF(customName);
        outStream.writeUTF(phoneNumber);

        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, String.format(Locale.US,
                "%sreceived %s:\nnick: %s, phoneNum: %s",
                msgClient.strFromAddress, getClass().getSimpleName(), customName, phoneNumber));
        DbTablePhoneNumbers.smartUpdateEntry(phoneNumber, customName);
        CustomActivity.forceRefreshUIInTopActivity();
    }

}
