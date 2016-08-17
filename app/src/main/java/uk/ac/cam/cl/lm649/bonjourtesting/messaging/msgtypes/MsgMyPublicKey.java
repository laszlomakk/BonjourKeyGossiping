package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Asymmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePublicKeys;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

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

    public static MsgMyPublicKey createNewMsgWithMyCurrentData(Context context) {
        SaveIdentityData saveIdentityData = SaveIdentityData.getInstance(context);
        String phoneNumber = saveIdentityData.getPhoneNumber();
        String publicKey = saveIdentityData.getMyPublicKey();
        long curTime = System.currentTimeMillis();
        return new MsgMyPublicKey(phoneNumber, publicKey, curTime);
    }

    @UsedViaReflection
    public static MsgMyPublicKey createFromStream(DataInputStream inStream) throws IOException {
        String phoneNumber = inStream.readUTF();
        String publicKey = inStream.readUTF();
        long timestamp = inStream.readLong();
        return new MsgMyPublicKey(phoneNumber, publicKey, timestamp);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(phoneNumber);
        outStream.writeUTF(publicKey);
        outStream.writeLong(timestamp);
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        String fingerprint = Asymmetric.getFingerprint(publicKey);
        FLogger.i(msgClient.logTag, String.format(Locale.US,
                "%sreceived %s:\nphoneNum: %s, pubKey: %s",
                msgClient.strFromAddress, getClass().getSimpleName(), phoneNumber, fingerprint));
        boolean validPublicKey = Asymmetric.isValidKey(publicKey);
        FLogger.i(msgClient.logTag, "pubKey: " + fingerprint + " tested to be valid: " + validPublicKey);
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
