package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Badge {

    private final UUID badgeId;
    private String customName;
    private String routerMac;
    private long timestamp;

    public Badge(UUID badgeId) {
        this.badgeId = badgeId;
    }

    public UUID getBadgeId() {
        return badgeId;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public String getRouterMac() {
        return routerMac;
    }

    public void setRouterMac(String routerMac) {
        this.routerMac = routerMac;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "badgeID: %s\nnick: %s\nrouter_MAC: %s\ntime: %s",
                badgeId.toString(), customName, routerMac, new Date(timestamp));
    }

    public enum SortOrder {
        MOST_RECENT_FIRST, ALPHABETICAL
    }

}
