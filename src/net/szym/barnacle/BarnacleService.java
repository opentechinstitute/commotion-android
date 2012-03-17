/*
*  This file is part of Barnacle Wifi Tether
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

package net.szym.barnacle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

/**
* Manages the running process, client list, and log
*/
public class BarnacleService extends android.app.Service {
    final static String TAG = "BarnacleService";
    // messages from the process
    final static int MSG_OUTPUT     = 1;
    final static int MSG_ERROR      = 2;
    // messages from self
    final static int MSG_EXCEPTION  = 3;
    final static int MSG_NETSCHANGE = 4;
    // requests from activities
    final static int MSG_START      = 5;
    final static int MSG_STOP       = 6;
    final static int MSG_ASSOC      = 7;
    final static int MSG_STATS      = 8;
    // app states
    public final static int STATE_STOPPED  = 0;
    public final static int STATE_STARTING = 1;
    public final static int STATE_RUNNING  = 2; // process said OK

    // private state
    private int state = STATE_STOPPED;
    private Process process = null; // the barnacle process
    // output monitoring threads
    private Thread[] threads = new Thread[2];
    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mHandler.sendEmptyMessage(MSG_NETSCHANGE);
        }
    };

    // public state
    public final Util.StyledStringBuilder log = new Util.StyledStringBuilder();

    final static int COLOR_ERROR    = 0xffff2222;
    final static int COLOR_LOG      = 0xff888888;//android.R.color.primary_text_dark;
    final static int COLOR_TIME     = 0xffffffff;

    public static class ClientData {
        final String mac;
        final String ip;
        final String hostname;
        boolean allowed;
        ClientData(String m, String i, String h) { mac = m; ip = i; hostname = h; allowed = false; }
        public String toString() { return mac + " " + ip + " " + hostname; }
        public String toNiceString() { return hostname != null ? hostname : mac; }
    }
    public final ArrayList<ClientData> clients = new ArrayList<ClientData>();
    public final Util.TrafficStats stats = new Util.TrafficStats();

    // WARNING: this is not entirely safe
    public static BarnacleService singleton = null;

    // cached for convenience
    private String if_lan = "";
    private Util.MACAddress if_mac = null;
    private BarnacleApp app;
    private WifiManager wifiManager;
    private ConnectivityManager connManager;
    private boolean filteringEnabled = false;
    private Method mStartForeground = null;

    /** public service interface */
    public void startRequest() {
        mHandler.sendEmptyMessage(MSG_START);
    }

    public void assocRequest() {
        mHandler.sendEmptyMessage(MSG_ASSOC);
    }

    public void stopRequest() {
        mHandler.sendEmptyMessage(MSG_STOP);
    }

    public void statsRequest(long delay) {
        Message msg = mHandler.obtainMessage(MSG_STATS);
        mHandler.sendMessageDelayed(msg, delay);
    }

    public int getState() {
        return state;
    }

    public boolean hasFiltering() {
        return filteringEnabled;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        singleton = this;

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        try {
            mStartForeground = getClass().getMethod("startForeground", new Class[] {
                    int.class, Notification.class});
        } catch (NoSuchMethodException e) {
            mStartForeground = null;
        }

        state = STATE_STOPPED;
        filteringEnabled = false;

        app = (BarnacleApp)getApplication();
        app.serviceStarted(this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BarnacleService");
        wakeLock.acquire();

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, filter);
    }

    @Override
    public void onDestroy() {
        if (state != STATE_STOPPED)
            Log.e(TAG, "service destroyed while running!");
        // ensure we clean up
        stopProcess();
        state = STATE_STOPPED;
        app.processStopped();
        wakeLock.release();

        try {
            unregisterReceiver(connectivityReceiver);
        } catch (Exception e) {
            // ignore
        }

        singleton = null;
        super.onDestroy();
    }

    // our handler
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) { handle(msg); }
    };

    private void handle(Message msg) {
        switch (msg.what) {
        case MSG_EXCEPTION:
            if (state == STATE_STOPPED) return;
            Throwable thr = (Throwable)msg.obj;
            log(true, getString(R.string.exception) + " " + thr.getMessage());
            Log.e(TAG, "Exception " + thr.getMessage() + " " + Log.getStackTraceString(thr));
            stopProcess();
            state = STATE_STOPPED;
            break;
        case MSG_ERROR:
            if (state == STATE_STOPPED) return;
            if (process == null) return; // don't kill it again...
            if (msg.obj != null) {
                String line = (String)msg.obj;
                log(true, line); // just dump it and ignore it
                if (line.startsWith("dnsmasq: DHCPACK")) {
                    String[] vals = line.split(" +");
                    if (vals.length > 3) {
                        ClientData cd = new ClientData(vals[3], vals[2], vals.length > 4 ? vals[4] : null);
                        clientAdded(cd);
                    }
                }
            } else {
                // no message, means process died
                log(true, getString(R.string.unexpected));
                stopProcess();

                if ((state == STATE_STARTING)) {
                    String err = log.toString();
                    if (isRootError(err)) {
                        app.failed(BarnacleApp.ERROR_ROOT);
                    } else if (isSupplicantError(err)) {
                        app.failed(BarnacleApp.ERROR_SUPPLICANT);
                    } else {
                        app.failed(BarnacleApp.ERROR_OTHER);
                    }
                } else {
                    app.failed(BarnacleApp.ERROR_OTHER);
                }
                state = STATE_STOPPED;
            }
            break;
        case MSG_OUTPUT:
            if (state == STATE_STOPPED) return;
            if (process == null) return; // cut the gibberish
            String line = (String)msg.obj;
            if (line == null) {
                // ignore it, wait for MSG_ERROR(null)
                break;
            }
            if (line.startsWith("DHCP: ACK")) {
                // DHCP: ACK <MAC> <IP> [<HOSTNAME>]
                String[] vals = line.split(" +");
                ClientData cd = new ClientData(vals[2], vals[3], vals.length > 4 ? vals[4] : null);
                clientAdded(cd);
            } else if (line.startsWith("WIFI: OK")) {
                // WIFI: OK <IFNAME> <MAC>
                String[] parts = line.split(" +");
                if_lan = parts[2];
                if_mac = Util.MACAddress.parse(parts[3]);
                if (state == STATE_STARTING) {
                    state = STATE_RUNNING;
                    log(false, getString(R.string.running));
                    clients.clear();
                    stats.init(Util.fetchTrafficData(if_lan));
                    //app.foundIfLan(if_lan); // this will allow 3G <-> 4G with simple restart
                    app.processStarted();
                    mHandler.sendEmptyMessage(MSG_ASSOC);
                }
            } else {
                log(false, line);
            }
            break;
        case MSG_START:

            if (state != STATE_STOPPED) return;
            log.clear();
            log(false, getString(R.string.starting));

            // TODO make this only overwrite on upgrade to new version
            if (!NativeHelper.unzipAssets(this)) {
                log(true, getString(R.string.unpackerr));
                state = STATE_STOPPED;
                break;
            }
            state = STATE_STARTING;
            // FALL THROUGH!
        case MSG_NETSCHANGE:
            int wifiState = wifiManager.getWifiState();
            Log.e(TAG, String.format("NETSCHANGE: %d %d %s", wifiState, state, process == null ? "null" : "proc"));
            if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
                // wifi is good (or lost), we can start now...
            	if ((state == STATE_STARTING) && (process == null)) {
            		if (app.findIfWan()) {
            			// TODO if WAN found with checkUplink(), then setup Hna4 routing
            			log(false, "Found active WAN interface");
            		} else {
                        ///log(true, getString(R.string.wanerr));
                        //state = STATE_STOPPED;
                        //break;
                        log(false, "no active WAN interface found");
                    }
                    if (!startProcess()) {
                        log(true, getString(R.string.starterr));
                        state = STATE_STOPPED;
                        break;
                    }
                } // if not checkUpLink then we simply wait...
            } else {
                if (state == STATE_RUNNING) {
                    // this is super bad, will have to restart!
                    app.updateToast(getString(R.string.conflictwifi), true);
                    log(true, getString(R.string.conflictwifi));
                    log(false, getString(R.string.restarting));
                    stopProcess(); // this tears down wifi
                    wifiManager.setWifiEnabled(false); // this will send MSG_NETSCHANGE
                    // we should wait until wifi is disabled...
                    state = STATE_STARTING;
                } else if (state == STATE_STARTING) {
                    if ((wifiState == WifiManager.WIFI_STATE_ENABLED) ||
                        (wifiState == WifiManager.WIFI_STATE_ENABLING)) {
                        app.updateToast(getString(R.string.disablewifi), false);
                        wifiManager.setWifiEnabled(false);
                        log(false, getString(R.string.waitwifi));
                    }
                }
            }
            break;
        case MSG_STOP:
            if (state == STATE_STOPPED) return;
            stopProcess();
            log(false, getString(R.string.stopped));
            state = STATE_STOPPED;
            break;
        case MSG_ASSOC:
            if (state != STATE_RUNNING) return;
            if (tellProcess("WLAN")) {
                app.updateToast(getString(R.string.beaconing), true);
            }
            if (clients.isEmpty() && app.prefs.getBoolean("lan_autoassoc", false)) {
                mHandler.removeMessages(MSG_ASSOC);
                // rebeacon, in 5 seconds
                mHandler.sendEmptyMessageDelayed(MSG_ASSOC, 5000);
            }
            break;
        case MSG_STATS:
            mHandler.removeMessages(MSG_STATS);
            if (state != STATE_RUNNING || if_lan.length() == 0) return;
            stats.update(Util.fetchTrafficData(if_lan));
            break;
        }
        app.updateStatus();
        if (state == STATE_STOPPED)
            app.processStopped();
    }

    protected void log(boolean error, String msg) {
        android.text.format.Time time = new android.text.format.Time();
        time.setToNow();
        Log.i(TAG, "log: " + msg);
        log.append(COLOR_TIME, time.format("%H:%M:%S\t"))
          .append(error ? COLOR_ERROR : COLOR_LOG, msg)
          .append("\n");
    }

    /** Worker Threads */
    private class OutputMonitor implements Runnable {
        private final java.io.BufferedReader br;
        private final int msg;
        public OutputMonitor(int t, java.io.InputStream is) {
            br = Util.toReader(is);
            msg = t;
        }
        public void run() {
            try{
                String line;
                do {
                    line = br.readLine();
                    mHandler.obtainMessage(msg, line).sendToTarget(); // NOTE: the last null is also sent!
                } while(line != null);
            } catch (Exception e) {
                mHandler.obtainMessage(MSG_EXCEPTION, e).sendToTarget();
            }
        }
    }

    private void clientAdded(ClientData cd) {

        for (int i = 0; i < clients.size(); ++i) {
            ClientData c = clients.get(i);
            if (c.mac.equals(cd.mac)) {
                if (c.ip.equals(cd.ip)) {
                    log(false, String.format(getString(R.string.renewed), cd.toNiceString()));
                    return; // no change
                }
                cd.allowed = c.allowed;
                clients.remove(i); // we'll add it at the end
                break;
            }
        }
        clients.add(cd);

        log(false, String.format(getString(R.string.connected), cd.toNiceString()));
        app.clientAdded(cd);

    }

    private boolean checkUplink() {
        if (app.prefs.getBoolean("wan_nowait", false)) {
            return true;
        }
        NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wimaxInfo = connManager.getNetworkInfo(6);
        return (mobileInfo.isConnected() || ((wimaxInfo != null) && wimaxInfo.isConnected()));
    }

    /** Prepare env vars for ./wifi from preferences */
    protected String[] buildEnvFromPrefs() {
    	ArrayList<String> envlist = new ArrayList<String>();

    	// get the existing environment first, since many programs like 'su' require 
    	// env vars like LD_LIBRARY_PATH to be set
    	Map<String, String> env = System.getenv();
    	for (String envName : env.keySet()) {
    		if (envName.equals("LD_LIBRARY_PATH")) {
    			// set LD_LIBRARY_PATH to load olsrd plugins
    			envlist.add(envName + "=" + NativeHelper.app_bin.getAbsolutePath() + ":" + env.get(envName));    			
    		} else {
    			envlist.add(envName + "=" + env.get(envName));
    		}
    	}

    	// initialize default values if not done this in the past
    	PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

    	final int[] ids = SettingsActivity.prefids;
    	for (int i = 0; i < ids.length; ++i) {
    		String k = getString(ids[i]);
    		String v = prefs.getString(k, null);
    		if (v != null && v.length() != 0) {
    			// TODO some chars need to be escaped, but this seems to add "" to the ESSID name
//    			if (ids[i] == R.string.lan_essid) {
//    				v = '"'+v+'"';
//    			}
    			envlist.add("brncl_" + k + "=" + v);
    		}
    	}
    	// not included in prefids are checkboxes
    	final int[] checks = SettingsActivity.checks;
    	for (int i = 0; i < checks.length; ++i) {
    		String k = getString(checks[i]);
    		if (prefs.getBoolean(k, false))
    			envlist.add("brncl_" + k + "=1");
    	}
    	envlist.add("brncl_path=" + NativeHelper.app_bin.getAbsolutePath());

    	String[] ret = (String[]) envlist.toArray(new String[0]);
    	for (String s : ret) {
    		Log.i(TAG, "env var: " + s);
    	}
    	return ret;
    }

    private boolean startProcess() {
    	// calling 'su -c' from Java doesn't work so we use a helper script
    	String cmd = NativeHelper.SU_C;
        try {
            process = Runtime.getRuntime().exec(cmd,
            		buildEnvFromPrefs(), NativeHelper.app_bin);
            threads[0] = new Thread(new OutputMonitor(MSG_OUTPUT, process.getInputStream()));
            threads[1] = new Thread(new OutputMonitor(MSG_ERROR, process.getErrorStream()));
            threads[0].start();
            threads[1].start();
        } catch (Exception e) {
            log(true, String.format(getString(R.string.execerr), cmd));
            Log.e(TAG, "start failed " + e.toString());
            return false;
        }
        return true;
    }

    private boolean tellProcess(String msg) {
        if (process != null) {
            try {
                process.getOutputStream().write((msg+"\n").getBytes());
                return true;
            } catch (Exception e) {} // just ignore it
        }
        return false;
    }

    private void stopProcess() {
        if (process != null) {
            // first, just close the stream
            if (state != STATE_STOPPED) {
                try {
                    process.getOutputStream().close();
                } catch (Exception e) {
                    Log.w(TAG, "Exception while closing process");
                }
            }
            try {
                process.waitFor(); // blocking!
            } catch (InterruptedException e) {
                Log.e(TAG, "");
            }

            try {
                int exit_status = process.exitValue();
                Log.i(TAG, "Process exited with status: " + exit_status);
            } catch (IllegalThreadStateException e) {
                // this is not good
                log(true, getString(R.string.dirtystop));
            }
            process.destroy();
            process = null;
            threads[0].interrupt();
            threads[1].interrupt();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
    * This is a wrapper around the new startForeground method, using the older
    * APIs if it is not available.
    */
    public void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            try {
                mStartForeground.invoke(this, new Object[] {Integer.valueOf(id), notification});
            } catch (InvocationTargetException e) {
                Log.w(TAG, "Unable to invoke startForeground", e);
            } catch (IllegalAccessException e) {
                Log.w(TAG, "Unable to invoke startForeground", e);
            }
            return;
        }
        // Fall back on the old API.
        setForeground(true);
    }

    public static boolean isSupplicantError(String msg) {
        return msg.contains("supplicant");
    }

    public static boolean isRootError(String msg) {
        return msg.contains("ermission") || msg.contains("su: not found");
    }
}

