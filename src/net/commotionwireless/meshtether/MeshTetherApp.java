/*
*  This file is part of Commotion Mesh Tether
*  Copyright (C) 2010 by Szymon Jakubczak
*
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.commotionwireless.meshtether;

import java.util.ArrayList;
import java.util.List;

import net.commotionwireless.meshtether.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


/**
* Manages preferences, activities and prepares the service
*/
public class MeshTetherApp extends android.app.Application {
    final static String TAG = "MeshTetherApp";

    final static String FILE_INI    = "brncl.ini";

    final static String ACTION_CLIENTS = "net.commotionwireless.meshtether.SHOW_CLIENTS";
    final static String ACTION_TOGGLE = "net.commotionwireless.meshtether.TOGGLE_STATE";
    final static String ACTION_CHECK = "net.commotionwireless.meshtether.CHECK_STATE";
    final static String ACTION_CHANGED = "net.commotionwireless.meshtether.STATE_CHANGED";

    final static int ERROR_ROOT = 1;
    final static int ERROR_OTHER = 2;
    final static int ERROR_SUPPLICANT = 3;

    SharedPreferences prefs;
    private StatusActivity  statusActivity = null;
    private ClientsActivity clientsActivity = null;
    private Toast toast;

    private WifiManager wifiManager;

    // notifications
    private NotificationManager notificationManager;
    private Notification notification;
    private Notification notificationClientAdded;
    private Notification notificationError;
    final static int NOTIFY_RUNNING = 0;
    final static int NOTIFY_CLIENT = 1;
    final static int NOTIFY_ERROR = 2;

    public MeshService service = null;
    public Util.StyledStringBuilder log = null; // == service.log, unless service is dead

    private List<ScanResult> lastScanResult;
    private boolean shouldDisableWifi;

    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                scanResultsAvailable(wifiManager.getScanResults());
                if (shouldDisableWifi) {
                    wifiManager.setWifiEnabled(false);
                } // else stop disabling it
                shouldDisableWifi = false;
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        NativeHelper.setup(this);

        // initialize default values if not done this in the past
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // if IP address isn't set, generate one
        if (prefs.getString(getString(R.string.adhoc_ip), "").equals("")) {
        	SharedPreferences.Editor e = prefs.edit();
        	String myIP = "10." + String.valueOf((int)(Math.random() * 255))
        			+ "." + String.valueOf((int)(Math.random() * 255))
        			+ "." + String.valueOf((int)(Math.random() * 255));
        	e.putString(getString(R.string.adhoc_ip), myIP);
        	e.commit();
        	Log.i(TAG, "Generated IP: " + myIP);
        }
        
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        notification = new Notification(R.drawable.barnacle, getString(R.string.notify_running), 0);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notificationClientAdded = new Notification(android.R.drawable.stat_sys_warning,
                                                  getString(R.string.notify_client), 0);
        notificationClientAdded.flags = Notification.FLAG_AUTO_CANCEL;
        notificationError = new Notification(R.drawable.barnacle_error,
                                            getString(R.string.notify_error), 0);
        notificationError.setLatestEventInfo(this,
                getString(R.string.app_name),
                getString(R.string.notify_error),
                PendingIntent.getActivity(this, 0, new Intent(this, StatusActivity.class), 0));
        notificationError.flags = Notification.FLAG_AUTO_CANCEL;

        registerReceiver(scanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
            shouldDisableWifi = true;
        }
        wifiManager.startScan();
    }

    @Override
    public void onTerminate() {
        if (service != null) {
            Log.e(TAG, "The app is terminated while the service is running!");
            service.stopRequest();
        }
        // clean up after yourself
        if (shouldDisableWifi) {
            wifiManager.setWifiEnabled(false);
        }
        try {
            unregisterReceiver(scanReceiver);
        } catch (Exception e) {
            // ignore IntentReceiverLeaked
        }
        super.onTerminate();
    }

    public void startService() {
        if (service == null)
            startService(new Intent(this, MeshService.class));
    }

    public void stopService() {
        if (service != null)
            service.stopRequest();
    }

    public int getState() {
        if (service != null)
            return service.getState();
        return MeshService.STATE_STOPPED;
    }

    public boolean isChanging() {
        return getState() == MeshService.STATE_STARTING;
    }

    public boolean isRunning() {
        return getState() == MeshService.STATE_RUNNING;
    }

    public boolean isStopped() {
        return getState() == MeshService.STATE_STOPPED;
    }

    void setStatusActivity(StatusActivity a) { // for updates
        statusActivity = a;
    }
    void setClientsActivity(ClientsActivity a) { // for updates
        clientsActivity = a;
    }

    void serviceStarted(MeshService s) {
        Log.w(TAG, "serviceStarted");
        service = s;
        log = service.log;
        service.startRequest();
        if (clientsActivity != null)
            clientsActivity.update();
    }

    static void broadcastState(Context ctx, int state) {
        Intent intent = new Intent(ACTION_CHANGED);
        intent.putExtra("state", state);
        ctx.sendBroadcast(intent, "net.commotionwireless.meshtether.ACCESS_STATE");
    }

    void updateStatus() {
        if (statusActivity != null)
            statusActivity.update();
        // TODO: only broadcast state if changed or stale
        broadcastState(this, getState());
    }

    void updateToast(String msg, boolean islong) {
        toast.setText(msg);
        toast.setDuration(islong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        toast.show();
    }

    private void scanResultsAvailable(List<ScanResult> result) {
        lastScanResult = result;
    }

    void clientAdded(MeshService.ClientData cd) {
        if (prefs.getBoolean("client_notify", true)) {
            notificationClientAdded.defaults = 0;
            if (prefs.getBoolean("client_light", false)) {
                notificationClientAdded.flags |= Notification.FLAG_SHOW_LIGHTS;
                notificationClientAdded.ledARGB = 0xffffff00; // yellow
                notificationClientAdded.ledOnMS = 500;
                notificationClientAdded.ledOffMS = 1000;
            } else {
                notificationClientAdded.flags &= ~Notification.FLAG_SHOW_LIGHTS;
            }
            String sound = prefs.getString("client_sound", null);
            if (sound == null) {
                notificationClientAdded.defaults |= Notification.DEFAULT_SOUND;
                // | Notification.DEFAULT_VIBRATE // requires permission
            } else {
                if (sound.length() > 0)
                    notificationClientAdded.sound = Uri.parse(sound);
            }
            Intent notificationIntent = new Intent(this, StatusActivity.class);
            notificationIntent.setAction(ACTION_CLIENTS);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            notificationClientAdded.setLatestEventInfo(this, getString(R.string.app_name),
                            getString(R.string.notify_client) + " " + cd.toNiceString(), contentIntent);
            notificationManager.notify(NOTIFY_CLIENT, notificationClientAdded);
        }

        if (clientsActivity != null)
            clientsActivity.update();
    }

    void cancelClientNotify() {
        notificationManager.cancel(NOTIFY_CLIENT);
    }

    void processStarted() {
        Log.w(TAG, "processStarted");
        Intent notificationIntent = new Intent(this, StatusActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(this, getString(R.string.app_name),
                        getString(R.string.notify_running), contentIntent);
        notificationManager.notify(NOTIFY_RUNNING, notification);
        service.startForegroundCompat(NOTIFY_RUNNING, notification);
    }

    void processStopped() {
        Log.w(TAG, "processStopped");
        notificationManager.cancel(NOTIFY_RUNNING);
        notificationManager.cancel(NOTIFY_CLIENT);
        if (service != null) service.stopSelf();
        service = null;
        updateStatus();
    }

    void failed(int err) {
        if (statusActivity != null) {
            if (err == ERROR_ROOT) {
                statusActivity.showDialog(StatusActivity.DLG_ROOT);
            } else if (err == ERROR_SUPPLICANT) {
                statusActivity.showDialog(StatusActivity.DLG_SUPPLICANT);
            } else if (err == ERROR_OTHER) {
                statusActivity.getTabHost().setCurrentTab(0); // show log
                statusActivity.showDialog(StatusActivity.DLG_ERROR);
            }
        }
        if ((statusActivity == null) || !statusActivity.hasWindowFocus()) {
            Log.d(TAG, "notifying error");
            notificationManager.notify(NOTIFY_ERROR, notificationError);
        }
    }

    /** find default route interface */
    protected boolean findIfWan() {
        String if_wan = prefs.getString(getString(R.string.if_wan), "");
        if (if_wan.length() != 0) return true;

        // must find mobile data interface
        ArrayList<String> routes = Util.readLinesFromFile("/proc/net/route");
        for (int i = 1; i < routes.size(); ++i) {
            String line = routes.get(i);
            String[] tokens = line.split("\\s+");
            if (tokens[1].equals("00000000")) {
                // this is our default route
                if_wan = tokens[0];
                break;
            }
        }
        if (if_wan.length() != 0) {
            updateToast(getString(R.string.wanok) + if_wan, false);
            prefs.edit().putString(getString(R.string.if_wan), if_wan).commit();
            return true;
        }
        // it might be okay in local mode
        return prefs.getBoolean("wan_nowait", false);
    }

    protected void foundIfLan(String found_if_lan) {
        String if_lan = prefs.getString(getString(R.string.if_lan), "");
        if (if_lan.length() == 0) {
            updateToast(getString(R.string.lanok) + found_if_lan, false);
        }
        // NOTE: always use the name found by the process
        if_lan = found_if_lan;
        prefs.edit().putString(getString(R.string.if_lan), if_lan).commit();
    }

    void cleanUpNotifications() {
        if ((service != null) && (service.getState() == MeshService.STATE_STOPPED))
            processStopped(); // clean up notifications
    }
}

