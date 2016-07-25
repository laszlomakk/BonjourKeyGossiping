package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MainMenuActivity extends CustomActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_view);

        setupUI();
    }

    private void setupUI() {
        Button btnActiveBadge = (Button) findViewById(R.id.buttonActiveBadge);
        btnActiveBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.ACTIVEBADGE"));
            }
        });

        Button btnBonjourDebug = (Button) findViewById(R.id.buttonBonjourDebug);
        btnBonjourDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.BONJOURDEBUG"));
            }
        });

        Button btnSettings = (Button) findViewById(R.id.buttonSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.SETTINGS"));
            }
        });

        Button btnLicenses = (Button) findViewById(R.id.buttonLicenses);
        btnLicenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.LICENSES"));
            }
        });

        TextView tvVersionNumber = (TextView) findViewById(R.id.textViewVersionNumber);
        tvVersionNumber.setText(HelperMethods.getVersionName(this));
    }
}
