package net.commotionwireless.route;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RRouteInfo extends RClass {
	
	public static final String NATIVE_CLASS_NAME = "android.net.RouteInfo";
	
	private static final String GET_DESTINATION_METHOD_NAME = "getDestination";
	private static final String GET_GATEWAY_METHOD_NAME = "getGateway";

	RLinkAddress mLinkAddress;
	InetAddress mGatewayAddress;
	
	public static RRouteInfo parseFromCommand(String cmd) throws UnknownHostException {
		InetAddress gateway, prefix;
		RLinkAddress linkAddress;
		String cmdParts[];
		int prefixLen;
		
		cmdParts = cmd.split(" ");
		
		prefix = InetAddress.getByName(cmdParts[1]);
		prefixLen = Integer.valueOf(cmdParts[2]);
		gateway = InetAddress.getByName(cmdParts[3]);
		linkAddress = new RLinkAddress(prefix, prefixLen);
		
		return new RRouteInfo(linkAddress, gateway);
	}
	
	/*
	 * TODO: handle is default route, is route to host.
	 */
	public RRouteInfo(Object route) {
		try {
			Class LinkAddressClass = Class.forName(RLinkAddress.NATIVE_CLASS_NAME);
			Constructor routeInfoConstructor;
			mNativeClass = Class.forName(NATIVE_CLASS_NAME);
			Method getDestination = mNativeClass.getMethod(GET_DESTINATION_METHOD_NAME, null);
			Method getGateway = mNativeClass.getMethod(GET_GATEWAY_METHOD_NAME, null);
			routeInfoConstructor = mNativeClass.getConstructor(LinkAddressClass, InetAddress.class);

			Object destination = getDestination.invoke(route, null);
			InetAddress gateway = (InetAddress)getGateway.invoke(route, null);
			
			/*
			 * Since we cannot do
			 * this(new LinkAddressR(destination), gateway);
			 * we have to duplicate code. 
			 */
			mLinkAddress = new RLinkAddress(destination);
			mGatewayAddress = InetAddress.getByAddress(gateway.getAddress());
			mNativeObject = routeInfoConstructor.newInstance(mLinkAddress.getNativeObject(), mGatewayAddress);

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
	}
	
	public RRouteInfo(RRouteInfo routeInfo) {
		this(routeInfo.getLinkAddress(), routeInfo.getGatewayAddress());
	}
	
	public RRouteInfo(RLinkAddress linkAddress, InetAddress gatewayAddress) {
		try {
			Constructor routeInfoConstructor = null;
			Class LinkAddressClass = Class.forName(RLinkAddress.NATIVE_CLASS_NAME);
			
			mNativeClass = Class.forName(NATIVE_CLASS_NAME);

			routeInfoConstructor = mNativeClass.getConstructor(LinkAddressClass, InetAddress.class);
			
			mLinkAddress = new RLinkAddress(linkAddress);
			mGatewayAddress = InetAddress.getByAddress(gatewayAddress.getAddress());
			
			mNativeObject = routeInfoConstructor.newInstance(mLinkAddress.getNativeObject(), mGatewayAddress);
			//= routeInfoConstructor.newInstance(linkAddress, InetAddress.getByName("5.5.5.5"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public InetAddress getGatewayAddress() {
		return mGatewayAddress;
	}
	
	public RLinkAddress getLinkAddress() {
		return mLinkAddress;
	}
}
