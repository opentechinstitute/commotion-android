package net.commotionwireless.meshtether;
import java.util.List;

import net.commotionwireless.olsrd.OlsrdService;
import net.commotionwireless.profiles.Profiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;


public class NetworkStateChangeReceiver extends BroadcastReceiver {
	
	public static WifiConfiguration getActiveWifiConfiguration(WifiManager wifiManager) {
		List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
		if (configs == null)
			return null;
		
		for (WifiConfiguration i : configs) {
			if (i.status == WifiConfiguration.Status.CURRENT) {
				return i;
			}
		}
		return null;
	}
	
	
	private void messageOlsrdService(Context context, int message) {
		messageOlsrdService(context, message, null);
	}
	private void messageOlsrdService(Context context, int message, String profileName) {
		Intent startOlsrdServiceIntent = new Intent(context, OlsrdService.class);
		if (message != 0)
			startOlsrdServiceIntent.addFlags(message);
		if (profileName != null) 
			startOlsrdServiceIntent.putExtra("profile_name", profileName);
		context.startService(startOlsrdServiceIntent);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		WifiManager mgr = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
		NetworkInfo ni = (NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		Profiles profiles = new Profiles(context);
		
		Log.i("NetworkStateChangeReceiver", "This is the intent: " + intent.getAction());
		/*
		 * always try to start the OlsrdService, just in case.
		 */
		messageOlsrdService(context, OlsrdService.START_MESSAGE);
		
		Log.d("onReceive", "Running onReceive()");
		if (ni != null && ni.isConnected()) {
			Log.d("onReceive", "Connected? " + ni.toString());
			WifiConfiguration activeConfiguration = getActiveWifiConfiguration(mgr);
			if (activeConfiguration != null) {
				Log.d("onReceive", "This is the active configuration: " + activeConfiguration.toString());
				if (activeConfiguration.isIBSS) {
					Log.d("onReceive", "Connect (an ad hoc network)");
					messageOlsrdService(context, OlsrdService.CONNECTED_MESSAGE, profiles.getActiveProfileName());
				}
				else {
					Log.d("onReceive", "Disconnect (no ad hoc network).");
					messageOlsrdService(context, OlsrdService.DISCONNECTED_MESSAGE);
				}
			}
			else {
				Log.d("onReceive", "Disconnect (no active network)");
				messageOlsrdService(context, OlsrdService.DISCONNECTED_MESSAGE);
			}
		}
		else {
			Log.d("onReceive", "Disconnect (no network).");
			messageOlsrdService(context, OlsrdService.DISCONNECTED_MESSAGE);
		}
		Log.d("onReceive", "Changed: " + mgr.toString());
	}		
}