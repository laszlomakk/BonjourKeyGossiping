package uk.ac.cam.cl.lm649.bonjourtesting.menu.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;

import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class SettingsActivity extends CustomActivity {

    private static final String TAG = "SettingsActivity";

    private EditText editTextCustomNameInput;
    private EditText editTextPhoneNumberInput;
    private CheckBox checkBoxRandomServiceName;
    private Switch switchMaster;
    private Switch switchAutoContactPoll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);

        setupUI();
    }

    private void setupUI() {
        editTextCustomNameInput = (EditText)findViewById(R.id.editTextCustomNameInput);
        editTextCustomNameInput.setText(saveIdentityData.getMyCustomName());

        editTextPhoneNumberInput = (EditText)findViewById(R.id.editTextPhoneNumberInput);
        editTextPhoneNumberInput.setText(saveIdentityData.getPhoneNumber());

        checkBoxRandomServiceName = (CheckBox)findViewById(R.id.checkBoxRandomServiceName);
        checkBoxRandomServiceName.setChecked(saveSettingsData.isUsingRandomServiceName());

        setupButtonRestartBonjourService();
        setupSwitchMaster();
        setupSwitchAutoContactPoll();
        setupButtonGenerateNewKeyPair();
    }

    private void setupButtonRestartBonjourService() {
        Button button = (Button) findViewById(R.id.buttonRestartBonjourService);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.i(TAG, "user clicked buttonRestartBonjourService");
                BonjourService bonjourService = app.getBonjourService();
                if (null != bonjourService) {
                    saveContentsOfFieldsToStorage();
                    bonjourService.restartWork(false);
                } else {
                    FLogger.e(TAG, "bonjourService is null.");
                    HelperMethods.displayMsgToUser(app, "error: bonjourService is null");
                }
            }
        });
    }

    private void setupButtonGenerateNewKeyPair() {
        Button button = (Button) findViewById(R.id.buttonGenerateNewKeyPair);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.i(TAG, "user clicked buttonGenerateNewKeyPair");
                DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        FLogger.i(TAG, "user decided to regenerate his/her key pair");
                        saveIdentityData.asyncGenerateAndSaveMyKeypair(true);
                    }
                };
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Confirm Key Pair Regeneration")
                        .setMessage("Regenerating your key pair means losing your current one. Are you sure?")
                        //.setIcon(R.drawable.maybe_a_cool_icon_here)
                        .setPositiveButton(android.R.string.yes, clickListener)
                        .setNegativeButton(android.R.string.no, null).show();
            }
        });
    }

    private void setupSwitchMaster() {
        switchMaster = (Switch)findViewById(R.id.masterSwitch);
        switchMaster.setChecked(saveSettingsData.isAppOperationalCoreEnabled());
        switchMaster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchMaster.setEnabled(false);
                boolean oldState = saveSettingsData.isAppOperationalCoreEnabled();
                boolean newState = !oldState;
                FLogger.i(TAG, "user clicked the MASTER SWITCH, new state: " + newState);
                saveSettingsData.saveAppOperationalCoreEnabled(newState);
                if (newState) {
                    app.startupOperationalCore();
                } else {
                    app.shutdownOperationalCore();
                }
                Handler handler = new Handler(getMainLooper());
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switchMaster.setEnabled(true);
                    }
                }, 10_000);
            }
        });
    }

    private void setupSwitchAutoContactPoll() {
        switchAutoContactPoll = (Switch) findViewById(R.id.switchAutoContactPoll);
        switchAutoContactPoll.setChecked(saveSettingsData.isAutomaticContactPollingEnabled());
        switchAutoContactPoll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean oldState = saveSettingsData.isAutomaticContactPollingEnabled();
                FLogger.i(TAG, "user clicked the AUTO CONTACT POLL SWITCH, old state: " + oldState);
                if (oldState) {
                    saveSettingsData.saveAutomaticContactPollingEnabled(false);
                    return;
                }
                if (HelperMethods.doWeHavePermissionToReadContacts(SettingsActivity.this)) {
                    FLogger.d(TAG, "onClick(). we have permissions to read contacts.");
                    saveSettingsData.saveAutomaticContactPollingEnabled(true);
                } else {
                    FLogger.d(TAG, "onClick(). we don't have permissions to read contacts -> asking now.");
                    HelperMethods.askForPermissionToReadContacts(SettingsActivity.this);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        saveContentsOfFieldsToStorage();
        super.onBackPressed();
    }

    private void saveContentsOfFieldsToStorage() {
        FLogger.i(TAG, "saveContentsOfFieldsToStorage() called.");

        boolean usingRandomServiceName = checkBoxRandomServiceName.isChecked();
        String badgeName = editTextCustomNameInput.getText().toString();
        String phoneNumber = editTextPhoneNumberInput.getText().toString();

        FLogger.i(TAG, String.format(Locale.US,
                "usingRandServName: %s, badgeName: %s, phoneNumber: %s",
                String.valueOf(usingRandomServiceName), badgeName, phoneNumber));

        saveSettingsData.saveUsingRandomServiceName(usingRandomServiceName);
        saveIdentityData.saveMyCustomName(badgeName);
        saveIdentityData.savePhoneNumber(phoneNumber);
    }

    public static void quickRenameBadgeAndService(Context context, String name) {
        FLogger.i(TAG, "quickRenameBadgeAndService() called. name: " + name);
        SaveSettingsData saveSettingsData = SaveSettingsData.getInstance(context);
        saveSettingsData.saveUsingRandomServiceName(false);
        SaveIdentityData saveIdentityData = SaveIdentityData.getInstance(context);
        saveIdentityData.saveMyCustomName(name);
    }

    public static void quickChangePhoneNumber(Context context, String phoneNumber) {
        FLogger.i(TAG, "quickChangePhoneNumber() called. phone number: " + phoneNumber);
        SaveIdentityData saveIdentityData = SaveIdentityData.getInstance(context);
        saveIdentityData.savePhoneNumber(phoneNumber);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    FLogger.d(TAG, "onRequestPermissionsResult(). READ_CONTACTS permission was granted");
                    saveSettingsData.saveAutomaticContactPollingEnabled(true);
                } else {
                    FLogger.d(TAG, "onRequestPermissionsResult(). READ_CONTACTS permission was denied");
                    switchAutoContactPoll.setChecked(false);
                }
                break;
            }
        }
    }

}
