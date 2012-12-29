package com.techversat.ledimanager;

import com.techversat.ledimanager.LEDIService.Preferences;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootUpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		
		
		if (sharedPreferences.getBoolean("StartOnBoot", Preferences.startOnBoot)) {
        	context.startService(new Intent(context, LEDIService.class));
		}
        	
    }

}
