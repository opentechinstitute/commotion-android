package net.commotionwireless.route;

import java.lang.reflect.Field;

import android.net.wifi.WifiConfiguration;
import android.util.Log;

public class EWifiConfiguration {
	private WifiConfiguration mWifiConfiguration;
	private Field mLinkPropertiesField;
	private Field mIpAssignmentField;
	private Class IpAssignmentEnum;

	/*
	 * ABI - Do not modify order.
	 */
	public enum IpAssignmentType { 
		STATIC,
		DHCP,
		UNASSIGNED
	}

	public EWifiConfiguration(WifiConfiguration configuration) {
		mWifiConfiguration = configuration;
		Log.d("EWifiConfiguration", "Class name: " + configuration.getClass().toString());
		try {
			IpAssignmentEnum = Class.forName("android.net.wifi.WifiConfiguration$IpAssignment");
			mLinkPropertiesField = WifiConfiguration.class.getField("linkProperties");
			mIpAssignmentField = WifiConfiguration.class.getField("ipAssignment");
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void setIpAssignmentType(IpAssignmentType type) {
		Object types[] = IpAssignmentEnum.getEnumConstants();
		try {
			mIpAssignmentField.set(mWifiConfiguration, types[type.ordinal()]);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public Object getLinkProperties() {
		try {
			return mLinkPropertiesField.get(mWifiConfiguration);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void setLinkProperties(RLinkProperties linkProperties) {
		try {
			mLinkPropertiesField.set(mWifiConfiguration, linkProperties.getNativeObject());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
