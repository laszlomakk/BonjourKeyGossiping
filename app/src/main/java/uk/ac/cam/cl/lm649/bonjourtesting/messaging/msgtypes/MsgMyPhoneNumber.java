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
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgMyPhoneNumber extends Message {

    public final String customName;
    public final String phoneNumber;

    public MsgMyPhoneNumber(@NonNull String customName, @NonNull String phoneNumber) {
        super();
        this.customName = customName;
        this.phoneNumber = phoneNumber;
    }

    @UsedViaReflection
    public static MsgMyPhoneNumber createFromStream(DataInputStream inStream) throws IOException {
        String customName = inStream.readUTF();
        String phoneNumber = inStream.readUTF();
        return new MsgMyPhoneNumber(customName, phoneNumber);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(customName);
        outStream.writeUTF(phoneNumber);
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, String.format(Locale.US,
                "%sreceived %s:\nnick: %s, phoneNum: %s",
                msgClient.strFromAddress, getClass().getSimpleName(), customName, phoneNumber));
        DbTablePhoneNumbers.Entry contact = new DbTablePhoneNumbers.Entry(phoneNumber, customName);
        DbTablePhoneNumbers.smartUpdateEntry(contact);
        CustomActivity.forceRefreshUIInTopActivity();
    }

}
