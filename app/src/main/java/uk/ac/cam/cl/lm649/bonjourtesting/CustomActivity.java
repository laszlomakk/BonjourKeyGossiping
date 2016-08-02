package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class CustomActivity extends Activity {

    private static final String TAG = "CustomActivity";

    protected CustomApplication app;
    protected Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (CustomApplication) getApplication();
        context = app;
    }

    @Override
    protected void onStart() {
        FLogger.i(TAG, "Activity starting up. (" + this.getClass().getSimpleName() + ")");
        super.onStart();
        app.setTopActivity(this);
    }

    @Override
    protected void onStop() {
        FLogger.i(TAG, "Activity stopping. (" + this.getClass().getSimpleName() + ")");
        super.onStop();
    }

    public void forceRefreshUI() {}

    public static void forceRefreshUIInTopActivity() {
        CustomApplication app = CustomApplication.getInstance();
        if (null == app) {
            FLogger.e(TAG, "forceRefreshUIInTopActivity(). app is null");
            return;
        }
        CustomActivity topAct = app.getTopActivity();
        if (null == topAct) {
            FLogger.d(TAG, "forceRefreshUIInTopActivity(). topAct is null");
            return;
        }
        topAct.forceRefreshUI();
    }

}
