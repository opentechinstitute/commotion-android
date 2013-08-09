package net.commotionwireless.route;

import java.lang.reflect.Field;

import android.net.wifi.WifiConfiguration;

public class EWifiConfiguration {
	private WifiConfiguration mWifiConfiguration;
	private Field mLinkPropertiesField;

	public EWifiConfiguration(WifiConfiguration configuration) {
		mWifiConfiguration = configuration;
		try {
			mLinkPropertiesField = WifiConfiguration.class.getField("linkProperties");
		} catch (NoSuchFieldException e) {
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
