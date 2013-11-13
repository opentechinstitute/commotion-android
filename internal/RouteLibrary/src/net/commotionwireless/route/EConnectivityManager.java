package net.commotionwireless.route;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.net.ConnectivityManager;

public class EConnectivityManager {
	private ConnectivityManager mConnectivityManager;
	
	public EConnectivityManager(ConnectivityManager manager) {
		mConnectivityManager = manager;
	}
	
	public int tether(String iface) {
		Method tetherMethod = null;
		try {
			tetherMethod = mConnectivityManager.getClass().getMethod("tether", String.class);
			return (Integer)tetherMethod.invoke(mConnectivityManager, iface);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public int untether(String iface) {
		Method untetherMethod = null;
		try {
			untetherMethod = mConnectivityManager.getClass().getMethod("untether", String.class);
			return (Integer)untetherMethod.invoke(mConnectivityManager, iface);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return 0;	
	}
}
