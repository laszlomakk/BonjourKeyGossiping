package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import uk.ac.cam.cl.lm649.bonjourtesting.util.NetworkUtil;

public class Badge {

    private UUID badgeId;
    private String customName;
    private String routerMac;
    private long timestamp;

    private Badge() {}

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

    public void serialiseToStream(ObjectOutputStream outStream) throws IOException {
        outStream.writeUTF(badgeId.toString());
        outStream.writeUTF(customName);
        outStream.writeUTF(routerMac);
        outStream.writeLong(timestamp);
    }

    public static Badge createFromStream(ObjectInputStream inStream) throws IOException {
        Badge badge = new Badge();
        badge.badgeId = UUID.fromString(inStream.readUTF());
        badge.customName = inStream.readUTF();
        badge.routerMac = inStream.readUTF();
        badge.timestamp = inStream.readLong();
        return badge;
    }

}
