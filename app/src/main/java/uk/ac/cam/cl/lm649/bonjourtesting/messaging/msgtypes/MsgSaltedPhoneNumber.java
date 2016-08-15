package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Base64;

import org.bouncycastle.util.encoders.Hex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Hash;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MsgSaltedPhoneNumber extends Message {

    public final byte[] salt;
    public final int nRevealedBitsOfHash;
    public final byte[] hashOfSaltedPhoneNumber;

    public MsgSaltedPhoneNumber(
            @NonNull byte[] salt,
            @NonNull int nRevealedBitsOfHash,
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

    public static MsgSaltedPhoneNumber createFromStream(DataInputStream inStream) throws IOException {
        int saltLength = inStream.readInt();
        byte[] salt = new byte[saltLength];
        inStream.readFully(salt);

        int nRevealedBitsOfHash = inStream.readInt();

        int hashOfSaltedPhoneNumberLength = inStream.readInt();
        byte[] hashOfSaltedPhoneNumber = new byte[hashOfSaltedPhoneNumberLength];
        inStream.readFully(hashOfSaltedPhoneNumber);

        return new MsgSaltedPhoneNumber(salt, nRevealedBitsOfHash, hashOfSaltedPhoneNumber);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);

        outStream.writeInt(salt.length);
        outStream.write(salt);
        outStream.writeInt(nRevealedBitsOfHash);
        outStream.writeInt(hashOfSaltedPhoneNumber.length);
        outStream.write(hashOfSaltedPhoneNumber);

        Message.writeMessageEndMarker(outStream);
        outStream.flush();
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

    }

    public static byte[] calcPartialHashOfPhoneNumberAndSalt(String phoneNumber, byte[] salt, int nBitsToReveal) {
        String base64Salt = Base64.encodeToString(salt, Base64.DEFAULT);
        byte[] hashOfSaltedPhoneNumber = Hash.hashString(phoneNumber + base64Salt);

        return HelperMethods.getNLowBitsOfByteArray(hashOfSaltedPhoneNumber, nBitsToReveal);
    }

}
