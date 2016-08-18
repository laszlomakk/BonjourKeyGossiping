package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Asymmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.DataSizeException;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Hash;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.KeyDecodingException;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePublicKeys;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgMyPublicKey extends Message implements MessageRequiringEncryption {

    private static final String TAG = "MsgMyPublicKey";

    public final String publicKey;
    public final long timestamp;
    public final byte[] signedHash;

    public MsgMyPublicKey(@NonNull String publicKey, long timestamp, @NonNull byte[] signedHash) {
        super();
        this.publicKey = publicKey;
        this.timestamp = timestamp;
        this.signedHash = signedHash;
    }

    @Nullable
    public static MsgMyPublicKey createNewMsgWithMyCurrentData(Context context) {
        SaveIdentityData saveIdentityData = SaveIdentityData.getInstance(context);
        String phoneNumber = saveIdentityData.getPhoneNumber();
        String publicKey = saveIdentityData.getMyPublicKey();
        long timestamp = System.currentTimeMillis();

        byte[] hash = calcHashOfContents(publicKey, timestamp, phoneNumber);

        byte[] signedHash;
        try {
            String myStrPrivateKey = saveIdentityData.getMyPrivateKey();
            AsymmetricKeyParameter myPrivateKey = Asymmetric.stringKeyToKey(myStrPrivateKey);
            signedHash = Asymmetric.encryptBytes(hash, myPrivateKey);
        } catch (KeyDecodingException | InvalidCipherTextException | DataSizeException e) {
            FLogger.e(TAG, "createNewMsgWithMyCurrentData(). Exception: " + e.getMessage());
            FLogger.d(TAG, e);
            return null;
        }
        return new MsgMyPublicKey(publicKey, timestamp, signedHash);
    }

    @UsedViaReflection
    public static MsgMyPublicKey createFromStream(DataInputStream inStream) throws IOException {
        byte[] signedHash = Util.createByteArrayFromStream(inStream);

        String publicKey;
        try {
            publicKey = Asymmetric.byteKeyToStringKey(Util.createByteArrayFromStream(inStream));
        } catch (KeyDecodingException e) {
            FLogger.e(TAG, "createFromStream(). KeyDecodingException: " + e.getMessage());
            FLogger.d(TAG, e);
            return null;
        }

        long timestamp = inStream.readLong();
        return new MsgMyPublicKey(publicKey, timestamp, signedHash);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        Util.serialiseToStream(outStream, signedHash);

        try {
            Util.serialiseToStream(outStream, Asymmetric.stringKeyToByteKey(publicKey));
        } catch (KeyDecodingException e) {
            FLogger.e(TAG, "createFromStream(). KeyDecodingException: " + e.getMessage());
            FLogger.d(TAG, e);
            throw new IOException(e);
        }

        outStream.writeLong(timestamp);
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        String fingerprint = Asymmetric.getFingerprint(publicKey);
        String phoneNumber = msgClient.sessionData.getPhoneNumberOfOtherParticipant();
        FLogger.i(msgClient.logTag, String.format(Locale.US,
                "%sreceived %s:\npubKey: %s, time: %s, (phoneNum: %s)",
                msgClient.strFromAddress,
                getClass().getSimpleName(),
                fingerprint,
                new Date(timestamp),
                phoneNumber));

        boolean verifiedSignature = verifySignedHash(signedHash, publicKey, timestamp, phoneNumber);
        boolean plausibleTimestamp = isTimestampPlausible(timestamp);
        String logTestResults = String.format(Locale.US,
                "pubKey: %s - verified signature: %b, plausible timestamp: %b",
                fingerprint, verifiedSignature, plausibleTimestamp);
        if (verifiedSignature && plausibleTimestamp) {
            FLogger.i(TAG, logTestResults);
            updatePublicKeyInDatabase(phoneNumber);
        } else {
            FLogger.w(TAG, logTestResults);
        }
    }

    private void updatePublicKeyInDatabase(String phoneNumber) {
        DbTablePublicKeys.Entry entry = new DbTablePublicKeys.Entry();
        entry.setPublicKey(publicKey);
        entry.setPhoneNumber(phoneNumber);
        entry.setTimestampLastSeenAlivePublicKey(timestamp);
        DbTablePublicKeys.smartUpdateEntry(entry);
        CustomActivity.forceRefreshUIInTopActivity();
    }

    private static byte[] calcHashOfContents(String publicKey, long timestamp, String phoneNumber) {
        String contents = publicKey + timestamp + phoneNumber;
        return Hash.hashString(contents);
    }

    private static boolean verifySignedHash(
            @Nullable byte[] signedHash,
            @Nullable String strPublicKey,
            long timestamp,
            @Nullable String phoneNumber)
    {
        if (null == signedHash) {
            FLogger.e(TAG, "verifySignedHash(). signedHash == null");
            return false;
        }
        if (null == strPublicKey) {
            FLogger.e(TAG, "verifySignedHash(). strPublicKey == null");
            return false;
        }
        if (null == phoneNumber) {
            FLogger.e(TAG, "verifySignedHash(). phoneNumber == null");
            return false;
        }

        byte[] trustedHash = calcHashOfContents(strPublicKey, timestamp, phoneNumber);

        try {
            AsymmetricKeyParameter publicKey = Asymmetric.stringKeyToKey(strPublicKey);
            byte[] untrustedHash = Asymmetric.decryptBytes(signedHash, publicKey);
            return Arrays.equals(trustedHash, untrustedHash);
        } catch (KeyDecodingException | InvalidCipherTextException e) {
            FLogger.e(TAG, "createNewMsgWithMyCurrentData(). Exception: " + e.getMessage());
            FLogger.d(TAG, e);
            return false;
        }
    }

    private static boolean isTimestampPlausible(long timestamp) {
        long localTime = System.currentTimeMillis();
        long threshold = 10 * 60 * 1000;
        return Math.abs(timestamp - localTime) < threshold;
    }

}
