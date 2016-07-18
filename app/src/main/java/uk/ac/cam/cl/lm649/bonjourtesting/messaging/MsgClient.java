package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MsgClient {

    private static final String TAG = "ConnectedClient";
    private Socket socket = null;

    private Context context;
    private SaveSettingsData saveSettingsData;

    private final ExecutorService workerThreadIncoming = Executors.newFixedThreadPool(1);
    private final ExecutorService workerThreadOutgoing = Executors.newFixedThreadPool(1);

    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private boolean outStreamReady = false;

    private MsgClient() {
        context = CustomApplication.getInstance();
        saveSettingsData = SaveSettingsData.getInstance(context);
    }

    protected MsgClient(Socket socket) {
        this();
        this.socket = socket;
        workerThreadIncoming.execute(new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }

    public MsgClient(final ServiceInfo serviceInfo) {
        this();
        final InetAddress address = getAddress(serviceInfo);
        workerThreadIncoming.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    MsgClient.this.socket = new Socket(address, serviceInfo.getPort());
                    init();
                } catch (IOException e) { // TODO this is not properly handled...
                    Log.e(TAG, "failed to open socket");
                    e.printStackTrace();
                }
            }
        });

    }

    private void init() {
        try {
            outStream = new ObjectOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));
            outStreamReady = true;
            inStream = new ObjectInputStream(
                    new BufferedInputStream(socket.getInputStream()));
            startWaitingForMessages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startWaitingForMessages(){
        try {
            while (true) {
                receiveMsg();
            }
        } catch (EOFException e) {
            Log.e(TAG, "startWaitingForMessages(). EOF");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "startWaitingForMessages(). IOE");
            e.printStackTrace();
        }
    }

    private class MessageType {
        public static final int ARBITRARY_TEXT = 0;
        public static final int WHO_ARE_YOU_QUESTION = 1;
        public static final int WHO_ARE_YOU_REPLY = 2;
    }

    private void receiveMsg() throws IOException {
        int msgType = inStream.readInt();
        switch (msgType) {
            case MessageType.ARBITRARY_TEXT:
                String text = inStream.readUTF();
                Log.i(TAG, "received msg with type ARBITRARY_TEXT: " + text);
                HelperMethods.displayMsgToUser(CustomApplication.getInstance(), text);
                break;
            case MessageType.WHO_ARE_YOU_QUESTION:
                Log.i(TAG, "received msg with type WHO_ARE_YOU_QUESTION");
                outStream.writeInt(MessageType.WHO_ARE_YOU_REPLY);
                outStream.writeUTF(saveSettingsData.getCustomServiceName()); // TODO
                outStream.flush();
                break;
            case MessageType.WHO_ARE_YOU_REPLY: // TODO
                String identity = inStream.readUTF();
                Log.i(TAG, "received msg with type WHO_ARE_YOU_REPLY, identity: " + identity);
                break;
            default: // unknown
                Log.e(TAG, "received msg with unknown msgType: " + msgType);
                break;
        }
    }

    public void sendTextMessage(final String senderID, final String msg){
        final Runnable runnable = new Runnable() {
            @Override
            public void run(){
                if (!outStreamReady) { // TODO this is an ugly hack, pls do this properly
                    //Log.d(TAG, "outStream not ready!");
                    workerThreadOutgoing.execute(this);
                    return;
                }
                try {
                    outStream.writeInt(MessageType.ARBITRARY_TEXT);
                    outStream.writeUTF(String.format(Locale.US, "%s: %s", senderID, msg));
                    outStream.flush();
                    HelperMethods.displayMsgToUser(context, "msg sent");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "sendTextMessage(). IOException");
                    HelperMethods.displayMsgToUser(context, "error sending msg: IOException");
                }
            }
        };
        workerThreadOutgoing.execute(runnable);
    }

    public void sendWhoAreYouMessage(){
        final Runnable runnable = new Runnable() {
            @Override
            public void run(){
                if (!outStreamReady) { // TODO this is an ugly hack, pls do this properly
                    //Log.d(TAG, "outStream not ready!");
                    workerThreadOutgoing.execute(this);
                    return;
                }
                try {
                    outStream.writeInt(MessageType.WHO_ARE_YOU_QUESTION);
                    outStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "sendWhoAreYouMessage(). IOException");
                }
            }
        };
        workerThreadOutgoing.execute(runnable);
    }

    private static InetAddress getAddress(ServiceInfo serviceInfoOfDst) {
        if (null == serviceInfoOfDst){
            Log.e(TAG, "getAddress(). serviceInfo is null");
            return null;
        }
        InetAddress[] arrAddresses = serviceInfoOfDst.getInet4Addresses();
        if (null == arrAddresses || arrAddresses.length < 1){
            Log.e(TAG, "getAddress(). inappropriate addresses");
            return null;
        }
        return arrAddresses[0];
    }

}
