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
    private static BufferedWriter writerImportant;
    private static BufferedWriter writerDetailed;
    private static final ExecutorService workerThread = Executors.newFixedThreadPool(1);

    public static final boolean LOGGING_TO_FILE = true;
    public static final boolean LOGGING_TO_LOGCAT = true;

    public static final LogLevel LOGGING_TO_FILE_MINIMUM_LOGLEVEL_FOR_IMPORTANT = LogLevel.INFO;
    public static final LogLevel LOGGING_TO_FILE_MINIMUM_LOGLEVEL_FOR_DETAILED = LogLevel.DEBUG;

    public static void init(Context context) throws IOException {
        File fileImportant = openFile(context, "logs",
                "_" + LOGGING_TO_FILE_MINIMUM_LOGLEVEL_FOR_IMPORTANT.name());
        File fileDetailed = openFile(context, "logs",
                "_" + LOGGING_TO_FILE_MINIMUM_LOGLEVEL_FOR_DETAILED.name());
        Log.i(TAG, "logging to file (important): " + fileImportant.getAbsolutePath());
        Log.i(TAG, "logging to file (detailed): " + fileDetailed.getAbsolutePath());
        try {
            writerImportant = new BufferedWriter(new FileWriter(fileImportant, true));
            writerDetailed = new BufferedWriter(new FileWriter(fileDetailed, true));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "wtf. file not found... but we just created it?");
            throw e;
        }
        initialised = true;
        FLogger.i(TAG, "--------------------------------------------------");
        FLogger.i(TAG, "init() finished.");
    }

    private static File openFile(Context context, String dir, String fnamePostfix) throws IOException {
        // create folder
        File logFolder = new File(context.getExternalFilesDir(null), dir);
        logFolder.mkdirs();
        // create file
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Timestamp(System.currentTimeMillis()));
        String fname = "log_" + timeStamp + fnamePostfix + ".txt";
        File logFile = new File(logFolder, fname);
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
        return logFile;
    }

    private static void printRawLine(
            final BufferedWriter writer, final String rawLine, final long logLineTime){
        if (!initialised) {
            Log.e(TAG, "log(). error - Logger has not been initialised.");
            return;
        }
        workerThread.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    long curTime = System.currentTimeMillis();
                    if (curTime - logLineTime > 60_000) {
                        // write time of printing
                        writer.append("// current time reported by device: ");
                        writer.append(HelperMethods.getTimeStamp(curTime));
                        writer.newLine();
                    }
                    // write intended log line
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
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    private static void printLine(LogLevel logLevel, String tag, String msg) {
        if (logLevel.ordinal() < LOGGING_TO_FILE_MINIMUM_LOGLEVEL_FOR_DETAILED.ordinal()) {
            return;
        }
        long time = System.currentTimeMillis();
        String strTimeStamp = HelperMethods.getTimeStamp(time);
        String rawLine = String.format(Locale.US, "%s %s/%s: %s",
                strTimeStamp, logLevel.name().charAt(0), tag, msg);

        printRawLine(writerDetailed, rawLine, time);
        if (logLevel.ordinal() >= LOGGING_TO_FILE_MINIMUM_LOGLEVEL_FOR_IMPORTANT.ordinal()) {
            printRawLine(writerImportant, rawLine, time);
        }
    }

    public static void v(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.v(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.VERBOSE, tag, msg);
    }

    public static void v(String tag, Throwable e) {
        v(tag, HelperMethods.formatStackTraceAsString(e));
    }

    public static void d(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.d(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.DEBUG, tag, msg);
    }

    public static void d(String tag, Throwable e) {
        d(tag, HelperMethods.formatStackTraceAsString(e));
    }

    public static void i(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.i(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.INFO, tag, msg);
    }

    public static void i(String tag, Throwable e) {
        i(tag, HelperMethods.formatStackTraceAsString(e));
    }

    public static void w(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.w(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.WARN, tag, msg);
    }

    public static void w(String tag, Throwable e) {
        w(tag, HelperMethods.formatStackTraceAsString(e));
    }

    public static void e(String tag, String msg) {
        if (LOGGING_TO_LOGCAT) Log.e(tag, msg);
        if (LOGGING_TO_FILE) printLine(LogLevel.ERROR, tag, msg);
    }

    public static void e(String tag, Throwable e) {
        e(tag, HelperMethods.formatStackTraceAsString(e));
    }

}
