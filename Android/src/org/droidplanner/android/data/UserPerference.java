package org.droidplanner.android.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.io.Serializable;


/**
 * @author Administrator
 *
 */
public class UserPerference implements Serializable {
	private UserPerference() {
	}

	/**
	 * UID
	 */
	private static final long serialVersionUID = 1L;

	private final static String FILE_SETTING_DATA = "Setting";
	private final static int SAVE_MODE = 0;

	private final static String HOME_VALID = "isHomeValid";
	private final static String DRONE_CONNECTED = "isDroneConnected";





	private static UserPerference mUserPerference = new UserPerference();

	public static UserPerference getUserPerference(Context con) {
		context = con;
		mSharedPreferences = context.getSharedPreferences(FILE_SETTING_DATA,
				SAVE_MODE);
		return mUserPerference;
	}

	private static SharedPreferences mSharedPreferences = null;
	private static Context context = null;

//	public SharedPreferences setContext(final Context con) {
//
//		return mSharedPreferences;
//	}

	private String getStringValue(String key) {
		String value = mSharedPreferences.getString(key, null);
		return value;
	}

	private void putStringValue(String key, String value) {
		Editor editor = mSharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}

	private boolean getBooleanValue(String key) {
		boolean bl = mSharedPreferences.getBoolean(key, false);
		return bl;
	}

	private void putBooleanValue(String key, boolean value) {
		Editor editor = mSharedPreferences.edit();
		editor.putBoolean(key, value);
		editor.commit();
	}
	
	private int getIntValue(String key) {
		int value = mSharedPreferences.getInt(key, 0);
		return value;
	}

	private void putIntValue(String key, int value) {
		Editor editor = mSharedPreferences.edit();
		editor.putInt(key, value);
		editor.commit();
	}

	public void setIsHomeValid(boolean bl){
		putBooleanValue(HOME_VALID, bl);
	}

	public boolean getIsHomeValid(){
		return getBooleanValue(HOME_VALID);
	}

	public void setIsDroneConnected(boolean bl){
		putBooleanValue(DRONE_CONNECTED,bl);
	}

	public boolean getIsDroneConnected(){
		return getBooleanValue(DRONE_CONNECTED);
	}

}
