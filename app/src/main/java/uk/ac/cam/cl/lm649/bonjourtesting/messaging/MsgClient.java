package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
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
import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbTableBadges;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbTableHistoryTransfer;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgHistoryTransfer;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.UnknownMessageTypeException;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.JmdnsUtil;

public class MsgClient {

    public static final String TAG = "MsgClient";
    private Socket socket = null;

    public final CustomApplication app;
    public final Context context;
    private final SaveBadgeData saveBadgeData;

    private final ExecutorService workerThreadIncoming = Executors.newFixedThreadPool(1);
    private final ExecutorService workerThreadOutgoing = Executors.newFixedThreadPool(1);

    private DataInputStream inStream;
    private DataOutputStream outStream;
    private CountDownLatch outStreamReadyLatch = new CountDownLatch(1);

    private boolean closed = false;

    public String socketAddress;
    public String sFromAddress;
    public String sToAddress;

    private UUID badgeIdOfOtherEnd = null;
    protected JPAKEClient jpakeClient;

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
        final InetAddress address = JmdnsUtil.getAddress(serviceInfo);
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
            socketAddress = socket.getInetAddress().getHostAddress();
            sFromAddress = "from addr: " + socketAddress + ", ";
            sToAddress = "to addr: " + socketAddress + ", ";
            outStream = new DataOutputStream(
                    new BufferedOutputStream(socket.getOutputStream()));
            outStreamReadyLatch.countDown();
            inStream = new DataInputStream(
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
        close();
    }

    private void receiveMsg() throws IOException {
        Message msg;
        try {
            msg = Message.createFromStream(inStream);
        } catch (UnknownMessageTypeException e) {
            FLogger.e(TAG, sFromAddress + "received msg. UnknownMessageTypeException: " + e.getMessage());
            return;
        }
        if (null == msg) {
            FLogger.e(TAG, sFromAddress + "received msg. But parsing it returned null.");
            return;
        }
        msg.onReceive(this);
    }

    public void sendMessage(final Message msg){
        final Runnable runnable = new Runnable() {
            @Override
            public void run(){
                try {
                    outStreamReadyLatch.await();
                } catch (InterruptedException e) {
                    FLogger.e(TAG, "sendMessage(). latch await interrupted - " + e.getMessage());
                    return;
                }
                try {
                    msg.send(MsgClient.this);
                    FLogger.i(TAG, sToAddress + "sent msg with type " + msg.getType()
                            + "/" + msg.getClass().getSimpleName());
                } catch (IOException e) {
                    FLogger.e(TAG, "sendMessage(). IOE - " + e.getMessage());
                }
            }
        };
        try {
            workerThreadOutgoing.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.e(TAG, "sendMessage(). runnable was rejected by executor - " + e.getMessage());
        }
    }

    public void considerDoingAHistoryTransfer() {
        FLogger.d(TAG, "considering doing a historyTransfer to " + badgeIdOfOtherEnd);
        if (null == badgeIdOfOtherEnd) {
            FLogger.e(TAG, "considerDoingAHistoryTransfer(). badgeIdOfOtherEnd is null.");
            return;
        }
        Long lastTimeWeSentHistoryToThatBadge = DbTableHistoryTransfer.getTimestamp(badgeIdOfOtherEnd);
        long curTime = System.currentTimeMillis();
        if (null == lastTimeWeSentHistoryToThatBadge
                || curTime - lastTimeWeSentHistoryToThatBadge > Constants.HISTORY_TRANSFER_TO_SAME_CLIENT_COOLDOWN) {
            FLogger.d(TAG, "decided to do historyTransfer to " + badgeIdOfOtherEnd.toString());
            doHistoryTransfer();
        } else {
            long timeElapsed = curTime - lastTimeWeSentHistoryToThatBadge;
            FLogger.d(TAG, "won't do historyTransfer to " + badgeIdOfOtherEnd
                    + ", last transfer was " + timeElapsed/1000 + " seconds ago ");
        }
    }

    private void doHistoryTransfer() {
        long curTime = System.currentTimeMillis();
        Long timeStampLastHistoryTransfer = DbTableHistoryTransfer.getTimestamp(badgeIdOfOtherEnd);
        List<BadgeStatus> badgeStatuses = DbTableBadges.getBadgesUpdatedSince(timeStampLastHistoryTransfer);
        for (BadgeStatus badgeStatus : badgeStatuses) {
            FLogger.d(TAG, "historyTransfer to " + badgeIdOfOtherEnd + " contains badge:\n"
                    + badgeStatus.toString());
        }
        Message msgHistoryTransfer = new MsgHistoryTransfer(badgeStatuses);
        sendMessage(msgHistoryTransfer);
        DbTableHistoryTransfer.smartUpdateEntry(badgeIdOfOtherEnd, curTime);
    }

    public void close() {
        FLogger.d(TAG, "close() called. address: " + socketAddress);
        if (closed) {
            FLogger.d(TAG, "close(). already closed.");
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

    public boolean isClosed() {
        return closed;
    }

    public DataOutputStream getOutStream() {
        return outStream;
    }

    public UUID getBadgeIdOfOtherEnd() {
        return badgeIdOfOtherEnd;
    }

    /**
     * Sets this.badgeIdOfOtherEnd to badgeId on first call. Tests for equality and
     * "produces" error on mismatch for subsequent calls.
     */
    public void reconfirmBadgeId(UUID badgeId) {
        if (null == badgeIdOfOtherEnd) {
            if (null != badgeId) {
                badgeIdOfOtherEnd = badgeId;
                FLogger.d(TAG, "reconfirmBadgeId(). badgeIdOfOtherEnd set to " + badgeIdOfOtherEnd);
            } else {
                FLogger.e(TAG, "reconfirmBadgeId(). badgeId == badgeIdOfOtherEnd == null.");
            }
            return;
        }
        if (!badgeIdOfOtherEnd.equals(badgeId)) {
            FLogger.e(TAG, String.format(Locale.US,
                    "reconfirmBadgeId(). wtf. badgeId mismatch! old: %s, new: %s",
                    badgeIdOfOtherEnd, badgeId));
            // TODO maybe throw an exception or call close() ?
        }
    }

    public JPAKEClient getJpakeClient() {
        return jpakeClient;
    }
}
