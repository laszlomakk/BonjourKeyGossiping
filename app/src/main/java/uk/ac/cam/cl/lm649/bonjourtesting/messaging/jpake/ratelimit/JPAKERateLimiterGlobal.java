package uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake.ratelimit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;

public class JPAKERateLimiterGlobal {

    private static final String TAG = "JPAKERateLimiterGlobal";

    // sorted list of timestamps of all JPAKE handshakes (both directions)
    private final List<Long> globalJpakeHandshakeTimestamps = Collections.synchronizedList(new LinkedList<Long>());
    private static final long GLOBAL_LIMIT_CONSIDERS_TIME_INTERVAL = 30 * Constants.MSECONDS_IN_MINUTE;
    private static final int GLOBAL_LIMIT_ALLOWS_NUM_HANDSHAKES_IN_INTERVAL = 50;

    protected JPAKERateLimiterGlobal() {}

    /**
     * @return whether starting a new handshake is allowed
     */
    protected boolean canStartNewJpakeHandshake() {
        JPAKERateLimiter.deleteOldTimestampsFromList(globalJpakeHandshakeTimestamps, GLOBAL_LIMIT_CONSIDERS_TIME_INTERVAL);

        int numHandshakesDoneInRollingTimePeriod = globalJpakeHandshakeTimestamps.size();
        return numHandshakesDoneInRollingTimePeriod < GLOBAL_LIMIT_ALLOWS_NUM_HANDSHAKES_IN_INTERVAL;
    }

    protected void startedNewJpakeHandshake() {
        long curTime = JPAKERateLimiter.getCurrentTimestamp();
        globalJpakeHandshakeTimestamps.add(curTime);
    }

}
