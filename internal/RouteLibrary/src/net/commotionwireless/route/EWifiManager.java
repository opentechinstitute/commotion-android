package net.commotionwireless.route;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

public class EWifiManager {
	private WifiManager mWifiManager;
	Method mSave = null;
	
	public EWifiManager(WifiManager mgr) {
		mWifiManager = mgr;
		try {
			Class ActionListenerClass = Class.forName("android.net.wifi.WifiManager$ActionListener");
			mSave = WifiManager.class.getMethod("save", WifiConfiguration.class, ActionListenerClass);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	public void save(WifiConfiguration configuration) {
		try {
			mSave.invoke(mWifiManager, configuration, null);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

}
