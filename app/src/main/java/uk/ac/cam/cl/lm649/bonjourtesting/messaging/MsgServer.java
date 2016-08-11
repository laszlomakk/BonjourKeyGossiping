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
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class MsgServer {

    private final String logTag;

    private ServerSocket serverSocket;

    private boolean started = false;

    protected MsgServer(String logTag) {
        this.logTag = logTag;
    }

    protected synchronized void start() throws IOException {
        if (started) return;

        FLogger.i(logTag, "Starting MsgServer.");
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
                new MsgClient(clientSocket);
            }
        } catch (IOException e) {
            FLogger.e(logTag, "startWaitingForConnections(). error -- closing main thread. IOE - " + e.getMessage());
            //e.printStackTrace();
        }
    }

    public int getPort(){
        return null == serverSocket ? 0 : serverSocket.getLocalPort();
    }

    protected synchronized void stop(){
        if (!started) return;

        FLogger.i(logTag, "Stopping MsgServer.");
        try {
            serverSocket.close();
            FLogger.i(logTag, "serverSocket successfully closed.");
        } catch (IOException e) {
            FLogger.e(logTag, "error while closing serverSocket. IOE - " + e.getMessage());
            FLogger.e(logTag, HelperMethods.formatStackTraceAsString(e));
        }

        started = false;
    }

}
