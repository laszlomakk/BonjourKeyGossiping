package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.database.DbTableBadges;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgBadgeStatusUpdate extends Message {

    public final BadgeStatus badgeStatus;

    public MsgBadgeStatusUpdate(BadgeStatus badgeStatus) {
        super();
        this.badgeStatus = badgeStatus;
    }

    public static MsgBadgeStatusUpdate createFromStream(DataInputStream inStream) throws IOException {
        BadgeStatus badgeStatus = BadgeStatus.createFromStream(inStream);
        return new MsgBadgeStatusUpdate(badgeStatus);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);
        badgeStatus.serialiseToStream(outStream);
        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    @Override
    public void onReceive(MsgClient msgClient) throws IOException {
        FLogger.i(msgClient.logTag, msgClient.strFromAddress + "received " + getClass().getSimpleName()
                + ":\n" + badgeStatus.toString());
        msgClient.reconfirmBadgeId(badgeStatus.getBadgeCore().getBadgeId());
        DbTableBadges.smartUpdateBadge(badgeStatus);
        CustomActivity.forceRefreshUIInTopActivity();
        if (Constants.HISTORY_TRANSFER_ENABLED) {
            msgClient.considerDoingAHistoryTransfer();
        } else {
            FLogger.d(msgClient.logTag, "would consider doing a historyTransfer now, but it is disabled");
        }
    }

}
