package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.settings.SettingsActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class MainMenuActivity extends CustomActivity {

    private static final String TAG = "MainMenuActivity";
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.main_menu_view);

        setupUI();
    }

    private void setupUI() {
        setupActiveBadgeButton();
        setupBonjourDebugButton();
        setupSettingsButton();
        setupLicensesButton();

        TextView tvVersionNumber = (TextView) findViewById(R.id.textViewVersionNumber);
        tvVersionNumber.setText(HelperMethods.getVersionNameExtended(this));
    }

    private void setupActiveBadgeButton() {
        Button btnActiveBadge = (Button) findViewById(R.id.buttonActiveBadge);
        btnActiveBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.ACTIVEBADGE"));
            }
        });
    }

    private void setupBonjourDebugButton() {
        Button btnBonjourDebug = (Button) findViewById(R.id.buttonBonjourDebug);
        btnBonjourDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.BONJOURDEBUG"));
            }
        });
    }

    private void setupSettingsButton() {
        Button btnSettings = (Button) findViewById(R.id.buttonSettings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.i(TAG, "user clicked Settings button");
                LayoutInflater li = LayoutInflater.from(context);
                View promptView = li.inflate(R.layout.settings_custom_name_prompt, null);
                final EditText userInput = (EditText) promptView.findViewById(R.id.editTextCustomNameInput);

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context)
                        .setView(promptView)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,int id) {
                                        SettingsActivity.quickRenameBadgeAndService(
                                                context, userInput.getText().toString());

                                        BonjourService bonjourService = app.getBonjourService();
                                        if (null != bonjourService) {
                                            bonjourService.restartWork(false);
                                        }
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null);
                alertDialogBuilder.create().show();
            }
        });
        btnSettings.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.SETTINGS"));
                return true;
            }
        });
    }

    private void setupLicensesButton() {
        Button btnLicenses = (Button) findViewById(R.id.buttonLicenses);
        btnLicenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.LICENSES"));
            }
        });
    }


}
