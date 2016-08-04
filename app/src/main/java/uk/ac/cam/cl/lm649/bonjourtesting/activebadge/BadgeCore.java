package uk.ac.cam.cl.lm649.bonjourtesting.activebadge;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class BadgeCore {

    private UUID badgeId;
    private String customName;

    private BadgeCore() {}

    public BadgeCore(UUID badgeId) {
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

    @Override
    public String toString() {
        return String.format(Locale.US, "badgeID: %s\nnick: %s", badgeId.toString(), customName);
    }

    protected void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeUTF(badgeId.toString());
        outStream.writeUTF(customName);
    }

    protected static BadgeCore createFromStream(DataInputStream inStream) throws IOException {
        BadgeCore badgeCore = new BadgeCore();
        badgeCore.badgeId = UUID.fromString(inStream.readUTF());
        badgeCore.customName = inStream.readUTF();
        return badgeCore;
    }

}
