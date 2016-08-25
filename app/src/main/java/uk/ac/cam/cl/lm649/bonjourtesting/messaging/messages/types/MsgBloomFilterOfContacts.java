package uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types;

import android.support.annotation.NonNull;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.phonenumbers.Contact;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.phonenumbers.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.MessageRequiringEncryption;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.UsedViaReflection;

public class MsgBloomFilterOfContacts extends Message implements MessageRequiringEncryption {

    private static final String TAG = "MsgBloomFilterContacts";

    public static final long EXPECTED_NUMBER_OF_CONTACTS_IN_PHONE_BOOK = 400;
    public static final double DESIRED_FALSE_POSITIVE_PROBABILITY = 0.05;

    public final BloomFilter<CharSequence> bloomFilterOfContacts;

    public MsgBloomFilterOfContacts(@NonNull BloomFilter<CharSequence> bloomFilterOfContacts) {
        super();
        this.bloomFilterOfContacts = bloomFilterOfContacts;
    }

    public static MsgBloomFilterOfContacts createNewMsgWithMyCurrentData() {
        BloomFilter<CharSequence> bloomFilterOfContacts = BloomFilter.create(
                Funnels.stringFunnel(Charsets.UTF_8),
                EXPECTED_NUMBER_OF_CONTACTS_IN_PHONE_BOOK,
                DESIRED_FALSE_POSITIVE_PROBABILITY);
        for (Contact entry : DbTablePhoneNumbers.getAllEntries()) {
            if (entry.getGossipingStatus().isEnabled()) {
                FLogger.d(TAG, "putting contact in bloom filter: " + entry.getPhoneNumber());
                bloomFilterOfContacts.put(entry.getPhoneNumber());
            }
        }
        return new MsgBloomFilterOfContacts(bloomFilterOfContacts);
    }

    @UsedViaReflection
    public static MsgBloomFilterOfContacts createFromStream(DataInputStream inStream) throws IOException {
        BloomFilter<CharSequence> bloomFilterOfContacts = BloomFilter.readFrom(inStream, Funnels.stringFunnel(Charsets.UTF_8));
        return new MsgBloomFilterOfContacts(bloomFilterOfContacts);
    }

    @Override
    protected void serialiseBodyToStream(DataOutputStream outStream) throws IOException {
        bloomFilterOfContacts.writeTo(outStream);
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, String.format(Locale.US,
                "%sreceived %s",
                msgClient.strFromAddress, getClass().getSimpleName()));

        List<String> possiblyCommonContacts = new ArrayList<>();
        for (Contact entry : DbTablePhoneNumbers.getAllEntries()) {
            String phoneNum = entry.getPhoneNumber();
            if (entry.getGossipingStatus().isEnabled()
                    && bloomFilterOfContacts.mightContain(phoneNum)) {
                FLogger.d(TAG, "possibly common contact: " + phoneNum);
                possiblyCommonContacts.add(phoneNum);
            }
        }

        Message msg = new MsgCommonContactsPhoneNumbers(possiblyCommonContacts);
        msgClient.sendMessage(msg);
    }

}
