package net.commotionwireless.olsrd;
import java.io.IOException;
import java.util.ArrayList;

import junit.framework.Assert;
import net.commotionwireless.meshtether.MeshTetherProcess;
import net.commotionwireless.meshtether.NativeHelper;
import net.commotionwireless.meshtether.Util;
import net.commotionwireless.profiles.NoMatchingProfileException;
import net.commotionwireless.profiles.Profile;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class OlsrdService extends Service {
	
	public static final int START_MESSAGE = 0;
	public static final int OLSRDSERVICE_MESSAGE_MIN = 1;
	public static final int CONNECTED_MESSAGE = 1;
	public static final int DISCONNECTED_MESSAGE = 2;
	public static final int NEWPROFILE_MESSAGE = 3;
	public static final int OLSRDSERVICE_MESSAGE_MAX = 3;

	public static final int TO_NOTHING = 0;
	public static final int TO_RUNNING = 1;
	public static final int TO_STOPPED = 2;
	public static final int TO_RESTART = 3;

	private boolean mRunning = false;
	private OlsrdControl mOlsrdControl = null;
	
	private enum OlsrdState { STOPPED, RUNNING;
		int mTransitions[][] = {
				/* STOPPED */ {/* CONNECTED */ TO_RUNNING, /* DISCONNECTED */ TO_NOTHING, /* NEWPROFILE */ TO_NOTHING},
				/* RUNNING */ {/* CONNECTED */ TO_NOTHING, /* DISCONNECTED */ TO_STOPPED, /* NEWPROFILE */ TO_RESTART}
		};
		public OlsrdState transition(int message, OlsrdControl control) {
			int transitionToDo = mTransitions[this.ordinal()][message-1];
			switch (transitionToDo) {
			case TO_RUNNING:
				control.start();
				return RUNNING;
			case TO_STOPPED:
				control.stop();
				return STOPPED;
			case TO_RESTART:
				control.restart();
				return RUNNING;
			default:
				return this;
			}
		}
	}

	class OlsrdControl {
		OlsrdState mState;
		int mPid;
		String mProfileName;
		Context mContext;
		MeshTetherProcess mProcess;
		
		OlsrdControl(Context context) {
			mState = OlsrdState.STOPPED;
			mPid = 0;
			mContext = context;
		}
		
		public void restart() {
			Log.i("OlsrdControl", "OlsrdControl.restart()");
			stop();
			start();
		}
		
		public void start() {
			ArrayList<String> profileEnvironment, systemEnvironment, combinedEnvironment;
			Profile p = new Profile(mProfileName, mContext);
			
			profileEnvironment = p.toEnvironment("brncl_");
			profileEnvironment.add("brncl_path=" + NativeHelper.app_bin.getAbsolutePath());
			profileEnvironment.add("olsrd_conf_path=" + NativeHelper.OLSRD_CONF);
			systemEnvironment = Util.getSystemEnvironment();
			
			(combinedEnvironment = (ArrayList<String>)systemEnvironment.clone()).addAll(profileEnvironment);

			Log.i("OlsrdControl", "OlsrdControl.start()");
			Log.i("OlsrdControl", "System environment: " + systemEnvironment.toString());
			Log.i("OlsrdControl", "Profile environment: " + profileEnvironment.toString());
			Log.i("OlsrdControl", "Combined environment: " + combinedEnvironment.toString());
			
			mProcess = new MeshTetherProcess(NativeHelper.SU_C, combinedEnvironment.toArray(new String[0]), NativeHelper.app_bin);
			try {
				mProcess.run(null, 1, 1);
			} catch (IOException e) {
				Log.e("OlsrdService", "Could not start process: " + e.toString());
			}
		}
		
		public void stop() {
			Log.i("OlsrdControl", "OlsrdControl.stop()");
			try {
				mProcess.stop();
			} catch (InterruptedException e) {
				Log.e("OlsrdService", "Could not stop process: " + e.toString());
			} catch (IOException e) {
				Log.e("OlsrdService", "Could not stop process: " + e.toString());
			}
		}
		
		public void setProfileName(String profileName) {
			mProfileName = profileName;
		}
		
		public String getProfileName() {
			return mProfileName;
		}
		
		public void transition(int message) {
			if (!(message>=OLSRDSERVICE_MESSAGE_MIN && message<=OLSRDSERVICE_MESSAGE_MAX)) {
				Log.e("OlsrdControl", "Transition message not appropriate");
				return;
			}
			
			OlsrdState oldState = mState;
			mState = mState.transition(message, this);
			
			Log.i("OlsrdControl", "Transitioned from " + oldState + " to " + mState + " on message " + message);
		}
	}
	
	@Override
	public void onCreate() {
    	Log.i("OlsrdService", "Starting ...");
		mRunning = true;
		mOlsrdControl = new OlsrdControl(this.getApplicationContext());
		NativeHelper.setup(this.getApplicationContext());
		NativeHelper.unzipAssets(this.getApplicationContext());
	}
	
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Assert.assertTrue(mRunning);
    	/*
    	 * N.B: intent may be null. This is the result of returning 
    	 * START_STICKY below. See http://developer.android.com/reference/android/app/Service.html#START_STICKY
    	 * for more information.
    	 */
        Log.i("OlsrdService", "Received start id " + startId + ": " + ((intent != null) ? intent : "No Intent"));        
        
        if (intent != null) {
        	String optionalProfileName = intent.getStringExtra("profile_name");
        	if (optionalProfileName != null) {
        		try {
        			Profile p = new Profile(optionalProfileName, this.getApplicationContext(), true);
        			mOlsrdControl.setProfileName(optionalProfileName);
            		Log.i("OlsrdService", "Intent has optional profile: " + p.toString());
        		} catch (NoMatchingProfileException e) {
        			/*
        			 * No matching profile.
        			 */
        			Log.e("OlsrdService", "No matching profile");
        		}
        	}
        	if (intent.getFlags() != 0) {
        		mOlsrdControl.transition(intent.getFlags());
        	}
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mRunning = false;
    }
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
