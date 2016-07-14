package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;

public class SettingsActivity extends Activity {

    private CustomApplication app;
    private SaveSettingsData saveSettingsData;

    private EditText editTextFixedServiceNameInput;
    private CheckBox checkBoxRandomServiceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);
        app = (CustomApplication) getApplication();

        saveSettingsData = SaveSettingsData.getInstance(this);

        editTextFixedServiceNameInput = (EditText)findViewById(R.id.editTextFixedServiceNameInput);
        editTextFixedServiceNameInput.setText(saveSettingsData.getCustomServiceName());

        checkBoxRandomServiceName = (CheckBox)findViewById(R.id.checkBoxRandomServiceName);
        checkBoxRandomServiceName.setChecked(saveSettingsData.isUsingRandomServiceName());
    }

    @Override
    public void onBackPressed() {
        saveSettingsData.saveCustomServiceName(editTextFixedServiceNameInput.getText().toString());
        saveSettingsData.saveUsingRandomServiceName(checkBoxRandomServiceName.isChecked());
        if (app.isBonjourServiceBound()) app.getBonjourService().reregisterOurService();
        super.onBackPressed();
    }



}
