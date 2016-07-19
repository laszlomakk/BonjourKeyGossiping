package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.TreeMap;

import javax.jmdns.ServiceEvent;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class ActiveBadgeActivity extends Activity {

    private static final String TAG = "ActiveBadgeActivity";

    private CustomApplication app;
    private Context context;

    private ArrayAdapter<String> listAdapterForDisplayedListOfBadges;

    private TextView textViewBadgeId;
    private TextView textViewCustomName;
    private TextView textViewRouterMac;
    private TextView textViewNumBadgesInList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (CustomApplication) getApplication();
        context = app;

        setupUI();
    }

    private void setupUI(){
        setContentView(R.layout.active_badge_view);

        // listView for displaying the badges
        ListView listView = (ListView) findViewById(R.id.mainListView);
        listAdapterForDisplayedListOfBadges = new ArrayAdapter<>(this, R.layout.active_badge_row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapterForDisplayedListOfBadges);

        // top area
        textViewBadgeId = (TextView) findViewById(R.id.badgeId);
        textViewCustomName = (TextView) findViewById(R.id.customName);
        textViewRouterMac = (TextView) findViewById(R.id.routerMac) ;
        textViewNumBadgesInList = (TextView) findViewById(R.id.nBadgesInList);

        // refresh button
        findViewById(R.id.refreshButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO refresh stuff
            }
        });

        updateListView();
    }

    public void refreshTopUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                refreshTopUIInternal();
            }
        });
    }

    private void refreshTopUIInternal() {
        textViewBadgeId.setText(SaveBadgeData.getInstance(context).getMyBadgeId().toString());

        String customName = "-";
        textViewCustomName.setText(customName);

        textViewRouterMac.setText(NetworkUtil.getRouterMacAddress(context));

        String numBadgesInList = "" + listAdapterForDisplayedListOfBadges.getCount();
        textViewNumBadgesInList.setText(numBadgesInList);
    }

    public void updateListView() {
        Log.v(TAG, "updateListView() called.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "updateListView() doing actual update.");
                listAdapterForDisplayedListOfBadges.clear();
                for (Badge badge : BadgeDbHelper.getInstance(context).getAllBadges()) {
                    listAdapterForDisplayedListOfBadges.add(badge.toString());
                }
                listAdapterForDisplayedListOfBadges.notifyDataSetChanged();
                refreshTopUIInternal();
            }
        });
    }

}