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
                            break; // disconnected
                        } else {
                            mainActivity.displayMsgToUser(msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    public static void sendMessage(final ServiceInfo serviceInfo, final String senderID, final String msg){
        if (null == serviceInfo){
            Log.e(TAG, "sendMessage(). serviceInfo is null");
            return;
        }
        InetAddress[] arrAddresses = serviceInfo.getInet4Addresses();
        if (null == arrAddresses || arrAddresses.length < 1){
            Log.e(TAG, "sendMessage(). inappropriate addresses");
            return;
        }
        final InetAddress address = arrAddresses[0];
        new Thread(){
            @Override
            public void run(){
                try {
                    Socket socket = new Socket(address, serviceInfo.getPort());
                    PrintWriter out = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream()));
                    out.println(String.format(Locale.US, "%s: %s", senderID, msg));
                    out.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public int getPort(){
        return serverSocket.getLocalPort();
    }

}
