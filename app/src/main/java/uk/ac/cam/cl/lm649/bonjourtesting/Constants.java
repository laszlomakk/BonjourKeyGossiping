package uk.ac.cam.cl.lm649.bonjourtesting;

import ch.qos.logback.classic.Level;

public class Constants {

    private Constants(){}

    public static final String DEFAULT_SERVICE_TYPE = "_vsecserv4._tcp.local.";

    public static final String RANDOM_SERVICE_NAMES_START_WITH = "client_";

    public static final Level JmDnsLogLevel = Level.ERROR;

    public static final long HISTORY_TRANSFER_TO_SAME_CLIENT_COOLDOWN = 5 * 60 * 1000;

    public static final boolean HISTORY_TRANSFER_ENABLED = false;

}
