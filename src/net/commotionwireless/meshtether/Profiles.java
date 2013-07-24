
package net.commotionwireless.meshtether;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;


public class Profiles {
	
	private static Profiles mProfiles;
	private Activity mActivity;
	private SharedPreferences mPreferences;
	
	/*
	 * Constructor: Use the activity's name as the name of the Preference file.
	 */
	public static synchronized void instantiateSharedProfiles(Activity activity) {
		Assert.assertNull("instantiateSharedProfiles() already called.", mProfiles);
		mProfiles = new Profiles(activity);
	}
	/*
	 * Constructor: Use a non-default name as the name of the Preference file.
	 */
	public static synchronized void instantiateSharedProfiles(Activity activity, String nonDefaultName) {
		Assert.assertNull("instantiateSharedProfiles() already called.", mProfiles);
		mProfiles = new Profiles(activity, nonDefaultName);
	}
	/*
	 * Deconstructor.
	 */
	public static synchronized void unInstantiateSharedProfiles() {
		Assert.assertNotNull("instantiateSharedProfiles not called prior to unInstantiateSharedProfiles", mProfiles);
		mProfiles = null;
	}
	/*
	 * Singleton: Get the singleton.
	 */
	public static synchronized Profiles getSharedProfiles() {
		Assert.assertNotNull("instantiateSharedProfiles not called prior to getSharedProfiles()", mProfiles);
		return mProfiles;
	}
	
	private Profiles(Activity activity) {
		mActivity = activity;
		mPreferences = mActivity.getPreferences(Activity.MODE_PRIVATE);
	}
	
	private Profiles(Activity activity, String nonDefaultName) {
		mActivity = activity;
		mPreferences = mActivity.getSharedPreferences(nonDefaultName, Activity.MODE_PRIVATE);
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
		return 0;
	}
	
	/*
	 * TODO Integrate!
	 */
	public String getNewProfileName() {
		String newProfileName = "New Profile #" + mPreferences.getInt("profile_count", 1);
		newProfileName = newProfileName.replace(' ', '_');
		return newProfileName;
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
	
	synchronized public void deleteProfile(String profileToDelete) {
		File profileToDeleteFile = new File(mActivity.getFilesDir().getParent() + "/shared_prefs/" + profileToDelete + ".xml");
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
