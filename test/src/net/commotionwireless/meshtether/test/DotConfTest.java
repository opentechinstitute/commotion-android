package net.commotionwireless.meshtether.test;

import java.io.FileOutputStream;
import java.io.IOException;

import net.commotionwireless.olsrd.DotConf;
import android.content.Context;
import android.test.AndroidTestCase;
import android.util.Log;

public class DotConfTest extends AndroidTestCase {
	private DotConf mConf; 
	public void setUp() {
		try {
			mConf = new DotConf(getContext().getAssets().open("olsrd.conf"));
		} catch (IOException e) {
			assertFalse("IOException: " + e.toString(), true);
		}
	}
	
	public void tearDown() {
		
	}
	
	public void testDotConfToString() {
		Log.i("DotConfTest", mConf.toString());
	}
	
	public void testAddDefaultStanza() {
		DotConf.Stanza stanza = new DotConf.Stanza();
		stanza.addKeyValue("key1", "value1");
		mConf.addStanza(stanza);
		Log.i("DotConfTest", mConf.toString());
	}
	
	public void testAddLoadPluginStanza() {
		DotConf.Stanza stanza = new DotConf.PluginStanza("path1");
		stanza.addKeyValue("pluginkey1", "pluginvalue1");
		mConf.addStanza(stanza);
		Log.i("DotConfTest", mConf.toString());
	}
	
	public void testWrite() {
		try {
			FileOutputStream f = getContext().openFileOutput("test.txt", Context.MODE_WORLD_WRITEABLE);
			mConf.write(f);
		} catch (IOException e) {
			assertFalse("IOException :" + e.toString(), true);
		}
	}
}
