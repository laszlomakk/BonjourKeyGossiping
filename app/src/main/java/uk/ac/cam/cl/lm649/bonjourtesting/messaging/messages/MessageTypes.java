package uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages;

import java.util.HashMap;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgArbitraryText;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgBloomFilterOfContacts;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgCommonContactsPhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgJPAKERound1;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgJPAKERound2;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgJPAKERound3;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgJPAKERound3Ack;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgMyPhoneNumber;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgMyPublicKey;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgPublicKeyFlashes;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.messages.types.MsgSaltedPhoneNumber;

public class MessageTypes {

    protected static final HashMap<Integer, Class<? extends Message>> msgNumToMsgClassMap = new HashMap<>();
    protected static final HashMap<Class<? extends Message>, Integer> msgClassToMsgNumMap = new HashMap<>();

    static {
        Type msgType = Type.ARBITRARY_TEXT; // this is a *hack* to have the constructors of the enum run
    }

    public enum Type {
        ARBITRARY_TEXT                (0,  MsgArbitraryText.class),
        JPAKE_ROUND1                  (4,  MsgJPAKERound1.class),
        JPAKE_ROUND2                  (5,  MsgJPAKERound2.class),
        JPAKE_ROUND3                  (6,  MsgJPAKERound3.class),
        JPAKE_ROUND3_ACK              (10, MsgJPAKERound3Ack.class),
        MY_PHONE_NUMBER               (7,  MsgMyPhoneNumber.class),
        MY_PUBLIC_KEY                 (8,  MsgMyPublicKey.class),
        SALTED_PHONE_NUMBER           (9,  MsgSaltedPhoneNumber.class),
        BLOOM_FILTER_OF_CONTACTS      (11, MsgBloomFilterOfContacts.class),
        COMMON_CONTACTS_PHONE_NUMBERS (12, MsgCommonContactsPhoneNumbers.class),
        PUBLIC_KEY_FLASHES            (13, MsgPublicKeyFlashes.class),
        ;

        private int msgNum;
        private Class<? extends Message> msgClass;

        Type(int msgNum, Class<? extends Message> msgClass) {
            this.msgNum = msgNum;
            this.msgClass = msgClass;
            msgNumToMsgClassMap.put(msgNum, msgClass);
            msgClassToMsgNumMap.put(msgClass, msgNum);
        }

        public int getMsgNum() {
            return msgNum;
        }

        public Class<? extends Message> getMsgClass() {
            return msgClass;
        }
    }

}
