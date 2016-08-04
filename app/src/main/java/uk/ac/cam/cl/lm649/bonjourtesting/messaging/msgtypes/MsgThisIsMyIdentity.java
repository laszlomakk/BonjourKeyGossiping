package uk.ac.cam.cl.lm649.bonjourtesting.messaging.msgtypes;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import uk.ac.cam.cl.lm649.bonjourtesting.activebadge.BadgeStatus;

public class MsgThisIsMyIdentity extends Message {

    public static final int TYPE_NUM = 2;

    public final BadgeStatus badgeStatus;

    public MsgThisIsMyIdentity(BadgeStatus badgeStatus) {
        super(TYPE_NUM);
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
        outStream.flush();
    }

}
