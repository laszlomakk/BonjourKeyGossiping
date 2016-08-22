package uk.ac.cam.cl.lm649.bonjourtesting;

import ch.qos.logback.classic.Level;

public class Constants {

    private Constants(){}

    // --------------------> TOGGLES start
    public static final boolean RESPONDER_ALSO_SENDS_PUBLIC_KEY = false;
    public static final boolean TEST_FOR_MEMORY_LEAKS_AND_LOG_IF_FOUND = true;
    // --------------------> TOGGLES end


    // --------------------> PARAMS start
    public static final String DEFAULT_SERVICE_TYPE = "_vsecserv4._tcp.local.";
    public static final String RANDOM_SERVICE_NAMES_START_WITH = "client_";
    public static final int STATIC_SALT_SIZE_IN_BYTES = 16;
    public static final int NUM_REVEALED_BITS_OF_PHONE_NUMBER_HASH = 9;
    public static final Level JmDnsLogLevel = Level.ERROR;
    // --------------------> PARAMS end


    // --------------------> CONSTANTS start
    public static final long MSECONDS_IN_SECOND = 1000;
    public static final long MSECONDS_IN_MINUTE = 60 * MSECONDS_IN_SECOND;
    public static final long MSECONDS_IN_HOUR = 60 * MSECONDS_IN_MINUTE;

    public static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 10001;
    // --------------------> CONSTANTS end


}
