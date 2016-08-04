package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public abstract class Message {

    protected final int type;
    private static final String TAG = "Message";

    private static final HashMap<Integer, Class<? extends Message>> typeNumToMessageTypeMap = new HashMap<>();
    static {
        typeNumToMessageTypeMap.put(MsgArbitraryText.TYPE_NUM,     MsgArbitraryText.class);
        typeNumToMessageTypeMap.put(MsgWhoAreYouQuestion.TYPE_NUM, MsgWhoAreYouQuestion.class);
        typeNumToMessageTypeMap.put(MsgThisIsMyIdentity.TYPE_NUM,  MsgThisIsMyIdentity.class);
        typeNumToMessageTypeMap.put(MsgHistoryTransfer.TYPE_NUM,   MsgHistoryTransfer.class);
    }

    protected Message(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static Message createFromStream(DataInputStream inStream) throws IOException, UnknownMessageTypeException {
        int typeNum = inStream.readInt();
        Class<? extends Message> msgClass = typeNumToMessageTypeMap.get(typeNum);
        if (null == msgClass) {
            // TODO we would need to swallow the contents of this unknown msg so the stream can progress...
            throw new UnknownMessageTypeException("typeNum: " + typeNum);
        }
        try {
            Method method = msgClass.getMethod("createFromStream", ObjectInputStream.class);
            Object obj = method.invoke(null, inStream);
            return (Message) obj;
        } catch (NoSuchMethodException e) {
            FLogger.e(TAG, "createFromStream(). caught NoSuchMethodException : " + e.getMessage());
            FLogger.d(TAG, HelperMethods.formatStackTraceAsString(e));
        } catch (InvocationTargetException e) {
            FLogger.e(TAG, "createFromStream(). caught InvocationTargetException : " + e.getMessage());
            FLogger.d(TAG, HelperMethods.formatStackTraceAsString(e));
        } catch (IllegalAccessException e) {
            FLogger.e(TAG, "createFromStream(). caught IllegalAccessException : " + e.getMessage());
            FLogger.d(TAG, HelperMethods.formatStackTraceAsString(e));
        }
        return null;
    }

    public abstract void serialiseToStream(DataOutputStream outStream) throws IOException;
    
}
