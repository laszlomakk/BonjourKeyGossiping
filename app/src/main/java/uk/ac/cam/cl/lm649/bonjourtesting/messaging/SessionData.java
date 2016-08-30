package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import android.support.annotation.Nullable;

public class SessionData {

    public final SessionKey sessionKey;
    private String phoneNumberOfOtherParticipant = null;
    private String publicKeyOfOtherParticipant = null;
    private boolean trustOtherParticipant = false;

    public SessionData(SessionKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    public SessionData() {
        this(null);
    }

    /**
     * Copy all data from previous SessionData except for the session key.
     *
     * @param sessionData copy data from here
     * @param sessionKey use as new key
     */
    public SessionData(SessionData sessionData, SessionKey sessionKey) {
        this(sessionKey);
        this.phoneNumberOfOtherParticipant = sessionData.phoneNumberOfOtherParticipant;
    }

    @Nullable
    public String getPhoneNumberOfOtherParticipant() {
        return phoneNumberOfOtherParticipant;
    }

    public void setPhoneNumberOfOtherParticipant(String phoneNumberOfOtherParticipant) {
        this.phoneNumberOfOtherParticipant = phoneNumberOfOtherParticipant;
    }

    @Nullable
    public String getPublicKeyOfOtherParticipant() {
        return publicKeyOfOtherParticipant;
    }

    public void setPublicKeyOfOtherParticipant(String publicKeyOfOtherParticipant) {
        this.publicKeyOfOtherParticipant = publicKeyOfOtherParticipant;
    }

    public boolean doWeTrustOtherParticipant() {
        return trustOtherParticipant;
    }

    public void setTrustOtherParticipant(boolean trustOtherParticipant) {
        this.trustOtherParticipant = trustOtherParticipant;
    }
}
