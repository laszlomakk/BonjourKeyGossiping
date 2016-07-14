package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Application;
import android.util.Log;

import java.io.IOException;

public class CustomApplication extends Application {

    private static final String TAG = "CustomApplication";

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate() called.");

        try {
            MsgServer.initInstance();
        } catch (IOException e) {
            Log.e(TAG, "onCreate(). Failed to init MsgServer.");
            e.printStackTrace();
        }

        super.onCreate();
    }


}
