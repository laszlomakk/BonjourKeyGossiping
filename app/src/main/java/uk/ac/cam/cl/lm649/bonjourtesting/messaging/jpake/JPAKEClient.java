package uk.ac.cam.cl.lm649.bonjourtesting.messaging.jpake;

import android.support.annotation.NonNull;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.jpake.JPAKEParticipant;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroup;
import org.bouncycastle.crypto.agreement.jpake.JPAKEPrimeOrderGroups;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound1Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound2Payload;
import org.bouncycastle.crypto.agreement.jpake.JPAKERound3Payload;
import org.bouncycastle.crypto.digests.SHA256Digest;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgServerManager;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.Message;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgJPAKERound1;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgJPAKERound2;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes.MsgJPAKERound3;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

// TODO converting a BigInteger to String and sending that over the network is wasteful
public class JPAKEClient {

    private static final String TAG = "JPAKEClient";

    private JPAKEParticipant participant;
    private final String myParticipantId;
    private final String otherParticipantId;
    public final boolean iAmTheInitiator;

    private BigInteger keyingMaterial;
    private byte[] sessionKey = null;
    private boolean retrievedSessionKey = false;

    private final UUID handshakeId;
    private final String strHandshakeId;

    private final long creationTime;

    public static final long TIMEOUT_IN_MSEC = 5 * 60 * 1000;

    public enum State {
        INITIALISED,
        ROUND_1_SEND,
        ROUND_1_RECEIVE,
        ROUND_2_SEND,
        ROUND_2_RECEIVE,
        ROUND_3_SEND,
        ROUND_3_RECEIVE,
        FINISHED,
        FAILED
    }
    // note: being in a state shows what the last thing we did is
    // e.g. being in state State.ROUND_1_SEND means round1Send() has already been called
    private State state = State.INITIALISED;

    protected JPAKEClient(boolean iAmTheInitiator, UUID handshakeId, @NonNull String sharedSecret) {
        this.iAmTheInitiator = iAmTheInitiator;
        if (iAmTheInitiator) {
            myParticipantId = "alice";
            otherParticipantId = "bob";
        } else {
            myParticipantId = "bob";
            otherParticipantId = "alice";
        }

        this.handshakeId = handshakeId;
        this.strHandshakeId = createHandshakeIdLogString(handshakeId);

        creationTime = android.os.SystemClock.elapsedRealtime();

        initJPAKEParticipant(sharedSecret);

        FLogger.d(TAG,  "JPAKEClient created. sharedSecret: " + sharedSecret + strHandshakeId);
    }

    private void initJPAKEParticipant(@NonNull String sharedSecret) {
        JPAKEPrimeOrderGroup group = JPAKEPrimeOrderGroups.NIST_3072;
        Digest digest = new SHA256Digest();
        SecureRandom random = new SecureRandom();

        participant = new JPAKEParticipant(
                myParticipantId, sharedSecret.toCharArray(), group, digest, random);
    }

    public synchronized boolean round1Send(@NonNull MsgClient msgClient) throws IOException {
        FLogger.d(TAG, "round1Send() called while in state: " + state + strHandshakeId);
        if (state == State.ROUND_1_SEND) return true;
        if (state != State.INITIALISED) {
            FLogger.w(TAG, "round1Send() called in invalid state " + state.name() + strHandshakeId);
            return false;
        }

        JPAKERound1Payload round1Payload = participant.createRound1PayloadToSend();

        Message msg = new MsgJPAKERound1(
                handshakeId,
                round1Payload.getGx1(),
                round1Payload.getGx2(),
                round1Payload.getKnowledgeProofForX1(),
                round1Payload.getKnowledgeProofForX2()
        );
        msgClient.sendMessage(msg);

        state = State.ROUND_1_SEND;
        return true;
    }

    private synchronized boolean _round1Receive(@NonNull MsgClient msgClient, @NonNull MsgJPAKERound1 msg) throws CryptoException {
        FLogger.d(TAG, "round1Receive() called while in state: " + state + strHandshakeId);
        switch (state) {
            case INITIALISED:
                try {
                    round1Send(msgClient);
                } catch (IOException e) {
                    FLogger.e(TAG, "round1Receive() tried to call round1Send() but IOE - " + e.getMessage() + strHandshakeId);
                    return false;
                }
                break;
            case ROUND_1_SEND:
                break;
            default:
                FLogger.w(TAG, "round1Receive() called while in state " + state.name() + strHandshakeId);
                return false;
        }
        if (state != State.ROUND_1_SEND) throw new IllegalStateException("state: " + state);

        JPAKERound1Payload round1Payload = new JPAKERound1Payload(
                otherParticipantId, msg.gx1, msg.gx2, msg.knowledgeProofForX1, msg.knowledgeProofForX2);
        participant.validateRound1PayloadReceived(round1Payload);

        state = State.ROUND_1_RECEIVE;
        return true;
    }

    /**
     * @return whether validation succeeded
     */
    public boolean round1Receive(@NonNull MsgClient msgClient, @NonNull MsgJPAKERound1 msg) {
        try {
            return _round1Receive(msgClient, msg);
        } catch (CryptoException e) {
            state = State.FAILED;
            FLogger.w(TAG, String.format(Locale.US,
                    "round1Receive(). validation failed. IP: %s, oParticipantId: %s, Exception: %s" + strHandshakeId,
                    msgClient.strSocketAddress,
                    otherParticipantId,
                    e.getMessage()));
            return false;
        }
    }

    public synchronized boolean round2Send(@NonNull MsgClient msgClient) throws IOException {
        FLogger.d(TAG, "round2Send() called while in state: " + state + strHandshakeId);
        if (state == State.ROUND_2_SEND) return true;
        if (state != State.ROUND_1_RECEIVE) {
            FLogger.w(TAG, "round2Send() called in invalid state " + state.name() + strHandshakeId);
            return false;
        }

        JPAKERound2Payload round2Payload = participant.createRound2PayloadToSend();

        Message msg = new MsgJPAKERound2(
                handshakeId,
                round2Payload.getA(),
                round2Payload.getKnowledgeProofForX2s()
        );
        msgClient.sendMessage(msg);

        state = State.ROUND_2_SEND;
        return true;
    }

    private synchronized boolean _round2Receive(@NonNull MsgClient msgClient, @NonNull MsgJPAKERound2 msg) throws CryptoException {
        FLogger.d(TAG, "round2Receive() called while in state: " + state + strHandshakeId);
        switch (state) {
            case ROUND_1_RECEIVE:
                try {
                    round2Send(msgClient);
                } catch (IOException e) {
                    FLogger.e(TAG, "round2Receive() tried to call round2Send() but IOE - " + e.getMessage() + strHandshakeId);
                    return false;
                }
                break;
            case ROUND_2_SEND:
                break;
            default:
                FLogger.w(TAG, "round2Receive() called while in state " + state.name() + strHandshakeId);
                return false;
        }
        if (state != State.ROUND_2_SEND) throw new IllegalStateException("state: " + state);

        JPAKERound2Payload round2Payload = new JPAKERound2Payload(otherParticipantId, msg.a, msg.knowledgeProofForX2s);
        participant.validateRound2PayloadReceived(round2Payload);

        calcKeyingMaterial();

        state = State.ROUND_2_RECEIVE;
        return true;
    }

    /**
     * @return whether validation succeeded
     */
    public boolean round2Receive(@NonNull MsgClient msgClient, @NonNull MsgJPAKERound2 msg) {
        try {
            return _round2Receive(msgClient, msg);
        } catch (CryptoException e) {
            state = State.FAILED;
            FLogger.w(TAG, String.format(Locale.US,
                    "round2Receive(). validation failed. IP: %s, oParticipantId: %s, Exception: %s" + strHandshakeId,
                    msgClient.strSocketAddress,
                    otherParticipantId,
                    e.getMessage()));
            return false;
        }
    }

    private synchronized void calcKeyingMaterial() {
        keyingMaterial = participant.calculateKeyingMaterial();
        sessionKey = deriveSessionKey(keyingMaterial);
    }

    public synchronized boolean round3Send(@NonNull MsgClient msgClient) throws IOException {
        FLogger.d(TAG, "round3Send() called while in state: " + state + strHandshakeId);
        if (state == State.ROUND_3_SEND) return true;
        if (state != State.ROUND_2_RECEIVE) {
            FLogger.w(TAG, "round3Send() called in invalid state " + state.name() + strHandshakeId);
            return false;
        }

        JPAKERound3Payload round3Payload = participant.createRound3PayloadToSend(keyingMaterial);

        Message msg = new MsgJPAKERound3(
                handshakeId,
                round3Payload.getMacTag());
        msgClient.sendMessage(msg);

        state = State.ROUND_3_SEND;
        return true;
    }

    private synchronized boolean _round3Receive(@NonNull MsgClient msgClient, @NonNull MsgJPAKERound3 msg) throws CryptoException {
        FLogger.d(TAG, "round3Receive() called while in state: " + state + strHandshakeId);
        switch (state) {
            case ROUND_2_RECEIVE:
                try {
                    round3Send(msgClient);
                } catch (IOException e) {
                    FLogger.e(TAG, "round3Receive() tried to call round3Send() but IOE - " + e.getMessage() + strHandshakeId);
                    return false;
                }
                break;
            case ROUND_3_SEND:
                break;
            default:
                FLogger.w(TAG, "round3Receive() called while in state " + state.name() + strHandshakeId);
                return false;
        }
        if (state != State.ROUND_3_SEND) throw new IllegalStateException("state: " + state);

        JPAKERound3Payload round3Payload = new JPAKERound3Payload(otherParticipantId, msg.macTag);
        participant.validateRound3PayloadReceived(round3Payload, keyingMaterial);

        state = State.ROUND_3_RECEIVE;
        if (retrievedSessionKey) {
            state = State.FINISHED;
        }
        return true;
    }

    /**
     * @return whether validation succeeded
     */
    public boolean round3Receive(@NonNull MsgClient msgClient, @NonNull MsgJPAKERound3 msg) {
        try {
            return _round3Receive(msgClient, msg);
        } catch (CryptoException e) {
            state = State.FAILED;
            FLogger.w(TAG, String.format(Locale.US,
                    "round3Receive(). validation failed. IP: %s, oParticipantId: %s, Exception: %s" + strHandshakeId,
                    msgClient.strSocketAddress,
                    otherParticipantId,
                    e.getMessage()));
            return false;
        }
    }

    // TODO use a secure key derivation function
    private static byte[] deriveSessionKey(@NonNull BigInteger keyingMaterial) {
        SHA256Digest digest = new SHA256Digest();
        byte[] keyByteArray = keyingMaterial.toByteArray();
        byte[] output = new byte[digest.getDigestSize()];
        digest.update(keyByteArray, 0, keyByteArray.length);
        digest.doFinal(output, 0);
        return output;
    }

    public byte[] getSessionKey() {
        retrievedSessionKey = true;
        switch (state) {
            case ROUND_3_RECEIVE:
                state = State.FINISHED;
                // fall through
            case FINISHED:
                return sessionKey;
            default:
                throw new IllegalStateException("state: " + state);
        }
    }

    public boolean isInProgress() {
        switch (state) {
            case FAILED:
            case FINISHED:
                return false;
            default:
                return !hasTimedOut();
        }
    }

    public boolean hasHandshakeFinishedSuccessfully() {
        return state == State.FINISHED || state == State.ROUND_3_RECEIVE;
    }

    private boolean hasTimedOut() {
        long curTime = android.os.SystemClock.elapsedRealtime();
        return creationTime + TIMEOUT_IN_MSEC < curTime;
    }

    public static String createHandshakeIdLogString(UUID handshakeId) {
        return " (hs-" + handshakeId + ")";
    }

}
