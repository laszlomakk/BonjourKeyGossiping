package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;

public class MsgHistoryTransfer extends Message {

    public static final int TYPE_NUM = 3;

    public final List<BadgeStatus> badgeStatuses;

    public MsgHistoryTransfer(List<BadgeStatus> badgeStatuses) {
        super(TYPE_NUM);
        this.badgeStatuses = badgeStatuses;
    }

    public static MsgHistoryTransfer createFromStream(DataInputStream inStream) throws IOException {
        int numBadges = inStream.readInt();
        List<BadgeStatus> badgeStatuses = new ArrayList<>(numBadges);
        for (int badgeIndex = 0; badgeIndex < numBadges; badgeIndex++) {
            badgeStatuses.add(BadgeStatus.createFromStream(inStream));
        }
        return new MsgHistoryTransfer(badgeStatuses);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);
        outStream.writeInt(badgeStatuses.size());
        for (BadgeStatus badgeStatus : badgeStatuses) {
            badgeStatus.serialiseToStream(outStream);
        }
        outStream.flush();
    }

}