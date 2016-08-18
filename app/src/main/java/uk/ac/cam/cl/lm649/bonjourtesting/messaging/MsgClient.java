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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Symmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.JPAKEManager;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MessageRequiringEncryption;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.UnknownMessageTypeException;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.ServiceStub;

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

    public final SessionData sessionData;

    public final boolean encrypted;

    private boolean closed = false;

    private InetAddress socketAddress;
    public String strSocketAddress;
    public String strFromAddress;
    public String strToAddress;

    public final JPAKEManager jpakeManager = new JPAKEManager();

    private MsgClient(@Nullable SessionData _sessionData, boolean iAmTheInitiator) {
        app = CustomApplication.getInstance();
        context = app.getApplicationContext();

        this.sessionData = null != _sessionData ? _sessionData : new SessionData();
        this.iAmTheInitiator = iAmTheInitiator;
        encrypted = (null != sessionData.sessionKey);
        if (encrypted) {
            logTag = TAG_BASE + "-Encrypted";
        } else {
            logTag = TAG_BASE + "-PlainText";
        }
    }

    protected MsgClient(Socket socket, @Nullable final SessionData sessionData) {
        this(sessionData, false);
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
            FLogger.e(logTag, "MsgClient() 1. runnable was rejected by executor - " + e.getMessage());
        }
    }

    public MsgClient(@NonNull final InetAddress address, final int port, @Nullable final SessionData sessionData) {
        this(sessionData, true);

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    MsgClient.this.socket = new Socket(address, port);
                    init();
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

    private void init() {
        try {
            socketAddress = socket.getInetAddress();
            strSocketAddress = socketAddress.getHostAddress();
            strFromAddress = "from addr: " + strSocketAddress + ", ";
            strToAddress = "to addr: " + strSocketAddress + ", ";

            outStream = initOutStream(socket, sessionData.sessionKey);
            if (null == outStream) {
                FLogger.e(logTag, "outStream is null.");
                this.close();
                return;
            }
            outStreamReadyLatch.countDown();
            inStream = initInStream(socket, sessionData.sessionKey);
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

    private static DataOutputStream initOutStream(Socket socket, @Nullable SessionKey sessionKey) throws IOException {
        OutputStream outStream = socket.getOutputStream();
        boolean encrypted = (null != sessionKey);
        if (!encrypted) {
            return new DataOutputStream(new BufferedOutputStream(outStream));
        } else {
            Cipher cipher;
            try {
                cipher = Symmetric.getInitialisedCipher(Cipher.ENCRYPT_MODE, sessionKey.secretKeyBytes, sessionKey.ivBytes);
            } catch (InvalidAlgorithmParameterException | InvalidKeyException
                    | NoSuchPaddingException | NoSuchAlgorithmException e) {
                FLogger.e(TAG_BASE, "initOutStream(). Exception: " + e.getMessage());
                FLogger.e(TAG_BASE, e);
                return null;
            }
            return new DataOutputStream(new BufferedOutputStream(new CipherOutputStream(outStream, cipher)));
        }
    }

    private static DataInputStream initInStream(Socket socket, @Nullable SessionKey sessionKey) throws IOException {
        InputStream inStream = socket.getInputStream();
        boolean encrypted = (null != sessionKey);
        if (!encrypted) {
            return new DataInputStream(new BufferedInputStream(inStream));
        } else {
            Cipher cipher;
            try {
                cipher = Symmetric.getInitialisedCipher(Cipher.DECRYPT_MODE, sessionKey.secretKeyBytes, sessionKey.ivBytes);
            } catch (InvalidAlgorithmParameterException | InvalidKeyException
                    | NoSuchPaddingException | NoSuchAlgorithmException e) {
                FLogger.e(TAG_BASE, "initInStream(). Exception: " + e.getMessage());
                FLogger.e(TAG_BASE, e);
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

    public void sendMessage(@Nullable final Message msg){
        if (null == msg) {
            FLogger.e(logTag, "sendMessage(). msg == null");
            return;
        }
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
                    if (encrypted || !(msg instanceof MessageRequiringEncryption)) {
                        msg.send(MsgClient.this);
                        FLogger.i(logTag, strToAddress + "sent msg with type " + msg.getType()
                                + "/" + msg.getClass().getSimpleName());
                    } else {
                        FLogger.e(logTag, strToAddress + "CAN'T send msg with type " + msg.getType()
                                + "/" + msg.getClass().getSimpleName() + ", stream is not encrypted");
                    }
                } catch (IOException e) {
                    FLogger.e(logTag, "sendMessage(). IOE - " + e.getMessage());
                }
            }
        };
        try {
            workerThreadOutgoing.execute(runnable);
        } catch (RejectedExecutionException e) {
            FLogger.w(logTag, "sendMessage(). runnable was rejected by executor - " + e.getMessage());
        }
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

    public InetAddress getSocketAddress() {
        return socketAddress;
    }
    
    public void setServiceStubWeAreBoundTo(ServiceStub serviceStubWeAreBoundTo) {
        this.serviceStubWeAreBoundTo = serviceStubWeAreBoundTo;
    }

    /**
     * note: a msgClient can only be upgraded if (msgClient.iAmTheInitiator == true)
     */
    public static MsgClient upgradeToEncryptedMsgClient(MsgClient msgClient, int port, final SessionKey sessionKey) {
        if (!msgClient.iAmTheInitiator) {
            throw new IllegalArgumentException("msgClient.iAmTheInitiator == false");
        }
        if (null == sessionKey) {
            throw new IllegalArgumentException("sessionKey == null");
        }

        SessionData sessionData = new SessionData(msgClient.sessionData, sessionKey);
        MsgClient msgClientEncrypted = new MsgClient(msgClient.socket.getInetAddress(), port, sessionData);

        ServiceStub serviceStub = msgClient.serviceStubWeAreBoundTo;
        msgClientEncrypted.serviceStubWeAreBoundTo = serviceStub;
        MsgServerManager.getInstance().serviceToMsgClientMap.put(serviceStub, msgClientEncrypted);

        msgClient.close();

        return msgClientEncrypted;
    }

}
