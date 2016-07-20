package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class ActiveBadgeActivity extends CustomActivity {

    private static final String TAG = "ActiveBadgeActivity";

    private ArrayAdapter<String> listAdapterForDisplayedListOfBadges;

    private TextView textViewBadgeId;
    private TextView textViewCustomName;
    private TextView textViewRouterMac;
    private TextView textViewNumBadgesInList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                updateListView();
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

        String customName = SaveBadgeData.getInstance(context).getMyBadgeCustomName();
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
                Log.v(TAG, "updateListView() doing actual update.");
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
