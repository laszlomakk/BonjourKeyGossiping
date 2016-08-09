package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.support.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Asymmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Hash;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePublicKeys;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgMyPublicKey extends Message {

    public final String phoneNumber;
    public final String publicKey;
    public final long timestamp;

    public MsgMyPublicKey(@NonNull String phoneNumber, @NonNull String publicKey, long timestamp) {
        super();
        this.phoneNumber = phoneNumber;
        this.publicKey = publicKey;
        this.timestamp = timestamp;
    }

    public static MsgMyPublicKey createFromStream(DataInputStream inStream) throws IOException {
        String phoneNumber = inStream.readUTF();
        String publicKey = inStream.readUTF();
        long timestamp = inStream.readLong();
        return new MsgMyPublicKey(phoneNumber, publicKey, timestamp);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        outStream.writeUTF(phoneNumber);
        outStream.writeUTF(publicKey);
        outStream.writeLong(timestamp);

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
        String fingerprint = Hash.hashStringToString(publicKey); // TODO hash byteKeys instead
        FLogger.i(MsgClient.TAG, String.format(Locale.US,
                "%sreceived %s:\nphoneNum: %s, pubKey: %s",
                msgClient.sFromAddress, getClass().getSimpleName(), phoneNumber, fingerprint));
        boolean validPublicKey = Asymmetric.isValidKey(publicKey);
        FLogger.i(MsgClient.TAG, "pubKey: " + fingerprint + " tested to be valid: " + validPublicKey);
        if (validPublicKey) {
            DbTablePublicKeys.Entry entry = new DbTablePublicKeys.Entry();
            entry.setPublicKey(publicKey);
            entry.setPhoneNumber(phoneNumber);
            entry.setTimestampLastSeenAlivePublicKey(timestamp);
            DbTablePublicKeys.smartUpdateEntry(entry);
            CustomActivity.forceRefreshUIInTopActivity();
        }
    }

}
