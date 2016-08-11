package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Symmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTableBadges;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTableHistoryTransfer;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgHistoryTransfer;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.UnknownMessageTypeException;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class MsgClient {

    private static final String TAG_BASE = "MsgClient";
    public final String logTag;

    private Socket socket = null;

    public final CustomApplication app;
    public final Context context;

    private final ExecutorService workerThreadIncoming = Executors.newFixedThreadPool(1);
    private final ExecutorService workerThreadOutgoing = Executors.newFixedThreadPool(1);

    private DataInputStream inStream;
    private DataOutputStream outStream;
    private CountDownLatch outStreamReadyLatch = new CountDownLatch(1);

    public final boolean iAmTheInitiator;

    // if this MsgClient is in MsgServerManager.serviceToMsgClientMap,
    // then the corresponding key for it is:
    private ServiceStub serviceStubWeAreBoundTo;

    public final boolean encrypted;

    private boolean closed = false;

    private InetAddress socketAddress;
    public String strSocketAddress;
    public String strFromAddress;
    public String strToAddress;

    private UUID badgeIdOfOtherEnd = null;
    public JPAKEClient jpakeClient;

    private MsgClient(@Nullable byte[] secretKeyBytes, boolean iAmTheInitiator) {
        checkSecretKeyLength(secretKeyBytes);

        app = CustomApplication.getInstance();
        context = app.getApplicationContext();
        this.iAmTheInitiator = iAmTheInitiator;
        encrypted = (null != secretKeyBytes);
        if (encrypted) {
            logTag = TAG_BASE + "-Encrypted";
        } else {
            logTag = TAG_BASE + "-PlainText";
        }
    }

    protected MsgClient(Socket socket, @Nullable final byte[] secretKeyBytes) {
        this(secretKeyBytes, false);
        this.socket = socket;

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                init(secretKeyBytes);
            }
        };
        try {
            workerThreadIncoming.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.e(logTag, "MsgClient() 1. runnable was rejected by executor - " + e.getMessage());
        }
    }

    public MsgClient(@NonNull final InetAddress address, final int port, @Nullable final byte[] secretKeyBytes) {
        this(secretKeyBytes, true);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    MsgClient.this.socket = new Socket(address, port);
                    init(secretKeyBytes);
                } catch (IOException e) { // TODO think about this
                    FLogger.e(logTag, "MsgClient(). Failed to open socket. closing MsgClient. IOE - " + e.getMessage());
                    MsgClient.this.close();
                }
            }
        };
        try {
            workerThreadIncoming.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.e(logTag, "MsgClient() 2. runnable was rejected by executor - " + e.getMessage());
        }

    }

    private void init(@Nullable byte[] secretKeyBytes) {
        try {
            socketAddress = socket.getInetAddress();
            strSocketAddress = socketAddress.getHostAddress();
            strFromAddress = "from addr: " + strSocketAddress + ", ";
            strToAddress = "to addr: " + strSocketAddress + ", ";

            outStream = initOutStream(socket, secretKeyBytes);
            if (null == outStream) {
                FLogger.e(logTag, "outStream is null.");
                this.close();
                return;
            }
            outStreamReadyLatch.countDown();
            inStream = initInStream(socket, secretKeyBytes);
            if (null == inStream) {
                FLogger.e(logTag, "inStream is null.");
                this.close();
                return;
            }
            startWaitingForMessages();
        } catch (IOException e) {
            FLogger.e(logTag, "init() failed. IOE - " + e.getMessage());
        }
    }

    private static DataOutputStream initOutStream(Socket socket, @Nullable byte[] secretKeyBytes) throws IOException {
        OutputStream outStream = socket.getOutputStream();
        boolean encrypted = (null != secretKeyBytes);
        if (!encrypted) {
            return new DataOutputStream(new BufferedOutputStream(outStream));
        } else {
            Cipher cipher;
            try {
                cipher = Symmetric.getInitialisedCipher(Cipher.ENCRYPT_MODE, secretKeyBytes);
            } catch (InvalidAlgorithmParameterException | InvalidKeyException
                    | NoSuchPaddingException | NoSuchAlgorithmException e) {
                FLogger.e(TAG_BASE, "initOutStream(). Exception: " + e.getMessage());
                FLogger.e(TAG_BASE, HelperMethods.formatStackTraceAsString(e));
                return null;
            }
            return new DataOutputStream(new BufferedOutputStream(new CipherOutputStream(outStream, cipher)));
        }
    }

    private static DataInputStream initInStream(Socket socket, @Nullable byte[] secretKeyBytes) throws IOException {
        InputStream inStream = socket.getInputStream();
        boolean encrypted = (null != secretKeyBytes);
        if (!encrypted) {
            return new DataInputStream(new BufferedInputStream(inStream));
        } else {
            Cipher cipher;
            try {
                cipher = Symmetric.getInitialisedCipher(Cipher.DECRYPT_MODE, secretKeyBytes);
            } catch (InvalidAlgorithmParameterException | InvalidKeyException
                    | NoSuchPaddingException | NoSuchAlgorithmException e) {
                FLogger.e(TAG_BASE, "initInStream(). Exception: " + e.getMessage());
                FLogger.e(TAG_BASE, HelperMethods.formatStackTraceAsString(e));
                return null;
            }
            return new DataInputStream(new BufferedInputStream(new CipherInputStream(inStream, cipher)));
        }
    }

    private void startWaitingForMessages(){
        try {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    FLogger.i(logTag, "startWaitingForMessages() interrupted. finishing.");
                    break;
                }
                receiveMsg();
            }
        } catch (EOFException e) {
            FLogger.i(logTag, "startWaitingForMessages(). EOF - " + e.getMessage());
        } catch (SocketException e) {
            FLogger.w(logTag, "startWaitingForMessages(). SockExc - " + e.getMessage());
        } catch (IOException e) {
            FLogger.e(logTag, "startWaitingForMessages(). IOE - " + e.getMessage());
        }
        close();
    }

    private void receiveMsg() throws IOException {
        Message msg;
        try {
            msg = Message.createFromStream(inStream);
        } catch (UnknownMessageTypeException e) {
            FLogger.e(logTag, strFromAddress + "received msg. UnknownMessageTypeException: " + e.getMessage());
            return;
        }
        if (null == msg) {
            FLogger.e(logTag, strFromAddress + "received msg. But parsing it returned null.");
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
                    FLogger.e(logTag, "sendMessage(). latch await interrupted - " + e.getMessage());
                    return;
                }
                try {
                    msg.send(MsgClient.this);
                    FLogger.i(logTag, strToAddress + "sent msg with type " + msg.getType()
                            + "/" + msg.getClass().getSimpleName());
                } catch (IOException e) {
                    FLogger.e(logTag, "sendMessage(). IOE - " + e.getMessage());
                }
            }
        };
        try {
            workerThreadOutgoing.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.e(logTag, "sendMessage(). runnable was rejected by executor - " + e.getMessage());
        }
    }

    public void considerDoingAHistoryTransfer() {
        FLogger.d(logTag, "considering doing a historyTransfer to " + badgeIdOfOtherEnd);
        if (null == badgeIdOfOtherEnd) {
            FLogger.e(logTag, "considerDoingAHistoryTransfer(). badgeIdOfOtherEnd is null.");
            return;
        }
        Long lastTimeWeSentHistoryToThatBadge = DbTableHistoryTransfer.getTimestamp(badgeIdOfOtherEnd);
        long curTime = System.currentTimeMillis();
        if (null == lastTimeWeSentHistoryToThatBadge
                || curTime - lastTimeWeSentHistoryToThatBadge > Constants.HISTORY_TRANSFER_TO_SAME_CLIENT_COOLDOWN) {
            FLogger.d(logTag, "decided to do historyTransfer to " + badgeIdOfOtherEnd.toString());
            doHistoryTransfer();
        } else {
            long timeElapsed = curTime - lastTimeWeSentHistoryToThatBadge;
            FLogger.d(logTag, "won't do historyTransfer to " + badgeIdOfOtherEnd
                    + ", last transfer was " + timeElapsed/1000 + " seconds ago ");
        }
    }

    private void doHistoryTransfer() {
        long curTime = System.currentTimeMillis();
        Long timeStampLastHistoryTransfer = DbTableHistoryTransfer.getTimestamp(badgeIdOfOtherEnd);
        List<BadgeStatus> badgeStatuses = DbTableBadges.getBadgesUpdatedSince(timeStampLastHistoryTransfer);
        for (BadgeStatus badgeStatus : badgeStatuses) {
            FLogger.d(logTag, "historyTransfer to " + badgeIdOfOtherEnd + " contains badge:\n"
                    + badgeStatus.toString());
        }
        Message msgHistoryTransfer = new MsgHistoryTransfer(badgeStatuses);
        sendMessage(msgHistoryTransfer);
        DbTableHistoryTransfer.smartUpdateEntry(badgeIdOfOtherEnd, curTime);
    }

    public synchronized void close() {
        FLogger.d(logTag, "close() called. address: " + strSocketAddress);
        if (closed) {
            FLogger.d(logTag, "close(). already closed.");
            return;
        }

        workerThreadOutgoing.shutdown();
        workerThreadIncoming.shutdownNow();
        try {
            if (null != outStream) outStream.close();
            if (null != inStream) inStream.close();
            if (null != socket) socket.close();
        } catch (IOException e) {
            FLogger.e(logTag, "close(). trying to close streams and socket, IOE - " + e.getMessage());
        }
        closed = true;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    public DataOutputStream getOutStream() {
        return outStream;
    }

    public UUID getBadgeIdOfOtherEnd() {
        return badgeIdOfOtherEnd;
    }

    public InetAddress getSocketAddress() {
        return socketAddress;
    }

    /**
     * Sets this.badgeIdOfOtherEnd to badgeId on first call. Tests for equality and
     * "produces" error on mismatch for subsequent calls.
     */
    public void reconfirmBadgeId(UUID badgeId) {
        if (null == badgeIdOfOtherEnd) {
            if (null != badgeId) {
                badgeIdOfOtherEnd = badgeId;
                FLogger.d(logTag, "reconfirmBadgeId(). badgeIdOfOtherEnd set to " + badgeIdOfOtherEnd);
            } else {
                FLogger.e(logTag, "reconfirmBadgeId(). badgeId == badgeIdOfOtherEnd == null.");
            }
            return;
        }
        if (!badgeIdOfOtherEnd.equals(badgeId)) {
            FLogger.e(logTag, String.format(Locale.US,
                    "reconfirmBadgeId(). wtf. badgeId mismatch! old: %s, new: %s",
                    badgeIdOfOtherEnd, badgeId));
            // TODO maybe throw an exception or call close() ?
        }
    }

    public ServiceStub getServiceStubWeAreBoundTo() {
        return serviceStubWeAreBoundTo;
    }

    public void setServiceStubWeAreBoundTo(ServiceStub serviceStubWeAreBoundTo) {
        this.serviceStubWeAreBoundTo = serviceStubWeAreBoundTo;
    }

    /**
     * note: a msgClient can only be upgraded if (msgClient.iAmTheInitiator == true)
     */
    public static MsgClient upgradeToEncryptedMsgClient(MsgClient msgClient, int port, @NonNull final byte[] secretKeyBytes) {
        if (!msgClient.iAmTheInitiator) {
            throw new IllegalArgumentException("msgClient.iAmTheInitiator == false");
        }

        MsgClient msgClientEncrypted = new MsgClient(msgClient.socket.getInetAddress(), port, secretKeyBytes);
        ServiceStub serviceStub = msgClient.serviceStubWeAreBoundTo;
        msgClientEncrypted.setServiceStubWeAreBoundTo(serviceStub);
        MsgServerManager.getInstance().serviceToMsgClientMap.put(serviceStub, msgClientEncrypted);

        msgClient.close();

        return msgClientEncrypted;
    }

    private static void checkSecretKeyLength(@Nullable final byte[] secretKeyBytes) {
        if (isSecretKeyLengthValid(secretKeyBytes)) return;
        throw new RuntimeException("invalid secret key length: " + secretKeyBytes.length + " bytes");
    }

    public static boolean isSecretKeyLengthValid(@Nullable final byte[] secretKeyBytes) {
        return (null == secretKeyBytes) || (secretKeyBytes.length == Symmetric.KEY_LENGTH_IN_BYTES);
    }

}
