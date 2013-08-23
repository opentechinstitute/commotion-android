package net.commotionwireless.meshtether.test;

import net.commotionwireless.profiles.Profiles;
import android.test.AndroidTestCase;

public class ProfilesTest extends AndroidTestCase {
	
	private Profiles mProfiles;
	
	public void setUp() {
		mProfiles = new Profiles(getContext());
	}
	
	public void tearDown() {
		mProfiles = null;
	}
	
	public void testNewProfileName() {
		String newProfileName = null;
		int newProfileIndex = 0;
		
		newProfileName = mProfiles.getNewProfileName();
		mProfiles.newProfile(newProfileName);
		newProfileIndex = mProfiles.getProfileIndex(newProfileName);
		
		assertTrue("New profile not found!", newProfileIndex != Profiles.PROFILE_NOT_FOUND);
	}
	
	public void testRemoveProfile() {
		String newProfileName = null;
		int newProfileIndex = 0;
		
		newProfileName = mProfiles.getNewProfileName();
		mProfiles.newProfile(newProfileName);
		newProfileIndex = mProfiles.getProfileIndex(newProfileName);
		
		assertTrue("New profile not found! (in testRemoveProfile())", newProfileIndex != Profiles.PROFILE_NOT_FOUND);
		
		mProfiles.deleteProfile(newProfileName);
		
		newProfileIndex = mProfiles.getProfileIndex(newProfileName);
		assertTrue("Profile not removed!", newProfileIndex == Profiles.PROFILE_NOT_FOUND);
	}
}
