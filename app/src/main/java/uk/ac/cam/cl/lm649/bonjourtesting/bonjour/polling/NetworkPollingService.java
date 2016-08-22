package uk.ac.cam.cl.lm649.bonjourtesting.bonjour.polling;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class NetworkPollingService extends IntentService {

    private static final String TAG = "NetworkPollingService";

    private CustomApplication app;
    private static final long TIME_TO_KEEP_DEVICE_AWAKE = 15 * Constants.MSECONDS_IN_SECOND;
    private static final long POLL_PERIOD = 2 * Constants.MSECONDS_IN_MINUTE;

    public static boolean automaticPollingEnabled = true;

    public NetworkPollingService() {
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
        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG + "-wakelock");
        wakeLock.acquire();

        try {
            doPolling();
            FLogger.i(TAG, "onHandleIntent(). Sleeping for a bit while keeping WakeLocks. " +
                    "Hoping for other threads to progress.");
            Thread.sleep(TIME_TO_KEEP_DEVICE_AWAKE);
        } catch (InterruptedException e) {
            FLogger.e(TAG, "onHandleIntent(). Sleep interrupted - " + e.getMessage());
            FLogger.e(TAG, e);
        } finally {
            if (automaticPollingEnabled) {
                FLogger.d(TAG, "onHandleIntent() finishing. Scheduling next poll.");
                schedulePolling(app);
            }
            FLogger.i(TAG, "onHandleIntent() finishing. Releasing WakeLocks.");
            wakeLock.release();
            NetworkPollingReceiver.completeWakefulIntent(intent);
        }
    }

    private void doPolling() {
        BonjourService bonjourService = app.getBonjourService();
        if (null == bonjourService) {
            FLogger.e(TAG, "bonjourService is null.");
            return;
        }
        bonjourService.restartDiscovery();
    }

    public static void schedulePolling(Context context) {
        FLogger.d(TAG, "schedulePolling() called.");
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(appContext, NetworkPollingReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        long triggerAtMillis = SystemClock.elapsedRealtime() + POLL_PERIOD;
        setAlarm(alarmManager, pendingIntent, triggerAtMillis);
        FLogger.i(TAG, "schedulePolling(). Next polling in " + POLL_PERIOD/1000 + " seconds");
    }

    public static void cancelPolling(Context context) {
        FLogger.i(TAG, "cancelPolling() called.");
        Context appContext = context.getApplicationContext();
        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(appContext, NetworkPollingReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
    }

    @SuppressLint("NewApi")
    private static void setAlarm(AlarmManager alarmManager, PendingIntent pendingIntent, long triggerAtMillis) {
        if (Build.VERSION.SDK_INT < 19) {
            FLogger.d(TAG, "alarm set using: alarmManager.set");
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis, pendingIntent);
        } else if (Build.VERSION.SDK_INT < 23) {
            FLogger.d(TAG, "alarm set using: alarmManager.setExact");
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis, pendingIntent);
        } else {
            FLogger.d(TAG, "alarm set using: alarmManager.setExactAndAllowWhileIdle");
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerAtMillis, pendingIntent);
        }
    }

}