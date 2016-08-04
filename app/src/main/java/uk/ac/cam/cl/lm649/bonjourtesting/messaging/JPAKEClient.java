package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.content.Context;

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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

// TODO make sure methods on JPAKEParticipant are only called once -- enforce a consistent state
// TODO converting a BigInteger to String and sending that over the network is wasteful
public class JPAKEClient {

    private static final String TAG = "JPAKEClient";

    private final Context context;
    private JPAKEParticipant participant;
    private String myParticipantId;
    private String otherParticipantId;

    private BigInteger keyingMaterial;

    public enum State {
        ROUND_1_SEND,
        ROUND_1_RECEIVE,
        ROUND_2_SEND,
        ROUND_2_RECEIVE,
        ROUND_3_SEND,
        ROUND_3_RECEIVE,
        GET_SESSION_KEY,
        FINISHED,
        ERROR
    }
    // note: being in a state shows what the next valid thing to do is
    // being in state State.ROUND_1_SEND means the next method to call is round1Send()
    private State state = State.ROUND_1_SEND;

    public JPAKEClient(Context context, UUID otherBadgeId) {
        this.context = context;

        myParticipantId = SaveBadgeData.getInstance(context).getMyBadgeId().toString();
        otherParticipantId = otherBadgeId.toString();

        String sharedSecret = "1234"; // TODO get phone number of otherBadgeId

        initJPAKEParticipant(sharedSecret);
    }

    private void initJPAKEParticipant(String sharedSecret) {
        JPAKEPrimeOrderGroup group = JPAKEPrimeOrderGroups.NIST_3072;
        Digest digest = new SHA256Digest();
        SecureRandom random = new SecureRandom();

        participant = new JPAKEParticipant(
                myParticipantId, sharedSecret.toCharArray(), group, digest, random);
    }

    private synchronized boolean round1Send(ObjectOutputStream outStream) throws IOException {
        if (state != State.ROUND_1_SEND) {
            FLogger.w(TAG, "round1Send() called while in state " + state.name());
            return false;
        }

        JPAKERound1Payload round1Payload = participant.createRound1PayloadToSend();

        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(round1Payload.getGx1().toString(radix));
        outStream.writeUTF(round1Payload.getGx2().toString(radix));
        outStream.writeUTF(round1Payload.getKnowledgeProofForX1()[0].toString(radix));
        outStream.writeUTF(round1Payload.getKnowledgeProofForX1()[1].toString(radix));
        outStream.writeUTF(round1Payload.getKnowledgeProofForX2()[0].toString(radix));
        outStream.writeUTF(round1Payload.getKnowledgeProofForX2()[1].toString(radix));

        state = State.ROUND_1_RECEIVE;
        return true;
    }

    private synchronized boolean round1Receive(ObjectInputStream inStream) throws IOException, CryptoException {
        int radix = inStream.readInt();
        BigInteger gx1 = new BigInteger(inStream.readUTF(), radix);
        BigInteger gx2 = new BigInteger(inStream.readUTF(), radix);
        BigInteger[] knowledgeProofForX1 = new BigInteger[] {
                new BigInteger(inStream.readUTF(), radix),
                new BigInteger(inStream.readUTF(), radix)
        };
        BigInteger[] knowledgeProofForX2 = new BigInteger[] {
                new BigInteger(inStream.readUTF(), radix),
                new BigInteger(inStream.readUTF(), radix)
        };

        JPAKERound1Payload round1Payload = new JPAKERound1Payload(
                otherParticipantId, gx1, gx2, knowledgeProofForX1, knowledgeProofForX2);
        participant.validateRound1PayloadReceived(round1Payload);

        state = State.ROUND_2_SEND;
        return true;
    }

    private synchronized boolean round2Send(ObjectOutputStream outStream) throws IOException {
        if (state != State.ROUND_2_SEND) {
            FLogger.w(TAG, "round2Send() called while in state " + state.name());
            return false;
        }

        JPAKERound2Payload round2Payload = participant.createRound2PayloadToSend();

        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(round2Payload.getA().toString(radix));
        outStream.writeUTF(round2Payload.getKnowledgeProofForX2s()[0].toString(radix));
        outStream.writeUTF(round2Payload.getKnowledgeProofForX2s()[1].toString(radix));

        state = State.ROUND_2_RECEIVE;
        return true;
    }

    private synchronized boolean round2Receive(ObjectInputStream inStream) throws IOException, CryptoException {
        int radix = inStream.readInt();
        BigInteger a = new BigInteger(inStream.readUTF(), radix);
        BigInteger[] knowledgeProofForX2s = new BigInteger[] {
                new BigInteger(inStream.readUTF(), radix),
                new BigInteger(inStream.readUTF(), radix)
        };

        JPAKERound2Payload round2Payload = new JPAKERound2Payload(otherParticipantId, a, knowledgeProofForX2s);
        participant.validateRound2PayloadReceived(round2Payload);

        calcKeyingMaterial();

        state = State.ROUND_3_SEND;
        return true;
    }

    private synchronized void calcKeyingMaterial() {
        keyingMaterial = participant.calculateKeyingMaterial();
    }

    private synchronized boolean round3Send(ObjectOutputStream outStream) throws IOException {
        if (state != State.ROUND_3_SEND) {
            FLogger.w(TAG, "round3Send() called while in state " + state.name());
            return false;
        }

        JPAKERound3Payload round3Payload = participant.createRound3PayloadToSend(keyingMaterial);

        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(round3Payload.getMacTag().toString(radix));

        state = State.ROUND_3_RECEIVE;
        return true;
    }

    private synchronized boolean round3Receive(ObjectInputStream inStream) throws IOException, CryptoException {
        int radix = inStream.readInt();
        BigInteger macTag = new BigInteger(inStream.readUTF(), radix);

        JPAKERound3Payload round3Payload = new JPAKERound3Payload(otherParticipantId, macTag);
        participant.validateRound3PayloadReceived(round3Payload, keyingMaterial);

        state = State.GET_SESSION_KEY;
        return true;
    }

    // TODO use a secure key derivation function
    private static BigInteger deriveSessionKey(BigInteger keyingMaterial) {
        SHA256Digest digest = new SHA256Digest();
        byte[] keyByteArray = keyingMaterial.toByteArray();
        byte[] output = new byte[digest.getDigestSize()];
        digest.update(keyByteArray, 0, keyByteArray.length);
        digest.doFinal(output, 0);
        return new BigInteger(output);
    }

    public BigInteger getSessionKey() {
        BigInteger sessionKey = deriveSessionKey(keyingMaterial);

        state = State.FINISHED;
        return sessionKey;
    }

}
