package uk.ac.cam.cl.lm649.bonjourtesting;

import ch.qos.logback.classic.Level;

public class Constants {

    private Constants(){}

    public static final String DEFAULT_SERVICE_TYPE = "_vsecserv4._tcp.local.";

    public static final String RANDOM_SERVICE_NAMES_START_WITH = "client_";

    public static final Level JmDnsLogLevel = Level.ERROR;

    public static final boolean HISTORY_TRANSFER_ENABLED = false;

    public static final int STATIC_SALT_SIZE_IN_BYTES = 16;

    public static final int NUM_REVEALED_BITS_OF_PHONE_NUMBER_HASH = 9;

    public static final boolean RESPONDER_ALSO_SENDS_PUBLIC_KEY = false;

}
