package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FLogger {

    private static final String TAG = "FLogger";
    private static boolean initialised = false;
    private static BufferedWriter writer;
    private static final ExecutorService workerThread = Executors.newFixedThreadPool(1);

    public static final boolean LOGGING_TO_FILE = true;
    public static final boolean LOGGING_TO_LOGCAT = true;

    public static final LogLevel LOGGING_TO_FILE_MINIMUM_LOGLEVEL = LogLevel.DEBUG;

    public static void init(Context context) throws IOException {
        File file = openFile(context);
        Log.i(TAG, "logging to file: " + file.getAbsolutePath());
        try {
            writer = new BufferedWriter(new FileWriter(file, true));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "wtf. file not found... but we just created it?");
            throw e;
        }
        initialised = true;
        FLogger.i(TAG, "--------------------------------------------------");
        FLogger.i(TAG, "init() finished.");
    }

    private static File openFile(Context context) throws IOException {
        // create folder
        File logFolder = new File(context.getExternalFilesDir(null), "logs");
        logFolder.mkdirs();
        // create file
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Timestamp(System.currentTimeMillis()));
        String fname = "log_" + timeStamp + ".txt";
        File logFile = new File(logFolder, fname);
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
        return logFile;
    }

    private static void printRawLine(final String rawLine){
        if (!initialised) {
            Log.e(TAG, "log(). error - Logger has not been initialised.");
            return;
        }
        workerThread.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    writer.append(rawLine);
                    writer.newLine();
                    writer.flush(); // TODO perhaps inefficient
                } catch (IOException e) {
                    Log.e(TAG, "log(). failed to write to file. IOE - " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR;
        static int getPriority(LogLevel logLevel) {
            switch (logLevel) {
                case VERBOSE:
                    return 1;
                case DEBUG:
                    return 2;
                case INFO:
                    return 3;
                case WARN:
                    return 4;
                case ERROR:
                    return 5;
                default:
                    Log.e(TAG, "unknown loglevel: " + logLevel.name());
                    return 0;
            }
        }
    }

    private static void printLine(LogLevel logLevel, String tag, String msg) {
        if (LogLevel.getPriority(logLevel) < LogLevel.getPriority(LOGGING_TO_FILE_MINIMUM_LOGLEVEL)) {
            return;
        }
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss", Locale.US).format(new Timestamp(System.currentTimeMillis()));
        String rawLine = String.format(Locale.US, "%s %s/%s: %s",
                timeStamp, logLevel.name().charAt(0), tag, msg);
        printRawLine(rawLine);
    }

    public static void v(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.v(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.VERBOSE, tag, msg);
    }

    public static void d(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.d(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.DEBUG, tag, msg);
    }

    public static void i(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.i(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.INFO, tag, msg);
    }

    public static void w(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.w(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.WARN, tag, msg);
    }

    public static void e(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.e(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.ERROR, tag, msg);
    }

}
