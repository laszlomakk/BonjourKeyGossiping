package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import android.util.Log;

import java.util.HashMap;

public class MessageTypes {

    protected static final HashMap<Integer, Class<? extends Message>> msgNumToMsgClassMap = new HashMap<>();
    protected static final HashMap<Class<? extends Message>, Integer> msgClassToMsgNumMap = new HashMap<>();

    static {
        Type msgType = Type.ARBITRARY_TEXT; // this is a *hack* to have the constructors of the enum run
    }

    public enum Type {
        ARBITRARY_TEXT      (0, MsgArbitraryText.class),
        WHO_ARE_YOU_QUESTION(1, MsgWhoAreYouQuestion.class),
        THIS_IS_MY_IDENTITY (2, MsgThisIsMyIdentity.class),
        HISTORY_TRANSFER    (3, MsgHistoryTransfer.class),
        JPAKE_ROUND1        (4, MsgJPAKERound1.class),
        JPAKE_ROUND2        (5, MsgJPAKERound2.class),
        JPAKE_ROUND3        (6, MsgJPAKERound3.class);

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
