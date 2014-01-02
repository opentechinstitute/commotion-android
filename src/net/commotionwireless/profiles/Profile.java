package net.commotionwireless.profiles;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Map;

import net.commotionwireless.meshtether.Util;
import net.commotionwireless.route.EWifiConfiguration;
import net.commotionwireless.route.EWifiConfiguration.IpAssignmentType;
import net.commotionwireless.route.RLinkAddress;
import net.commotionwireless.route.RLinkProperties;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.preference.PreferenceManager;
import android.util.Log;

public class Profile {

	public static final int COMMOTION_PRIORITY = 100001;

	private String mName;
	private SharedPreferences mSharedPreferences;
	
	public Profile(String profileName, Context context, boolean noCreate) throws NoMatchingProfileException {
		if (noCreate && !profileFileExists(profileName, context)) {
			throw new NoMatchingProfileException();
		}
		/*
		 * I do NOT like this copy/paste.
		 */
		mName = profileName;
		mSharedPreferences = context.getSharedPreferences(mName, Context.MODE_PRIVATE);
		setDefaults(context);
	}
	
	public Profile(String profileName, Context context) {
		mName = profileName;
		mSharedPreferences = context.getSharedPreferences(mName, Context.MODE_PRIVATE);
		setDefaults(context);
	}
	
	protected Profile(String profileName, SharedPreferences prefs) {
		mName = profileName;
		mSharedPreferences = prefs;
	}

	private void setDefaults(Context context) {
		SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Profile defaultProfile = new Profile(mName, defaultPreferences);
		deepCopy(defaultProfile, false);
	}

	private boolean profileFileExists(String profileName, Context context) {
		File file = new File(context.getFilesDir().getParent() + "/shared_prefs" + "/" + profileName + ".xml");
		return file.exists();
	}
	
	protected SharedPreferences getSharedPreferences() {
		return mSharedPreferences;
	}

	public void deepCopy(Profile p) {
		deepCopy(p, true);
	}

	public boolean deepEquals(Profile p) {
		boolean isEqual = true;
		Map<String, ?> comparisonProfile, thisProfile;

		comparisonProfile = p.getSharedPreferences().getAll();
		thisProfile = getSharedPreferences().getAll();

		/*
		 * first and foremost, the number of preferences
		 * must match.
		 */
		if (comparisonProfile.size() != thisProfile.size())
			return false;

		/*
		 * Now, check the values!
		 */
		for (Map.Entry<String, ?> comparisonEntry : comparisonProfile.entrySet()) {
			if (comparisonEntry.getValue().getClass() == Boolean.class) {
				if (getBooleanValue(comparisonEntry.getKey()) != (Boolean)comparisonEntry.getValue()) {
					isEqual = false;
					break;
				}
			} else if (comparisonEntry.getValue().getClass() == String.class) {
				if (!getStringValue(comparisonEntry.getKey()).equals(comparisonEntry.getValue())) {
					isEqual = false;
					break;
				}
			}
		}
		return isEqual;
	}

	public void deepCopy(Profile p, boolean overwrite) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		Map<String, ?> existingProfile = p.getSharedPreferences().getAll();
		for (Map.Entry<String, ?> entry : existingProfile.entrySet()) {

			/*
			 * check if entry.getKey() already exists in the local profile.
			 * If it does, and we are not overwrite(ing), then we skip!
			 */
			if (!overwrite && mSharedPreferences.contains(entry.getKey())) {
				Log.i("Profile", "Skipping deepCopy() of key " + entry.getKey());
				continue;
			} else {
				Log.i("Profile", "Okay with deepCopy() of key " + entry.getKey());
			}

			if (entry.getValue().getClass() == Boolean.class) {
				editor.putBoolean(entry.getKey(), (Boolean)entry.getValue());
			} else if (entry.getValue().getClass() == String.class) {
				editor.putString(entry.getKey(), (String)entry.getValue());
			}
		}
		editor.commit();
		editor.apply();
	}
	
	public ArrayList<String> toEnvironment(String keyPrefix) {
		ArrayList<String> environment = new ArrayList<String>();
		Map<String, ?> profileMap = mSharedPreferences.getAll();
		for (Map.Entry<String, ?> entry : profileMap.entrySet()) {
			String value = null;
			if (entry.getValue().getClass() == Boolean.class)
				if ((Boolean)entry.getValue())
					value = "1";
				else
					value = "0";
			else {
				value = (String)entry.getValue();
				if (value.equalsIgnoreCase("")) {
					value = null;
				}
			}
			if (value != null) environment.add(keyPrefix + entry.getKey() + "=" + value);
		}
		return environment;
	}
	
	public String toString() {
		String returnString = new String("");
		for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
			returnString = returnString + entry.getKey() + ": " + entry.getValue() + "\n";
		}
		return returnString;
	}
	
	public String getProfileName() {
		return mName;
	}
	
	public boolean getWifiConfiguration(WifiConfiguration config) {
		EWifiConfiguration eConfig = new EWifiConfiguration(config);
		RLinkProperties linkProperties = new RLinkProperties();
		RLinkAddress linkAddress;
		InetAddress linkAddressAddress = null, linkAddressNetmask = null, dnsAddress = null;

		try {
			/*
			 * do the stuff that might fail first.
			 */
			String linkAddressFromProfile = mSharedPreferences.getString("adhoc_ip", "");
			if (linkAddressFromProfile.equalsIgnoreCase("")) {
				linkAddressFromProfile = Util.generateMeshAddress();
			}

			linkAddressAddress = InetAddress.getByName(linkAddressFromProfile);
			linkAddressNetmask = InetAddress.getByName(mSharedPreferences.getString("adhoc_netmask", "255.0.0.0"));
			dnsAddress = InetAddress.getByName(mSharedPreferences.getString("adhoc_dns_server", "8.8.8.8"));

			linkAddress = new RLinkAddress(linkAddressAddress, prefixLengthFromNetmask(linkAddressNetmask.getAddress()));

			linkProperties.addLinkAddress(linkAddress);
			linkProperties.addDns(dnsAddress);

			eConfig.setLinkProperties(linkProperties);
		} catch (UnknownHostException e) {
			return false;
		}

		/*
		 * Now do the stuff that won't fail. Like Rudy.
		 */
		eConfig.setIpAssignmentType(IpAssignmentType.STATIC);

		config.priority = COMMOTION_PRIORITY;
		config.status = WifiConfiguration.Status.ENABLED;
		config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		config.BSSID = mSharedPreferences.getString("lan_bssid", "02:CA:FF:EE:BA:BE");
		config.SSID = "\"" + mSharedPreferences.getString("lan_essid", "commotionwireless.net") + "\"";
		config.isIBSS = true;
		config.frequency = channelToFrequency(Integer.valueOf(mSharedPreferences.getString("lan_channel", "1")));

		return true;
	}

	private int channelToFrequency(int channel) {
		if (channel <1 ) return 2412;
		if (channel > 14) return 2484;
		return 2412 + ((channel-1)*5);
	}
	private int prefixLengthFromNetmask(byte address[]) {
		/*
		 * Assume that the input array is big-endian (network order).
		 * We are going to calculate into native endianness, although it 
		 * doesn't really matter in this case since we are just counting
		 * bits.
		 */
		int addressInt = 0;
		if (ByteOrder.BIG_ENDIAN == ByteOrder.nativeOrder()) {
			addressInt= (((address[0] & 0xff) << 24)| ((address[1] & 0xff) << 16) |
						 ((address[2] & 0xff) << 8) | ((address[3] & 0xff) << 0));
		} else {
			addressInt= (((address[3] & 0xff) << 24)| ((address[2] & 0xff) << 16) |
					 ((address[1] & 0xff) << 8) | ((address[0] & 0xff) << 0));
		}
		return Integer.bitCount(addressInt);
	}

	public String getStringValue(String key) {
		return mSharedPreferences.getString(key, "");
	}

	public boolean getBooleanValue(String key) {
		return mSharedPreferences.getBoolean(key, false);
	}

	public String[] getKeys() {
		return (String[])mSharedPreferences.getAll().keySet().toArray();
	}

	public void setValue(String key, String value) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		
		editor.putString(key, value);
		
		editor.commit();
		editor.apply();
	}
	public void setValue(String key, boolean value) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		
		editor.putBoolean(key, value);
		
		editor.commit();
		editor.apply();
	}
	
}
