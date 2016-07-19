package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainMenuActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_view);

        setupButtons();
    }

    private void setupButtons() {
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
    }
}
