
package net.commotionwireless.profiles;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


public class Profiles {
	
	private Context mContext;
	private SharedPreferences mPreferences;
	private static final String DefaultProfileName = "DefaultProfile";
	private String mActiveProfileName = null;
	
	public static final int PROFILE_NOT_FOUND = -1;
	
	public Profiles(Context context) {
		this(context, DefaultProfileName);
	}
	
	public Profiles(Context context, String nonDefaultName) {
		mContext = context;
		mPreferences = mContext.getSharedPreferences(nonDefaultName, Activity.MODE_PRIVATE);
		mActiveProfileName = mPreferences.getString("active_profile", "");
	}

	/*
	 * Getters.
	 */
	public String[] getProfileList() {
		String[] profileNamesArray = new String[0];
		profileNamesArray = getExistingProfiles().toArray(profileNamesArray);	
		return profileNamesArray;
	}
	
	public int getProfileIndex(String profileName) {
		String profileNames[] = getProfileList();
		int counter = 0;
		for (String name : profileNames) {
			if (name.equalsIgnoreCase(profileName)) {
				return counter;
			}
			counter++;
		}
		return PROFILE_NOT_FOUND;
	}
	
	public String getNewProfileName() {
		String newProfileName = "New Profile #" + mPreferences.getInt("profiles_count", 1);
		newProfileName = newProfileName.replace(' ', '_');
		return newProfileName;
	}
	
	public String getActiveProfileName() {
		return mActiveProfileName;
	}
	
	/*
	 * Setters.
	 */
	synchronized public void newProfile(String newProfileName) {
		Set<String> existingProfiles = getExistingProfiles();
		SharedPreferences.Editor editor = null;
		
		existingProfiles.add(newProfileName);
		
		editor = mPreferences.edit();
		Log.i("getExistingProfiles", "writing these to disk: " + existingProfiles.toString());
		editor.putStringSet("profile_names", existingProfiles);
		updateProfileCount(editor);
		editor.commit();
	}
	
	public void setActiveProfileName(String activeProfile) {
		SharedPreferences.Editor editor = null;
		
		mActiveProfileName = activeProfile;
		editor = mPreferences.edit();
		editor.putString("active_profile", mActiveProfileName);
		editor.apply();
	}
	
	synchronized public void deleteProfile(String profileToDelete) {
		File profileToDeleteFile = new File(mContext.getFilesDir().getParent() + "/shared_prefs/" + profileToDelete + ".xml");
		Set<String> existingProfiles = getExistingProfiles();
		SharedPreferences.Editor editor = null;
		
		existingProfiles.remove(profileToDelete);
		
		editor = mPreferences.edit();
		editor.putStringSet("profile_names", existingProfiles);
		updateProfileCount(editor);
		editor.commit();
		
		if (!profileToDeleteFile.delete()) {
			Log.e("Profiles", "Failed to delete " + profileToDelete + "'s file");
		}
	}
	
	/*
	 * The profile_count keeps track of how many changes
	 * we've made to this file. This can be used for many things,
	 * but mostly we use it for 
	 * a) convincing Android that this editor.commit() really
	 * needs to hit the disk and
	 * b) generating new profile names.
	 */
	private void updateProfileCount(SharedPreferences.Editor editor) {
		editor.putInt("profiles_count", mPreferences.getInt("profiles_count", 1)+1);

	}
	
	/*
	 * Implementation helper functions.
	 */
	private Set<String> getExistingProfiles() {
		Set<String> defaultProfileNames = new HashSet<String>();
		Set<String> profileNames = null;
		
		defaultProfileNames.add("commotionwireless.net");
		
		try {
			profileNames = mPreferences.getStringSet("profile_names", null);
			if (profileNames == null) {
				throw new ClassCastException();
			}
		} catch (ClassCastException exception) {
			SharedPreferences.Editor editor = mPreferences.edit();
			editor.putStringSet("profile_names", defaultProfileNames);
			editor.putInt("profiles_count", mPreferences.getInt("profiles_count", 1));
			editor.commit();
			profileNames = defaultProfileNames;
		}
		Log.i("getExistingProfiles", profileNames.toString());
		return profileNames;
	}
	
}
