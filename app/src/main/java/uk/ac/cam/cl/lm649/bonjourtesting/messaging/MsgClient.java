package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.jmdns.ServiceInfo;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.ActiveBadgeActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeCore;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbTableBadges;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbTableHistoryTransfer;
import uk.ac.cam.cl.lm649.bonjourtesting.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class MsgClient {

    private static final String TAG = "MsgClient";
    private Socket socket = null;

    private CustomApplication app;
    private Context context;
    private SaveBadgeData saveBadgeData;

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
        saveBadgeData = SaveBadgeData.getInstance(context);
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
        if (null == address) {
            FLogger.e(TAG, "MsgClient(). address is null. closing MsgClient");
            MsgClient.this.close();
            return;
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    MsgClient.this.socket = new Socket(address, serviceInfo.getPort());
                    init();
                } catch (IOException e) { // TODO think about this
                    FLogger.e(TAG, "MsgClient(). Failed to open socket. closing MsgClient. IOE - " + e.getMessage());
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
        public static final int HISTORY_TRANSFER = 3;
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
                BadgeStatus badgeStatus = BadgeStatus.createFromStream(inStream);
                FLogger.i(TAG, sFromAddress + "received msg with type THIS_IS_MY_IDENTITY, " + badgeStatus.toString());
                DbTableBadges.smartUpdateBadge(badgeStatus);
                CustomActivity.forceRefreshUIInTopActivity();
                if (Constants.HISTORY_TRANSFER_ENABLED) {
                    considerDoingAHistoryTransfer(badgeStatus.getBadgeCore().getBadgeId());
                } else {
                    FLogger.d(TAG, "would consider doing a historyTransfer now, but it is disabled");
                }
                break;
            case MessageType.HISTORY_TRANSFER:
                int numBadges = inStream.readInt();
                FLogger.i(TAG, sFromAddress + "received msg with type HISTORY_TRANSFER, containing "
                        + numBadges + " badges");
                if (!Constants.HISTORY_TRANSFER_ENABLED) {
                    FLogger.i(TAG, "rejecting message. historyTransfer is disabled.");
                    // we still need to process (swallow) the message though...
                    for (int badgeIndex = 0; badgeIndex < numBadges; badgeIndex++) {
                        /* BadgeStatus throwAway = */ BadgeStatus.createFromStream(inStream);
                    }
                    break;
                }
                for (int badgeIndex = 0; badgeIndex < numBadges; badgeIndex++) {
                    BadgeStatus badgeStatus2 = BadgeStatus.createFromStream(inStream);
                    if (!saveBadgeData.getMyBadgeId().equals(badgeStatus2.getBadgeCore().getBadgeId())) {
                        DbTableBadges.smartUpdateBadge(badgeStatus2);
                        FLogger.d(TAG, sFromAddress + "historyTransfer contains badge:\n"
                                + badgeStatus2.toString());
                    } else {
                        // FLogger.e(TAG, "received history transfer included our own badge! (came from "
                        //        + sFromAddress + ")");
                    }
                }
                CustomActivity.forceRefreshUIInTopActivity();
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
                    BadgeCore myBadgeCore = new BadgeCore(saveBadgeData.getMyBadgeId());
                    myBadgeCore.setCustomName(saveBadgeData.getMyBadgeCustomName());
                    BadgeStatus badgeStatus = new BadgeStatus(myBadgeCore);
                    badgeStatus.setRouterMac(NetworkUtil.getRouterMacAddress(context));
                    badgeStatus.setTimestampLastSeenAlive(System.currentTimeMillis());

                    outStream.writeInt(MessageType.THIS_IS_MY_IDENTITY);
                    badgeStatus.serialiseToStream(outStream);
                    outStream.flush();
                    FLogger.i(TAG, sToAddress + "sent msg with type THIS_IS_MY_IDENTITY:\n" + myBadgeCore.toString());
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

    public void considerDoingAHistoryTransfer(final UUID badgeIdOfReceiver) {
        FLogger.d(TAG, "considering doing a historyTransfer to " + badgeIdOfReceiver.toString());
        Long lastTimeWeSentHistoryToThatBadge = DbTableHistoryTransfer.getTimestamp(badgeIdOfReceiver);
        long curTime = System.currentTimeMillis();
        if (null == lastTimeWeSentHistoryToThatBadge
                || curTime - lastTimeWeSentHistoryToThatBadge > Constants.HISTORY_TRANSFER_TO_SAME_CLIENT_COOLDOWN) {
            FLogger.d(TAG, "decided to do historyTransfer to " + badgeIdOfReceiver.toString());
            sendMessageHistoryTransfer(badgeIdOfReceiver);
        } else {
            long timeElapsed = curTime - lastTimeWeSentHistoryToThatBadge;
            FLogger.d(TAG, "won't do historyTransfer to " + badgeIdOfReceiver.toString()
                    + ", last transfer was " + timeElapsed/1000 + " seconds ago ");
        }
    }

    public void sendMessageHistoryTransfer(final UUID badgeIdOfReceiver){
        if (!Constants.HISTORY_TRANSFER_ENABLED) {
            FLogger.e(TAG, "sendMessageHistoryTransfer() called, but historyTransfer is disabled.");
            return;
        }
        final Runnable runnable = new Runnable() {
            @Override
            public void run(){
                try {
                    outStreamReadyLatch.await();
                } catch (InterruptedException e) {
                    FLogger.e(TAG, "sendMessageHistoryTransfer(). latch await interrupted - " + e.getMessage());
                    return;
                }
                try {
                    outStream.writeInt(MessageType.HISTORY_TRANSFER);
                    long curTime = System.currentTimeMillis();
                    Long timeStampLastHistoryTransfer = DbTableHistoryTransfer.getTimestamp(badgeIdOfReceiver);
                    List<BadgeStatus> badgeStatuses = DbTableBadges.getBadgesUpdatedSince(timeStampLastHistoryTransfer);
                    outStream.writeInt(badgeStatuses.size());
                    for (BadgeStatus badgeStatus : badgeStatuses) {
                        FLogger.d(TAG, "historyTransfer to " + badgeIdOfReceiver + " contains badge:\n"
                                + badgeStatus.toString());
                        badgeStatus.serialiseToStream(outStream);
                    }
                    outStream.flush();
                    DbTableHistoryTransfer.smartUpdateEntry(badgeIdOfReceiver, curTime);
                    FLogger.i(TAG, sToAddress + "sent msg with type HISTORY_TRANSFER, containing "
                            + badgeStatuses.size() + " badges");
                } catch (IOException e) {
                    FLogger.e(TAG, "sendMessageHistoryTransfer(). IOE - " + e.getMessage());
                }
            }
        };
        try {
            workerThreadOutgoing.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.e(TAG, "sendMessageHistoryTransfer(). runnable was rejected by executor - " + e.getMessage());
        }
    }

    @Nullable
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
