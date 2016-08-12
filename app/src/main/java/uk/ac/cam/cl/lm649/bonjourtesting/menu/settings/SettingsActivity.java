package uk.ac.cam.cl.lm649.bonjourtesting.menu.settings;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Switch;

import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class SettingsActivity extends CustomActivity {

    private static final String TAG = "SettingsActivity";

    private EditText editTextFixedServiceNameInput;
    private EditText editTextBadgeCustomNameInput;
    private EditText editTextPhoneNumberInput;
    private CheckBox checkBoxRandomServiceName;
    private Button buttonRestartBonjourService;
    private Switch switchMaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);

        setupUI();
    }

    private void setupUI() {
        editTextFixedServiceNameInput = (EditText)findViewById(R.id.editTextFixedServiceNameInput);
        editTextFixedServiceNameInput.setText(saveSettingsData.getCustomServiceName());

        editTextBadgeCustomNameInput = (EditText)findViewById(R.id.editTextBadgeCustomNameInput);
        editTextBadgeCustomNameInput.setText(saveIdentityData.getMyCustomName());

        editTextPhoneNumberInput = (EditText)findViewById(R.id.editTextPhoneNumberInput);
        editTextPhoneNumberInput.setText(saveIdentityData.getPhoneNumber());

        checkBoxRandomServiceName = (CheckBox)findViewById(R.id.checkBoxRandomServiceName);
        checkBoxRandomServiceName.setChecked(saveSettingsData.isUsingRandomServiceName());

        buttonRestartBonjourService = (Button) findViewById(R.id.buttonRestartBonjourService);
        buttonRestartBonjourService.setOnClickListener(new View.OnClickListener() {
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

    @Override
    public void onBackPressed() {
        saveContentsOfFieldsToStorage();
        super.onBackPressed();
    }

    private void saveContentsOfFieldsToStorage() {
        FLogger.i(TAG, "saveContentsOfFieldsToStorage() called.");

        String serviceName = editTextFixedServiceNameInput.getText().toString();
        boolean usingRandomServiceName = checkBoxRandomServiceName.isChecked();
        String badgeName = editTextBadgeCustomNameInput.getText().toString();
        String phoneNumber = editTextPhoneNumberInput.getText().toString();

        FLogger.i(TAG, String.format(Locale.US,
                "serviceName: %s, usingRandServName: %s, badgeName: %s, phoneNumber: %s",
                serviceName, String.valueOf(usingRandomServiceName), badgeName, phoneNumber));

        saveSettingsData.saveCustomServiceName(serviceName);
        saveSettingsData.saveUsingRandomServiceName(usingRandomServiceName);
        saveIdentityData.saveMyCustomName(badgeName);
        saveIdentityData.savePhoneNumber(phoneNumber);
    }

    public static void quickRenameBadgeAndService(Context context, String name) {
        FLogger.i(TAG, "quickRenameBadgeAndService() called. name: " + name);
        SaveSettingsData saveSettingsData = SaveSettingsData.getInstance(context);
        saveSettingsData.saveCustomServiceName(name);
        saveSettingsData.saveUsingRandomServiceName(false);
        SaveIdentityData saveIdentityData = SaveIdentityData.getInstance(context);
        saveIdentityData.saveMyCustomName(name);
    }

    public static void quickChangePhoneNumber(Context context, String phoneNumber) {
        FLogger.i(TAG, "quickChangePhoneNumber() called. phone number: " + phoneNumber);
        SaveIdentityData saveIdentityData = SaveIdentityData.getInstance(context);
        saveIdentityData.savePhoneNumber(phoneNumber);
    }

}
