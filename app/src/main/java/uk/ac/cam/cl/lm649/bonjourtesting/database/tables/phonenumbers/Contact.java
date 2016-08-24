package uk.ac.cam.cl.lm649.bonjourtesting.database.tables.phonenumbers;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Locale;

public class Contact {

    private static final String TAG = "Contact";

    private String phoneNumber;
    private String customName;
    protected GossipingStatus gossipingStatus = null;

    public Contact() {}

    public Contact(String phoneNumber, String customName) {
        this.phoneNumber = phoneNumber;
        this.customName = customName;
    }

    public Contact(Contact entry) {
        this.phoneNumber = entry.phoneNumber;
        this.customName = entry.customName;
        this.gossipingStatus = entry.gossipingStatus;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @NonNull
    public GossipingStatus getGossipingStatus() {
        return null == gossipingStatus ? GossipingStatus.UNTOUCHED : gossipingStatus;
    }

    public void setGossipingStatus(GossipingStatus gossipingStatus) {
        if (null == gossipingStatus) return;
        this.gossipingStatus = gossipingStatus;
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "nick: %s\nphoneNum: %s",
                customName, phoneNumber);
    }

    private static final HashMap<Integer, GossipingStatus> gossipingStatusIntValToEnumMap = new HashMap<>();

    public enum GossipingStatus {

        USER_DISABLED(0, "disabled", false),
        UNTOUCHED(1, "default", true),
        USER_ENABLED(2, "enabled", true);

        private final int value;
        private final String text;
        private final boolean gossipingEnabled;

        GossipingStatus(final int value, final String text, final boolean gossipingEnabled) {
            this.value = value;
            this.text = text;
            this.gossipingEnabled = gossipingEnabled;
            gossipingStatusIntValToEnumMap.put(value, this);
        }

        public int getValue() { return value; }

        public String getText() {
            return text;
        }

        public static GossipingStatus fromIntVal(int value) {
            return gossipingStatusIntValToEnumMap.get(value);
        }

        public boolean isEnabled() {
            return gossipingEnabled;
        }

    }

}
