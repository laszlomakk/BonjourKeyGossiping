package uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.ratelimit;

import android.support.annotation.Nullable;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class JPAKERateLimiterIndividual {

    private static final String TAG = "JPAKERateLimiterIndivid";

    // key: MAC address
    // value: sorted list of timestamps of failed JPAKE handshakes since the last successful handshake
    // TODO periodically iterate through map to delete old values, ensuring its size stays reasonable
    protected final ConcurrentSkipListMap<String, List<Long>> individualJpakeHandshakeTimestamps = new ConcurrentSkipListMap<>();
    private static final long INDIVIDUAL_LIMIT_CONSIDERS_TIME_INTERVAL = 30 * Constants.MSECONDS_IN_MINUTE;
    private static final int INDIVIDUAL_LIMIT_ALLOWS_NUM_HANDSHAKES_IN_INTERVAL = 10;

    protected JPAKERateLimiterIndividual() {}

    /**
     * @param ipAddress IP of other participant
     * @param macAddress MAC of other participant
     * @return whether starting a new handshake is allowed
     */
    protected boolean canStartNewJpakeHandshake(@Nullable InetAddress ipAddress, @Nullable String macAddress) {
        String strIpAddress = null == ipAddress ? "null" : ipAddress.getHostAddress();

        if (null == macAddress) {
            FLogger.e(TAG, "canStartNewJpakeHandshake(). macAddress == null. (IP: " + strIpAddress + ")");
        } else {
            List<Long> individualTimestamps = individualJpakeHandshakeTimestamps.get(macAddress);
            if (null == individualTimestamps) {
                individualTimestamps = Collections.synchronizedList(new LinkedList<Long>());
                individualJpakeHandshakeTimestamps.put(macAddress, individualTimestamps);
            }

            JPAKERateLimiter.deleteOldTimestampsFromList(individualTimestamps, INDIVIDUAL_LIMIT_CONSIDERS_TIME_INTERVAL);
            int numHandshakesDoneInRollingTimePeriod = individualTimestamps.size();
            if (numHandshakesDoneInRollingTimePeriod >= INDIVIDUAL_LIMIT_ALLOWS_NUM_HANDSHAKES_IN_INTERVAL) {
                return false;
            }
        }

        return true;
    }

    protected void startedNewJpakeHandshake(@Nullable InetAddress ipAddress, @Nullable String macAddress) {
        String strIpAddress = null == ipAddress ? "null" : ipAddress.getHostAddress();

        long curTime = JPAKERateLimiter.getCurrentTimestamp();
        if (null == macAddress) {
            FLogger.e(TAG, "startedNewJpakeHandshake(). macAddress == null. (IP: " + strIpAddress + ")");
        } else {
            List<Long> individualTimestamps = individualJpakeHandshakeTimestamps.get(macAddress);
            if (null == individualTimestamps) {
                FLogger.e(TAG, "startedNewJpakeHandshake(). wtf. individualTimestamps == null");
            } else {
                individualTimestamps.add(curTime);
            }
        }
    }

}
