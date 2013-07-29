package net.commotionwireless.meshtether;
import junit.framework.Assert;
import android.app.Service;
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
		
		OlsrdControl() {
			mState = OlsrdState.STOPPED;
			mPid = 0;
		}
		
		public void restart() {
			Log.i("OlsrdControl", "OlsrdControl.restart()");
			stop();
			start();
		}
		
		public void start() {
			Log.i("OlsrdControl", "OlsrdControl.start()");
		}
		
		public void stop() {
			Log.i("OlsrdControl", "OlsrdControl.stop()");
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
		mOlsrdControl = new OlsrdControl();
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
