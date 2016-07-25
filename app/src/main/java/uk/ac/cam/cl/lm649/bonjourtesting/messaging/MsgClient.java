package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.ActiveBadgeActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.Badge;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeDbHelper;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class MsgClient {

    private static final String TAG = "MsgClient";
    private Socket socket = null;

    private CustomApplication app;
    private Context context;
    private SaveSettingsData saveSettingsData;

    private final ExecutorService workerThreadIncoming = Executors.newFixedThreadPool(1);
    private final ExecutorService workerThreadOutgoing = Executors.newFixedThreadPool(1);

    private ObjectInputStream inStream;
    private ObjectOutputStream outStream;
    private CountDownLatch outStreamReadyLatch = new CountDownLatch(1);

    private boolean closed = false;

    private String sFromAddress;
    private String sToAddress;

    private MsgClient() {
        app = CustomApplication.getInstance();
        context = app;
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
            FLogger.e(TAG, "MsgClient() 1. runnable was rejected by executor - " + e.getMessage());
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
                    FLogger.e(TAG, "Failed to open socket. closing MsgClient. IOE - " + e.getMessage());
                    MsgClient.this.close();
                }
            }
        };
        try {
            workerThreadIncoming.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.e(TAG, "MsgClient() 2. runnable was rejected by executor - " + e.getMessage());
        }

    }

    private void init() {
        try {
            String address = socket.getInetAddress().getHostAddress();
            sFromAddress = "from addr: " + address + ", ";
            sToAddress = "to addr: " + address + ", ";
            outStream = new ObjectOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));
            outStreamReadyLatch.countDown();
            inStream = new ObjectInputStream(
                    new BufferedInputStream(socket.getInputStream()));
            startWaitingForMessages();
        } catch (IOException e) {
            FLogger.e(TAG, "init() failed. IOE - " + e.getMessage());
        }
    }

    private void startWaitingForMessages(){
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    FLogger.i(TAG, "startWaitingForMessages() interrupted. finishing.");
                    break;
                }
                receiveMsg();
            }
        } catch (EOFException e) {
            FLogger.i(TAG, "startWaitingForMessages(). EOF - " + e.getMessage());
        } catch (SocketException e) {
            FLogger.w(TAG, "startWaitingForMessages(). SockExc - " + e.getMessage());
        } catch (IOException e) {
            FLogger.e(TAG, "startWaitingForMessages(). IOE - " + e.getMessage());
        }
    }

    private class MessageType {
        public static final int ARBITRARY_TEXT = 0;
        public static final int WHO_ARE_YOU_QUESTION = 1;
        public static final int THIS_IS_MY_IDENTITY = 2;
    }

    private void receiveMsg() throws IOException {
        int msgType = inStream.readInt();
        switch (msgType) {
            case MessageType.ARBITRARY_TEXT:
                String text = inStream.readUTF();
                FLogger.i(TAG, sFromAddress + "received msg with type ARBITRARY_TEXT: " + text);
                HelperMethods.displayMsgToUser(app, text);
                break;
            case MessageType.WHO_ARE_YOU_QUESTION:
                FLogger.i(TAG, sFromAddress + "received msg with type WHO_ARE_YOU_QUESTION");
                sendMessageThisIsMyIdentity();
                break;
            case MessageType.THIS_IS_MY_IDENTITY:
                String strBadgeId = inStream.readUTF();
                String customName = inStream.readUTF();
                String macAddress = inStream.readUTF();
                FLogger.i(TAG, sFromAddress + "received msg with type THIS_IS_MY_IDENTITY, badgeID: "
                        + strBadgeId + ", nick: " + customName + ", MAC: " + macAddress);
                UUID badgeId = UUID.fromString(strBadgeId);
                Badge badge = new Badge(badgeId);
                badge.setCustomName(customName);
                badge.setRouterMac(macAddress);
                badge.setTimestamp(System.currentTimeMillis());
                BadgeDbHelper.getInstance(context).smartUpdateBadge(badge);
                if (app.getTopActivity() instanceof ActiveBadgeActivity) ((ActiveBadgeActivity)app.getTopActivity()).updateListView();
                break;
            default: // unknown
                FLogger.e(TAG, sFromAddress + "received msg with unknown msgType: " + msgType);
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
                    FLogger.e(TAG, "sendMessageArbitraryText(). latch await interrupted - " + e.getMessage());
                    return;
                }
                try {
                    outStream.writeInt(MessageType.ARBITRARY_TEXT);
                    outStream.writeUTF(String.format(Locale.US, "%s: %s", senderID, msg));
                    outStream.flush();
                    FLogger.i(TAG, sToAddress + "sent msg with type ARBITRARY_TEXT");
                    HelperMethods.displayMsgToUser(context, "msg sent");
                } catch (IOException e) {
                    FLogger.e(TAG, "sendMessageArbitraryText(). IOE - " + e.getMessage());
                    HelperMethods.displayMsgToUser(context, "error sending msg: IOException");
                }
            }
        };
        try {
            workerThreadOutgoing.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.e(TAG, "sendMessageArbitraryText(). runnable was rejected by executor - " + e.getMessage());
        }
    }

    public void sendMessageWhoAreYouQuestion(){
        final Runnable runnable = new Runnable() {
            @Override
            public void run(){
                try {
                    outStreamReadyLatch.await();
                } catch (InterruptedException e) {
                    FLogger.e(TAG, "sendMessageWhoAreYouQuestion(). latch await interrupted - " + e.getMessage());
                    return;
                }
                try {
                    outStream.writeInt(MessageType.WHO_ARE_YOU_QUESTION);
                    outStream.flush();
                    FLogger.i(TAG, sToAddress + "sent msg with type WHO_ARE_YOU_QUESTION");
                } catch (IOException e) {
                    FLogger.e(TAG, "sendMessageWhoAreYouQuestion(). IOE - " + e.getMessage());
                }
            }
        };
        try {
            workerThreadOutgoing.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.e(TAG, "sendMessageWhoAreYouQuestion(). runnable was rejected by executor - " + e.getMessage());
        }
    }

    public void sendMessageThisIsMyIdentity(){
        final Runnable runnable = new Runnable() {
            @Override
            public void run(){
                try {
                    outStreamReadyLatch.await();
                } catch (InterruptedException e) {
                    FLogger.e(TAG, "sendMessageThisIsMyIdentity(). latch await interrupted - " + e.getMessage());
                    return;
                }
                try {
                    SaveBadgeData saveBadgeData = SaveBadgeData.getInstance(context);
                    outStream.writeInt(MessageType.THIS_IS_MY_IDENTITY);
                    outStream.writeUTF(saveBadgeData.getMyBadgeId().toString());
                    outStream.writeUTF(saveBadgeData.getMyBadgeCustomName());
                    outStream.writeUTF(NetworkUtil.getRouterMacAddress(context));
                    outStream.flush();
                    FLogger.i(TAG, sToAddress + "sent msg with type THIS_IS_MY_IDENTITY");
                } catch (IOException e) {
                    FLogger.e(TAG, "sendMessageThisIsMyIdentity(). IOE - " + e.getMessage());
                }
            }
        };
        try {
            workerThreadOutgoing.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.e(TAG, "sendMessageThisIsMyIdentity(). runnable was rejected by executor - " + e.getMessage());
        }
    }

    private static InetAddress getAddress(ServiceInfo serviceInfoOfDst) {
        if (null == serviceInfoOfDst){
            FLogger.e(TAG, "getAddress(). serviceInfo is null");
            return null;
        }
        InetAddress[] arrAddresses = serviceInfoOfDst.getInet4Addresses();
        if (null == arrAddresses || arrAddresses.length < 1){
            FLogger.e(TAG, "getAddress(). inappropriate addresses");
            return null;
        }
        return arrAddresses[0];
    }

    public void close() {
        if (closed) {
            FLogger.e(TAG, "close() called. but it is already closed!");
            return;
        }
        closed = true;
        workerThreadOutgoing.shutdown();
        workerThreadIncoming.shutdownNow();
        try {
            if (null != outStream) outStream.close();
            if (null != inStream) inStream.close();
            if (null != socket) socket.close();
        } catch (IOException e) {
            FLogger.e(TAG, "close(). trying to close streams and socket, IOE - " + e.getMessage());
        }
    }

}
