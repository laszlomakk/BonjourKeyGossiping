package uk.ac.cam.cl.lm649.bonjourtesting;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MainMenuActivity extends CustomActivity {

    private static final String TAG = "MainMenuActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_view);

        setupButtons();
        askUserToIgnoreBatteryOptimisations();
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

        Button btnLicenses = (Button) findViewById(R.id.buttonLicenses);
        btnLicenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.LICENSES"));
            }
        });
    }

    private void askUserToIgnoreBatteryOptimisations() {
        FLogger.i(TAG, "askUserToIgnoreBatteryOptimisations() called. device running API " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT >= 23) {
            FLogger.i(TAG, "battery optimisations are present in this version of Android...");
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)) {
                FLogger.i(TAG, "not exempt from optimisations...");
                try {
                    FLogger.i(TAG, "asking to be white-listed");
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    FLogger.e(TAG, "caught ActivityNotFoundException: " + e.getMessage());
                    askUserToIgnoreBatteryOptimisationsUsingCustomDialog();
                }
            } else {
                FLogger.i(TAG, "we already are exempt from battery optimisations, yay");
            }
        }
    }

    @SuppressLint("NewApi")
    private void askUserToIgnoreBatteryOptimisationsUsingCustomDialog() {
        FLogger.i(TAG, "askUserToIgnoreBatteryOptimisations() called.");
        DialogInterface.OnClickListener goToSettings = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    FLogger.e(TAG, "caught ActivityNotFoundException: " + e.getMessage());
                    FLogger.e(TAG, "failed to ask to opt out of battery optimisations... giving up.");
                    HelperMethods.displayMsgToUser(MainMenuActivity.this, "error opening Battery Optimisations Settings");
                }
            }
        };
        new AlertDialog.Builder(this)
                .setTitle("Battery Optimisations")
                .setMessage("This app needs to be exempt from battery optimisations to function properly. Do you want to turn off optimisations for this app?")
                //.setIcon(R.drawable.cool_icon_to_be_added_later)
                .setPositiveButton(android.R.string.yes, goToSettings)
                .setNegativeButton(android.R.string.no, null).show();
    }

}
