package uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake;

import android.content.Context;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;

import uk.ac.cam.cl.lm649.bonjourtesting.CustomApplication;
import uk.ac.cam.cl.lm649.bonjourtesting.SaveIdentityData;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class JPAKEManager {

    public static final String TAG = "JPAKEManager";

    private final ConcurrentSkipListMap<UUID, JPAKEClient> handshakeIdToClientMap = new ConcurrentSkipListMap<>();

    public synchronized boolean canNewJPAKEWaveBeStarted() {
        FLogger.d(TAG, "canNewJPAKEWaveBeStarted() called.");
        boolean allHandshakesFinished = true;
        for (Map.Entry<UUID, JPAKEClient> entry : handshakeIdToClientMap.entrySet()) {
            UUID handshakeId = entry.getKey();
            JPAKEClient jpakeClient = entry.getValue();
            boolean inProgress = jpakeClient.isInProgress();
            FLogger.d(TAG, "canNewJPAKEWaveBeStarted(). found handshake: " + handshakeId + " - in progress: " + inProgress);

            if (inProgress) {
                allHandshakesFinished = false;
            } else {
                handshakeIdToClientMap.remove(handshakeId);
            }
        }
        return allHandshakesFinished;
    }

    /**
     * Tries to start one JPAKE handshake for every given sharedSecret using the given MsgClient instance.
     *
     * @return the number of handshakes started, or -1 if a previous JPAKE wave is still in progress
     */
    public static int startJPAKEWave(MsgClient msgClient, List<String> sharedSecrets) {
        FLogger.i(TAG, "startJPAKEWave() called.");
        JPAKEManager jpakeManager = msgClient.jpakeManager;
        if (!jpakeManager.canNewJPAKEWaveBeStarted()) {
            FLogger.i(TAG, "startJPAKEWave(). can't start new wave.");
            return -1;
        }
        int nHandshakesStarted = 0;
        for (String sharedSecret : sharedSecrets) {
            nHandshakesStarted += startJPAKEHandshake(msgClient, sharedSecret) ? 1 : 0;
        }
        return nHandshakesStarted;
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

    /**
     * @return the shared secret used if the other party initiated
     */
    private static String getMyOwnSharedSecret() {
        Context context = CustomApplication.getInstance();
        return SaveIdentityData.getInstance(context).getPhoneNumber();
    }

}
