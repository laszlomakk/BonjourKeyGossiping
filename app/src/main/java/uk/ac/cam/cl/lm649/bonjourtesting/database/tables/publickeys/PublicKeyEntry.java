package uk.ac.cam.cl.lm649.bonjourtesting.database.tables.publickeys;

import java.util.Date;
import java.util.Locale;

import uk.ac.cam.cl.lm649.bonjourtesting.crypto.Asymmetric;
import uk.ac.cam.cl.lm649.bonjourtesting.util.HelperMethods;

public class PublicKeyEntry {

    protected String publicKey;
    protected String phoneNumber;
    protected Long timestampFirstSeenPublicKey = null;
    protected Long timestampLastSeenAlivePublicKey = null;
    protected byte[] signedHash;

    public PublicKeyEntry() {}

    public PublicKeyEntry(PublicKeyEntry entry) {
        this.publicKey = entry.publicKey;
        this.phoneNumber = entry.phoneNumber;
        this.timestampFirstSeenPublicKey = entry.timestampFirstSeenPublicKey;
        this.timestampLastSeenAlivePublicKey = entry.timestampLastSeenAlivePublicKey;
        this.signedHash = entry.signedHash;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Long getTimestampFirstSeenPublicKey() {
        return timestampFirstSeenPublicKey;
    }

    public void setTimestampFirstSeenPublicKey(Long timestampFirstSeenPublicKey) {
        this.timestampFirstSeenPublicKey = timestampFirstSeenPublicKey;
    }

    public Long getTimestampLastSeenAlivePublicKey() {
        return timestampLastSeenAlivePublicKey;
    }

    public void setTimestampLastSeenAlivePublicKey(Long timestampLastSeenAlivePublicKey) {
        this.timestampLastSeenAlivePublicKey = timestampLastSeenAlivePublicKey;
    }

    public byte[] getSignedHash() {
        return signedHash;
    }

    public void setSignedHash(byte[] signedHash) {
        this.signedHash = signedHash;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "phoneNum: %s\npubKey: %s\nFS_time: %s\nLSA_time: %s",
                phoneNumber,
                Asymmetric.getFingerprint(publicKey),
                HelperMethods.getTimeStamp(timestampFirstSeenPublicKey),
                HelperMethods.getTimeStamp(timestampLastSeenAlivePublicKey));
    }

}
