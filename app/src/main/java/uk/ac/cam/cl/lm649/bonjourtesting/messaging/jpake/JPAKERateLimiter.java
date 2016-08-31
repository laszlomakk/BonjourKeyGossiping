package uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake;

import android.os.SystemClock;
import android.support.annotation.Nullable;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public final class JPAKERateLimiter {

    private static final String TAG = "JPAKERateLimiter";

    private static JPAKERateLimiter INSTANCE = null;

    private final List<Long> globalJpakeHandshakeTimestamps = Collections.synchronizedList(new LinkedList<Long>());
    private static final long GLOBAL_LIMIT_CONSIDERS_TIME_INTERVAL = 30 * Constants.MSECONDS_IN_MINUTE;
    private static final long GLOBAL_LIMIT_ALLOWS_NUM_HANDSHAKES_IN_INTERVAL = 50;

    private JPAKERateLimiter() {}

    public static synchronized JPAKERateLimiter getInstance() {
        if (null == INSTANCE) {
            INSTANCE = new JPAKERateLimiter();
        }
        return INSTANCE;
    }

    /**
     * @param ipAddress IP of other participant
     * @param macAddress MAC of other participant
     * @return whether starting a new handshake is allowed (note that a positive answer has side-effects)
     */
    public synchronized boolean startNewJpakeHandshake(@Nullable InetAddress ipAddress, @Nullable String macAddress) {
        String strIpAddress = null == ipAddress ? "null" : ipAddress.getHostAddress();

        deleteOldValuesFromGlobalJpakeHandshakeTimestamps();

        long numHandshakesDoneInRollingTimePeriod = globalJpakeHandshakeTimestamps.size();
        if (numHandshakesDoneInRollingTimePeriod >= GLOBAL_LIMIT_ALLOWS_NUM_HANDSHAKES_IN_INTERVAL) {
            FLogger.w(TAG, String.format(Locale.US,
                    "startNewJpakeHandshake(). DENIED new handshake with IP %s, MAC %s due to GLOBAL limit",
                    strIpAddress, macAddress));
            return false;
        }

        long curTime = SystemClock.elapsedRealtime();
        globalJpakeHandshakeTimestamps.add(curTime);
        FLogger.i(TAG, String.format(Locale.US,
                "startNewJpakeHandshake(). ALLOWED new handshake with IP %s, MAC %s",
                strIpAddress, macAddress));
        return true;
    }

    public boolean startNewJpakeHandshake(@Nullable MsgClient msgClient) {
        if (null == msgClient) {
            FLogger.e(TAG, "startNewJpakeHandshake(). msgClient == null");
            return false;
        }
        return startNewJpakeHandshake(msgClient.getSocketAddress(), msgClient.getMacAddress());
    }

    private void deleteOldValuesFromGlobalJpakeHandshakeTimestamps() {
        long curTime = SystemClock.elapsedRealtime();
        synchronized (globalJpakeHandshakeTimestamps) {
            ListIterator<Long> iterator = globalJpakeHandshakeTimestamps.listIterator();
            while (iterator.hasNext()) {
                Long timestamp = iterator.next();
                if (timestamp < curTime - GLOBAL_LIMIT_CONSIDERS_TIME_INTERVAL) {
                    iterator.remove();
                } else {
                    break;
                }
            }
        }
    }

}
