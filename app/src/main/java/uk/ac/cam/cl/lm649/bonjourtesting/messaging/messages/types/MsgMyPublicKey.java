package uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.util.encoders.Hex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Asymmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.DataSizeException;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Hash;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.KeyDecodingException;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.publickeys.DbTablePublicKeys;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.publickeys.PublicKeyEntry;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.SerialisationUtil;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgMyPublicKey extends Message implements Message.RequiresEncryption {

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
            FLogger.e(TAG, "createNewMsgWithMyCurrentData(). Exception: " + e);
            FLogger.d(TAG, e);
            return null;
        }
        return new MsgMyPublicKey(publicKey, timestamp, signedHash);
    }

    @UsedViaReflection
    public static MsgMyPublicKey createFromStream(DataInputStream inStream) throws IOException {
        byte[] signedHash = SerialisationUtil.createByteArrayFromStream(inStream);

        String publicKey;
        try {
            publicKey = Asymmetric.byteKeyToStringKey(SerialisationUtil.createByteArrayFromStream(inStream));
        } catch (KeyDecodingException e) {
            FLogger.e(TAG, "createFromStream(). Exception: " + e);
            FLogger.d(TAG, e);
            return null;
        }

        long timestamp = inStream.readLong();
        return new MsgMyPublicKey(publicKey, timestamp, signedHash);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        SerialisationUtil.serialiseToStream(outStream, signedHash);

        try {
            SerialisationUtil.serialiseToStream(outStream, Asymmetric.stringKeyToByteKey(publicKey));
        } catch (KeyDecodingException e) {
            FLogger.e(TAG, "createFromStream(). Exception: " + e);
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
                HelperMethods.getTimeStamp(timestamp),
                phoneNumber));

        if (null == phoneNumber) {
            FLogger.e(TAG, "onReceive(). phoneNumber == null");
            return;
        }

        boolean verifiedSignature = verifySignedHash(signedHash, publicKey, timestamp, phoneNumber);
        boolean plausibleTimestamp = isTimestampPlausible(timestamp);
        String logMessage = String.format(Locale.US,
                "pubKey: %s - verified signature: %b, plausible timestamp: %b",
                fingerprint, verifiedSignature, plausibleTimestamp);
        if (verifiedSignature && plausibleTimestamp) {
            // at this point, the message is known to be self-consistent
            // but is it consistent with our previous knowledge? (DB)
            processMessageGivenSelfConsistency(msgClient, phoneNumber, logMessage);
        } else {
            FLogger.w(TAG, logMessage + " -> REJECTING, failed self-consistency test");
            FLogger.i(TAG, "closing MsgClient with " + msgClient.strSocketAddress);
            msgClient.close();
        }
    }

    private void processMessageGivenSelfConsistency(
            @NonNull MsgClient msgClient, @NonNull String phoneNumber, @NonNull String logMessage)
    {
        PUBLIC_KEY_STANDING_FOR_PHONE_NUMBER publicKeyStandingForPhoneNumber =
                determinePublicKeyStanding(phoneNumber, publicKey);

        boolean acceptMessage = false;
        switch (publicKeyStandingForPhoneNumber) {
            case FIRST_USE:
                FLogger.i(TAG, logMessage + " - ACCEPTING, trust on first use");
                acceptMessage = true;
                break;
            case UPDATE_FOR_ALREADY_KNOWN:
                FLogger.i(TAG, logMessage + " - ACCEPTING, already known public key");
                acceptMessage = true;
                break;
            case DIFFERENT_THAN_KNOWN:
                FLogger.w(TAG, logMessage + " - REJECTING, different public key");
                break;
            default:
                FLogger.e(TAG, logMessage + " - ERROR, unknown publicKeyStandingForPhoneNumber");
                break;
        }

        if (acceptMessage) {
            updatePublicKeyInDatabase(phoneNumber);
            msgClient.sessionData.setPublicKeyOfOtherParticipant(publicKey);
            msgClient.sessionData.setTrustOtherParticipant(true);
            CustomActivity.forceRefreshUIInTopActivity();
        } else {
            FLogger.i(TAG, "closing MsgClient with " + msgClient.strSocketAddress);
            msgClient.close();
        }
    }

    private static PUBLIC_KEY_STANDING_FOR_PHONE_NUMBER determinePublicKeyStanding(
            @NonNull String phoneNumber, @NonNull String publicKey)
    {
        List<PublicKeyEntry> publicKeyEntryListForPhoneNumber =
                DbTablePublicKeys.getEntriesForPhoneNumber(phoneNumber);
        if (publicKeyEntryListForPhoneNumber.isEmpty()) {
            // no known public key for phone number yet
            // trust on first use
            return PUBLIC_KEY_STANDING_FOR_PHONE_NUMBER.FIRST_USE;
        } else {
            // we already know a public key for this phone number
            // we are not willing to accept other public keys
            PublicKeyEntry oldEntry = publicKeyEntryListForPhoneNumber.get(0);
            if (oldEntry.getPublicKey().equals(publicKey)) {
                // this is the public key we've already known
                return PUBLIC_KEY_STANDING_FOR_PHONE_NUMBER.UPDATE_FOR_ALREADY_KNOWN;
            } else {
                // the public key for the phone number changed ??
                // this is either a message from an impostor or a legitimate change of keys
                return PUBLIC_KEY_STANDING_FOR_PHONE_NUMBER.DIFFERENT_THAN_KNOWN;
            }
        }
    }

    private void updatePublicKeyInDatabase(@NonNull String phoneNumber) {
        PublicKeyEntry entry = new PublicKeyEntry();
        entry.setPublicKey(publicKey);
        entry.setPhoneNumber(phoneNumber);
        entry.setTimestampLastSeenAlivePublicKey(timestamp);
        entry.setSignedHash(signedHash);
        DbTablePublicKeys.smartUpdateEntry(entry);
    }

    private static byte[] calcHashOfContents(String publicKey, long timestamp, String phoneNumber) {
        String contents = publicKey + timestamp + phoneNumber;
        return Hash.hashString(contents);
    }

    public static boolean verifySignedHash(
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

        try {
            return _verifySignedHash(signedHash, strPublicKey, timestamp, phoneNumber);
        } catch (KeyDecodingException | InvalidCipherTextException e) {
            FLogger.d(TAG, "verifySignedHash(). Exception: " + e);
            return false;
        } catch (DataSizeException e) {
            FLogger.e(TAG, "verifySignedHash(). Exception: " + e);
            FLogger.e(TAG, "verifySignedHash(). signed hash: " + Hex.toHexString(signedHash));
            return false;
        }
    }

    private static boolean _verifySignedHash(
            @NonNull byte[] signedHash,
            @NonNull String strPublicKey,
            long timestamp,
            @NonNull String phoneNumber) throws KeyDecodingException, InvalidCipherTextException, DataSizeException
    {
        byte[] trustedHash = calcHashOfContents(strPublicKey, timestamp, phoneNumber);

        AsymmetricKeyParameter publicKey = Asymmetric.stringKeyToKey(strPublicKey);
        byte[] untrustedHash = Asymmetric.decryptBytes(signedHash, publicKey);

        return Arrays.equals(trustedHash, untrustedHash);
    }

    private static boolean isTimestampPlausible(long timestamp) {
        long localTime = System.currentTimeMillis();
        long threshold = 10 * Constants.MSECONDS_IN_MINUTE;
        return Math.abs(timestamp - localTime) < threshold;
    }

    public enum PUBLIC_KEY_STANDING_FOR_PHONE_NUMBER {
        FIRST_USE, UPDATE_FOR_ALREADY_KNOWN, DIFFERENT_THAN_KNOWN
    }

}