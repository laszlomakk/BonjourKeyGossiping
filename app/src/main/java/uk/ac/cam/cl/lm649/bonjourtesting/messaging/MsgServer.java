/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public abstract class MsgServer {

    protected static final String TAG = "MsgServer";

    private ServerSocket serverSocket;

    private boolean started = false;

    protected MsgServer() {}

    protected synchronized void start() throws IOException {
        if (started) return;

        FLogger.i(TAG, "Starting MsgServer.");
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

    private void startWaitingForConnections(final ServerSocket serverSocket) {
        try {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                createMsgClientForIncomingConnection(clientSocket);
            }
        } catch (IOException e) {
            FLogger.e(TAG, "startWaitingForConnections(). error -- closing main thread. IOE - " + e.getMessage());
        }
    }

    protected abstract MsgClient createMsgClientForIncomingConnection(Socket socket);

    public int getPort(){
        return null == serverSocket ? 0 : serverSocket.getLocalPort();
    }

    protected synchronized void stop(){
        if (!started) return;

        FLogger.i(TAG, "Stopping MsgServer.");
        try {
            serverSocket.close();
            FLogger.i(TAG, "serverSocket successfully closed.");
        } catch (IOException e) {
            FLogger.e(TAG, "error while closing serverSocket. IOE - " + e.getMessage());
            FLogger.e(TAG, HelperMethods.formatStackTraceAsString(e));
        }

        started = false;
    }

}
