package net.commotionwireless.route;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Collection;
import java.util.HashSet;

import android.util.Log;

public class RLinkProperties extends RClass {	
	
	public static final String NATIVE_CLASS_NAME = "android.net.LinkProperties";
	
	private static final String GET_INTERFACE_NAME_METHOD_NAME = "getInterfaceName";
	private static final String SET_INTERFACE_NAME_METHOD_NAME = "setInterfaceName";
	private static final String GET_LINK_ADDRESSES_METHOD_NAME = "getLinkAddresses";
	private static final String ADD_LINK_ADDRESS_METHOD_NAME = "addLinkAddress";
	private static final String GET_DNSES_METHOD_NAME = "getDnses";
	private static final String ADD_DNS_METHOD_NAME = "addDns";
	private static final String GET_HTTP_PROXY_METHOD_NAME = "getHttpProxy";
	private static final String SET_HTTP_PROXY_METHOD_NAME = "setHttpProxy";
	private static final String ADD_ROUTE_METHOD_NAME = "addRoute";
	private static final String GET_ROUTES_METHOD_NAME = "getRoutes";

	private Collection<RRouteInfo> mRoutes = null;
	
	public RLinkProperties(Object linkProperties) {
		try {
			Constructor nativeCopyConstructor = null;
			
			mNativeClass = Class.forName(NATIVE_CLASS_NAME);
			nativeCopyConstructor = mNativeClass.getConstructor(mNativeClass);
			mNativeObject = nativeCopyConstructor.newInstance(linkProperties);
			
			initializeRouteSet();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public RLinkProperties(RLinkProperties linkProperties) {
		this(linkProperties.getNativeObject());
	}
	
	private void initializeRouteSet() {
		mRoutes = new HashSet<RRouteInfo>();
		try {
			Method getRoutes = mNativeClass.getMethod(GET_ROUTES_METHOD_NAME, null);

			for (Object r : (Collection<Object>)getRoutes.invoke(mNativeObject,null)) mRoutes.add(new RRouteInfo(r));
			
			printRoutes("After Constructor");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}
	
	private void printRoutes(String tag) {
		String outputString = "";
		
		outputString = "Routes: [";
		for (RRouteInfo r : mRoutes) {
			outputString += r.toString() + ",";
		}
		outputString += "]";
		Log.i("RLinkProperties", tag);
		Log.i("RLinkProperties", outputString);
	}
	
	public void clearRoutes() {
		Object newNativeObject = null;
		Method getInterfaceName, setInterfaceName, getLinkAddresses, addLinkAddress, 
			addDns, getDnses, setHttpProxy, getHttpProxy;
		Class LinkAddressClass, ProxyPropertiesClass;
		
		try {
			LinkAddressClass = Class.forName(RLinkAddress.NATIVE_CLASS_NAME);
			ProxyPropertiesClass = Class.forName(RProxyProperties.NATIVE_CLASS_NAME);
			newNativeObject = mNativeClass.newInstance();
			getInterfaceName = mNativeClass.getMethod(GET_INTERFACE_NAME_METHOD_NAME, null);
			setInterfaceName = mNativeClass.getMethod(SET_INTERFACE_NAME_METHOD_NAME, String.class);
			getLinkAddresses = mNativeClass.getMethod(GET_LINK_ADDRESSES_METHOD_NAME, null);
			addLinkAddress = mNativeClass.getMethod(ADD_LINK_ADDRESS_METHOD_NAME, LinkAddressClass);
			getDnses = mNativeClass.getMethod(GET_DNSES_METHOD_NAME, null);
			addDns = mNativeClass.getMethod(ADD_DNS_METHOD_NAME, InetAddress.class);
			getHttpProxy = mNativeClass.getMethod(GET_HTTP_PROXY_METHOD_NAME, null);
			setHttpProxy = mNativeClass.getMethod(SET_HTTP_PROXY_METHOD_NAME, ProxyPropertiesClass);

			setInterfaceName.invoke(newNativeObject, (String)getInterfaceName.invoke(mNativeObject, null));

			for (Object l : (Collection<Object>)getLinkAddresses.invoke(mNativeObject, null)) addLinkAddress.invoke(newNativeObject, l);
			for (InetAddress i : (Collection<InetAddress>)getDnses.invoke(mNativeObject,null)) addDns.invoke(newNativeObject, i);

			setHttpProxy.invoke(newNativeObject, (new RProxyProperties((Object)getHttpProxy.invoke(mNativeObject, null))).getNativeObject());

			Log.i("LinkPropertiesR", "mNativeObject: " + mNativeObject.toString());
			Log.i("LinkPropertiesR", "newNativeObject: " + newNativeObject.toString());
			mNativeObject = newNativeObject;

		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	public void removeRoute(RRouteInfo oldRoute) {
		mRoutes.remove(oldRoute);
		clearRoutes();
		for (RRouteInfo r : mRoutes) addRouteNative(r);
		printRoutes("After removeRoute()");
	}
	
	public void addRoute(RRouteInfo newRoute) {
		mRoutes.add(newRoute);
		addRouteNative(newRoute);

		printRoutes("After addRoute()");
	}
	
	private void addRouteNative(RRouteInfo newRoute) {
		Method addRouteMethod = null;
		try {
			addRouteMethod = mNativeClass.getMethod(ADD_ROUTE_METHOD_NAME, newRoute.getNativeClass());
			addRouteMethod.invoke(mNativeObject, newRoute.getNativeObject());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	public void setHttpProxy(RProxyProperties proxyProperties) {
		Method setHttpProxyMethod = null;
		try {
			setHttpProxyMethod = mNativeClass.getMethod(SET_HTTP_PROXY_METHOD_NAME, proxyProperties.getNativeClass());
			setHttpProxyMethod.invoke(mNativeObject, proxyProperties.getNativeObject());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
}
