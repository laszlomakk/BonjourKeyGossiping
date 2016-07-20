package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.Map;
import java.util.Set;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.BonjourService;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServer;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.util.ServiceStub;

public class ActiveBadgePoller {

    private static final String TAG = "ActiveBadgePoller";
    private static ActiveBadgePoller INSTANCE = null;

    private static final long POLL_PERIOD = 120_000;

    private CustomApplication app;
    private final Handler handler;

    private ActiveBadgePoller() {
        app = CustomApplication.getInstance();

        HandlerThread thread = new HandlerThread("ActBadgePoll-handler");
        thread.start();
        handler = new Handler(thread.getLooper());

        schedulePoll();
    }

    public static synchronized ActiveBadgePoller getInstance() {
        if (null == INSTANCE) {
            INSTANCE = new ActiveBadgePoller();
        }
        return INSTANCE;
    }

    private void schedulePoll() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //pollActiveBadges();
                restartBonjourDiscovery();

                schedulePoll();
            }
        }, POLL_PERIOD);
    }

    private void pollActiveBadges() {
        FLogger.i(TAG, "pollActiveBadges() called.");
        if (!app.isBonjourServiceBound()) {
            FLogger.w(TAG, "bonjourService not bound. This is concerning... going to poll nevertheless");
        }
        Set<Map.Entry<ServiceStub, MsgClient>> entrySet = MsgServer.getInstance().serviceToMsgClientMap.entrySet();
        FLogger.d(TAG, "found " + entrySet.size() + " clients to poll");
        for (Map.Entry<ServiceStub, MsgClient> entry : entrySet) {
            ServiceStub serviceStub = entry.getKey();
            MsgClient msgClient = entry.getValue();
            FLogger.d(TAG, "polling mDNS service: " + serviceStub.name);
            msgClient.sendMessageWhoAreYouQuestion();
        }
    }

    private void restartBonjourDiscovery() {
        FLogger.i(TAG, "restartBonjourDiscovery() called.");
        if (!app.isBonjourServiceBound()) {
            FLogger.e(TAG, "bonjourService not bound.");
            return;
        }
        BonjourService bonjourService = app.getBonjourService();
        bonjourService.restartDiscovery();
    }

}
