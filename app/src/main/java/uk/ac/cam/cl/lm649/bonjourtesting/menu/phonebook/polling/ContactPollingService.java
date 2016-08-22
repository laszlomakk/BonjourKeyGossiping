package uk.ac.cam.cl.lm649.bonjourtesting.menu.phonebook.polling;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.menu.phonebook.PhoneBookActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.menu.settings.SaveSettingsData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class ContactPollingService extends IntentService {

    private static final String TAG = "ContactPollingService";

    private CustomApplication app;
    private static final long INITIAL_DELAY = 5 * Constants.MSECONDS_IN_MINUTE;
    private static final long POLL_PERIOD = AlarmManager.INTERVAL_HALF_DAY;

    public ContactPollingService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        FLogger.d(TAG, "onCreate() called.");
        super.onCreate();

        app = (CustomApplication) getApplication();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        FLogger.d(TAG, "onHandleIntent() called.");
        doPolling();
    }

    private void doPolling() {
        boolean autoPollingSettingEnabled = SaveSettingsData.getInstance(app).isAutomaticContactPollingEnabled();
        boolean weHaveContactsPermission = HelperMethods.doWeHavePermissionToReadContacts(app);
        FLogger.d(TAG, "doPolling(). autoPollingSettingEnabled: " + autoPollingSettingEnabled
                + ", weHaveContactsPermission: " + weHaveContactsPermission);
        if (!autoPollingSettingEnabled || !weHaveContactsPermission) {
            FLogger.i(TAG, "Won't poll. Either setting is disabled or we don't have necessary permission.");
            SaveSettingsData.getInstance(app).saveAutomaticContactPollingEnabled(false);
            return;
        }
        PhoneBookActivity.asyncImportContactsFromSystemToInternalDb(app);
    }

    public static void schedulePolling(Context context) {
        FLogger.i(TAG, "schedulePolling() called.");
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(appContext, ContactPollingReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        long triggerAtMillis = SystemClock.elapsedRealtime() + INITIAL_DELAY;
        setAlarm(alarmManager, pendingIntent, triggerAtMillis);
    }

    private static void setAlarm(AlarmManager alarmManager, PendingIntent pendingIntent, long triggerAtMillis) {
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtMillis, POLL_PERIOD, pendingIntent);
    }

}
