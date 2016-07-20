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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.Badge;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeDbHelper;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class MsgClient {

    private static final String TAG = "MsgClient";
    private Socket socket = null;

    private Context context;
    private SaveSettingsData saveSettingsData;

    private final ExecutorService workerThreadIncoming = Executors.newFixedThreadPool(1);
    private final ExecutorService workerThreadOutgoing = Executors.newFixedThreadPool(1);

    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private CountDownLatch outStreamReadyLatch = new CountDownLatch(1);

    private boolean closed = false;

    private MsgClient() {
        context = CustomApplication.getInstance();
        saveSettingsData = SaveSettingsData.getInstance(context);
    }

    protected MsgClient(Socket socket) {
        this();
        this.socket = socket;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                init();
            }
        };
        try {
            workerThreadIncoming.execute(runnable);
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "MsgClient() 1. runnable was rejected by executor");
        }
    }

    public MsgClient(final ServiceInfo serviceInfo) {
        this();
        final InetAddress address = getAddress(serviceInfo);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    MsgClient.this.socket = new Socket(address, serviceInfo.getPort());
                    init();
                } catch (IOException e) { // TODO think about this
                    Log.e(TAG, "Failed to open socket. closing MsgClient. IOE - " + e.getMessage());
                    e.printStackTrace();
                    MsgClient.this.close();
                }
            }
        };
        try {
            workerThreadIncoming.execute(runnable);
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "MsgClient() 2. runnable was rejected by executor");
        }

    }

    private void init() {
        try {
            outStream = new ObjectOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));
            outStreamReadyLatch.countDown();
            inStream = new ObjectInputStream(
                    new BufferedInputStream(socket.getInputStream()));
            startWaitingForMessages();
        } catch (IOException e) {
            Log.e(TAG, "init() failed. IOE - " + e.getMessage());
        }
    }

    private void startWaitingForMessages(){
        try {
            while (true) {
                receiveMsg();
            }
        } catch (EOFException e) {
            Log.e(TAG, "startWaitingForMessages(). EOF - " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "startWaitingForMessages(). IOE - " + e.getMessage());
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
                SaveBadgeData saveBadgeData = SaveBadgeData.getInstance(context);
                outStream.writeInt(MessageType.WHO_ARE_YOU_REPLY);
                outStream.writeUTF(saveBadgeData.getMyBadgeId().toString());
                outStream.writeUTF(saveBadgeData.getMyBadgeCustomName());
                outStream.writeUTF(NetworkUtil.getRouterMacAddress(context));
                outStream.flush();
                Log.i(TAG, "sent msg with type WHO_ARE_YOU_REPLY");
                break;
            case MessageType.WHO_ARE_YOU_REPLY:
                String strBadgeId = inStream.readUTF();
                String customName = inStream.readUTF();
                String macAddress = inStream.readUTF();
                Log.i(TAG, "received msg with type WHO_ARE_YOU_REPLY, badgeID: " + strBadgeId
                        + ", nick: " + customName + ", MAC: " + macAddress);
                UUID badgeId = UUID.fromString(strBadgeId);
                Badge badge = new Badge(badgeId);
                badge.setCustomName(customName);
                badge.setRouterMac(macAddress);
                badge.setTimestamp(System.currentTimeMillis());
                BadgeDbHelper.getInstance(context).smartUpdateBadge(badge);
                break;
            default: // unknown
                Log.e(TAG, "received msg with unknown msgType: " + msgType);
                break;
        }
    }

    public void sendMessageArbitraryText(final String senderID, final String msg){
        final Runnable runnable = new Runnable() {
            @Override
            public void run(){
                try {
                    outStreamReadyLatch.await();
                } catch (InterruptedException e) {
                    Log.e(TAG, "sendMessageArbitraryText(). latch await interrupted - " + e.getMessage());
                    return;
                }
                try {
                    outStream.writeInt(MessageType.ARBITRARY_TEXT);
                    outStream.writeUTF(String.format(Locale.US, "%s: %s", senderID, msg));
                    outStream.flush();
                    Log.i(TAG, "sent msg with type ARBITRARY_TEXT");
                    HelperMethods.displayMsgToUser(context, "msg sent");
                } catch (IOException e) {
                    Log.e(TAG, "sendMessageArbitraryText(). IOE - " + e.getMessage());
                    HelperMethods.displayMsgToUser(context, "error sending msg: IOException");
                }
            }
        };
        try {
            workerThreadOutgoing.execute(runnable);
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "sendMessageArbitraryText(). runnable was rejected by executor");
        }
    }

    public void sendMessageWhoAreYouQuestion(){
        final Runnable runnable = new Runnable() {
            @Override
            public void run(){
                try {
                    outStreamReadyLatch.await();
                } catch (InterruptedException e) {
                    Log.e(TAG, "sendMessageWhoAreYouQuestion(). latch await interrupted - " + e.getMessage());
                    return;
                }
                try {
                    outStream.writeInt(MessageType.WHO_ARE_YOU_QUESTION);
                    outStream.flush();
                    Log.i(TAG, "sent msg with type WHO_ARE_YOU_QUESTION");
                } catch (IOException e) {
                    Log.e(TAG, "sendMessageWhoAreYouQuestion(). IOE - " + e.getMessage());
                }
            }
        };
        try {
            workerThreadOutgoing.execute(runnable);
        } catch (RejectedExecutionException e) {
            Log.e(TAG, "sendMessageWhoAreYouQuestion(). runnable was rejected by executor");
        }
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

    public void close() {
        if (closed) {
            Log.e(TAG, "close() called. but it is already closed!");
            return;
        }
        closed = true;
        workerThreadOutgoing.shutdown();
        workerThreadIncoming.shutdown();
        try {
            outStream.close();
            inStream.close();
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "close(). trying to close streams and socket, IOE - " + e.getMessage());
        }
    }

}
