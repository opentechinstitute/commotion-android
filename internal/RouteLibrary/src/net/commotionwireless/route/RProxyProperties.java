package net.commotionwireless.route;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class RProxyProperties extends RClass {
	public static final String NATIVE_CLASS_NAME = "android.net.ProxyProperties";
	
	private static final String GET_HOST_METHOD_NAME = "getHost";
	private static final String GET_PORT_METHOD_NAME = "getPort";
	private static final String GET_EXCLUSION_LIST_METHOD_NAME = "getExclusionList";

	private String mServer;
	private String mExclusions;
	private int mPort;
	
	public RProxyProperties(String server, int port, String exclusions) {
		try {
			Constructor proxyPropertiesConstructor = null;
			mNativeClass = Class.forName(NATIVE_CLASS_NAME);
			proxyPropertiesConstructor = mNativeClass.getConstructor(String.class, int.class, String.class);
			
			mPort = port;
			mServer = new String(server);
			mExclusions = new String(exclusions);

			mNativeObject = proxyPropertiesConstructor.newInstance(mServer, mPort, mExclusions);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public RProxyProperties(Object proxyProperties) {
		try {
			Constructor proxyPropertiesCopyConstructor = null;
			Method getHost, getExclusionList, getPort;
			
			mNativeClass = Class.forName(NATIVE_CLASS_NAME);
			proxyPropertiesCopyConstructor = mNativeClass.getConstructor(mNativeClass);
			getHost = mNativeClass.getMethod(GET_HOST_METHOD_NAME, null);
			getPort = mNativeClass.getMethod(GET_PORT_METHOD_NAME, null);
			getExclusionList = mNativeClass.getMethod(GET_EXCLUSION_LIST_METHOD_NAME, null);

			mNativeObject = proxyPropertiesCopyConstructor.newInstance(proxyProperties);
			
			mServer = new String((String) getHost.invoke(mNativeObject, null));
			mPort = (Integer) getPort.invoke(mNativeObject, null);
			mExclusions = new String((String) getExclusionList.invoke(mNativeObject, null));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public int getPort() {
		return mPort;
	}
	public String getServer() {
		return mServer;
	}
	public String getExclusions() {
		return mExclusions;
	}
}
