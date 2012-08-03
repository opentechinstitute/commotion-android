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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import net.commotionwireless.meshtether.Util.MACAddress;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Manages the running process, client list, and log
 */
public class MeshService extends android.app.Service {
	final static String TAG = "MeshService";
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
	final static int MSG_STOP_OLSRD_OUTPUT = 10;
	// messages for the ProgressDialog
	final static int MSG_SHOW_PROGRESSDIALOG = 11;
	final static int MSG_HIDE_PROGRESSDIALOG = 12;
	// app states
	public final static int STATE_STOPPED  = 0;
	public final static int STATE_STARTING = 1;
	public final static int STATE_RUNNING  = 2; // process said OK

	// private state
	private int state = STATE_STOPPED;
	private WifiManager.WifiLock wifiLock;
	private MeshTetherProcess WifiProcess = null;

	private String activeSSID = "";
	private String activeBSSID = "";
	private String activeIP = "";
	private String activeNetmask = "";
	private String activeIpGeneration = "";
	private String activeDNS = "";
	private String activeOlsrdConf = "";

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
		final String remoteIP;
		final float linkQuality;
		final float neighborLinkQuality;
		final int linkCost;
		final int validityTime;
		boolean hasHna;
		ClientData(String ip, float lq, float nlq, int lc, int vt) {
			remoteIP = ip;
			linkQuality = lq;
			neighborLinkQuality = nlq;
			linkCost = lc;
			validityTime = vt;
			hasHna = false;
		}
		public String toString() { return remoteIP + " " + linkQuality + " " + neighborLinkQuality; }
		public String toNiceString() { return remoteIP; }
	}
	public final ArrayList<ClientData> clients = new ArrayList<ClientData>();
	public final Util.TrafficStats stats = new Util.TrafficStats();

	// WARNING: this is not entirely safe
	public static MeshService singleton = null;

	// cached for convenience
	private String if_lan = "";
	private Util.MACAddress if_mac = null;
	private MeshTetherApp app;
	private WifiManager wifiManager;
	private int currentNetId = -1;
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

		app = (MeshTetherApp)getApplication();
		app.serviceStarted(this);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MeshService");
		wakeLock.acquire();

		wifiLock = wifiManager.createWifiLock (WifiManager.WIFI_MODE_FULL, "MeshService");
		// WifiManager.WIFI_MODE_FULL_HIGH_PERF is only available at API level 12

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

	private static String getWifiStateString(int state) {
		switch (state) {
		case WifiManager.WIFI_STATE_ENABLED:
			return "WIFI_STATE_ENABLED";
		case WifiManager.WIFI_STATE_ENABLING:
			return "WIFI_STATE_ENABLING";
		case WifiManager.WIFI_STATE_DISABLED:
			return "WIFI_STATE_DISABLED";
		case WifiManager.WIFI_STATE_DISABLING:
			return "WIFI_STATE_DISABLING";
		case WifiManager.WIFI_STATE_UNKNOWN:
			return "WIFI_STATE_UNKNOWN";
		default:
			return "(none)";
		}
	}

	// our handler
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) { handle(msg); }
	};

	private void handle(Message msg) {
		switch (msg.what) {
		case MSG_SHOW_PROGRESSDIALOG:
			Log.i(TAG, "handle MSG_SHOW_PROGRESSDIALOG");
			app.showProgressMessage((String)msg.obj);
			break;
		case MSG_HIDE_PROGRESSDIALOG:
			Log.i(TAG, "handle MSG_HIDE_PROGRESSDIALOG");
			app.hideProgressDialog();
			break;
		case MSG_EXCEPTION:
			Log.i(TAG, "handle MSG_EXCEPTION");
			if (state == STATE_STOPPED) return;
			Throwable thr = (Throwable)msg.obj;
			log(true, getString(R.string.exception) + " " + thr.getMessage());
			Log.e(TAG, "Exception " + thr.getMessage() + " " + Log.getStackTraceString(thr));
			stopProcess();
			state = STATE_STOPPED;
			break;
		case MSG_ERROR:
			Log.i(TAG, "handle MSG_ERROR");
			if (state == STATE_STOPPED) return;
			if (WifiProcess == null) return; // don't kill it again...
			if (msg.obj != null) {
				String line = (String)msg.obj;
				log(true, line); // just dump it and ignore it
			} else {
				// no message, means process died
				log(true, getString(R.string.unexpected));
				stopProcess();

				if ((state == STATE_STARTING)) {
					String err = log.toString();
					if (isRootError(err)) {
						app.failed(MeshTetherApp.ERROR_ROOT);
					} else if (isSupplicantError(err)) {
						app.failed(MeshTetherApp.ERROR_SUPPLICANT);
					} else {
						app.failed(MeshTetherApp.ERROR_OTHER);
					}
				} else {
					app.failed(MeshTetherApp.ERROR_OTHER);
				}
				state = STATE_STOPPED;
			}
			break;
		case MSG_OUTPUT:
			Log.i(TAG, "handle MSG_OUTPUT");
			if (state == STATE_STOPPED) return;
			if (WifiProcess == null) return; // cut the gibberish
			String line = (String)msg.obj;
			if (line == null) {
				// ignore it, wait for MSG_ERROR(null)
				break;
			}
			if (line.startsWith("WIFI: OK")) {
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
					mHandler.sendEmptyMessage(MSG_HIDE_PROGRESSDIALOG);
				}
			} else {
				log(false, line);
			}
			break;
		case MSG_START:
			Log.i(TAG, "handle MSG_START");

			if (state != STATE_STOPPED) return;
			log.clear();

			if ( !setMeshProfile())
				break;
			log(false, getString(R.string.starting) + " " + activeSSID);

			// TODO make this only overwrite on upgrade to new version
			log(false, getString(R.string.unpacking));
			if (!NativeHelper.unzipAssets(this) || !NativeHelper.installBusyboxSymlinks()) {
				log(true, getString(R.string.unpackerr));
				state = STATE_STOPPED;
				break;
			}

			if ( !wifiManager.isWifiEnabled()) {
				log(false, getString(R.string.enablingwifi));
				wifiManager.setWifiEnabled(true); // this will send MSG_NETSCHANGE
			}
			if (wifiManager.getConnectionInfo().getIpAddress() != 0) // if connected, disconnect
				wifiManager.disconnect(); // this will send MSG_NETSCHANGE

			state = STATE_STARTING;
			// FALL THROUGH!
		case MSG_NETSCHANGE:
			Log.i(TAG, "handle MSG_NETSCHANGE");
			int wifiState = wifiManager.getWifiState();
			String wifiStateString = getWifiStateString(wifiState);
			Log.e(TAG, String.format("NETSCHANGE: wifiState: %s state: %d WifiProcess: %s",
					wifiStateString, state, WifiProcess == null ? "null" : "proc"));
			if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
				// wifi is good (or lost), we can start now...
				if ((state == STATE_STARTING) && (WifiProcess == null)) {
					if (app.findIfWan()) {
						// TODO if WAN found with checkUplink(), then setup Hna4 routing
						log(false, "Found active WAN interface");
					} else {
						///log(true, getString(R.string.wanerr));
						//state = STATE_STOPPED;
						//break;
						log(false, "No active WAN interface found");
					}

					log(false, "Enabling wifi for " + activeSSID);
					WifiConfiguration wc = new WifiConfiguration();
					wc.priority = 100000;
					wc.hiddenSSID = true; // Android won't see Adhoc SSIDs
					wc.status = WifiConfiguration.Status.ENABLED;
					wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
					wc.SSID = activeSSID;
					wc.BSSID = activeBSSID;
					currentNetId = wifiManager.addNetwork(wc);
					if(currentNetId < 0) {
						System.out.println("Unable to add network configuration for " + activeSSID);
						return;
					}
					wifiManager.enableNetwork(currentNetId, true);

					// set up the static IP settings
					// TODO read and store the current settings so they can be restored on stop
					log(false, "Setting up IP config");
					final ContentResolver cr = getContentResolver();
					Settings.System.putInt(cr, Settings.System.WIFI_USE_STATIC_IP, 1);
					Settings.System.putString(cr, Settings.System.WIFI_STATIC_IP, activeIP);
					Settings.System.putString(cr, Settings.System.WIFI_STATIC_NETMASK, activeNetmask);
					Settings.System.putString(cr, Settings.System.WIFI_STATIC_GATEWAY, activeIP);
					Settings.System.putString(cr, Settings.System.WIFI_STATIC_DNS1, activeDNS);

					// meshing works very poorly with wifi sleep enabled
					// TODO check if this sticks after the app quits, if so
					// save the current value and restore it on stopping
					Settings.System.putInt(cr, Settings.System.WIFI_SLEEP_POLICY,
							Settings.System.WIFI_SLEEP_POLICY_NEVER);

					if (!startProcess()) {
						log(true, getString(R.string.starterr));
						state = STATE_STOPPED;
						break;
					}
				} // if not checkUpLink then we simply wait...
			} else { // not WIFI_STATE_ENABLED
				app.updateToast("WIFI: " + wifiStateString, false); log(false, "WIFI: " + wifiStateString);
				switch (state) {
				case STATE_RUNNING:
					app.updateToast("STATE_RUNNING", true); log(false, "STATE_RUNNING");
					stopProcess(); // this tears down everything
					state = STATE_STARTING;
					break;
				case STATE_STARTING:
					app.updateToast("STATE_STARTING", false); log(false, "STATE_STARTING");
					break;
				case STATE_STOPPED:
					app.updateToast("STATE_STOPPED", false); log(false, "STATE_STOPPED");
					break;
				default:
					app.updateToast("Something went wrong...", false); 	log(false, "Something went wrong...");
				}
			}
			break;
		case MSG_STOP:
			Log.i(TAG, "handle MSG_STOP");
			if (state == STATE_STOPPED) return;
			stopProcess();
			wifiManager.disableNetwork(currentNetId);
			wifiManager.removeNetwork(currentNetId);
			final ContentResolver cr = getContentResolver();
			Settings.System.putInt(cr, Settings.System.WIFI_USE_STATIC_IP, 0);
			log(false, getString(R.string.stopped));
			state = STATE_STOPPED;
			break;
		case MSG_ASSOC:
			Log.i(TAG, "handle MSG_ASSOC");
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
			//Log.i(TAG, "handle MSG_STATS");
			mHandler.removeMessages(MSG_STATS);
			if (state != STATE_RUNNING || if_lan.length() == 0) return;
			stats.update(Util.fetchTrafficData(if_lan));
			break;
		}
		app.updateStatus();
		if (state == STATE_STOPPED) {
			app.processStopped();
			app.hideProgressDialog();
		}
	}

	protected void log(boolean error, String msg) {
		android.text.format.Time time = new android.text.format.Time();
		time.setToNow();
		Log.i(TAG, "log: " + msg);
		log.append(COLOR_TIME, time.format("%H:%M:%S\t"))
		.append(error ? COLOR_ERROR : COLOR_LOG, msg)
		.append("\n");
		Message message = mHandler.obtainMessage();
		message.what = MSG_SHOW_PROGRESSDIALOG;
		message.obj = msg;
		mHandler.sendMessage(message);
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

	private class MeshTetherProcess {
		private final static int INPUT_THREAD = 0;
		private final static int ERROR_THREAD = 1;

		private boolean mRunning;
		private Thread mIoThreads[] = new Thread[2];
		private Process mProcess = null;
		private String mProg;
		private String[] mEnvp;
		private File mDirectory;
		private int mExitValue;

		public MeshTetherProcess(String prog, String[] envp, File directory) {
			mProg = prog;
			mEnvp = envp;
			mDirectory = directory;
			mRunning = false;
			mExitValue = 0;
		}

		public void stop() throws IOException, InterruptedException {
			if (mProcess != null) {
				mProcess.getOutputStream().close();
				mProcess.getErrorStream().close();

				mIoThreads[INPUT_THREAD].interrupt();
				mIoThreads[ERROR_THREAD].interrupt();

				mIoThreads[INPUT_THREAD] = null;
				mIoThreads[ERROR_THREAD] = null;

				mProcess.destroy();
				mProcess.waitFor();
				mExitValue = mProcess.exitValue();
				mProcess = null;
				mRunning = false;
			}
		}

		public void run(Handler handler, int outputTag, int errorTag) throws IOException {
			mProcess = Runtime.getRuntime().exec(mProg, mEnvp, mDirectory);
			mIoThreads[INPUT_THREAD] = new Thread(new OutputMonitor(outputTag, mProcess.getInputStream()));
			mIoThreads[ERROR_THREAD] = new Thread(new OutputMonitor(errorTag, mProcess.getErrorStream()));
			mIoThreads[INPUT_THREAD].start();
			mIoThreads[ERROR_THREAD].start();
			mRunning = true;
		}

		public void tell(byte[] msg) throws IOException {
			if (mRunning)
				mProcess.getOutputStream().write(msg);
		}
		public void runUntilExit(Handler handler, int outputTag, int errorTag) throws IOException, InterruptedException {
			run(handler,outputTag,errorTag);
			mProcess.waitFor();

			mIoThreads[INPUT_THREAD].interrupt();
			mIoThreads[ERROR_THREAD].interrupt();

			mIoThreads[INPUT_THREAD] = null;
			mIoThreads[ERROR_THREAD] = null;

			mProcess.destroy();
			mExitValue = mProcess.exitValue();

			mProcess = null;
			mRunning = false;
		}
		public int exitValue() {
			if (!mRunning)
				return mExitValue;
			else
				return 0;
		}
	}

	private boolean checkUplink() {
		if (app.prefs.getBoolean("wan_nowait", false)) {
			return true;
		}
		NetworkInfo mobileInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		NetworkInfo wimaxInfo = connManager.getNetworkInfo(6);
		return (mobileInfo.isConnected() || ((wimaxInfo != null) && wimaxInfo.isConnected()));
	}

	private String getPrefValue(String pref) {

		// initialize default values if not done this in the past
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		final int[] ids = SettingsActivity.prefids;
		for (int i = 0; i < ids.length; ++i) {
			String k = getString(ids[i]);
			String v = prefs.getString(k, null);
			if (k.equalsIgnoreCase(pref))
				return v;
		}
		return null;
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
				// Special handling for Mesh Prefix 
				if (k.equalsIgnoreCase(getString(R.string.adhoc_ip)))
				{
					if (v.matches("^[0-9]{1,3}$"))
					{
						// Convert the Mesh Prefix into a Mesh IP.
						WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
						if (wifiManager != null)
						{
							WifiInfo wifiInfo = wifiManager.getConnectionInfo();
							if (wifiInfo != null)
							{
								Log.d(TAG, "Mac addr: " + wifiInfo.getMacAddress());
								MACAddress macAddress = MACAddress.parse(wifiInfo.getMacAddress());
								v += "." + macAddress.getOctet(4) + "." + macAddress.getOctet(5) + "." + macAddress.getOctet(6);
							}
						}
					}
				}
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
		envlist.add("olsrd_conf_path=" + activeOlsrdConf);
		
		String[] ret = (String[]) envlist.toArray(new String[0]);
		for (String s : ret) {
			Log.i(TAG, "env var: " + s);
		}
		return ret;
	}

	private String formatSSID(String ssid) {
		if (ssid == null)
			return null;
		return "\"".concat(ssid).concat("\"");
	}

	private String generateIP(String base, String netmask) {
		String[] netmaskBytes = netmask.split("[.]+");
		String[] baseBytes = base.split("[.]+");
		// start at byte 1 since a completely random IP would be stupid
		String ret = baseBytes[0];
		for (int i=1; i<baseBytes.length; i++) {
			// if the netmask byte doesn't exist or is zero, generate that byte
			ret += ".";
			if (baseBytes[i].equals("0") && (i >= netmaskBytes.length || netmaskBytes[i].equals("0")))
				ret += String.valueOf((int)(Math.random() * 254));
			else
				ret += Byte.parseByte(baseBytes[i]);
		}
		return ret;
	}

	private boolean setMeshProfile() {
		activeOlsrdConf = new File(NativeHelper.app_bin, "olsrd.conf").getAbsolutePath();
		if (app.activeProfile.equals(getString(R.string.defaultprofile))) {
			activeSSID = formatSSID(getPrefValue(getString(R.string.lan_essid)));
			activeBSSID = getPrefValue(getString(R.string.lan_bssid));
			activeIP = getPrefValue(getString(R.string.adhoc_ip));
			activeNetmask = getPrefValue(getString(R.string.adhoc_netmask));
			activeDNS = getPrefValue(getString(R.string.adhoc_dns_server));
		} else {
			// TODO
			Properties prop = new Properties();
			String propFilename = app.profileProperties.get(app.activeProfile);
			String confFilename = propFilename.replace(".properties", ".conf");
			if (new File(confFilename).exists())
				activeOlsrdConf = confFilename;
			try {
				prop.load(new FileInputStream(propFilename));
			} catch (Exception e) {
				e.printStackTrace();
				app.updateToast(getString(R.string.profile_load_error) + " " + propFilename, false);
				return false;
			}
			activeSSID = formatSSID(prop.getProperty("ssid"));
			activeBSSID = prop.getProperty("bssid");
			activeNetmask = prop.getProperty("netmask");
			activeIpGeneration = prop.getProperty("ipgenerate");
			if (activeIpGeneration != null && 
					(activeIpGeneration.equals("true") || activeIpGeneration.equals("1"))) {
				activeIP = generateIP(prop.getProperty("ip"), activeNetmask);
				activeIpGeneration = "true";
			} else {
				activeIP = prop.getProperty("ip");
				activeIpGeneration = "false";
			}
			activeDNS = prop.getProperty("dns");
		}
		return true;
	}

	String[] getActiveMeshProfile() {
		 String[] ret = { activeSSID, activeBSSID, activeIP, activeNetmask, activeIpGeneration, activeDNS, activeOlsrdConf };
		 return ret;
	}

	private boolean startProcess() {
		// calling 'su -c' from Java doesn't work so we use a helper script
		log(false, "Aquiring wifi lock");
		wifiLock.acquire();
		app.showProgressMessage(R.string.startingolsrd);
		String cmd = NativeHelper.SU_C;
		try {
			WifiProcess = new MeshTetherProcess(cmd, buildEnvFromPrefs(), NativeHelper.app_bin);
			WifiProcess.run(mHandler, MSG_OUTPUT, MSG_ERROR);
		} catch (Exception e) {
			log(true, String.format(getString(R.string.execerr), cmd));
			Log.e(TAG, "start failed " + e.toString());
			return false;
		}
		return true;
	}

	private void stopProcess() {
		/*
		 * TODO: UN copy and paste!!
		 */
		if (state != STATE_STOPPED) {
			app.showProgressMessage(R.string.servicestopping);
			try {	
				MeshTetherProcess StopOlsrProcess = new MeshTetherProcess(NativeHelper.STOP_OLSRD,
						null, NativeHelper.app_bin);
				StopOlsrProcess.runUntilExit(mHandler, MSG_STOP_OLSRD_OUTPUT, MSG_STOP_OLSRD_OUTPUT);

			} catch (IOException e) {
				log(false, "Error occurred while stopping Olsrd: " + e.getMessage());
				e.printStackTrace();
			} catch (InterruptedException e) {
				log(false, "Error occurred while stopping Olsrd: " + e.getMessage());
				e.printStackTrace();
			}

			if (WifiProcess != null) {
				// first, just close the stream
				if (state != STATE_STOPPED) {
					try {
						WifiProcess.stop();
					} catch (Exception e) {
						Log.w(TAG, "Exception while closing process");
					}
				}

				try {
					int exit_status = WifiProcess.exitValue();
					Log.i(TAG, "Wifi Process exited with status: " + exit_status);
				} catch (IllegalThreadStateException e) {
					// this is not good
					log(true, getString(R.string.dirtystop));
				}
				WifiProcess = null;
			}
			log(false, "Disconnecting from " + activeSSID);
			wifiManager.disconnect();
			log(false, "Releasing wifi lock");
			wifiLock.release();
			clients.clear();
			app.clientsActivity.update();
			app.hideProgressDialog();
		}
	}


	private boolean tellProcess(String msg) {
		if (WifiProcess != null) {
			try {
				WifiProcess.tell((msg+"\n").getBytes());
				return true;
			} catch (Exception e) {} // just ignore it
		}
		return false;
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
		//setForeground(true);
	}

	public static boolean isSupplicantError(String msg) {
		return msg.contains("supplicant");
	}

	public static boolean isRootError(String msg) {
		return msg.contains("ermission") || msg.contains("su: not found");
	}

}

