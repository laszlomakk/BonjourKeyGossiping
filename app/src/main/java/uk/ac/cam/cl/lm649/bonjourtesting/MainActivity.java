/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import javax.jmdns.JmDNS;

public class MainActivity extends AppCompatActivity {

    public static final String SERVICE_TYPE = "_http._tcp.local."; // _http._tcp

    protected View rootView;

    private static final String TAG = "MainActivity";
    private Context context;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;

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

        new Thread(){
            @Override
            public void run(){
                try {
                    Log.d(TAG, "Creating JmDNS instance");
                    JmDNS jmdns = JmDNS.create();
                    jmdns.addServiceListener(SERVICE_TYPE, new CustomServiceListener(MainActivity.this));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
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
