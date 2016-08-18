package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.content.Context;
import android.support.annotation.NonNull;

import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Hash;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.JPAKEManager;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgSaltedPhoneNumber extends Message {

    private static final String TAG = "MsgSaltedPhoneNumber";

    public final byte[] salt;
    public final int nRevealedBitsOfHash;
    public final byte[] hashOfSaltedPhoneNumber;

    public MsgSaltedPhoneNumber(
            @NonNull byte[] salt,
            int nRevealedBitsOfHash,
            @NonNull byte[] hashOfSaltedPhoneNumber) {
        super();
        this.salt = salt;
        this.nRevealedBitsOfHash = nRevealedBitsOfHash;
        this.hashOfSaltedPhoneNumber = hashOfSaltedPhoneNumber;
    }

    public static MsgSaltedPhoneNumber createNewMsgWithMyCurrentData(Context context) {
        SaveIdentityData saveIdentityData = SaveIdentityData.getInstance(context);
        String phoneNumber = saveIdentityData.getPhoneNumber();
        byte[] salt = saveIdentityData.getStaticSalt();

        int nRevealedBitsOfHash = Constants.NUM_REVEALED_BITS_OF_PHONE_NUMBER_HASH;
        byte[] partialHash = calcPartialHashOfPhoneNumberAndSalt(phoneNumber, salt, nRevealedBitsOfHash);

        return new MsgSaltedPhoneNumber(salt, nRevealedBitsOfHash, partialHash);
    }

    @UsedViaReflection
    public static MsgSaltedPhoneNumber createFromStream(DataInputStream inStream) throws IOException {
        byte[] salt = SerialisationUtil.createByteArrayFromStream(inStream);
        int nRevealedBitsOfHash = inStream.readInt();
        byte[] hashOfSaltedPhoneNumber = SerialisationUtil.createByteArrayFromStream(inStream);

        return new MsgSaltedPhoneNumber(salt, nRevealedBitsOfHash, hashOfSaltedPhoneNumber);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        SerialisationUtil.serialiseToStream(outStream, salt);
        outStream.writeInt(nRevealedBitsOfHash);
        SerialisationUtil.serialiseToStream(outStream, hashOfSaltedPhoneNumber);
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, String.format(Locale.US,
                "%sreceived %s:\nhash: %s, salt: %s, nBits: %d",
                msgClient.strFromAddress,
                getClass().getSimpleName(),
                Hex.toHexString(hashOfSaltedPhoneNumber),
                Hex.toHexString(salt),
                nRevealedBitsOfHash));

        List<String> hopefulPhoneNumbers = getHopefulPhoneNumbers();

        if (hopefulPhoneNumbers.size() == 0) {
            FLogger.i(TAG, "no match found for hash " + Hex.toHexString(hashOfSaltedPhoneNumber));
        } else {
            JPAKEManager.startJPAKEWave(msgClient, hopefulPhoneNumbers);
        }
    }

    private List<String> getHopefulPhoneNumbers() {
        List<String> hopefulPhoneNumbers = new ArrayList<>();
        for (DbTablePhoneNumbers.Entry entry : DbTablePhoneNumbers.getAllEntries()) {
            byte[] partialHashCandidate = calcPartialHashOfPhoneNumberAndSalt(
                    entry.getPhoneNumber(), salt, nRevealedBitsOfHash);
            if (Arrays.equals(partialHashCandidate, hashOfSaltedPhoneNumber)) {
                FLogger.i(TAG, String.format(Locale.US,
                        "match found for hash %s, where phoneNumber is %s",
                        Hex.toHexString(hashOfSaltedPhoneNumber),
                        entry.getPhoneNumber()));
                hopefulPhoneNumbers.add(entry.getPhoneNumber());
            }
        }
        return hopefulPhoneNumbers;
    }

    public static byte[] calcPartialHashOfPhoneNumberAndSalt(String phoneNumber, byte[] salt, int nBitsToReveal) {
        String base64Salt = Base64.toBase64String(salt);
        byte[] hashOfSaltedPhoneNumber = Hash.hashString(phoneNumber + base64Salt);

        return HelperMethods.getNLowBitsOfByteArray(hashOfSaltedPhoneNumber, nBitsToReveal);
    }

    public static void main(String args[]) {
        // hash pre-image attack
        String phoneNumber = bruteforceAPhoneNumber("e801", "a550c1730c9da0b74feec252750dbf9d", 9);
        System.out.println("phone number found: " + phoneNumber);
    }

    private static String bruteforceAPhoneNumber(String hexPartialHashTarget, String hexSalt, int nRevealedBitsOfHash) {
        byte[] partialHashTarget = Hex.decode(hexPartialHashTarget);
        byte[] salt = Hex.decode(hexSalt);

        return bruteforceAPhoneNumber(partialHashTarget, salt, nRevealedBitsOfHash);
    }

    private static String bruteforceAPhoneNumber(byte[] partialHashTarget, byte[] salt, int nRevealedBitsOfHash) {
        String phoneNumber;
        for (int phoneNumberInt = new SecureRandom().nextInt(); ; phoneNumberInt++) {
            phoneNumber = Integer.toString(phoneNumberInt);
            byte[] partialHash = calcPartialHashOfPhoneNumberAndSalt(phoneNumber, salt, nRevealedBitsOfHash);
            if (Arrays.equals(partialHashTarget, partialHash)) {
                break;
            }
        }
        return phoneNumber;
    }

}
