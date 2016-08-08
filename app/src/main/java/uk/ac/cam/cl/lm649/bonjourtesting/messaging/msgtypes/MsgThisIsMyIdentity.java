package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.Constants;
import uk.ac.cam.cl.lm649.bonjourtesting.CustomActivity;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;
import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.database.DbTableBadges;
import uk.ac.cam.cl.lm649.bonjourtesting.messaging.MsgClient;
import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class MsgThisIsMyIdentity extends Message {

    public final BadgeStatus badgeStatus;

    public MsgThisIsMyIdentity(BadgeStatus badgeStatus) {
        super(MessageTypes.msgClassToMsgNumMap.get(MsgThisIsMyIdentity.class));
        this.badgeStatus = badgeStatus;
    }

    public static MsgThisIsMyIdentity createFromStream(DataInputStream inStream) throws IOException {
        BadgeStatus badgeStatus = BadgeStatus.createFromStream(inStream);
        return new MsgThisIsMyIdentity(badgeStatus);
    }

    @Override
    public void serialiseToStream(DataOutputStream outStream) throws IOException {
        outStream.writeInt(type);
        badgeStatus.serialiseToStream(outStream);
        Message.writeMessageEndMarker(outStream);
        outStream.flush();
    }

    @Override
    public void send(MsgClient msgClient) throws IOException {
        DataOutputStream outStream = msgClient.getOutStream();
        serialiseToStream(outStream);
    }

    @Override
    public void receive(MsgClient msgClient) throws IOException {
        FLogger.i(MsgClient.TAG, msgClient.sFromAddress + "received " + getClass().getSimpleName()
                + ":\n" + badgeStatus.toString());
        msgClient.reconfirmBadgeId(badgeStatus.getBadgeCore().getBadgeId());
        DbTableBadges.smartUpdateBadge(badgeStatus);
        CustomActivity.forceRefreshUIInTopActivity();
        if (Constants.HISTORY_TRANSFER_ENABLED) {
            msgClient.considerDoingAHistoryTransfer();
        } else {
            FLogger.d(MsgClient.TAG, "would consider doing a historyTransfer now, but it is disabled");
        }
    }

}
