package uk.ac.cam.cl.lm649.bonjourtesting.menu;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.menu.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.menu.settings.SettingsActivity;
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

    @Override
    protected void onStart() {
        super.onStart();

        boolean autoPollingContactsSettingEnabled = SaveSettingsData.getInstance(app).isAutomaticContactPollingEnabled();
        boolean weHaveContactsPermission = HelperMethods.doWeHavePermissionToReadContacts(app);
        if (autoPollingContactsSettingEnabled && !weHaveContactsPermission) {
            // ask for permission -- if denied, disable autoPollingContacts
            FLogger.d(TAG, "onStart(). contact polling is enabled, but we don't have permissions" +
                    "to read contacts -> asking now.");
            HelperMethods.askForPermissionToReadContacts(this);
        }
    }

    private void setupUI() {
        setupPublicKeysButton();
        setupPhoneBookButton();
        setupBonjourDebugButton();
        setupSettingsButton();
        setupLicensesButton();

        TextView tvVersionNumber = (TextView) findViewById(R.id.textViewVersionNumber);
        tvVersionNumber.setText(HelperMethods.getVersionNameExtended(this));
    }

    private void setupPublicKeysButton() {
        Button btn = (Button) findViewById(R.id.buttonPublicKeys);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.PUBLICKEYS"));
            }
        });
    }

    private void setupPhoneBookButton() {
        Button btn = (Button) findViewById(R.id.buttonPhoneBook);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.PHONEBOOK"));
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
        Button btn = (Button) findViewById(R.id.buttonSettings);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FLogger.i(TAG, "user clicked Settings button");
                LayoutInflater li = LayoutInflater.from(context);

                AlertDialog.Builder phoneNumberDialogBuilder = createPhoneNumberPromptBuilder(li, null);
                AlertDialog.Builder customNameDialogBuilder = createCustomNamePromptBuilder(li, phoneNumberDialogBuilder);

                customNameDialogBuilder.create().show();
            }
        });
        btn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.SETTINGS"));
                return true;
            }
        });
    }

    private void setupLicensesButton() {
        Button btn = (Button) findViewById(R.id.buttonLicenses);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent("uk.ac.cam.cl.lm649.bonjourtesting.LICENSES"));
            }
        });
    }

    private AlertDialog.Builder createCustomNamePromptBuilder(LayoutInflater li, final AlertDialog.Builder nextPrompt) {
        View customNamePromptView = li.inflate(R.layout.settings_custom_name_prompt, null);
        final EditText editTextCustomNameInput = (EditText) customNamePromptView.findViewById(R.id.editTextCustomNameInput);
        editTextCustomNameInput.setText(saveIdentityData.getMyCustomName());

        final AlertDialog.Builder customNameDialogBuilder = new AlertDialog.Builder(context)
                .setView(customNamePromptView)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                SettingsActivity.quickRenameBadgeAndService(
                                        context, editTextCustomNameInput.getText().toString());

                                BonjourService bonjourService = app.getBonjourService();
                                if (null != bonjourService) {
                                    bonjourService.restartWork(false);
                                }

                                if (null != nextPrompt) nextPrompt.create().show();
                            }
                        })
                .setNegativeButton(android.R.string.no, null);
        return customNameDialogBuilder;
    }

    private AlertDialog.Builder createPhoneNumberPromptBuilder(LayoutInflater li, final AlertDialog.Builder nextPrompt) {
        View phoneNumberPromptView = li.inflate(R.layout.settings_phone_number_prompt, null);
        final EditText editTextPhoneNumberInput = (EditText) phoneNumberPromptView.findViewById(R.id.editTextPhoneNumberInput);
        editTextPhoneNumberInput.setText(saveIdentityData.getPhoneNumber());

        final AlertDialog.Builder phoneNumberDialogBuilder = new AlertDialog.Builder(context)
                .setView(phoneNumberPromptView)
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                SettingsActivity.quickChangePhoneNumber(
                                        context, editTextPhoneNumberInput.getText().toString());

                                if (null != nextPrompt) nextPrompt.create().show();
                            }
                        })
                .setNegativeButton(android.R.string.no, null);
        return phoneNumberDialogBuilder;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    FLogger.d(TAG, "onRequestPermissionsResult(). READ_CONTACTS permission was granted");
                } else {
                    FLogger.d(TAG, "onRequestPermissionsResult(). READ_CONTACTS permission was denied");
                    saveSettingsData.saveAutomaticContactPollingEnabled(false);
                }
                break;
            }
        }
    }


}
