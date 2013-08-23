package net.commotionwireless.meshtether.test;

import net.commotionwireless.profiles.Profile;
import net.commotionwireless.profiles.Profiles;
import android.test.AndroidTestCase;


public class ProfileTest extends AndroidTestCase {
	private Profiles mProfiles;
	private String mProfileTestName;
	
	public void setUp() {
		mProfiles = new Profiles(getContext());
	}
	
	public void tearDown() {
		mProfiles = null;
	}
	
	public void testDeepEquals() {
		mProfileTestName = mProfiles.getNewProfileName();
		boolean isEqual = false;
		
		mProfiles.newProfile(mProfileTestName);
		Profile p = new Profile(mProfileTestName, getContext());
		
		isEqual = p.deepEquals(p);
		
		mProfiles.deleteProfile(mProfileTestName);
		
		assertTrue("Profile does not deep equal itself.", isEqual);
	}
	
	public void testNotDeepEqualsByValue() {
		mProfileTestName = mProfiles.getNewProfileName();
		String mProfileTest2Name = null;
		boolean isEqual = false;
		
		mProfiles.newProfile(mProfileTestName);
		mProfileTest2Name = mProfiles.getNewProfileName();
		mProfiles.newProfile(mProfileTest2Name);
		
		Profile p = new Profile(mProfileTestName, getContext());
		Profile p2 = new Profile(mProfileTest2Name, getContext());

		p2.setValue("lan_essid", "not default");
		isEqual = p.deepEquals(p2);
		
		mProfiles.deleteProfile(mProfileTestName);
		mProfiles.deleteProfile(mProfileTest2Name);

		assertFalse("Profile and Profile(2) do deep equal by value.", isEqual);
	}
	public void testNotDeepEqualsBySize() {
		mProfileTestName = mProfiles.getNewProfileName();
		String mProfileTest2Name = null;
		boolean isEqual = false;
		
		mProfiles.newProfile(mProfileTestName);
		mProfileTest2Name = mProfiles.getNewProfileName();
		mProfiles.newProfile(mProfileTest2Name);
		
		Profile p = new Profile(mProfileTestName, getContext());
		Profile p2 = new Profile(mProfileTest2Name, getContext());

		p2.setValue("new_key", "new_value");
		isEqual = p.deepEquals(p2);
		
		mProfiles.deleteProfile(mProfileTestName);
		mProfiles.deleteProfile(mProfileTest2Name);

		assertFalse("Profile and Profile(2) do deep equal by size.", isEqual);
	}
	
	public void testSetStringValue() {
		String setValue, getValue;
		mProfileTestName = mProfiles.getNewProfileName();
		Profile p;
		
		setValue = "set value";
		mProfiles.newProfile(mProfileTestName);
		p = new Profile(mProfileTestName, getContext());
		
		p.setValue("test_key", setValue);
		getValue = p.getStringValue("test_key");
		
		mProfiles.deleteProfile(mProfileTestName);
		
		assertTrue("Get value does not equal set value (String)", getValue.equals(setValue));
	}
	
	public void testSetBooleanValue() {
		boolean setValue, getValue;
		mProfileTestName = mProfiles.getNewProfileName();
		Profile p;
		
		setValue = true;
		mProfiles.newProfile(mProfileTestName);
		p = new Profile(mProfileTestName, getContext());
		
		p.setValue("test_key", setValue);
		getValue = p.getBooleanValue("test_key");
		
		mProfiles.deleteProfile(mProfileTestName);
		
		assertTrue("Get value does not equal set value (Boolean)", getValue == setValue);
	}
}
