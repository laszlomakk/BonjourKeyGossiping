package uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake;

import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class JPAKEManager {

    public static final String TAG = "JPAKEManager";

    private final ConcurrentSkipListMap<UUID, JPAKEClient> handshakeIdToClientMap = new ConcurrentSkipListMap<>();

    public synchronized boolean canNewJPAKEWaveBeStarted() {
        for (JPAKEClient jpakeClient : handshakeIdToClientMap.values()) {
            if (jpakeClient.isInProgress()) {
                return false;
            }
        }
        return true;
    }

    public static boolean startJPAKEWave(MsgClient msgClient) {
        FLogger.i(TAG, "startJPAKEWave() called.");
        JPAKEManager jpakeManager = msgClient.jpakeManager;
        if (!jpakeManager.canNewJPAKEWaveBeStarted()) {
            FLogger.i(TAG, "startJPAKEWave(). can't start new wave.");
            return false;
        }

        String sharedSecret = determineSharedSecret();
        return startJPAKEHandshake(msgClient, sharedSecret);
    }

    /**
     * Tries to start JPAKE using the given MsgClient instance from the init / round 1 sending phase.
     *
     * @return if JPAKE was started
     */
    private static boolean startJPAKEHandshake(MsgClient msgClient, String sharedSecret) {
        FLogger.d(TAG, "startJPAKEHandshake() called.");
        if (null == sharedSecret) {
            FLogger.w(TAG, "startJPAKEHandshake(). sharedSecret is null.");
            return false;
        }
        JPAKEManager jpakeManager = msgClient.jpakeManager;
        try {
            UUID handshakeId = UUID.randomUUID();
            JPAKEClient jpakeClient = new JPAKEClient(true, handshakeId, sharedSecret);
            jpakeManager.handshakeIdToClientMap.put(handshakeId, jpakeClient);
            return jpakeClient.round1Send(msgClient);
        } catch (IOException e) {
            FLogger.e(TAG, "startJPAKEHandshake(). IOE - " + e.getMessage());
        }
        return false;
    }

    @Nullable
    public synchronized JPAKEClient findJPAKEClient(UUID handshakeId) {
        return handshakeIdToClientMap.get(handshakeId);
    }

    public synchronized JPAKEClient createJPAKEClientDueToIncomingMessage(UUID handshakeId) {
        JPAKEClient oldJpakeClient = handshakeIdToClientMap.get(handshakeId);
        if (null != oldJpakeClient) {
            FLogger.e(TAG, "createJPAKEClientDueToIncomingMessage(). Found an already existing JPAKEClient" +
                    "with this handshakeId (" + handshakeId + ") !!");
            return oldJpakeClient;
        }

        String sharedSecret = getMyOwnSharedSecret();
        JPAKEClient jpakeClient = new JPAKEClient(false, handshakeId, sharedSecret);
        handshakeIdToClientMap.put(handshakeId, jpakeClient);
        return jpakeClient;
    }

    @Nullable
    private static String determineSharedSecret() {
        return "123456";
    }

    /**
     * @return the shared secret used if the other party initiated
     */
    private static String getMyOwnSharedSecret() {
        return determineSharedSecret();
        // Context context = CustomApplication.getInstance();
        // return SaveSettingsData.getInstance(context).getPhoneNumber();
    }

}
