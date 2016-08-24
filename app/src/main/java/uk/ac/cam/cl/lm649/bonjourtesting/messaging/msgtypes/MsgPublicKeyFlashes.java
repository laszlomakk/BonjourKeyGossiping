package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.support.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Asymmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.publickeys.DbTablePublicKeys;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.publickeys.PublicKeyEntry;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgPublicKeyFlashes extends Message implements MessageRequiringEncryption {

    private static final String TAG = "MsgCommonContactPubKeys";

    private final List<PublicKeyFlash> publicKeyFlashList;

    public MsgPublicKeyFlashes(@NonNull List<PublicKeyFlash> publicKeyFlashList) {
        super();
        this.publicKeyFlashList = publicKeyFlashList;
    }

    @UsedViaReflection
    public static MsgPublicKeyFlashes createFromStream(DataInputStream inStream) throws IOException {
        int size = inStream.readInt();
        List<PublicKeyFlash> publicKeyFlashList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            publicKeyFlashList.add(PublicKeyFlash.createFromStream(inStream));
        }
        return new MsgPublicKeyFlashes(publicKeyFlashList);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(publicKeyFlashList.size());
        for (PublicKeyFlash publicKeyFlash : publicKeyFlashList) {
            publicKeyFlash.serialiseToStream(outStream);
        }
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, String.format(Locale.US,
                "%sreceived %s",
                msgClient.strFromAddress, getClass().getSimpleName()));

        for (PublicKeyFlash publicKeyFlash : publicKeyFlashList) {
            processReceivedPublicKeyFlash(publicKeyFlash);
        }
    }

    private static void processReceivedPublicKeyFlash(PublicKeyFlash publicKeyFlash) {
        List<PublicKeyEntry> publicKeyEntries =  DbTablePublicKeys.getEntriesForPhoneNumber(publicKeyFlash.phoneNumber);
        FLogger.d(TAG, "received publicKeyFlash - " + publicKeyFlash
                + " -- num of known public keys: " + publicKeyEntries.size());
        boolean matchingPublicKeyFoundForFlash = false;
        for (PublicKeyEntry publicKeyEntry : publicKeyEntries) {
            if (doPublicKeyEntryAndPublicKeyFlashShareTheSamePublicKey(publicKeyEntry, publicKeyFlash)) {
                matchingPublicKeyFoundForFlash = true;
                tryToRefreshPublicKeyEntryWithPublicKeyFlash(publicKeyEntry, publicKeyFlash);
            }
        }
        if (!matchingPublicKeyFoundForFlash) {
            FLogger.d(TAG, "couldn't find matching public key for flash - " + publicKeyFlash);
        }
    }

    private static boolean doPublicKeyEntryAndPublicKeyFlashShareTheSamePublicKey(
            PublicKeyEntry publicKeyEntry, PublicKeyFlash publicKeyFlash) {
        String publicKey = publicKeyEntry.getPublicKey();
        return MsgMyPublicKey.verifySignedHash(
                publicKeyFlash.signedHash,
                publicKey,
                publicKeyFlash.timestamp,
                publicKeyFlash.phoneNumber);
    }

    /**
     * asserts doPublicKeyEntryAndPublicKeyFlashShareTheSamePublicKey(publicKeyEntry, publicKeyFlash) == true
     */
    private static void tryToRefreshPublicKeyEntryWithPublicKeyFlash(
            PublicKeyEntry publicKeyEntry, PublicKeyFlash publicKeyFlash)
    {
        boolean flashIsNewer = publicKeyFlash.timestamp > publicKeyEntry.getTimestampLastSeenAlivePublicKey();
        String logMessage = String.format(Locale.US,
                "Matching public key (%s) found for flash (phoneNum: %s).\nDB_timestamp: %s, flash_timestamp: %s\n",
                Asymmetric.getFingerprint(publicKeyEntry.getPublicKey()),
                publicKeyFlash.phoneNumber,
                new Date(publicKeyEntry.getTimestampLastSeenAlivePublicKey()),
                new Date(publicKeyFlash.timestamp));

        if (flashIsNewer) {
            logMessage += "Yay, flash timestamp is NEWER than what we have! -> refreshing";
            PublicKeyEntry refreshedEntry = new PublicKeyEntry(publicKeyEntry);
            refreshedEntry.setTimestampLastSeenAlivePublicKey(publicKeyFlash.timestamp);
            refreshedEntry.setSignedHash(publicKeyFlash.signedHash);
            DbTablePublicKeys.smartUpdateEntry(refreshedEntry);
        } else {
            logMessage += "Flash timestamp is older than what we have though.";
        }
        FLogger.i(TAG, logMessage);
    }

    public static class PublicKeyFlash {
        private String phoneNumber;
        private long timestamp;
        private byte[] signedHash;

        private PublicKeyFlash() {}

        protected PublicKeyFlash(PublicKeyEntry publicKeyEntry) {
            this.phoneNumber = publicKeyEntry.getPhoneNumber();
            this.timestamp = publicKeyEntry.getTimestampLastSeenAlivePublicKey();
            this.signedHash = publicKeyEntry.getSignedHash();
        }

        private void serialiseToStream(DataOutputStream outStream) throws IOException {
            outStream.writeUTF(phoneNumber);
            outStream.writeLong(timestamp);
            SerialisationUtil.serialiseToStream(outStream, signedHash);
        }

        private static PublicKeyFlash createFromStream(DataInputStream inStream) throws IOException {
            PublicKeyFlash publicKeyFlash = new PublicKeyFlash();
            publicKeyFlash.phoneNumber = inStream.readUTF();
            publicKeyFlash.timestamp = inStream.readLong();
            publicKeyFlash.signedHash = SerialisationUtil.createByteArrayFromStream(inStream);
            return publicKeyFlash;
        }

        @Override
        public String toString() {
            return String.format(Locale.US,
                    "phoneNum: %s, timestamp: %s",
                    phoneNumber, new Date(timestamp));
        }
    }

}
