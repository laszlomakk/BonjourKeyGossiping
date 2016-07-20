package uk.ac.cam.cl.lm649.bonjourtesting.settings;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class SettingsActivity extends CustomActivity {

    private static final String TAG = "SettingsActivity";

    private SaveSettingsData saveSettingsData;
    private SaveBadgeData saveBadgeData;

    private EditText editTextFixedServiceNameInput;
    private EditText editTextBadgeCustomNameInput;
    private CheckBox checkBoxRandomServiceName;
    private Button buttonReRegisterService;

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

        editTextBadgeCustomNameInput = (EditText)findViewById(R.id.editTextBadgeCustomNameInput);
        editTextBadgeCustomNameInput.setText(saveBadgeData.getMyBadgeCustomName());

        checkBoxRandomServiceName = (CheckBox)findViewById(R.id.checkBoxRandomServiceName);
        checkBoxRandomServiceName.setChecked(saveSettingsData.isUsingRandomServiceName());

        buttonReRegisterService = (Button) findViewById(R.id.buttonReRegisterService);
        buttonReRegisterService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "user clicked buttonReRegisterService");
                if (app.isBonjourServiceBound()) {
                    saveContentsOfFieldsToStorage();
                    app.getBonjourService().reregisterOurService();
                } else {
                    Log.e(TAG, "bonjourService not bound");
                    HelperMethods.displayMsgToUser(app, "error: bonjourService not bound");
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
        saveSettingsData.saveCustomServiceName(editTextFixedServiceNameInput.getText().toString());
        saveSettingsData.saveUsingRandomServiceName(checkBoxRandomServiceName.isChecked());
        saveBadgeData.saveMyBadgeCustomName(editTextBadgeCustomNameInput.getText().toString());
    }

}
