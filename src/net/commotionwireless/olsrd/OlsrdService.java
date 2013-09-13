package net.commotionwireless.olsrd;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import junit.framework.Assert;
import net.commotionwireless.meshtether.MeshTetherApp;
import net.commotionwireless.meshtether.MeshTetherProcess;
import net.commotionwireless.meshtether.NativeHelper;
import net.commotionwireless.meshtether.NetworkStateChangeReceiver;
import net.commotionwireless.meshtether.R;
import net.commotionwireless.meshtether.Util;
import net.commotionwireless.profiles.NoMatchingProfileException;
import net.commotionwireless.profiles.Profile;
import net.commotionwireless.route.EWifiConfiguration;
import net.commotionwireless.route.EWifiManager;
import net.commotionwireless.route.RLinkProperties;
import net.commotionwireless.route.RRouteInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;


public class OlsrdService extends Service {
	
	public static final int START_MESSAGE = 0;
	public static final int OLSRDSERVICE_MESSAGE_MIN = 1;
	public static final int CONNECTING_MESSAGE = 1;
	public static final int CONNECTED_MESSAGE = 2;
	public static final int DISCONNECTED_MESSAGE = 3;
	public static final int NEWPROFILE_MESSAGE = 4;
	public static final int OLSRDSERVICE_MESSAGE_MAX = 4;
	
	public static final int TO_NOTHING = 0;
	public static final int TO_RUNNING = 1;
	public static final int TO_STOPPED = 2;
	public static final int TO_RESTART = 3;

	public static final String OLSRD_CHANGE_ACTION = "net.commotionwireless.olsrd.OLSRD_CHANGE";

	private boolean mRunning = false;
	private OlsrdControl mOlsrdControl = null;
	private WifiManager mMgr;
	private EWifiManager mEmgr;
	private WifiConfiguration mWifiConfig;
	private EWifiConfiguration mEwifiConfig;

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.i("OlsrdService", "Got a message: " + msg.obj.toString());
			String cmd = null;
			String cmdParts[] = null;
			cmd = msg.obj.toString();
			cmdParts = cmd.split(" ");
			if (cmdParts.length == 4 && (cmdParts[0].equalsIgnoreCase("ADD") || cmdParts[0].equalsIgnoreCase("DEL"))) {
				RRouteInfo route;
				RLinkProperties linkProperties;

				try {
					route = RRouteInfo.parseFromCommand(cmd);
				} catch (UnknownHostException unknown) {
					Log.e("OlsrdService", "Could not parse a route command from olsrd: " + cmd);
					return;
				}
				
				if (mWifiConfig == null || mEmgr == null || mEwifiConfig == null) {
					Log.e("OlsrdService", "It appears that things are bad!");
					return;
				}
				linkProperties = new RLinkProperties(mEwifiConfig.getLinkProperties());
				if ("ADD".equalsIgnoreCase(cmdParts[0])) {
					Log.i("OlsrdService", "Would add a route (" + cmdParts[1] + "/" + cmdParts[2] + "->" + cmdParts[3] + ")");
					linkProperties.addRoute(route);
					
				} else if ("DEL".equalsIgnoreCase(cmdParts[0])) {
					Log.i("OlsrdService", "Would delete a route (" + cmdParts[1] + "/" + cmdParts[2] + "->" + cmdParts[3] + ")");
					linkProperties.removeRoute(route);
				}
				mEwifiConfig.setLinkProperties(linkProperties);
				mEmgr.save(mWifiConfig);
			}
		}
	};
	
	private enum OlsrdState { STOPPED, RUNNING;
		int mTransitions[][] = {
				/* STOPPED */ {/* CONNECTING */ TO_NOTHING, /* CONNECTED */ TO_RUNNING, /* DISCONNECTED */ TO_NOTHING, /* NEWPROFILE */ TO_NOTHING},
				/* RUNNING */ {/* CONNECTING */ TO_NOTHING, /* CONNECTED */ TO_NOTHING, /* DISCONNECTED */ TO_STOPPED, /* NEWPROFILE */ TO_RESTART}
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
		String mProfileName;
		Context mContext;
		MeshTetherProcess mProcess;
		
		final String mOlsrdPidFilePath = NativeHelper.app_log.getAbsolutePath() + "/olsrd.pid";
		final String mOlsrdConfBaseFilePath = NativeHelper.app_bin.getAbsolutePath() + "/olsrd.conf";
		final String mOlsrdStopPath = NativeHelper.app_bin.getAbsolutePath() + "/stop_olsrd";
		final String mOlsrdStartPath = NativeHelper.app_bin.getAbsolutePath() + "/run_olsrd";
		final String mOlsrdEnvironmentVariablePrefix = "brncl_";

		OlsrdControl(Context context) {
			mState = OlsrdState.STOPPED;
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
			String olsrdConfFilePath = null;
			DotConf dotConf = null;
			File dotConfFile = null;
			FileOutputStream dotConfFileStream = null;

			/*
			 * Generate the conf file!
			 */
			try {
				 dotConf = new DotConf(mOlsrdConfBaseFilePath);
			} catch (IOException e) {
				Log.e("OlsrdControl", "Cannot open base olsrd.conf: " + e.toString());
				return;
			}
			if (p.getBooleanValue(mContext.getString(R.string.use_mdp))) {
				Log.i("OlsrdControl", "Using MDP");
				DotConf.PluginStanza mdpStanza = new DotConf.PluginStanza("/data/data/net.commotionwireless.meshtether/app_bin/olsrd_mdp.so.0.1");
				mdpStanza.addKeyValue("sid", p.getStringValue(mContext.getString(R.string.mdp_sid)));
				mdpStanza.addKeyValue("servalpath", p.getStringValue(mContext.getString(R.string.mdp_servalpath)));
				dotConf.addStanza(mdpStanza);
			}
			try {
				dotConfFileStream = mContext.openFileOutput("olsrd.conf", 0);
				dotConfFile = mContext.getFileStreamPath("olsrd.conf");
				dotConf.write(dotConfFileStream);
				dotConfFileStream.close();
			} catch (FileNotFoundException e) {
				Log.e("OlsrdControl", "Cannot open temp olsrd.conf: " + e.toString());
				return;
			} catch (IOException e) {
				Log.e("OlsrdControl", "Cannot open temp olsrd.conf: " + e.toString());
				return;
			}
			
			profileEnvironment = p.toEnvironment(mOlsrdEnvironmentVariablePrefix);
			profileEnvironment.add(mOlsrdEnvironmentVariablePrefix + "path=" + NativeHelper.app_bin.getAbsolutePath());
			profileEnvironment.add("olsrd_conf_path=" + dotConfFile);
			profileEnvironment.add(mOlsrdEnvironmentVariablePrefix + "olsrd_pid_file=" + mOlsrdPidFilePath);
			systemEnvironment = Util.getSystemEnvironment();
			
			(combinedEnvironment = (ArrayList<String>)systemEnvironment.clone()).addAll(profileEnvironment);

			Log.i("OlsrdControl", "OlsrdControl.start()");
			Log.i("OlsrdControl", "System environment: " + systemEnvironment.toString());
			Log.i("OlsrdControl", "Profile environment: " + profileEnvironment.toString());
			Log.i("OlsrdControl", "Combined environment: " + combinedEnvironment.toString());
			
			mWifiConfig = NetworkStateChangeReceiver.getActiveWifiConfiguration(mMgr);
			mEwifiConfig = new EWifiConfiguration(mWifiConfig);
			
			mProcess = new MeshTetherProcess(NativeHelper.SU_C + " " + mOlsrdStartPath, 
					combinedEnvironment.toArray(new String[0]), 
					NativeHelper.app_bin);
			
			try {
				Intent startIntent = new Intent();
				startIntent.setAction(OlsrdService.OLSRD_CHANGE_ACTION);
				mProcess.run(mHandler, 1, 1);
				mContext.sendBroadcast(startIntent);
			} catch (IOException e) {
				Log.e("OlsrdService", "Could not start process: " + e.toString());
			}
		}
		/*
		 * TODO: Should be able to make this much simpler now!
		 */
		public void stop() {
			ArrayList<String> systemEnvironment = null;
			systemEnvironment = Util.getSystemEnvironment();
			MeshTetherProcess olsrdStopper = null;
			
			systemEnvironment.add(mOlsrdEnvironmentVariablePrefix + "path=" + NativeHelper.app_bin.getAbsolutePath());
			systemEnvironment.add(mOlsrdEnvironmentVariablePrefix + "olsrd_pid_file=" + mOlsrdPidFilePath);
			
			olsrdStopper = new MeshTetherProcess(NativeHelper.SU_C + " " + mOlsrdStopPath,
					systemEnvironment.toArray(new String[0]), 
					NativeHelper.app_bin);
			
			Log.i("OlsrdControl", "OlsrdControl.stop()");
			Log.i("OlsrdControl", "Trying to stop with: " + mOlsrdStopPath);
			
			try {
				Intent stopIntent = new Intent();
				stopIntent.setAction(OlsrdService.OLSRD_CHANGE_ACTION);
				olsrdStopper.run(mHandler, 1,1);
				mProcess.stop();
				mProcess = null;
				mContext.sendBroadcast(stopIntent);
			} catch (InterruptedException e) {
				Log.e("OlsrdService", "Could not stop process: " + e.toString());
			} catch (IOException e) {
				Log.e("OlsrdService", "Could not stop process: " + e.toString());
			}
			mWifiConfig = null;
			mEwifiConfig = null;
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
	
	public boolean isOlsrdRunning() {
		return (mOlsrdControl.mState == OlsrdState.RUNNING);
	}

	@Override
	public void onCreate() {
    	Log.i("OlsrdService", "Starting ...");
		mRunning = true;
		mOlsrdControl = new OlsrdControl(this.getApplicationContext());
		NativeHelper.setup(this.getApplicationContext());
		NativeHelper.unzipAssets(this.getApplicationContext());
		mMgr = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		mEmgr = new EWifiManager(mMgr);
		MeshTetherApp app = (MeshTetherApp)this.getApplication();
		app.setOlsrdService(this);
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
