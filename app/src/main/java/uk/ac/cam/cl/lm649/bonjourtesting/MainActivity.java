/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.jmdns.JmDNS;

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MainActivity extends AppCompatActivity {

    public static final String SERVICE_TYPE = "_http._tcp.local."; // _http._tcp

    protected View rootView;

    private static final String TAG = "MainActivity";
    private Context context;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;

    protected JmDNS jmdns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();

        //UI
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = li.inflate(R.layout.activity_main, null);
        setContentView(rootView);
        listView = (ListView) findViewById(R.id.mainListView);
        listAdapter = new ArrayAdapter<>(this, R.layout.row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapter);
    }

    @Override
    protected void onStart(){
        Log.i(TAG, "Activity starting up.");
        super.onStart();
        new Thread(){
            @Override
            public void run(){
                try {
                    final Inet4Address inet4Address = HelperMethods.getWifiInetAddress(context, Inet4Address.class);
                    Log.i(TAG, "Creating jmDNS. Starting discovery...");
                    jmdns = JmDNS.create(inet4Address);
                    jmdns.addServiceListener(SERVICE_TYPE, new CustomServiceListener(MainActivity.this));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    protected void onStop(){
        Log.i(TAG, "Activity stopping.");
        super.onStop();
        if (jmdns != null) {
            Log.i(TAG, "Stopping jmDNS...");
            jmdns.unregisterAllServices();
            try {
                jmdns.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                jmdns = null;
            }
        }
    }

    protected void displayMsgToUser(final String msg){
        rootView.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    protected void addItemToList(final String str){
        rootView.post(new Runnable() {
            @Override
            public void run() {
                listAdapter.add(str);
                listAdapter.notifyDataSetChanged();
            }
        });
    }

    protected void removeItemFromList(final String str){
        rootView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    listAdapter.remove(str);
                    listAdapter.notifyDataSetChanged();
                } catch (RuntimeException e){
                    e.printStackTrace();
                }
            }
        });
    }

}
