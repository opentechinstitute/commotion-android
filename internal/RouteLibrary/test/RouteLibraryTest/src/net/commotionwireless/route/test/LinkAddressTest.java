package net.commotionwireless.route.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.commotionwireless.route.RLinkAddress;


public class LinkAddressTest extends TestBase {

	public LinkAddressTest() {
		super();
	}

	public void testLinkAddressSelfEquality() {
		InetAddress address = null;
		RLinkAddress linkAddress = null;
		try {
			address = InetAddress.getByName("7.7.7.7");
		} catch (UnknownHostException e) {
			assertFalse("getByName() failed", true);
		}
		linkAddress = new RLinkAddress(address, 8);
		assertEquals("Link address does not equal itself", linkAddress, linkAddress);
	}
	
	public void testLinkAddressNativeCopyEquality() {
		InetAddress address = null;
		RLinkAddress linkAddress = null, copyLinkAddress;
		try {
			address = InetAddress.getByName("7.7.7.7");
		} catch (UnknownHostException e) {
			assertFalse("getByName() failed", true);
		}
		linkAddress = new RLinkAddress(address, 8);
		copyLinkAddress = new RLinkAddress(linkAddress.getNativeObject());
		assertEquals("Link address is not equal to the (native) copy of itself", linkAddress, copyLinkAddress);
	}
	
	public void testLinkAddressCopyEquality() {
		InetAddress address = null;
		RLinkAddress linkAddress = null, copyLinkAddress;
		try {
			address = InetAddress.getByName("7.7.7.7");
		} catch (UnknownHostException e) {
			assertFalse("getByName() failed", true);
		}
		linkAddress = new RLinkAddress(address, 8);
		copyLinkAddress = new RLinkAddress(linkAddress);
		assertEquals("Link address is not equal to the copy of itself", linkAddress, copyLinkAddress);
	}
	
	public void testLinkAddresGetters() {
		InetAddress address = null;
		int prefixLength = 8;
		RLinkAddress linkAddress = null;
		try {
			address = InetAddress.getByName("7.7.7.7");
		} catch (UnknownHostException e) {
			assertFalse("getByName() failed", true);
		}
		linkAddress = new RLinkAddress(address, prefixLength);
		assertEquals("Link address addresses do not match", address, linkAddress.getAddress());
		assertEquals("Link address prefix lengths do not match", prefixLength, linkAddress.getNetworkPrefixLength());
	}
	
	public void testLinkAddressNativeConstructor() {
		InetAddress address = null;
		int prefixLength = 8;
		RLinkAddress linkAddress = null, copyLinkAddress;
		try {
			address = InetAddress.getByName("7.7.7.7");
		} catch (UnknownHostException e) {
			assertFalse("getByName() failed", true);
		}
		linkAddress = new RLinkAddress(address, prefixLength);
		copyLinkAddress = new RLinkAddress(linkAddress.getNativeObject());
		assertEquals("Link address addresses do not match", address, copyLinkAddress.getAddress());
		assertEquals("Link address prefix lengths do not match", prefixLength, copyLinkAddress.getNetworkPrefixLength());
	}
	

}