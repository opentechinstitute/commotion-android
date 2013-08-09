package net.commotionwireless.route.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.commotionwireless.route.RLinkAddress;
import net.commotionwireless.route.RProxyProperties;
import net.commotionwireless.route.RRouteInfo;



public class RouteInfoTest extends TestBase {

		InetAddress mSevens, mFives; 
		public RouteInfoTest() {
			super();
			
			try {
				mSevens = InetAddress.getByName("7.7.7.7");
				mFives = InetAddress.getByName("7.7.7.7");
			} catch (UnknownHostException e) {
				assertFalse("getByName() failed.", true);
			}
		}
		public void testRouteInfoSelfEquality() {
			RLinkAddress linkAddress = null;
			RRouteInfo routeInfo = null;
			
			linkAddress = new RLinkAddress(mSevens, 8);
			routeInfo = new RRouteInfo(linkAddress, mFives);
			assertEquals("Route info does not equal itself", routeInfo, routeInfo);
		}
		
		public void testRouteInfoCopyEquality() {
			RLinkAddress linkAddress = null;
			RRouteInfo routeInfo = null, copyRouteInfo;
			
			linkAddress = new RLinkAddress(mSevens, 8);
			routeInfo = new RRouteInfo(linkAddress, mFives);
			copyRouteInfo = new RRouteInfo(routeInfo);
			assertEquals("Route info is not equal to the copy of itself.", routeInfo, copyRouteInfo);
		}
		
		public void testRouteInfoNativeCopyEquality() {
			RLinkAddress linkAddress = null;
			RRouteInfo routeInfo = null, copyRouteInfo;
			
			linkAddress = new RLinkAddress(mSevens, 8);
			routeInfo = new RRouteInfo(linkAddress, mFives);
			copyRouteInfo = new RRouteInfo(routeInfo.getNativeObject());
			assertEquals("Route info is not equal to the (native) copy of itself.", routeInfo, copyRouteInfo);
		}
		
		public void testRouteInfoGetters() {
			int prefixLength = 8;
			RLinkAddress linkAddress = null;
			InetAddress gatewayAddress = mFives;
			RRouteInfo routeInfo;
			
			linkAddress = new RLinkAddress(mSevens, prefixLength);
			routeInfo = new RRouteInfo(linkAddress, gatewayAddress);
			
			assertEquals("Route link addresses do not match", linkAddress, routeInfo.getLinkAddress());
			assertEquals("Route gateways do not match", gatewayAddress, routeInfo.getGatewayAddress());
		}
		
		public void testRouteInfoNativeConstructor() {
			int prefixLength = 8;
			RRouteInfo routeInfo;
			RRouteInfo copyRouteInfo;
			RLinkAddress linkAddress = null;
			InetAddress gatewayAddress = mFives;
			
			linkAddress = new RLinkAddress(mSevens, prefixLength);
			routeInfo = new RRouteInfo(linkAddress, gatewayAddress);
			copyRouteInfo = new RRouteInfo(routeInfo.getNativeObject());
			
			assertEquals("Route gateways do not match", gatewayAddress, copyRouteInfo.getGatewayAddress());
			assertEquals("Route link addresses do not match; this is expected!", linkAddress, copyRouteInfo.getLinkAddress());
		}
}
