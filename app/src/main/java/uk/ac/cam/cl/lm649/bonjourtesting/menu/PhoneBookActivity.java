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
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.menu.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class PhoneBookActivity extends CustomActivity {

    private static final String TAG = "PhoneBookActivity";

    private ArrayAdapter<String> listAdapterForDisplayedListOfEntries;
    private ArrayList<DbTablePhoneNumbers.Entry> entriesArrList = new ArrayList<>();
    private final Object displayedEntriesLock = new Object();

    private TextView textViewBadgeId;
    private TextView textViewCustomName;
    private TextView textViewPhoneNumber;
    private TextView textViewNumEntriesInList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUI();
    }

    private void setupUI(){
        setContentView(R.layout.phone_book_view);

        // listView for displaying the entries
        ListView listView = (ListView) findViewById(R.id.mainListView);
        listAdapterForDisplayedListOfEntries = new ArrayAdapter<>(this, R.layout.row_in_list, new ArrayList<String>());
        listView.setAdapter(listAdapterForDisplayedListOfEntries);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                synchronized (displayedEntriesLock) {
                    FLogger.i(TAG, "onItemClick() - user clicked on an item in the list");
                    final DbTablePhoneNumbers.Entry entry = entriesArrList.get(position);
                    if (null == entry){
                        FLogger.e(TAG, "onItemClick(). clicked entry is null ??");
                        HelperMethods.displayMsgToUser(context, "error: clicked entry is null");
                        return true;
                    }
                    DialogInterface.OnClickListener deleteEntry = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FLogger.i(TAG, "user deleted entry from DB");
                            FLogger.d(TAG, "deleted badge entry details: " + entry.toString());
                            DbTablePhoneNumbers.deleteEntry(entry.getBadgeId());
                            updateListView();
                        }
                    };
                    new AlertDialog.Builder(PhoneBookActivity.this)
                            .setTitle("Confirm Delete")
                            .setMessage("Do you really want to delete that entry?")
                            //.setIcon(R.drawable.maybe_a_cool_icon_here)
                            .setPositiveButton(android.R.string.yes, deleteEntry)
                            .setNegativeButton(android.R.string.no, null).show();
                    return true;
                }
            }
        });

        // top area
        textViewBadgeId = (TextView) findViewById(R.id.badgeId);
        textViewCustomName = (TextView) findViewById(R.id.customName);
        textViewPhoneNumber = (TextView) findViewById(R.id.phoneNumber) ;
        textViewNumEntriesInList = (TextView) findViewById(R.id.nEntriesInList);

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

        textViewPhoneNumber.setText(SaveSettingsData.getInstance(context).getPhoneNumber());

        String numEntriesInList;
        synchronized (displayedEntriesLock) {
            numEntriesInList = "" + listAdapterForDisplayedListOfEntries.getCount();
        }
        textViewNumEntriesInList.setText(numEntriesInList);
    }

    public void updateListView() {
        FLogger.v(TAG, "updateListView() called.");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (displayedEntriesLock) {
                    FLogger.v(TAG, "updateListView() doing actual update.");
                    listAdapterForDisplayedListOfEntries.clear();
                    entriesArrList.clear();
                    for (DbTablePhoneNumbers.Entry entry : DbTablePhoneNumbers.getAllEntries()) {
                        listAdapterForDisplayedListOfEntries.add(entry.toString());
                        entriesArrList.add(entry);
                    }
                    listAdapterForDisplayedListOfEntries.notifyDataSetChanged();
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
