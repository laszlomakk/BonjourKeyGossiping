package uk.ac.cam.cl.lm649.bonjourtesting;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.IOException;
import java.util.Random;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
import uk.ac.cam.cl.lm649.bonjourtesting.settings.SettingsActivity;
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
        final long appearsAtTime = HelperMethods.getRandomLongBetween(1 * MINUTE, 3 * MINUTE);
        final long staysForTime = HelperMethods.getRandomLongBetween(1 * MINUTE, 3 * MINUTE);
        final long disappearsAtTime = appearsAtTime + staysForTime;
        FLogger.i(TAG, "simulateAPerson(). appearsAtTime: " + appearsAtTime/MINUTE
                + ", staysForTime: " + staysForTime/MINUTE);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                appearOnNetworkWithNewId();
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

    private void appearOnNetworkWithNewId() {
        FLogger.i(TAG, "appearOnNetworkWithNewId() called.");

        SaveBadgeData.getInstance(context).deleteMyBadge();
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
