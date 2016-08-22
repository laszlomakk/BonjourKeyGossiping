package uk.ac.cam.cl.lm649.bonjourtesting.menu.phonebook.polling;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import uk.ac.cam.cl.lm649.bonjourtesting.util.FLogger;

public class ContactPollingReceiver  extends BroadcastReceiver {

    private static final String TAG = "ContactPollingReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        FLogger.i(TAG, "onReceive(). received intent.  action: " + intent.getAction());
        Intent service = new Intent(context, ContactPollingService.class);
        context.startService(service);
    }

}
