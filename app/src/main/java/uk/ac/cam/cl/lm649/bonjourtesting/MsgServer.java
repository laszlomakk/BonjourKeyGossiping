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

    public MsgServer(MainActivity mainActivity, InetAddress host, int port) throws IOException {
        this.mainActivity = mainActivity;
        final ServerSocket serverSocket = new ServerSocket(port);
        new Thread() {
            @Override
            public void run() {
                startWaitingForConnections(serverSocket);
            }
        }.start();
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
        new Thread() {
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
        }.start();
    }

    public static void sendMessage(ServiceInfo serviceInfo, String senderID, String msg){
        if (null == serviceInfo){
            Log.e(TAG, "sendMessage(). serviceInfo is null");
            return;
        }
        InetAddress[] arrAddresses = serviceInfo.getInet4Addresses();
        if (null == arrAddresses || arrAddresses.length < 1){
            Log.e(TAG, "sendMessage(). inappropriate addresses");
            return;
        }
        InetAddress address = arrAddresses[0];
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

}
