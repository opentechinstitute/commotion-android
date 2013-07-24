package net.commotionwireless.meshtether;

import java.io.File;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;

public class Profile {
	private String mName;
	private SharedPreferences mSharedPreferences;
	
	public Profile(String profileName, Context context) {
		mName = profileName;
		mSharedPreferences = context.getSharedPreferences(mName, Context.MODE_PRIVATE);
	}
	
	protected SharedPreferences getSharedPreferences() {
		return mSharedPreferences;
	}
	
	public void deepCopy(Profile p) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		Map<String, ?> existingProfile = p.getSharedPreferences().getAll();
		for (Map.Entry<String, ?> entry : existingProfile.entrySet()) {
			if (entry.getValue().getClass() == Boolean.class) {
				editor.putBoolean(entry.getKey(), (Boolean)entry.getValue());
			} else if (entry.getValue().getClass() == String.class) {
				editor.putString(entry.getKey(), (String)entry.getValue());
			}
		}
		editor.commit();
		editor.apply();
	}
	public String getProfileName() {
		return mName;
	}
	
	public String getValue(String key) {
		return mSharedPreferences.getString(key, "");
	}
	
	public String[] getKeys() {
		return (String[])mSharedPreferences.getAll().keySet().toArray();
	}
	
	public void setValue(String key, String value) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		
		editor.putString(key, value);
		
		editor.commit();
		editor.apply();
	}
	public void setValue(String key, boolean value) {
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		
		editor.putBoolean(key, value);
		
		editor.commit();
		editor.apply();
	}
	
}
