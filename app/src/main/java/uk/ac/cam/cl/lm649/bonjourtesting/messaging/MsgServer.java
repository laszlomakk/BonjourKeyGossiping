/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class MsgServer {

    private final static String TAG = "MsgServer";
    private static MsgServer INSTANCE = null;

    public final ConcurrentHashMap<ServiceStub, MsgClient> serviceToMsgClientMap = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;

    private boolean started = false;

    private MsgServer() {}

    public void start() throws IOException {
        if (started) return;

        serverSocket = new ServerSocket(0);
        Thread t = new Thread() {
            @Override
            public void run() {
                startWaitingForConnections(serverSocket);
            }
        };
        t.setDaemon(true);
        t.start();
        started = true;
    }

    public static synchronized MsgServer getInstance() {
        if (null == INSTANCE) {
            INSTANCE = new MsgServer();
        }
        return INSTANCE;
    }

    private void startWaitingForConnections(final ServerSocket serverSocket) {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new MsgClient(clientSocket);
            }
        } catch (IOException e) {
            FLogger.e(TAG, "startWaitingForConnections(). error -- closing main thread. IOE - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int getPort(){
        return serverSocket.getLocalPort();
    }

    public void stop(){
        if (!started) return;

        FLogger.i(TAG, "Stopping MsgServer.");
        try {
            serverSocket.close();
            FLogger.i(TAG, "serverSocket successfully closed.");
        } catch (IOException e) {
            FLogger.e(TAG, "error while closing serverSocket. IOE - " + e.getMessage());
            e.printStackTrace();
        } finally {
            started = false;
        }
    }

}
