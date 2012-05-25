package net.commotionwireless.meshtether;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ToggleReceiver extends BroadcastReceiver {
    final static String TAG = "ToggleReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive " + intent.getAction());
        if (MeshTetherApp.ACTION_TOGGLE.equals(intent.getAction())) {
            // potential race conditions, but they are benign
            MeshService service = MeshService.singleton;
            //Log.d(TAG, "service " + ((service == null) ? "null" : "present"));
            if (service != null) {
                if (!intent.getBooleanExtra("start", false)) {
                    Log.d(TAG, "stop");
                    service.stopRequest();
                }
            } else {
                if (intent.getBooleanExtra("start", true)) {
                    Log.d(TAG, "start");
                    context.startService(new Intent(context, MeshService.class));
                }
            }
        } else if (MeshTetherApp.ACTION_CHECK.equals(intent.getAction())) {
            // FIXME: this is the most inefficient way of finding out the state
            MeshService service = MeshService.singleton;
            int state = (service != null) ? service.getState() : MeshService.STATE_STOPPED;
            MeshTetherApp.broadcastState(context, state);
        }
    }
}
