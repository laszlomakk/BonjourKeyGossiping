package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Locale;

public class BadgeStatus {

    private BadgeCore badgeCore;
    private String routerMac;
    private long timestampLastSeenAlive;

    private BadgeStatus() {}

    public BadgeStatus(BadgeCore badgeCore) {
        this.badgeCore = badgeCore;
    }

    public BadgeCore getBadgeCore() {
        return badgeCore;
    }

    public String getRouterMac() {
        return routerMac;
    }

    public void setRouterMac(String routerMac) {
        this.routerMac = routerMac;
    }

    public long getTimestampLastSeenAlive() {
        return timestampLastSeenAlive;
    }

    public void setTimestampLastSeenAlive(long timestampLastSeenAlive) {
        this.timestampLastSeenAlive = timestampLastSeenAlive;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s\nrouter_MAC: %s\nLSA_time: %s",
                badgeCore.toString(), routerMac, new Date(timestampLastSeenAlive));
    }

    public enum SortOrder {
        MOST_RECENT_ALIVE_FIRST, ALPHABETICAL
    }

    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        badgeCore.serialiseToStream(outStream);
        outStream.writeUTF(routerMac);
        outStream.writeLong(timestampLastSeenAlive);
    }

    public static BadgeStatus createFromStream(DataInputStream inStream) throws IOException {
        BadgeStatus badgeStatus = new BadgeStatus();
        badgeStatus.badgeCore = BadgeCore.createFromStream(inStream);
        badgeStatus.routerMac = inStream.readUTF();
        badgeStatus.timestampLastSeenAlive = inStream.readLong();
        return badgeStatus;
    }

}
