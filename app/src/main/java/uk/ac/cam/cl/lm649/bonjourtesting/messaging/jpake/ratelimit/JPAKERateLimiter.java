package uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.ratelimit;

import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.InetAddress;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.JPAKEClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public final class JPAKERateLimiter {

    private static final String TAG = "JPAKERateLimiter";

    private static JPAKERateLimiter INSTANCE = null;

    private final JPAKERateLimiterGlobal jpakeRateLimiterGlobal;
    private final JPAKERateLimiterIndividual jpakeRateLimiterIndividual;

    private JPAKERateLimiter() {
        jpakeRateLimiterGlobal = new JPAKERateLimiterGlobal();
        jpakeRateLimiterIndividual = new JPAKERateLimiterIndividual();
    }

    public static synchronized JPAKERateLimiter getInstance() {
        if (null == INSTANCE) {
            INSTANCE = new JPAKERateLimiter();
        }
        return INSTANCE;
    }

    /**
     * @param iAmTheJpakeInitiator the value of iAmTheInitiator in the desired JPAKEClient
     * @param ipAddress IP of other participant
     * @param macAddress MAC of other participant
     * @return whether starting a new handshake is allowed (note that a positive answer has side-effects,
     *         namely, it assumes we are starting the handshake)
     */
    public synchronized boolean startNewJpakeHandshake(
            boolean iAmTheJpakeInitiator, @Nullable InetAddress ipAddress, @Nullable String macAddress)
    {
        String strIpAddress = null == ipAddress ? "null" : ipAddress.getHostAddress();

        if (!jpakeRateLimiterGlobal.canStartNewJpakeHandshake()) {
            FLogger.w(TAG, String.format(Locale.US,
                    "startNewJpakeHandshake(). DENIED new handshake with IP %s, MAC %s due to GLOBAL limit",
                    strIpAddress, macAddress));
            return false;
        }

        if (!iAmTheJpakeInitiator && !jpakeRateLimiterIndividual.canStartNewJpakeHandshake(ipAddress, macAddress)) {
            FLogger.w(TAG, String.format(Locale.US,
                    "startNewJpakeHandshake(). DENIED new handshake with IP %s, MAC %s due to INDIVIDUAL limit",
                    strIpAddress, macAddress));
            return false;
        }

        FLogger.i(TAG, String.format(Locale.US,
                "startNewJpakeHandshake(). ALLOWED new handshake with IP %s, MAC %s",
                strIpAddress, macAddress));
        jpakeRateLimiterGlobal.startedNewJpakeHandshake();
        if (!iAmTheJpakeInitiator) {
            jpakeRateLimiterIndividual.startedNewJpakeHandshake(ipAddress, macAddress);
        }
        return true;
    }

    public boolean startNewJpakeHandshake(boolean iAmTheJpakeInitiator, @Nullable MsgClient msgClient) {
        if (null == msgClient) {
            FLogger.e(TAG, "startNewJpakeHandshake(). msgClient == null");
            return false;
        }
        return startNewJpakeHandshake(iAmTheJpakeInitiator, msgClient.getSocketAddress(), msgClient.getMacAddress());
    }

    public void notifyRegardingSuccessfulJpake(@Nullable JPAKEClient jpakeClient, @Nullable String macAddress) {
        if (null == jpakeClient) {
            FLogger.e(TAG, "notifyRegardingSuccessfulJpake(). jpakeClient == null");
            return;
        }
        if (null == macAddress) {
            FLogger.e(TAG, "notifyRegardingSuccessfulJpake(). macAddress == null");
            return;
        }

        if (!jpakeClient.iAmTheInitiator) {
            List<Long> individualTimestamps = jpakeRateLimiterIndividual.individualJpakeHandshakeTimestamps.get(macAddress);
            if (null == individualTimestamps) {
                FLogger.e(TAG, "notifyRegardingSuccessfulJpake(). individualTimestamps == null");
                return;
            }
            individualTimestamps.clear();
        }
    }

    protected static void deleteOldTimestampsFromList(@NonNull final List<Long> timestampList, long durationOfRollingInterval) {
        long curTime = getCurrentTimestamp();
        synchronized (timestampList) {
            ListIterator<Long> iterator = timestampList.listIterator();
            while (iterator.hasNext()) {
                Long timestamp = iterator.next();
                if (timestamp < curTime - durationOfRollingInterval) {
                    iterator.remove();
                } else {
                    break;
                }
            }
        }
    }

    protected static long getCurrentTimestamp() {
        return SystemClock.elapsedRealtime();
    }

}
