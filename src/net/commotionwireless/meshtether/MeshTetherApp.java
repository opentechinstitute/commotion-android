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

import net.commotionwireless.olsrd.OlsrdService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


/**
 * Manages preferences, activities and prepares the service
 */
public class MeshTetherApp extends android.app.Application {
	final static String TAG = "MeshTetherApp";

	SharedPreferences mPrefs;
	StatusActivity  statusActivity = null;
	LinksActivity linksActivity = null;
	InfoActivity infoActivity = null;
	private Toast toast;

	// notifications
	private NotificationManager notificationManager;
	private Notification notification;
	private Notification notificationClientAdded;
	private Notification notificationError;
	final static int NOTIFY_RUNNING = 0;
	final static int NOTIFY_CLIENT = 1;
	final static int NOTIFY_ERROR = 2;

	private OlsrdService mOlsrdService;

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
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		notification = new Notification(R.drawable.comlogo_sm, getString(R.string.notify_running), 0);
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notificationClientAdded = new Notification(android.R.drawable.stat_sys_warning,
				getString(R.string.notify_client), 0);
		notificationClientAdded.flags = Notification.FLAG_AUTO_CANCEL;
		notificationError = new Notification(R.drawable.comlogo_error,
				getString(R.string.notify_error), 0);
		notificationError.setLatestEventInfo(this,
				getString(R.string.app_name),
				getString(R.string.notify_error),
				PendingIntent.getActivity(this, 0, new Intent(this, StatusActivity.class), 0));
		notificationError.flags = Notification.FLAG_AUTO_CANCEL;

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

	public void setOlsrdService(OlsrdService s) {
		mOlsrdService = s;
	}

	public OlsrdService getOlsrdService() {
		return mOlsrdService;
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

	void updateStatus() {
		if (statusActivity != null)
			statusActivity.update();
	}

	void updateToast(String msg, boolean islong) {
		toast.setText(msg);
		toast.setDuration(islong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
		toast.show();
	}

	void clientAdded(Object cd) {
		if (linksActivity != null)
			linksActivity.update();
	}

	void cancelClientNotify() {
		notificationManager.cancel(NOTIFY_CLIENT);
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

