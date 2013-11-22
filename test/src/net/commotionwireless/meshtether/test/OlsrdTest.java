package net.commotionwireless.meshtether.test;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import net.commotionwireless.olsrd.DotConf;
import net.commotionwireless.olsrd.Olsrd;
import net.commotionwireless.olsrd.Olsrd.OlsrdRunningState;
import net.commotionwireless.profiles.Profile;
import android.content.Context;
import android.net.ConnectivityManager;
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
		mOlsrd = new Olsrd(mHandler);
	}
	
	public void tearDown() {
	}
	
	public void generateConf(boolean useMdp, String keyPath, String sid) {
		Profile p = new Profile("commotionwireless.net", this.getContext());
		DotConf dotConf = null;
		FileOutputStream dotConfFileStream = null;
		ConnectivityManager cMgr = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		String mTetherIface = "wlan0";
		/*
		 * Generate the conf file!
		 */
		try {
			dotConf = new DotConf("/data/data/net.commotionwireless.meshtether/app_bin/olsrd.conf");
		} catch (IOException e) {
			Log.e("OlsrdControl", "Cannot open base olsrd.conf: " + e.toString());
			return;
		}
		if (useMdp) {
			Log.i("generateConf", "Using MDP");
			DotConf.PluginStanza mdpStanza = new DotConf.PluginStanza("/data/data/net.commotionwireless.meshtether/app_bin/olsrd_mdp.so.0.1");
			mdpStanza.addKeyValue("sid", sid);
			mdpStanza.addKeyValue("servalpath", keyPath);
			dotConf.addStanza(mdpStanza);
		}
		try {
			dotConfFileStream = mContext.openFileOutput("olsrd.conf", 0);
			dotConf.write(dotConfFileStream);
			dotConfFileStream.close();
		} catch (FileNotFoundException e) {
			Log.e("OlsrdControl", "Cannot open temp olsrd.conf: " + e.toString());
			return;
		} catch (IOException e) {
			Log.e("OlsrdControl", "Cannot open temp olsrd.conf: " + e.toString());
			return;
		}
		if (!p.getStringValue("if_lan").equalsIgnoreCase("")) {
			mTetherIface = p.getStringValue("if_lan");
		}
	}

	public void testMdpMain() {
		generateConf(true, "/sdcard/", "B0BFAFD6EC32C7372A1D4D2C2522B04F5BFC486EB6E1F3A954A754E8DFD2F16F");
		runMain();
	}

	public void testMain() {
		generateConf(false, null, null);
		runMain();
	}

	public void runMain() {
		mOlsrd.startMain(new String[] {"-i", "wlan0", "-d", "4", "-nofork", "-f", "/data/data/net.commotionwireless.meshtether/files/olsrd.conf", "-pidfile", "/data/data/net.commotionwireless.meshtether/files/olsrd.pid"});

		try {
			/*
			 * Run olsrd for 10 seconds.
			 */
			Thread.sleep(10000);
		} catch (InterruptedException interruptedEx) {
		}
		mOlsrd.stopMain();

		/*
		 * Wait for olsrd to stop.
		 */
		synchronized (mOlsrd) {
			while (mOlsrd.isRunning()) {
				try {
					mOlsrd.wait();
				} catch (InterruptedException interruptedEx) {
				}
			}
		}
	}
}