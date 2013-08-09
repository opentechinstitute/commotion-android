package net.commotionwireless.route.test;

import net.commotionwireless.route.RProxyProperties;

public class ProxyPropertiesTest extends TestBase {

	public ProxyPropertiesTest() {
		super();
	}
	public void testProxyPropertiesSelfEquality() {
		RProxyProperties proxyProperties = new RProxyProperties("127.0.0.1", 9090, "exclude.com");
		assertEquals("Proxy properties is not equal to itself", proxyProperties, proxyProperties);
	}
	
	public void testProxyPropertiesCopyEquality() {
		RProxyProperties proxyProperties = new RProxyProperties("127.0.0.1", 9090, "exclude.com");
		RProxyProperties copyProxyProperties = new RProxyProperties(proxyProperties.getNativeObject());
		
		assertEquals("Proxy properties is not equal to a copy of itself", proxyProperties, copyProxyProperties);
	}
		public void testProxyPropertiesGetters() {
		RProxyProperties proxyProperties;
		String host = "example.com";
		String xl = "excluded.com";
		int port = 1;
		
		proxyProperties = new RProxyProperties(host, port, xl);
		
		assertEquals("Proxy hosts do not match.", host, proxyProperties.getServer());
		assertEquals("Proxy ports do not match.", port, proxyProperties.getPort());
		assertEquals("Proxy xls do not match.", xl, proxyProperties.getExclusions());
	}
	
	public void testProxyPropertiesNativeConstructor() {
		RProxyProperties proxyProperties;
		RProxyProperties copyProxyProperties;
		String host = "example.com";
		String xl = "excluded.com";
		int port = 1;
		
		proxyProperties = new RProxyProperties(host, port, xl);
		copyProxyProperties = new RProxyProperties(proxyProperties.getNativeObject());
				
		assertEquals("Proxy hosts do not match (Native).", host, copyProxyProperties.getServer());
		assertEquals("Proxy ports do not match (Native).", port, copyProxyProperties.getPort());
		assertEquals("Proxy xls do not match (Native).", xl, copyProxyProperties.getExclusions());
	}

}
