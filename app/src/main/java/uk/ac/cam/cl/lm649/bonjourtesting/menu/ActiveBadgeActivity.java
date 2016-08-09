package uk.ac.cam.cl.lm649.bonjourtesting.menu;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTableBadges;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class ActiveBadgeActivity extends CustomActivity {

    private static final String TAG = "ActiveBadgeActivity";

    private ArrayAdapter<String> listAdapterForDisplayedListOfBadges;
    private ArrayList<BadgeStatus> badgesArrList = new ArrayList<>();
    private final Object displayedBadgesLock = new Object();
    private BadgeStatus.SortOrder badgeSortOrder = BadgeStatus.SortOrder.MOST_RECENT_ALIVE_FIRST;

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
        listAdapterForDisplayedListOfBadges = new ArrayAdapter<>(this, R.layout.row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapterForDisplayedListOfBadges);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                synchronized (displayedBadgesLock) {
                    FLogger.i(TAG, "onItemClick() - user clicked on an item in the list");
                    final BadgeStatus badgeStatus = badgesArrList.get(position);
                    if (null == badgeStatus){
                        FLogger.e(TAG, "onItemClick(). clicked badge is null ??");
                        HelperMethods.displayMsgToUser(context, "error: clicked badge is null");
                        return true;
                    }
                    DialogInterface.OnClickListener deleteBadge = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FLogger.i(TAG, "user deleted badge from DB");
                            FLogger.d(TAG, "deleted badge had details: " + badgeStatus.toString());
                            DbTableBadges.deleteBadge(badgeStatus.getBadgeCore().getBadgeId());
                            updateListView();
                        }
                    };
                    new AlertDialog.Builder(ActiveBadgeActivity.this)
                            .setTitle("Confirm Delete")
                            .setMessage("Do you really want to delete that badge?")
                            //.setIcon(R.drawable.maybe_a_cool_icon_here)
                            .setPositiveButton(android.R.string.yes, deleteBadge)
                            .setNegativeButton(android.R.string.no, null).show();
                    return true;
                }
            }
        });

        // top area
        textViewBadgeId = (TextView) findViewById(R.id.badgeId);
        textViewCustomName = (TextView) findViewById(R.id.customName);
        textViewRouterMac = (TextView) findViewById(R.id.routerMac) ;
        textViewNumBadgesInList = (TextView) findViewById(R.id.nBadgesInList);

        // change sort order button
        findViewById(R.id.changeSortOrderButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(badgeSortOrder) {
                    case MOST_RECENT_ALIVE_FIRST:
                        badgeSortOrder = BadgeStatus.SortOrder.ALPHABETICAL;
                        break;
                    case ALPHABETICAL:
                        badgeSortOrder = BadgeStatus.SortOrder.MOST_RECENT_ALIVE_FIRST;
                        break;
                    default:
                        FLogger.e(TAG, "unknown badge sort order: " + badgeSortOrder.name());
                        badgeSortOrder = BadgeStatus.SortOrder.MOST_RECENT_ALIVE_FIRST;
                        break;
                }
                updateListView();
            }
        });

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

        String numBadgesInList;
        synchronized (displayedBadgesLock) {
            numBadgesInList = "" + listAdapterForDisplayedListOfBadges.getCount();
        }
        textViewNumBadgesInList.setText(numBadgesInList);
    }

    public void updateListView() {
        FLogger.v(TAG, "updateListView() called.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (displayedBadgesLock) {
                    FLogger.v(TAG, "updateListView() doing actual update.");
                    listAdapterForDisplayedListOfBadges.clear();
                    badgesArrList.clear();
                    for (BadgeStatus badgeStatus : DbTableBadges.getAllBadges(badgeSortOrder)) {
                        listAdapterForDisplayedListOfBadges.add(badgeStatus.toString());
                        badgesArrList.add(badgeStatus);
                    }
                    listAdapterForDisplayedListOfBadges.notifyDataSetChanged();
                    refreshTopUIInternal();
                }
            }
        });
    }

    @Override
    public void forceRefreshUI() {
        updateListView();
    }

}
