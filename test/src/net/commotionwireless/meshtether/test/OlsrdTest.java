package net.commotionwireless.meshtether.test;

import net.commotionwireless.olsrd.Olsrd;
import android.test.AndroidTestCase;
import android.util.Log;

public class OlsrdTest extends AndroidTestCase {
	private Olsrd mOlsrd; 
	public void setUp() {
		mOlsrd = new Olsrd();
	}
	
	public void tearDown() {
		
	}
	
	public void testMain() {
		Thread mThread = new Thread() {
			public void run() {
			mOlsrd.main(new String[] {"a", "b"});
			}
		};
		mThread.start();
		try {
			Thread.sleep(5000);
		} catch (InterruptedException ex) {
			
		}
		mThread.interrupt();
	}
}
