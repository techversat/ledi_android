

package com.techversat.ledimanager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.techversat.ledimanager.LEDIActivity;

public class AlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.d(LEDIActivity.TAG, "intent rec: "+ intent.toString());
		
		if (intent.hasExtra("action_update")) {
			
			// Monitors.updateWeatherData(context);
			
			return;
		}
		
	}

}
