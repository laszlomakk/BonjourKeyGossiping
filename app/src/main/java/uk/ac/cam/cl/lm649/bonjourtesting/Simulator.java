package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.ActiveBadgePollerService;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
import uk.ac.cam.cl.lm649.bonjourtesting.menu.settings.SettingsActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class Simulator {

    private static final String TAG = "Simulator";
    private static Simulator INSTANCE = null;

    private CustomApplication app;
    private Context context;
    private Handler handler;

    private static final long MINUTE = 60 * 1000;

    private Simulator() {
        app = CustomApplication.getInstance();
        context = app;

        HandlerThread thread = new HandlerThread("Simulator-handler");
        thread.start();
        handler = new Handler(thread.getLooper());

        simulateAPerson();
    }

    public static synchronized Simulator getInstance() {
        if (null == INSTANCE) {
            INSTANCE = new Simulator();
        }
        return INSTANCE;
    }

    private void simulateAPerson() {
        final long appearsAtTime = HelperMethods.getRandomLongBetween(10 * MINUTE, 20 * MINUTE);
        final long staysForTime = HelperMethods.getRandomLongBetween(10 * MINUTE, 30 * MINUTE);
        final long disappearsAtTime = appearsAtTime + staysForTime;
        FLogger.i(TAG, "simulateAPerson(). appearsAtTime: " + appearsAtTime/MINUTE
                + ", staysForTime: " + staysForTime/MINUTE);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                appearOnNetworkWithNewId(staysForTime);
            }
        }, appearsAtTime);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                disappearFromNetwork();
                simulateAPerson();
            }
        }, disappearsAtTime);
    }

    private void appearOnNetworkWithNewId(final long staysForTime) {
        FLogger.i(TAG, "appearOnNetworkWithNewId() called. Will stay for " + staysForTime/MINUTE + " mins");

        SaveBadgeData.getInstance(context).deleteMyBadge();
        SaveBadgeData.getInstance(context).getMyBadgeId();
        String newName = "name_" + HelperMethods.getRandomString();
        SettingsActivity.quickRenameBadgeAndService(context, newName);

        try {
            MsgServer.getInstance().start();
        } catch (IOException e) {
            FLogger.e(TAG, "appearOnNetworkWithNewId(). Failed to start MsgServer. IOE - " + e.getMessage());
        }

        BonjourService bonjourService = app.getBonjourService();
        if (null == bonjourService) {
            FLogger.e(TAG, "appearOnNetworkWithNewId(). bonjourService is null.");
            return;
        }
        bonjourService.restartWork(false);

        Thread poller = new Thread() {
            @Override
            public void run() {
                long numPolls = staysForTime / ActiveBadgePollerService.POLL_PERIOD - 1;
                for (int poll = 0; poll < numPolls; poll++) {
                    ActiveBadgePollerService.schedulePolling(context);
                    try {
                        Thread.sleep(ActiveBadgePollerService.POLL_PERIOD);
                    } catch (InterruptedException e) {
                        FLogger.e(TAG, "appearOnNetworkWithNewId(). Sleep between polls interrupted - " + e.getMessage());
                    }
                }
            }
        };
        poller.start();
    }

    private void disappearFromNetwork() {
        FLogger.i(TAG, "disappearFromNetwork() called.");

        BonjourService bonjourService = app.getBonjourService();
        if (null == bonjourService) {
            FLogger.e(TAG, "disappearFromNetwork(). bonjourService is null.");
            return;
        }
        bonjourService.stopAndCloseWork();

        MsgServer.getInstance().stop();
    }

}
