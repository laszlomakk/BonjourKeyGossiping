package uk.ac.cam.cl.lm649.bonjourtesting;

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

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;

// TODO make sure methods on JPAKEParticipant are only called once -- enforce a consistent state
// TODO converting a BigInteger to String and sending that over the network is wasteful
public class JPAKEClient {

    private Context context;
    private JPAKEParticipant participant;
    private String myParticipantId;

    private BigInteger keyingMaterial;

    public JPAKEClient(Context context, String sharedSecret) {
        init(sharedSecret);

    }

    private void init(String sharedSecret) {
        JPAKEPrimeOrderGroup group = JPAKEPrimeOrderGroups.NIST_3072;
        Digest digest = new SHA256Digest();
        SecureRandom random = new SecureRandom();

        myParticipantId = SaveBadgeData.getInstance(context).getMyBadgeId().toString();
        participant = new JPAKEParticipant(
                myParticipantId, sharedSecret.toCharArray(), group, digest, random);
    }

    private void round1Send(ObjectOutputStream outStream) throws IOException {
        JPAKERound1Payload round1Payload = participant.createRound1PayloadToSend();

        outStream.writeUTF(myParticipantId);
        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(round1Payload.getGx1().toString(radix));
        outStream.writeUTF(round1Payload.getGx2().toString(radix));
        outStream.writeUTF(round1Payload.getKnowledgeProofForX1()[0].toString(radix));
        outStream.writeUTF(round1Payload.getKnowledgeProofForX1()[1].toString(radix));
        outStream.writeUTF(round1Payload.getKnowledgeProofForX2()[0].toString(radix));
        outStream.writeUTF(round1Payload.getKnowledgeProofForX2()[1].toString(radix));
    }

    private void round1Receive(ObjectInputStream inStream) throws IOException, CryptoException {
        String participantId = inStream.readUTF();
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

        JPAKERound1Payload round1Payload = new JPAKERound1Payload(participantId, gx1, gx2, knowledgeProofForX1, knowledgeProofForX2);
        participant.validateRound1PayloadReceived(round1Payload);
    }

    private void round2Send(ObjectOutputStream outStream) throws IOException {
        JPAKERound2Payload round2Payload = participant.createRound2PayloadToSend();

        outStream.writeUTF(myParticipantId);
        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(round2Payload.getA().toString(radix));
        outStream.writeUTF(round2Payload.getKnowledgeProofForX2s()[0].toString(radix));
        outStream.writeUTF(round2Payload.getKnowledgeProofForX2s()[1].toString(radix));
    }

    private void round2Receive(ObjectInputStream inStream) throws IOException, CryptoException {
        String participantId = inStream.readUTF();
        int radix = inStream.readInt();
        BigInteger a = new BigInteger(inStream.readUTF(), radix);
        BigInteger[] knowledgeProofForX2s = new BigInteger[] {
                new BigInteger(inStream.readUTF(), radix),
                new BigInteger(inStream.readUTF(), radix)
        };

        JPAKERound2Payload round2Payload = new JPAKERound2Payload(participantId, a, knowledgeProofForX2s);
        participant.validateRound2PayloadReceived(round2Payload);
    }

    private void calcKeyingMaterial() {
        keyingMaterial = participant.calculateKeyingMaterial();
    }

    private void round3Send(ObjectOutputStream outStream) throws IOException {
        JPAKERound3Payload round3Payload = participant.createRound3PayloadToSend(keyingMaterial);

        outStream.writeUTF(myParticipantId);
        int radix = Character.MAX_RADIX;
        outStream.writeInt(radix);
        outStream.writeUTF(round3Payload.getMacTag().toString(radix));
    }

    private void round3Receive(ObjectInputStream inStream) throws IOException, CryptoException {
        String participantId = inStream.readUTF();
        int radix = inStream.readInt();
        BigInteger macTag = new BigInteger(inStream.readUTF(), radix);

        JPAKERound3Payload round3Payload = new JPAKERound3Payload(participantId, macTag);
        participant.validateRound3PayloadReceived(round3Payload, keyingMaterial);
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
        return deriveSessionKey(keyingMaterial);
    }

}
