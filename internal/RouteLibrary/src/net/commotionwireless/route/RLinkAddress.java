package net.commotionwireless.route;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RLinkAddress extends RClass {
	public static final String NATIVE_CLASS_NAME = "android.net.LinkAddress";
	
	private static final String GET_ADDRESS_METHOD_NAME = "getAddress";
	private static final String GET_NETWORK_PREFIX_LENGTH_METHOD_NAME = "getNetworkPrefixLength";
	
	InetAddress mAddress;
	int mPrefixLength;
	
	private RLinkAddress() {
		try {
			mNativeClass = Class.forName(NATIVE_CLASS_NAME);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public RLinkAddress(Object linkAddress) {
		this();
		
		try {
			Method getAddress, getNetworkPrefixLength;
			getAddress = mNativeClass.getMethod(GET_ADDRESS_METHOD_NAME, null);
			getNetworkPrefixLength = mNativeClass.getMethod(GET_NETWORK_PREFIX_LENGTH_METHOD_NAME, null);
			InetAddress address;
			
			address = (InetAddress) getAddress.invoke(linkAddress, null);
			mAddress = InetAddress.getByAddress(address.getAddress());
			
			mPrefixLength = (Integer) getNetworkPrefixLength.invoke(linkAddress, null);
			
			nativeConstructor(mAddress, mPrefixLength);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			/*
			 * Swallow because the object to be copied
			 * would not have been created if this 
			 * were not the case. 
			 */
			e.printStackTrace();
		}
	}
	public RLinkAddress(RLinkAddress linkAddress) {
		this(linkAddress.getAddress(), linkAddress.getNetworkPrefixLength());
	}
	
	public RLinkAddress(InetAddress address, int prefix) {
		this();
		try {
			mAddress = InetAddress.getByAddress(address.getAddress());
			mPrefixLength = prefix;
		} catch (UnknownHostException e) {
			/*
			 * Swallow because the parameter to be copied
			 * would not have been created if this 
			 * were not the case. 
			 */
			e.printStackTrace();
		}


		nativeConstructor(mAddress, mPrefixLength);

	}
	
	private void nativeConstructor(InetAddress address, int prefix) {
		Constructor linkAddressConstructor = null;
		try {
			linkAddressConstructor = mNativeClass.getConstructor(InetAddress.class, int.class);
			mNativeObject = linkAddressConstructor.newInstance(address, prefix);

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
	
	public InetAddress getAddress() {
		return mAddress;
	}
	public int getNetworkPrefixLength() {
		return mPrefixLength;
	}
}
