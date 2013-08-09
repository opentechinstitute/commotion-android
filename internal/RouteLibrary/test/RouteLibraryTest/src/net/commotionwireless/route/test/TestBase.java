package net.commotionwireless.route.test;

import java.util.List;

import net.commotionwireless.route.EWifiConfiguration;
import net.commotionwireless.route.EWifiManager;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.test.AndroidTestCase;


public class TestBase extends AndroidTestCase {
	protected WifiManager wm = null;
	protected WifiConfiguration activeConfiguration = null;
	protected List<WifiConfiguration> configs = null;
	protected EWifiManager extendedWifiManager = null; 
	protected EWifiConfiguration extendedWifiConfiguration = null; 
	
	public TestBase() {
		super();
	}
	
	public void setUp() {
		wm = (WifiManager)getContext().getSystemService(Context.WIFI_SERVICE);
		configs = wm.getConfiguredNetworks();
		
		assertNotNull("wm is NULL", wm);
		assertNotNull("configs is NULL; make sure wireless is ON in test device.", configs);

		for (WifiConfiguration i : configs) {
			if (i.status == WifiConfiguration.Status.CURRENT) {
				activeConfiguration = i;
			}
		}
		assertNotNull("activeConfiguration is NULL", activeConfiguration);
		
		extendedWifiManager = new EWifiManager(wm);
		extendedWifiConfiguration = new EWifiConfiguration(activeConfiguration);
	}
	
	public void tearDown() {
		
	}
}