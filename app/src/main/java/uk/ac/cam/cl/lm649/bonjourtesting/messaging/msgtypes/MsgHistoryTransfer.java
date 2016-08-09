package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.SaveBadgeData;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTableBadges;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgHistoryTransfer extends Message {

    private static final String TAG = "MsgHistoryTransfer";

    public final List<BadgeStatus> badgeStatuses;

    public MsgHistoryTransfer(List<BadgeStatus> badgeStatuses) {
        super();
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
        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    @Override
    public void send(MsgClient msgClient) throws IOException {
        if (!Constants.HISTORY_TRANSFER_ENABLED) {
            FLogger.e(TAG, "send() called, but historyTransfer is disabled.");
            return;
        }
        DataOutputStream outStream = msgClient.getOutStream();
        serialiseToStream(outStream);
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        int numBadges = badgeStatuses.size();
        FLogger.i(MsgClient.TAG, msgClient.sFromAddress + "received " + getClass().getSimpleName() + ", containing "
                + numBadges + " badges");
        if (!Constants.HISTORY_TRANSFER_ENABLED) {
            FLogger.i(MsgClient.TAG, "rejecting message. historyTransfer is disabled.");
            return;
        }
        SaveBadgeData saveBadgeData = SaveBadgeData.getInstance(msgClient.context);
        for (BadgeStatus badgeStatus : badgeStatuses) {
            if (!saveBadgeData.getMyBadgeId().equals(badgeStatus.getBadgeCore().getBadgeId())) {
                DbTableBadges.smartUpdateBadge(badgeStatus);
                FLogger.d(TAG, msgClient.sFromAddress + "historyTransfer contains badge:\n"
                        + badgeStatus.toString());
            }
        }
        CustomActivity.forceRefreshUIInTopActivity();
    }

}