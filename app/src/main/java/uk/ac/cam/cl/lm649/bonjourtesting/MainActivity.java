package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Context context;
    private NsdManager nsdManager;
    private NsdManager.DiscoveryListener discoveryListener;
    private ListView listView;
    private ArrayAdapter<String> listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);

        //UI
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.mainListView);
        listAdapter = new ArrayAdapter<>(this, R.layout.row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapter);
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
        nsdManager.discoverServices("_http._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void stopDiscovery(){
        nsdManager.stopServiceDiscovery(discoveryListener);
    }

    protected void displayMsgToUser(String msg){
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    protected void addItemToList(String str){
        listAdapter.add(str);
        listAdapter.notifyDataSetChanged();
    }

    protected void removeItemFromList(String str){
        try {
            listAdapter.remove(str);
            listAdapter.notifyDataSetChanged();
        } catch (RuntimeException e){
            e.printStackTrace();
        }
    }



}
