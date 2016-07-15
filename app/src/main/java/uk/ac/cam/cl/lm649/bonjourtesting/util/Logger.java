package uk.ac.cam.cl.lm649.bonjourtesting.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {

    private static final String TAG = "Logger";
    private static boolean initialised = false;
    private static BufferedOutputStream writer;
    private static final ExecutorService workerThread = Executors.newFixedThreadPool(1);

    public static void init(Context context) throws IOException {
        File file = openFile(context);
        try {
            FileOutputStream fos = context.openFileOutput(file.getName(), Context.MODE_APPEND);
            Log.e(TAG, "logging to file: " + file.getAbsolutePath()); // TODO change level to INFO
            writer = new BufferedOutputStream(fos);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "wtf. file not found... but we just created it?");
            throw e;
        }
        initialised = true;
        Logger.i(TAG, "init() finished.");
    }

    private static File openFile(Context context) throws IOException {
        File file = new File(context.getExternalFilesDir(null), "my_log_file2.txt");
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
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
                    writer.write(rawLine.getBytes());
                    writer.flush(); // TODO perhaps inefficient
                    Log.e(TAG, "written to file"); // TODO remove this
                } catch (IOException e) {
                    Log.e(TAG, "log(). failed to write to file.");
                    e.printStackTrace();
                }
            }
        });
    }

    private enum LogLevel {
        VERBOSE, DEBUG, INFO, WARN, ERROR
    }

    private static void printLine(LogLevel logLevel, String tag, String msg) {
        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss", Locale.US).format(new Timestamp(System.currentTimeMillis()));
        String rawLine = String.format(Locale.US, "%s %s/%s: %s",
                timeStamp, logLevel.name(), tag, msg);
        printRawLine(rawLine);
    }

    public static void v(String tag, String msg) {
        Log.v(tag, msg);
        printLine(LogLevel.VERBOSE, tag, msg);
    }

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        printLine(LogLevel.DEBUG, tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        printLine(LogLevel.INFO, tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        printLine(LogLevel.WARN, tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        printLine(LogLevel.ERROR, tag, msg);
    }

}
