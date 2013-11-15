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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import net.commotionwireless.olsrinfo.datatypes.AddressNetmask;
import net.commotionwireless.olsrinfo.datatypes.Config;
import net.commotionwireless.olsrinfo.datatypes.HNA;
import net.commotionwireless.olsrinfo.datatypes.LinkQualityMultiplier;
import net.commotionwireless.olsrinfo.datatypes.OlsrDataDump;
import net.commotionwireless.olsrinfo.datatypes.Plugin;
import net.commotionwireless.profiles.Profiles;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class InfoActivity extends android.app.ListActivity {
	public final static String TAG = "InfoActivity";

	private MeshTetherApp app;
	private String[] info = new String[0];
	private BaseAdapter adapter;
	private Thread mUpdateThread = null;
	private Handler mHandler = new Handler();

	private static class ViewHolder {
		TextView infoKey;
		TextView infoValue;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		app = (MeshTetherApp) getApplication();
		app.setInfoActivity(this);
		adapter = new BaseAdapter() {

			@Override
			public int getCount() {
				return info.length / 2;
			}

			@Override
			public long getItemId(int position) {
				return position;
			}

			@Override
			public Object getItem(int position) {
				String[] ret = new String[2];
				int i = position * 2;
				ret[0] = info[i];
				ret[1] = info[i + 1];
				return ret;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				ViewHolder holder;

				if (convertView == null) {
					View view = getLayoutInflater().inflate(R.layout.inforow,
							null);
					holder = new ViewHolder();
					holder.infoKey = (TextView) view
							.findViewById(R.id.info_key);
					holder.infoValue = (TextView) view
							.findViewById(R.id.info_value);
					view.setTag(holder);
					view.setClickable(false);
					convertView = view;
				} else {
					holder = (ViewHolder) convertView.getTag();
				}

				int i = position * 2;
				holder.infoKey.setText(info[i]);
				holder.infoValue.setText(info[i + 1]);
				return convertView;
			}
		};
		setListAdapter(adapter);
		setTitle(getString(R.string.clientview));
	}

	@Override
	public void onResume() {
		super.onResume();
		update();
	}

	void update() {
		if (mUpdateThread == null) {
			mUpdateThread = new Thread() {
				public void run() {
					info = generateConfigList();
					mHandler.post(new Runnable() {
						public void run() {
							adapter.notifyDataSetChanged();		
						}
					});
					mUpdateThread = null;
				}
			};
			mUpdateThread.start();
		}
	}

	private String[] makeStringArray(List<String> l) {
		String[] ret = new String[l.size()];
		ret = l.toArray(ret);
		return ret;
	}

	private String[] generateConfigList() {
		HashMap<String, Object> data = new HashMap<String, Object>();
		List<String> stringList = new ArrayList<String>();
		Profiles profiles = new Profiles(this);

		stringList.add("active profile");
		stringList.add(profiles.getActiveProfileName());

		// then add wifi info
		WifiInfo wi = app.wifiManager.getConnectionInfo();
		String wifiInfoString = "\n";
		wifiInfoString += "SSID: " + wi.getSSID() + "\n";
		wifiInfoString += "BSSID: " + wi.getBSSID() + "\n";
		int ipInt = wi.getIpAddress();
		String ipString = String.format("%d.%d.%d.%d",
				(ipInt & 0xff),
				(ipInt >> 8 & 0xff),
				(ipInt >> 16 & 0xff),
				(ipInt >> 24 & 0xff));
		wifiInfoString += "ip: " + ipString + "\n";
		wifiInfoString += "MAC: "  + wi.getMacAddress() + "\n";
		wifiInfoString += "speed: " + wi.getLinkSpeed() + " Mbps\n";
		wifiInfoString += "RSSI: " + wi.getRssi() + " dBm";
		stringList.add("wifi info");   // key
		stringList.add(wifiInfoString);// value

		OlsrDataDump dump = app.mJsonInfo.parseCommand("/config");
		if (dump == null || dump.config == null
				|| dump.config.unicastSourceIpAddress == null) {
			stringList.add(getString(R.string.waiting_for_olsrd));
			stringList.add(getString(R.string.no_data_yet));
			return makeStringArray(stringList);
		}

		Config config = dump.config;
		data.put("systemTime", dump.systemTime);
		data.put("timeSinceStartup", dump.timeSinceStartup);

		try {
			for (Field field : config.getClass().getDeclaredFields()) {
				String name = field.getName();
				String value = "";
				if (name.equals("defaultLinkQualityMultipliers")) {
					for (LinkQualityMultiplier lqm : config.defaultLinkQualityMultipliers) {
						value += lqm.route + " - " + lqm.multiplier + "\n";
					}
				} else if (name.equals("ipcAllowedAddresses")) {
					for (AddressNetmask addr : config.ipcAllowedAddresses) {
						value += addr.ipAddress + "/" + addr.netmask + "\n";
					}
				} else if (name.equals("hna")) {
					for (HNA hna : config.hna) {
						value += hna.destination + "  " + hna.gateway + "\n";
					}
				} else {
					Object o = field.get(config);
					if (o != null)
						value = o.toString() + "\n";
				}
				// remove the last, trailing \n
				if (value.length() > 0)
					data.put(name, value.substring(0, value.length() - 1));
				else
					data.put(name, "");
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		SortedSet<String> sortedKeys = new TreeSet<String>(data.keySet());
		Iterator<String> iter = sortedKeys.iterator();
		while (iter.hasNext()) {
			String key = iter.next();
			stringList.add(key);
			stringList.add(data.get(key).toString());
		}

		Collection<Plugin> plugins = app.mJsonInfo.plugins();
		for (Plugin p : plugins) {
			String value = "\n";
			if (p.port > 0 && p.port < 65536)
				value += "port: " + p.port + "\n";
			if (p.accept != null && !p.accept.equals(""))
				value += "accept: " + p.accept + "\n";
			if (p.host != null && !p.host.equals(""))
				value += "host: " + p.host + "\n";
			if (p.net != null && !p.net.equals(""))
				value += "net: " + p.net + "\n";
			if (p.ping != null && !p.ping.equals(""))
				value += "ping: " + p.ping + "\n";
			if (p.hna != null && !p.hna.equals(""))
				value += "hna: " + p.hna + "\n";
			if (p.uuidfile != null && !p.uuidfile.equals(""))
				value += "uuidfile: " + p.uuidfile + "\n";
			if (p.keyfile != null && !p.keyfile.equals(""))
				value += "keyfile: " + p.keyfile + "\n";

			String key = p.plugin;
			// remove the path to the plugin
			key = key.substring(key.lastIndexOf("/olsrd_") + 7, key.length());
			// remove the lib file name extension
			stringList.add(key.replace(".so.", " "));
			// remove the last, trailing \n
			stringList.add(value.substring(0, value.length() - 1));
		}
		return makeStringArray(stringList);
	}
}