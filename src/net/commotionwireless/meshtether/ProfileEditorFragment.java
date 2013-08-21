package net.commotionwireless.meshtether;

import net.commotionwireless.profiles.Profile;
import net.commotionwireless.profiles.Profiles;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;


public class ProfileEditorFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
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

		PreferenceManager mgr = this.getPreferenceManager();
		Intent resultIntent = new Intent();
		Intent intent = this.getActivity().getIntent();
		
		mProfileName = intent.getStringExtra("profile_name");
		mgr.setSharedPreferencesName(mProfileName);
		updateProfileName(mgr, mProfileName);
		resultIntent.putExtra("profile_name", mProfileName);
		this.getActivity().setResult(0, resultIntent);

		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onResume() {
		super.onResume();
		PreferenceManager mgr = this.getPreferenceManager();
		mProfiles = new Profiles(this.getActivity());
		mgr.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
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
			
			mgr.setSharedPreferencesName(mProfileName);
			updateProfileName(mgr, mProfileName);
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
}