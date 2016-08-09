package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.support.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgMyPhoneNumber extends Message {

    public final String badgeId;
    public final String phoneNumber;

    public MsgMyPhoneNumber(@NonNull String badgeId, @NonNull String phoneNumber) {
        super();
        this.badgeId = badgeId;
        this.phoneNumber = phoneNumber;
    }

    public static MsgMyPhoneNumber createFromStream(DataInputStream inStream) throws IOException {
        String badgeId = inStream.readUTF();
        String phoneNumber = inStream.readUTF();
        return new MsgMyPhoneNumber(badgeId, phoneNumber);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        outStream.writeUTF(badgeId);
        outStream.writeUTF(phoneNumber);

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
        FLogger.i(MsgClient.TAG, String.format(Locale.US,
                "%sreceived %s:\nbadgeId: %s, phoneNum: %s",
                msgClient.sFromAddress, getClass().getSimpleName(), badgeId, phoneNumber));
        DbTablePhoneNumbers.smartUpdateEntry(UUID.fromString(badgeId), phoneNumber);
        CustomActivity.forceRefreshUIInTopActivity();
    }

}
