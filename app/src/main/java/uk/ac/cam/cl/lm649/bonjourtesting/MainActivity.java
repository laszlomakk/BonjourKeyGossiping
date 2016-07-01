/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String SERVICE_TYPE = "_http._tcp";

    protected NsdManager nsdManager;
    protected ResolutionWorker resolutionWorker;
    protected View rootView;

    private static final String TAG = "MainActivity";
    private Context context;
    private NsdManager.DiscoveryListener discoveryListener;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        //UI
        LayoutInflater li = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rootView = li.inflate(R.layout.activity_main, null);
        setContentView(rootView);
        listView = (ListView) findViewById(R.id.mainListView);
        listAdapter = new ArrayAdapter<>(this, R.layout.row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapter);

        resolutionWorker = ResolutionWorker.getInstance(this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        startDiscovery();
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopDiscovery();
    }

    private void startDiscovery(){
        discoveryListener = new CustomDiscoveryListener(this);
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void stopDiscovery(){
        nsdManager.stopServiceDiscovery(discoveryListener);
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
