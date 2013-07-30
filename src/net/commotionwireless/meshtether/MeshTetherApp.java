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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import net.commotionwireless.olsrinfo.JsonInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
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
	StatusActivity  statusActivity = null;
	LinksActivity linksActivity = null;
	InfoActivity infoActivity = null;
	private Toast toast;

	WifiManager wifiManager;

	// notifications
	private NotificationManager notificationManager;
	private Notification notification;
	private Notification notificationClientAdded;
	private Notification notificationError;
	final static int NOTIFY_RUNNING = 0;
	final static int NOTIFY_CLIENT = 1;
	final static int NOTIFY_ERROR = 2;

	JsonInfo mJsonInfo = null;
	Set<String> profiles;
	Map<String, String> profileProperties;
	String activeProfile;

	public Util.StyledStringBuilder log = null; // == service.log, unless service is dead

	@Override
	public void onCreate() {
		super.onCreate();
		Intent selfStartIntent = null;
		ConnectivityManager cm = null;

		Log.d(TAG, "onCreate");
		NativeHelper.setup(this);

		// initialize default values if not done this in the past
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		// if IP address isn't set, generate one
		if (prefs.getString(getString(R.string.adhoc_ip), "").equals("")) {
			SharedPreferences.Editor e = prefs.edit();
			String myIP = "10." + String.valueOf((int)(Math.random() * 254))
			+ "." + String.valueOf((int)(Math.random() * 254))
			+ "." + String.valueOf((int)(Math.random() * 254));
			e.putString(getString(R.string.adhoc_ip), myIP);
			e.commit();
			Log.i(TAG, "Generated IP: " + myIP);
		}

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		notification = new Notification(R.drawable.comlogo_sm_on, getString(R.string.notify_running), 0);
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

		activeProfile = getString(R.string.defaultprofile);

		mJsonInfo = new JsonInfo();
		profiles = new CopyOnWriteArraySet<String>();
		profiles.add(activeProfile);
		profileProperties = new HashMap<String, String>();

		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

		/*
		 * We need to poke the service to get it going. 
		 * We do our best to mimic a android.net.wifi.STATE_CHANGE
		 * intent so that we can reuse code. This means that
		 * we need to putExtra() an instance of NetworkInfo.
		 */
		selfStartIntent = new Intent("net.commotionwireless.meshtether.SELF_START_INTENT");
		cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		selfStartIntent.putExtra(WifiManager.EXTRA_NETWORK_INFO, cm.getActiveNetworkInfo());
		sendBroadcast(selfStartIntent);
	}

	void setStatusActivity(StatusActivity a) { // for updates
		statusActivity = a;
	}

	void setLinksActivity(LinksActivity a) { // for updates
		linksActivity = a;
	}

	void setInfoActivity(InfoActivity a) { // for updates
		infoActivity = a;
	}

	static void broadcastState(Context ctx, int state) {
		Intent intent = new Intent(ACTION_CHANGED);
		intent.putExtra("state", state);
		ctx.sendBroadcast(intent, "net.commotionwireless.meshtether.ACCESS_STATE");
	}

	void updateStatus() {
		if (statusActivity != null)
			statusActivity.update();
		// TODO: only broadcast state if changed or stale, using a sticky intent broadcast
		/*
		 * TODO FIXME
		 */
		broadcastState(this, 0);
	}

	void updateToast(String msg, boolean islong) {
		toast.setText(msg);
		toast.setDuration(islong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
		toast.show();
	}

	/* void clientAdded(MeshService.ClientData cd) { */
	void clientAdded(Object cd) {
		if (prefs.getBoolean("client_notify", false)) {
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
					getString(R.string.notify_client) + " " + cd.toString(), contentIntent);
			notificationManager.notify(NOTIFY_CLIENT, notificationClientAdded);
		}

		if (linksActivity != null)
			linksActivity.update();
	}

	void cancelClientNotify() {
		notificationManager.cancel(NOTIFY_CLIENT);
	}

	void failed(int err) {
		if (statusActivity != null) {
			if (err == ERROR_ROOT) {
				statusActivity.showDialog(StatusActivity.DLG_ROOT);
			} else if (err == ERROR_SUPPLICANT) {
				statusActivity.showDialog(StatusActivity.DLG_SUPPLICANT);
			} else if (err == ERROR_OTHER) {
				statusActivity.getTabHost().setCurrentTab(0); // show links
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
		// TODO move to Util.java and actually detect if there is a GSM/CDMA net connection
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

	void showProgressMessage(int resId) {
		showProgressMessage(getString(resId));
	}

	void showProgressMessage(String messageText) {
		Log.i(TAG, "MSG_PROGRESSDIALOG");
		if (messageText == null) messageText = "(null)";
		// TODO remove these null check and fix the actual bug! these should
		// always exist when this is called
		if (statusActivity == null) {
			Log.e(TAG, "statusActivity is null!");
			return;
		}
		if (statusActivity.mProgressDialog == null) {
			Log.e(TAG, "statusActivity.mProgressDialog is null!");
			return;
		}
		statusActivity.mProgressDialog.setMessage(messageText);
		if ( !statusActivity.mProgressDialog.isShowing())
			statusActivity.mProgressDialog.show();
	}

	void hideProgressDialog() {
		Log.i(TAG, "MSG_PROGRESSDIALOG_DISMISS");
		statusActivity.mProgressDialog.dismiss();
	}
}

