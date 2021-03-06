/**
 Copyright (C) 2016 Laszlo Makk
 All code under the BonjourTesting project is licensed under the Apache 2.0 License
 */

package uk.ac.cam.cl.lm649.bonjourtesting.messaging;

import java.io.IOException;
import java.util.concurrent.ConcurrentSkipListMap;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;
import uk.ac.cam.cl.lm649.bonjourtesting.bonjour.ServiceStub;

public final class MsgServerManager {

    private final static String TAG = "MsgServerManager";
    private static MsgServerManager INSTANCE = null;

    public final ConcurrentSkipListMap<ServiceStub, MsgClient> serviceToMsgClientMap = new ConcurrentSkipListMap<>();

    private final MsgServerPlaintext msgServerPlaintext;
    private final MsgServerEncrypted msgServerEncrypted;

    private MsgServerManager() {
        msgServerPlaintext = new MsgServerPlaintext();
        msgServerEncrypted = new MsgServerEncrypted();
    }

    public static synchronized MsgServerManager getInstance() {
        if (null == INSTANCE) {
            INSTANCE = new MsgServerManager();
        }
        return INSTANCE;
    }

    public void start() throws IOException {
        FLogger.i(TAG, "start() called.");
        msgServerPlaintext.start();
        msgServerEncrypted.start();
    }

    public void stop(){
        FLogger.i(TAG, "stop() called.");

        msgServerPlaintext.stop();
        msgServerEncrypted.stop();

        for (MsgClient msgClient : serviceToMsgClientMap.values()) {
            msgClient.close();
        }
        serviceToMsgClientMap.clear();
    }

    public MsgServerPlaintext getMsgServerPlaintext() {
        return msgServerPlaintext;
    }

    public MsgServerEncrypted getMsgServerEncrypted() {
        return msgServerEncrypted;
    }
}
