package uk.ac.cam.cl.lm649.bonjourtesting;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

public class CustomActivity extends Activity {

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
        super.onStart();
        app.setTopActivity(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //app.setTopActivity(null);
    }
}
