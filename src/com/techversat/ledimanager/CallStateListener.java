                                                                     
                                                                     
package com.techversat.ledimanager;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import com.techversat.ledimanager.LEDIService;

class CallStateListener extends PhoneStateListener {
	
	Context context;
	
	public CallStateListener(Context ctx) {
		super();
		context = ctx;
	}

	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		super.onCallStateChanged(state, incomingNumber);
		
		if (!LEDIService.Preferences.notifyCall)
			return;
		
		if (incomingNumber == null)
			incomingNumber = "";

		switch (state) {
			case TelephonyManager.CALL_STATE_RINGING: 
				//String name = Utils.getContactNameFromNumber(context, incomingNumber);	
				//SendCommand.sendIncomingCallStart(incomingNumber, name, photo);
				Call.startCall(context, incomingNumber);
				break;
			case TelephonyManager.CALL_STATE_IDLE: 
				Call.endCall(context);
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK: 
				Call.endCall(context);
				break;
		}

	}
}
