                                                                     

package com.techversat.ledimanager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class Settings extends PreferenceActivity {
	
	Context context;
	
	PreferenceScreen preferenceScreen;
	Preference discovery;
	
	EditTextPreference editTextMac;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		
		setTitle(getString(R.string.app_name) + " - " + getString(R.string.activitiy_title_settings));
		
		addPreferencesFromResource(R.layout.settings);

		preferenceScreen = getPreferenceScreen();
		
	}

	@Override
	protected void onStart() {

		Log.d(LEDIActivity.TAG, "set onstart");
		
		editTextMac = (EditTextPreference) preferenceScreen.findPreference("MAC");
		editTextMac.setText(LEDIService.Preferences.watchMacAddress);
		
		discovery = preferenceScreen.findPreference("Discovery");
		discovery.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference arg0) {
				
				Log.d(LEDIActivity.TAG, "discovery click");
				
				startActivity(new Intent(context, DeviceSelection.class));
				
				return false;
			}
		});

		super.onStart();
	}
	
	

}
