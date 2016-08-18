package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

public class SessionData {

    public final SessionKey sessionKey;
    private String phoneNumberOfOtherParticipant;

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

    public String getPhoneNumberOfOtherParticipant() {
        return phoneNumberOfOtherParticipant;
    }

    public void setPhoneNumberOfOtherParticipant(String phoneNumberOfOtherParticipant) {
        this.phoneNumberOfOtherParticipant = phoneNumberOfOtherParticipant;
    }
}
