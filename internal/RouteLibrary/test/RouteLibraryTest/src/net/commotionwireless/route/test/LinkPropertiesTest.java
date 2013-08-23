package net.commotionwireless.route.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;

import net.commotionwireless.route.RLinkAddress;
import net.commotionwireless.route.RLinkProperties;
import net.commotionwireless.route.RRouteInfo;
import android.util.Log;


/*
 * There are some ways that the phone must be
 * set up in order for these tests to work.
 *
 * 0) The phone must have the wireless enabled.
 * 1) The phone must be associated with an AdHoc network.
 */
public class LinkPropertiesTest extends TestBase {

	public LinkPropertiesTest() {
		super();
	}

	public void testLinkPropertiesSelfEquality() {
		RLinkProperties linkProperties = null;
		linkProperties = new RLinkProperties(extendedWifiConfiguration.getLinkProperties());
		
		assertEquals("Link properties is not equal to itself.", linkProperties, linkProperties);
	}
	
	public void testLinkPropertiesCopyEquality() {
		RLinkProperties linkProperties = null, copyLinkProperties;
		linkProperties = new RLinkProperties(extendedWifiConfiguration.getLinkProperties());
		copyLinkProperties = new RLinkProperties(linkProperties);
		
		assertEquals("Link properties is not equal to the copy of itself.", linkProperties, copyLinkProperties);
	}
	
	public void testLinkPropertiesNativeCopyEquality() {
		RLinkProperties linkProperties = null, copyLinkProperties;
		linkProperties = new RLinkProperties(extendedWifiConfiguration.getLinkProperties());
		copyLinkProperties = new RLinkProperties(linkProperties.getNativeObject());
		
		assertEquals("Link properties is not equal to the (native) copy of itself.", linkProperties, copyLinkProperties);
	}
	
	public void testLinkPropertiesAddRemoveRoutes() {
		RLinkProperties linkProperties = null;
		RLinkProperties modifiedLinkProperties = null;
		RRouteInfo route = null;
		RLinkAddress linkAddress = null;
		InetAddress sevens = null, fives = null;
		try {
			sevens = InetAddress.getByName("7.7.7.7");
			fives = InetAddress.getByName("5.5.5.5");
		} catch (UnknownHostException e) {
			assertFalse("getByName() failed", true);
		}
		
		linkProperties = new RLinkProperties(extendedWifiConfiguration.getLinkProperties());
		modifiedLinkProperties = new RLinkProperties(linkProperties);
		
		linkAddress = new RLinkAddress(sevens, 8);
		route = new RRouteInfo(linkAddress, fives);
		
		
		modifiedLinkProperties.addRoute(route);
		modifiedLinkProperties.removeRoute(route);
		
		assertEquals("Adding/removing the same route does not work as expected.", modifiedLinkProperties, linkProperties);
	}
	

}