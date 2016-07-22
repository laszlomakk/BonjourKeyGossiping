/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.jmdns.ServiceEvent;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class MsgServer {

    private final static String TAG = "MsgServer";
    private static MsgServer INSTANCE = null;

    public final ConcurrentHashMap<ServiceStub, MsgClient> serviceToMsgClientMap = new ConcurrentHashMap<>();

    private final ServerSocket serverSocket;

    private MsgServer() throws IOException {
        serverSocket = new ServerSocket(0);
        Thread t = new Thread() {
            @Override
            public void run() {
                startWaitingForConnections(serverSocket);
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public static synchronized MsgServer getInstance() {
        if (null == INSTANCE) {
            FLogger.e(TAG, "getInstance(). trying to get MsgServer before calling initInstance()");
            throw new RuntimeException("MsgServer INSTANCE not yet initialised");
        }
        return INSTANCE;
    }

    public static synchronized void initInstance() throws IOException {
        if (null != INSTANCE) {
            FLogger.e(TAG, "initInstance(). trying to reinit already initialised MsgServer");
            throw new RuntimeException("MsgServer INSTANCE already initialised");
        }
        INSTANCE = new MsgServer();
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
        FLogger.i(TAG, "Stopping MsgServer.");
        try {
            serverSocket.close();
            FLogger.i(TAG, "serverSocket successfully closed.");
        } catch (IOException e) {
            FLogger.e(TAG, "error while closing serverSocket. IOE - " + e.getMessage());
            e.printStackTrace();
        }
    }

}
