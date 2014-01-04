/*
 *  This file is part of Commotion Mesh Tether
 *  Copyright (C) 2010 by Szymon Jakubczak
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.commotionwireless.meshtether;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;

import net.commotionwireless.olsrd.OlsrdService;
import net.commotionwireless.profiles.Profile;
import net.commotionwireless.profiles.Profiles;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;

public class StatusActivity extends android.app.TabActivity implements OnItemSelectedListener {
	private MeshTetherApp app;

	private TabHost tabs;
	private ImageButton mOnOffButton;
	private Spinner chooseProfile;
	private AlertDialog.Builder profileDialogBuilder;
	
	
	private Profiles mProfiles;
	private boolean paused;

	private TextView textDownloadRate;
	private TextView textUploadRate;

	private final static String LINKS = "links";
	private final static String INFO = "info";
	private final static String ABOUT = "about";

	private BroadcastReceiver mOlsrdStateReceiver;


	static NumberFormat nf = NumberFormat.getInstance();
	static {
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		nf.setMinimumIntegerDigits(1);
	}

	ProgressDialog mProgressDialog;
	final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mProgressDialog.setMessage(msg.getData().getString("message"));
		}
	};

	public void onOffClick(View v) {
		OlsrdService olsrdService = app.getOlsrdService();
		WifiManager wifiManager = (WifiManager)this.getSystemService(WIFI_SERVICE);
		if (olsrdService != null && olsrdService.isOlsrdRunning()) {
			/*
			 * stop!
			 */
			WifiConfiguration config = NetworkStateChangeReceiver.getActiveWifiConfiguration(wifiManager);
			int networkId = wifiManager.getConnectionInfo().getNetworkId();

			if (config.priority == Profile.COMMOTION_PRIORITY)
				Log.i("StatusActivity", "Would remove network!");

			wifiManager.disableNetwork(networkId);

			if (config.priority == Profile.COMMOTION_PRIORITY)
				wifiManager.removeNetwork(networkId);

			wifiManager.saveConfiguration();
		} else {
			/*
			 * start!
			 */
			int networkId = 0;
			WifiConfiguration newWc = new WifiConfiguration();
			Profile mProfile = new Profile(mProfiles.getActiveProfileName(), this);

			if (!mProfile.getWifiConfiguration(newWc)) {
				Log.e("StatusActivity", "Failing to start because getWifiConfiguration() failed.");
				return;
			}

			Log.w("StatusActivity", "Attempting to start with configuration: " + newWc.toString());
			networkId = wifiManager.addNetwork(newWc);
			wifiManager.saveConfiguration();
			wifiManager.enableNetwork(networkId, true);
		}
	}
	/*
	 * Lifecycle methods.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		app = (MeshTetherApp)getApplication();

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setProgressBarIndeterminate(true);
		setContentView(R.layout.main);
		
		
		mOnOffButton = (ImageButton)findViewById(R.id.onoff);

		profileDialogBuilder = new AlertDialog.Builder(this);
		profileDialogBuilder.setTitle(R.string.choose_profile);
		
		tabs = getTabHost();
		tabs.addTab(tabs.newTabSpec(LINKS)
				.setIndicator(LINKS, getResources().getDrawable(R.drawable.ic_tab_contacts))
				.setContent(new Intent(this, LinksActivity.class)));
		tabs.addTab(tabs.newTabSpec(INFO)
				.setIndicator(INFO, getResources().getDrawable(R.drawable.ic_tab_recent))
				.setContent(new Intent(this, InfoActivity.class)));
		tabs.addTab(tabs.newTabSpec(ABOUT)
				.setIndicator(ABOUT, getResources().getDrawable(R.drawable.comlogo_sm))
				.setContent(new Intent(this, AboutActivity.class)));
		tabs.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				update();
				// force refresh of up/down stats
				/*
				 * FIXME
				 */
				/*
				if (app.service != null)
					app.service.statsRequest(0);
				*/
				if (INFO.equals(tabId))
					app.infoActivity.update();
				if (app.linksActivity != null)
					if (LINKS.equals(tabId))
						app.linksActivity.mPauseOlsrInfoThread = false;
					else
						app.linksActivity.mPauseOlsrInfoThread = true;
			}
		});

		app.setStatusActivity(this);
		paused = false;

		textDownloadRate = ((TextView)findViewById(R.id.download_rate));
		textUploadRate = ((TextView)findViewById(R.id.upload_rate));

		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();
		app.setStatusActivity(null);
	}
	@Override
	protected void onPause() {
		super.onPause();
		paused = true;
		unregisterReceiver(mOlsrdStateReceiver);
		mOlsrdStateReceiver = null;
	}
	@Override
	protected void onResume() {
		super.onResume();
		
		mProfiles = new Profiles(this);
		chooseProfile = (Spinner)findViewById(R.id.choose_profile);
		chooseProfile.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mProfiles.getProfileList()));
		chooseProfile.setSelection(mProfiles.getProfileIndex(mProfiles.getActiveProfileName()));
		chooseProfile.setOnItemSelectedListener(this);
		
		paused = false;
		update();
		/* 
		 * FIXME
		 */
		/*
		app.cleanUpNotifications();
		*/
		IntentFilter olsrdIntentFilter = new IntentFilter("net.commotionwireless.meshtether.OLSRD_TRANSITION");
		registerReceiver(mOlsrdStateReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				OlsrdService.OlsrdState olsrdRunningState = OlsrdService.OlsrdState.RUNNING;
				int currentState = intent.getIntExtra("state", OlsrdService.OlsrdState.STOPPED.ordinal());
				if (currentState == olsrdRunningState.ordinal()) {
					/*
					 * running!
					 */
					mOnOffButton.setImageResource(R.drawable.commotion_power_on_icon);
				} else {
					/* 
					 * not running
					 */
					mOnOffButton.setImageResource(R.drawable.commotion_power_off_icon);
				}
			}
			
		}, olsrdIntentFilter);
		if (app.getOlsrdService() != null && app.getOlsrdService().isOlsrdRunning()) {
			mOnOffButton.setImageResource(R.drawable.commotion_power_on_icon);
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public String getSelectedProfileName() {
		Spinner spinner = (Spinner)findViewById(R.id.choose_profile);
		String selectedString = (String)spinner.getSelectedItem();
		
		return selectedString;
	}
	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		String newActiveProfile = (String)parent.getItemAtPosition(pos);
		mProfiles.setActiveProfileName(newActiveProfile);
	}
	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO FIXME
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		String profileName = null;
		
		switch (item.getItemId()) {
		case R.id.menu_new_profile:
			profileName = mProfiles.getNewProfileName();
			mProfiles.newProfile(profileName);
			mProfiles.setActiveProfileName(profileName);
			/* fall through */
		case R.id.menu_prefs:
			Intent intent = new Intent(this, ProfileEditorActivity.class);
			/*
			 * Put the profile_name that is either 
			 * a) newly generated (when we are making a new profile) or
			 * b) the selected profile (when are editing an existing profile).
			 */
			intent.putExtra("profile_name", (profileName != null) ? profileName : mProfiles.getActiveProfileName());
			this.startActivityForResult(intent, 0);
			return true;
		case R.id.menu_share_debug:
			zipAndShareFile(new File(NativeHelper.app_log, "olsrd.log"));
			return true;
		case R.id.menu_share_status:
			getOlsrdStatusAndShare();
			return true;
		}
		return(super.onOptionsItemSelected(item));
	}

	private void getOlsrdStatusAndShare() {
		
	}

	private void zipAndShareFile(File f) {
		if (! NativeHelper.isSdCardPresent()) {
			app.updateToast("Cannot find SD card, needed for saving the zip file.", true);
			return;
		}
		if (! f.exists()) {
			app.updateToast(f.getAbsolutePath() + " does not exist!", true);
			return;
		}
		final File zipFile = new File(NativeHelper.publicFiles, f.getName() + ".zip");
		try {
			NativeHelper.zip(f, zipFile);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		Intent i = new Intent(android.content.Intent.ACTION_SEND);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		i.setType("application/zip");
		i.putExtra(Intent.EXTRA_SUBJECT, "log from Commotion Mesh Tether");
		i.putExtra(Intent.EXTRA_TEXT, "Attached is an log sent by Commotion Mesh Tether.  For more info, see:\nhttps://code.commotionwireless.net/projects/commotion-android");
		i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(zipFile));
		startActivity(Intent.createChooser(i, "How do you want to share?"));
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		return null;
	}

	static String formatRate(long v) {
		if (v < 1048576)
			return nf.format(v /	1024.0f) + " KB";
		else
			return nf.format(v / 1048576.0f) + " MB";
	}

	void update() {
		
	}
}
