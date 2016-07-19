package uk.ac.cam.cl.lm649.bonjourtesting.settings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class SettingsActivity extends Activity {

    private static final String TAG = "SettingsActivity";

    private CustomApplication app;
    private SaveSettingsData saveSettingsData;

    private EditText editTextFixedServiceNameInput;
    private CheckBox checkBoxRandomServiceName;
    private Button buttonReRegisterService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);
        app = (CustomApplication) getApplication();

        saveSettingsData = SaveSettingsData.getInstance(this);

        setupUI();
    }

    private void setupUI() {
        editTextFixedServiceNameInput = (EditText)findViewById(R.id.editTextFixedServiceNameInput);
        editTextFixedServiceNameInput.setText(saveSettingsData.getCustomServiceName());

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
    }

}
