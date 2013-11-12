package net.commotionwireless.meshtether;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;

import net.commotionwireless.olsrd.OlsrdService;
import net.commotionwireless.profiles.Profiles;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
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
	
	private String getConnectedInterfaceName(int ipAddress) {
		NetworkInterface ni = null;
		InetAddress ia = null;
		byte[] ip = new byte[4];

		ip[3] = (byte) ((ipAddress >> 24) & 0xff);
		ip[2] = (byte) ((ipAddress >> 16) & 0xff);
		ip[1] = (byte) ((ipAddress >> 8) & 0xff);
		ip[0] = (byte) ((ipAddress >> 0) & 0xff);

		try {
			ia = InetAddress.getByAddress(ip);
			Log.d("NetworkStateChangeReceiver", Integer.toHexString(ipAddress));
			Log.d("NetworkStateChangeReceiver", ia.toString());
			ni = NetworkInterface.getByInetAddress(ia);
		} catch (UnknownHostException ex) {
		} catch (SocketException ex) {
		}
		if (ni != null) {
			return ni.getName();
		}
		return null;
	}
	
	private void messageOlsrdService(Context context, int message) {
		messageOlsrdService(context, message, null, null);
	}
	private void messageOlsrdService(Context context, int message, String profileName, String interfaceName) {
		Intent startOlsrdServiceIntent = new Intent(context, OlsrdService.class);
		if (message != 0)
			startOlsrdServiceIntent.addFlags(message);
		if (profileName != null) 
			startOlsrdServiceIntent.putExtra("profile_name", profileName);
		if (interfaceName != null)
			startOlsrdServiceIntent.putExtra("interface_name", interfaceName);
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
		if (ni != null && (NetworkInfo.State.CONNECTED == ni.getState() || 
						   NetworkInfo.State.CONNECTING == ni.getState())) {
			Log.d("onReceive", "Connected? " + ni.toString());
			WifiConfiguration activeConfiguration = getActiveWifiConfiguration(mgr);
			if (activeConfiguration != null) {
				Log.d("onReceive", "This is the active configuration: " + activeConfiguration.toString());
				if (activeConfiguration.isIBSS) {
					Log.d("onReceive", "Connect (an ad hoc network)");
					WifiInfo info = mgr.getConnectionInfo();
					String iface = getConnectedInterfaceName(info.getIpAddress());

					Log.d("NetworkStateChangeReceiver", "Matching interface:" + ((iface!=null) ? iface : "N/A"));

					messageOlsrdService(context,
							(NetworkInfo.State.CONNECTED == ni.getState()) ? 
									OlsrdService.CONNECTED_MESSAGE : 
										OlsrdService.CONNECTING_MESSAGE, 
										profiles.getActiveProfileName(),
										iface);
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