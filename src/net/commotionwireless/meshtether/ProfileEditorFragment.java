package net.commotionwireless.meshtether;

import java.io.File;

import net.commotionwireless.profiles.Profile;
import net.commotionwireless.profiles.Profiles;

import org.servalproject.servald.ServalD;
import org.servalproject.servald.ServalD.KeyringListResult;
import org.servalproject.servald.ServalDFailureException;
import org.servalproject.servald.ServalDInterfaceError;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ipaulpro.afilechooser.FileChooserActivity;
import com.ipaulpro.afilechooser.utils.FileUtils;


public class ProfileEditorFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
	protected static final int REQUEST_CHOOSER = 1234;

	private String mProfileName = null;
	private Profiles mProfiles = null;
	
	private void updateProfileName(PreferenceManager mgr, String newName) {
		SharedPreferences.Editor editor = mgr.getSharedPreferences().edit();
		editor.putString("profile_name", newName);
		editor.commit();
		editor.apply();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Preference servalpathPreference = null;
		PreferenceManager mgr = this.getPreferenceManager();
		Intent resultIntent = new Intent();
		final Activity thisActivity = getActivity();
		Intent intent = thisActivity.getIntent();

		mProfileName = intent.getStringExtra("profile_name");

		mgr.setSharedPreferencesName(mProfileName);
		updateProfileName(mgr, mProfileName);
		resultIntent.putExtra("profile_name", mProfileName);
		thisActivity.setResult(0, resultIntent);
		thisActivity.setTitle(mProfileName);

		addPreferencesFromResource(R.xml.preferences);

		servalpathPreference = findPreference(getString(R.string.mdp_servalpath));
		servalpathPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference p) {
				Intent getFileIntent = new Intent(thisActivity, FileChooserActivity.class);
			    startActivityForResult(getFileIntent, REQUEST_CHOOSER);
				return true;
			}
		});
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CHOOSER && resultCode == Activity.RESULT_OK) {
			final Uri uri = data.getData();
			File file = FileUtils.getFile(uri);
			StringPreference filePreference = (StringPreference) findPreference(getString(R.string.mdp_servalpath));
			filePreference.setText(file.getAbsolutePath());

			getMdpPeers();
		}
	}
	@Override
	public void onResume() {
		super.onResume();
		PreferenceManager mgr = this.getPreferenceManager();
		mProfiles = new Profiles(this.getActivity());
		mgr.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		getMdpPeers();

	}
	@Override
	public void onPause() {
		super.onPause();
		PreferenceManager mgr = this.getPreferenceManager();
		mgr.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equalsIgnoreCase(getString(R.string.profile_name))) {
			/*
			 * The user wants to change the name of the profile!
			 */
			Profile existingProfile, newProfile;
			String newProfileName = sharedPreferences.getString(key, "");
			PreferenceManager mgr = this.getPreferenceManager();

			if (newProfileName.length()==0) 
				return;
			
			Log.i("ProfileEditorFragment", "Changing profile name from " + mProfileName + " to " + newProfileName);			
			
			existingProfile = new Profile(mProfileName, this.getActivity());
			newProfile = new Profile(newProfileName, this.getActivity());
			
			newProfile.deepCopy(existingProfile);
			
			mProfileName = newProfileName.substring(0);
			
			mProfiles.deleteProfile(existingProfile.getProfileName());
			mProfiles.newProfile(newProfile.getProfileName());
			mProfiles.setActiveProfileName(newProfile.getProfileName());

			mgr.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
			mgr.setSharedPreferencesName(mProfileName);
			mgr.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
			updateProfileName(mgr, mProfileName);

			getActivity().setTitle(mProfileName);
		}
	}
	
	private String getCallingActivityXmlName() {
		String callingActivityName = null;
		ComponentName callingComponent = this.getActivity().getCallingActivity();

		/*
		 * Check for null here. callingComponent may be null if this 
		 * was not start with startActivityForResult(); 
		 */
		if (callingComponent == null) {
			callingActivityName = this.getActivity().getLocalClassName();
		} else {
			callingActivityName = callingComponent.getShortClassName();
		}
		
		if (callingActivityName.startsWith(".")) {
			return callingActivityName.substring(1);
		}
		return callingActivityName;
	}

	private synchronized void getMdpPeers() {
		ServalD.ServalInstancePath = getPreferenceManager().getSharedPreferences().getString(getString(R.string.mdp_servalpath), null);
		KeyringListResult keyringResult = null;
		int counter = 0;
		ListPreference sidPreference = (ListPreference)findPreference(getString(R.string.mdp_sid));

		String sidVisibles[] = null, sidValues[] = null;
		KeyringListResult.Entry sids[] = null;

		if (ServalD.ServalInstancePath != null) {
			try {
				keyringResult = ServalD.keyringList();
			} catch (ServalDFailureException e) {
				sidPreference.setEnabled(false);
				return;
			} catch (ServalDInterfaceError e){
				sidPreference.setEnabled(false);
				return;
			}
			sids = keyringResult.entries;
		}
		if (sids == null) {
			sidPreference.setEnabled(false);
			return;
		}
		sidVisibles = new String[sids.length];
		sidValues = new String[sids.length];
		for (KeyringListResult.Entry e : sids) {
			sidVisibles[counter] = e.subscriberId.abbreviation();
			sidValues[counter] = e.subscriberId.toString();
			counter++;
		}

		sidPreference.setEnabled(true);
		sidPreference.setEntries(sidVisibles);
		sidPreference.setEntryValues(sidValues);
	}
}