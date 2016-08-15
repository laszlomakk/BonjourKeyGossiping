package uk.ac.cam.cl.lm649.bonjourtesting.menu;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class PhoneBookActivity extends CustomActivity {

    private static final String TAG = "PhoneBookActivity";

    private ArrayAdapter<String> listAdapterForDisplayedListOfEntries;
    private ArrayList<DbTablePhoneNumbers.Entry> entriesArrList = new ArrayList<>();
    private final Object displayedEntriesLock = new Object();

    private TextView textViewCustomName;
    private TextView textViewPhoneNumber;
    private TextView textViewNumEntriesInList;

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 10001;

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
                            FLogger.d(TAG, "deleted entry details: " + entry.toString());
                            DbTablePhoneNumbers.deleteEntry(entry.getPhoneNumber());
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
        textViewCustomName = (TextView) findViewById(R.id.customName);
        textViewPhoneNumber = (TextView) findViewById(R.id.phoneNumber);
        textViewNumEntriesInList = (TextView) findViewById(R.id.nEntriesInList);

        // refresh button
        findViewById(R.id.refreshButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.d(TAG, "onClick(). user clicked refresh button.");
                if (doWeHavePermissionToReadContacts()) {
                    FLogger.d(TAG, "onClick(). we have permissions to read contacts.");
                    asyncImportContactsFromSystemToInternalDb();
                    forceRefreshUI();
                } else {
                    FLogger.d(TAG, "onClick(). we don't have permissions to read contacts -> asking now.");
                    askForPermissionToReadContacts();
                }
            }
        });

        forceRefreshUI();
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
        String customName = saveIdentityData.getMyCustomName();
        textViewCustomName.setText(customName);

        String phoneNumber = saveIdentityData.getPhoneNumber();
        textViewPhoneNumber.setText(phoneNumber);

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

    private boolean doWeHavePermissionToReadContacts() {
        return ContextCompat.checkSelfPermission(
                PhoneBookActivity.this,
                Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void askForPermissionToReadContacts() {
        ActivityCompat.requestPermissions(
                PhoneBookActivity.this,
                new String[]{Manifest.permission.READ_CONTACTS},
                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
    }

    private void asyncImportContactsFromSystemToInternalDb() {
        new Thread() {
            @Override
            public void run() {
                Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                if (null != cursor) {
                    int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int phoneNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    cursor.moveToFirst();
                    do {
                        String name = cursor.getString(nameIdx);
                        String phoneNumber = cursor.getString(phoneNumberIdx);
                        DbTablePhoneNumbers.smartUpdateEntry(phoneNumber, name);
                        FLogger.i(TAG, "contact imported - name: " + name +", phoneNum: " + phoneNumber);
                    } while (cursor.moveToNext());
                    cursor.close();
                }
            }
        }.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    FLogger.d(TAG, "onRequestPermissionsResult(). READ_CONTACTS permission was granted");
                    asyncImportContactsFromSystemToInternalDb();
                    forceRefreshUI();
                } else {
                    FLogger.d(TAG, "onRequestPermissionsResult(). READ_CONTACTS permission was denied");
                }
                break;
            }
        }
    }

}
