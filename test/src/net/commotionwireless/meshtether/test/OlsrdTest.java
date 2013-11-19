package net.commotionwireless.meshtether.test;

import net.commotionwireless.meshtether.NativeHelper;
import net.commotionwireless.olsrd.Olsrd;
import android.os.Handler;
import android.os.Message;
import android.test.AndroidTestCase;
import android.util.Log;

public class OlsrdTest extends AndroidTestCase {
	private Olsrd mOlsrd; 
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			String message = msg.obj.toString();
			Log.d("OlsrdTest", "msg: " + message);
		}
	};
	public void setUp() {
		NativeHelper.unzipAssets(this.getContext());
		mOlsrd = new Olsrd(mHandler);
	}
	
	public void tearDown() {
	}
	
	public void testMain() {
		mOlsrd.startMain(new String[] {"-h"});

		try {
			Thread.sleep(5000);
		} catch (InterruptedException interruptedEx) {
			/*
			 * eh
			 */
		}
		mOlsrd.stopMain();
	}
}