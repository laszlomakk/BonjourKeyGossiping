package uk.ac.cam.cl.lm649.bonjourtesting.settings;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class SettingsActivity extends CustomActivity {

    private static final String TAG = "SettingsActivity";

    private SaveSettingsData saveSettingsData;
    private SaveBadgeData saveBadgeData;

    private EditText editTextFixedServiceNameInput;
    private EditText editTextServiceTypeInput;
    private EditText editTextBadgeCustomNameInput;
    private CheckBox checkBoxRandomServiceName;
    private Button buttonRestartBonjourService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);

        saveSettingsData = SaveSettingsData.getInstance(this);
        saveBadgeData = SaveBadgeData.getInstance(this);

        setupUI();
    }

    private void setupUI() {
        editTextFixedServiceNameInput = (EditText)findViewById(R.id.editTextFixedServiceNameInput);
        editTextFixedServiceNameInput.setText(saveSettingsData.getCustomServiceName());

        editTextServiceTypeInput = (EditText)findViewById(R.id.editTextServiceTypeInput);
        editTextServiceTypeInput.setText(saveSettingsData.getServiceType());

        editTextBadgeCustomNameInput = (EditText)findViewById(R.id.editTextBadgeCustomNameInput);
        editTextBadgeCustomNameInput.setText(saveBadgeData.getMyBadgeCustomName());

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
        String serviceType = editTextServiceTypeInput.getText().toString();
        String badgeName = editTextBadgeCustomNameInput.getText().toString();

        FLogger.i(TAG, String.format(Locale.US,
                "serviceName: %s, usingRandServName: %s, serviceType: %s, badgeName: %s",
                serviceName, String.valueOf(usingRandomServiceName), serviceType, badgeName));

        saveSettingsData.saveCustomServiceName(serviceName);
        saveSettingsData.saveUsingRandomServiceName(usingRandomServiceName);
        saveSettingsData.saveServiceType(serviceType);
        saveBadgeData.saveMyBadgeCustomName(badgeName);
    }

    public static void quickRenameBadgeAndService(Context context, String name) {
        FLogger.i(TAG, "quickRenameBadgeAndService() called. name: " + name);
        SaveSettingsData saveSettingsData = SaveSettingsData.getInstance(context);
        saveSettingsData.saveCustomServiceName(name);
        saveSettingsData.saveUsingRandomServiceName(false);
        SaveBadgeData saveBadgeData = SaveBadgeData.getInstance(context);
        saveBadgeData.saveMyBadgeCustomName(name);
    }

}
