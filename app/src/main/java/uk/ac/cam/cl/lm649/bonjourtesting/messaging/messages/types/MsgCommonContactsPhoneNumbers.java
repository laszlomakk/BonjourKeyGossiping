package uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types;

import android.support.annotation.NonNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.phonenumbers.Contact;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.phonenumbers.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.publickeys.DbTablePublicKeys;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.publickeys.PublicKeyEntry;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgCommonContactsPhoneNumbers extends Message implements Message.RequiresEncryption {

    private static final String TAG = "MsgCommonContactPhNums";

    private final List<String> phoneNumberList;

    public MsgCommonContactsPhoneNumbers(@NonNull List<String> phoneNumberList) {
        super();
        this.phoneNumberList = phoneNumberList;
    }

    @UsedViaReflection
    public static MsgCommonContactsPhoneNumbers createFromStream(DataInputStream inStream) throws IOException {
        int size = inStream.readInt();
        List<String> phoneNumberList = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            phoneNumberList.add(inStream.readUTF());
        }
        return new MsgCommonContactsPhoneNumbers(phoneNumberList);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(phoneNumberList.size());
        for (String phoneNum : phoneNumberList) {
            outStream.writeUTF(phoneNum);
        }
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, String.format(Locale.US,
                "%sreceived %s",
                msgClient.strFromAddress, getClass().getSimpleName()));

        List<MsgPublicKeyFlashes.PublicKeyFlash> publicKeyFlashList =
                getListOfPublicKeysFlashesForPhoneNumbers(phoneNumberList);

        Message msg = new MsgPublicKeyFlashes(publicKeyFlashList);
        msgClient.sendMessage(msg);
    }

    private static List<MsgPublicKeyFlashes.PublicKeyFlash> getListOfPublicKeysFlashesForPhoneNumbers(
            List<String> phoneNumberList)
    {
        List<MsgPublicKeyFlashes.PublicKeyFlash> publicKeyFlashList = new ArrayList<>();
        for (String phoneNum : phoneNumberList) {
            FLogger.d(TAG, "other party claimed to know phone number: " + phoneNum);
            Contact contact = DbTablePhoneNumbers.getEntry(phoneNum);
            if (null == contact) {
                FLogger.d(TAG, "phone number: " + phoneNum + " is unknown to us.");
                continue;
            }
            if (!contact.getGossipingStatus().isEnabled()) {
                FLogger.d(TAG, "contact with phone number: " + phoneNum + " has gossiping status disabled.");
                continue;
            }
            List<PublicKeyEntry> publicKeyEntries = DbTablePublicKeys.getEntriesForPhoneNumber(phoneNum);
            if (publicKeyEntries.size() == 0) continue;
            FLogger.d(TAG, "found " + publicKeyEntries.size() + " public key entries for phone number: " + phoneNum);
            for (PublicKeyEntry publicKeyEntry : publicKeyEntries) {
                MsgPublicKeyFlashes.PublicKeyFlash publicKeyFlash = new MsgPublicKeyFlashes.PublicKeyFlash(publicKeyEntry);
                publicKeyFlashList.add(publicKeyFlash);
            }
        }
        return publicKeyFlashList;
    }

}
