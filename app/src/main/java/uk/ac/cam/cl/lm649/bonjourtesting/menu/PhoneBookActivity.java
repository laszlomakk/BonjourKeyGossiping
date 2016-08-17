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
import android.telephony.PhoneNumberUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTablePhoneNumbers;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;
import uk.ac.cam.cl.lm649.bonjourtesting.util.PhoneNumUtil;

public class PhoneBookActivity extends CustomActivity {

    private static final String TAG = "PhoneBookActivity";

    private ArrayAdapter<String> listAdapterForDisplayedListOfEntries;
    private ArrayList<DbTablePhoneNumbers.Entry> entriesArrList = new ArrayList<>();
    private final Object displayedEntriesLock = new Object();

    private TextView textViewCustomName;
    private TextView textViewPhoneNumber;
    private TextView textViewNumEntriesInList;

    private String phoneNumberOfLocalDevice;

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
                    FLogger.i(TAG, "onItemLongClick() - user clicked on an item in the list");
                    final DbTablePhoneNumbers.Entry entry = entriesArrList.get(position);
                    if (null == entry){
                        FLogger.e(TAG, "onItemLongClick(). clicked entry is null ??");
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

        // buttons
        setupRefreshButton();
        setupAddContactButton();

        forceRefreshUI();
    }

    private void setupRefreshButton() {
        View refreshButton = findViewById(R.id.buttonRefresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.d(TAG, "onClick(). user clicked refresh button.");
                if (doWeHavePermissionToReadContacts()) {
                    FLogger.d(TAG, "onClick(). we have permissions to read contacts.");
                    asyncImportContactsFromSystemToInternalDb();
                } else {
                    FLogger.d(TAG, "onClick(). we don't have permissions to read contacts -> asking now.");
                    askForPermissionToReadContacts();
                }
            }
        });
        refreshButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                synchronized (displayedEntriesLock) {
                    FLogger.i(TAG, "onLongClick(). user long-clicked refresh button.");
                    DialogInterface.OnClickListener deleteAllEntries = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FLogger.i(TAG, "user deleted all entries from phone book DB");
                            DbTablePhoneNumbers.deleteAllEntries();
                            forceRefreshUI();
                        }
                    };
                    new AlertDialog.Builder(PhoneBookActivity.this)
                            .setTitle("Confirm Delete All")
                            .setMessage("Do you really want to delete all entries from the internal phone book DB?")
                            //.setIcon(R.drawable.maybe_a_cool_icon_here)
                            .setPositiveButton(android.R.string.yes, deleteAllEntries)
                            .setNegativeButton(android.R.string.no, null).show();

                    return true;
                }
            }
        });
    }

    private void setupAddContactButton() {
        View addContactButton = findViewById(R.id.buttonAddContact);
        addContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.i(TAG, "onClick(). user clicked Add Contact button");
                AlertDialog.Builder addContactDialogBuilder = createAddContactPromptBuilder(true);
                addContactDialogBuilder.create().show();
            }
        });
        addContactButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                FLogger.i(TAG, "onLongClick(). user long-clicked Add Contact button");
                AlertDialog.Builder addContactDialogBuilder = createAddContactPromptBuilder(false);
                addContactDialogBuilder.create().show();
                return true;
            }
        });
    }

    private AlertDialog.Builder createAddContactPromptBuilder(final boolean sanitizePhoneNumber) {
        LayoutInflater li = LayoutInflater.from(PhoneBookActivity.this);
        View promptView = li.inflate(R.layout.phone_book_add_contact_prompt, null);
        final EditText editTextNameInput = (EditText) promptView.findViewById(R.id.editTextNameInput);
        final EditText editTextPhoneNumberInput = (EditText) promptView.findViewById(R.id.editTextPhoneNumberInput);

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PhoneBookActivity.this)
                .setView(promptView)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                String contactName = editTextNameInput.getText().toString();
                                String contactPhoneNumber = editTextPhoneNumberInput.getText().toString();
                                if (sanitizePhoneNumber) {
                                    contactPhoneNumber = PhoneNumUtil.formatPhoneNumber(contactPhoneNumber, phoneNumberOfLocalDevice);
                                }
                                DbTablePhoneNumbers.smartUpdateEntry(contactPhoneNumber, contactName);
                                forceRefreshUI();
                            }
                        })
                .setNegativeButton(android.R.string.no, null);
        return dialogBuilder;
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

        phoneNumberOfLocalDevice = saveIdentityData.getPhoneNumber();
        textViewPhoneNumber.setText(phoneNumberOfLocalDevice);

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
                synchronized (displayedEntriesLock) {
                    Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
                    if (null != cursor) {
                        int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                        int phoneNumberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        FLogger.i(TAG, "importing contacts from system...");
                        cursor.moveToFirst();
                        do {
                            String name = cursor.getString(nameIdx);
                            String phoneNumber = cursor.getString(phoneNumberIdx);
                            String normalisedPhoneNumber = PhoneNumUtil.formatPhoneNumber(phoneNumber, phoneNumberOfLocalDevice);
                            DbTablePhoneNumbers.smartUpdateEntry(normalisedPhoneNumber, name);
                            FLogger.d(TAG, String.format(Locale.US,
                                    "contact imported - name: %s, phone number: %s -> %s",
                                    name, phoneNumber, normalisedPhoneNumber));
                        } while (cursor.moveToNext());
                        cursor.close();
                    }
                    forceRefreshUI();
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
                } else {
                    FLogger.d(TAG, "onRequestPermissionsResult(). READ_CONTACTS permission was denied");
                }
                break;
            }
        }
    }

}
