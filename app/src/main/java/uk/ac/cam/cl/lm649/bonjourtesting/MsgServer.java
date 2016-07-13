/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;

import javax.jmdns.ServiceInfo;

public class MsgServer {

    private final MainActivity mainActivity;
    private final static String TAG = "MsgServer";
    private final ServerSocket serverSocket;

    public MsgServer(MainActivity mainActivity) throws IOException {
        this.mainActivity = mainActivity;
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

    private void startWaitingForConnections(final ServerSocket serverSocket){
        try {
            while (true) {
                Socket incoming = serverSocket.accept();
                final BufferedReader in = new BufferedReader(
                        new InputStreamReader(incoming.getInputStream()));
                startWaitingForMessages(in);
            }
        } catch (IOException e) {
            Log.e(TAG, "startWaitingForConnections(). error");
            e.printStackTrace();
        }
    }

    private void startWaitingForMessages(final BufferedReader in){
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        String msg = in.readLine();
                        if (msg == null) {
                            Log.i(TAG, "MsgServer closed a thread for msging.");
                            break; // disconnected
                        } else {
                            mainActivity.displayMsgToUser(msg);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "startWaitingForMessages(). error");
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public static void sendMessage(final MainActivity mainActivity, final ServiceInfo serviceInfoOfDst,
                                   final String senderID, final String msg){
        if (null == serviceInfoOfDst){
            Log.e(TAG, "sendMessage(). serviceInfo is null");
            mainActivity.displayMsgToUser("error sending msg: serviceInfo is null (2)");
            return;
        }
        InetAddress[] arrAddresses = serviceInfoOfDst.getInet4Addresses();
        if (null == arrAddresses || arrAddresses.length < 1){
            Log.e(TAG, "sendMessage(). inappropriate addresses");
            mainActivity.displayMsgToUser("error sending msg: inappropriate addresses");
            return;
        }
        final InetAddress address = arrAddresses[0];
        new Thread(){
            @Override
            public void run(){
                try {
                    Socket socket = new Socket(address, serviceInfoOfDst.getPort());
                    PrintWriter out = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream()));
                    out.println(String.format(Locale.US, "%s: %s", senderID, msg));
                    out.flush();
                    mainActivity.displayMsgToUser("msg sent");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "sendMessage(). IOException");
                    mainActivity.displayMsgToUser("error sending msg: IOException");
                }
            }
        }.start();
    }

    public int getPort(){
        return serverSocket.getLocalPort();
    }

    public void stop(){
        Log.i(TAG, "Stopping MsgServer.");
        try {
            serverSocket.close();
            Log.i(TAG, "serverSocket successfully closed.");
        } catch (IOException e) {
            Log.e(TAG, "error while closing serverSocket");
            e.printStackTrace();
        }

    }

}
