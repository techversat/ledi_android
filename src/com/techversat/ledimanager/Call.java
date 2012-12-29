
package com.techversat.ledimanager;


import android.content.Context;
import android.graphics.Bitmap;

public class Call {
	
	public static boolean isRinging = false;
	
	public static void startCall(Context context, String number) {
		toCall();
		
		isRinging = true;
		
		// Bitmap bitmap;
		String name = Utils.getContactNameFromNumber(context, number);
		
		/*
		if (name.equals(number))		
			bitmap = NotificationBuilder.smartLines(context, "phone.bmp", new String[] { number});
		else 
			bitmap = NotificationBuilder.smartLines(context, "phone.bmp", new String[] { number, name });
		*/
		// Notification.addOledNotification(context, Protocol.createOled1line(context, "phone.bmp", "Call from"), Protocol.createOled1line(context, null, name), null, 0, new VibratePattern(true, 500, 500, 3));
		Notification.addTextNotification(context, name + ": " + number, Notification.notificationTimeout);
		
		// Thread ringer = new Thread(new CallVibrate());
		// ringer.start();		
	}
	
	public static void endCall(Context context) {
		isRinging = false;
		exitCall(context);
	}
		
	static void toCall() {		
		LEDIService.watchState = LEDIService.LEDIStates.CALL;
		LEDIService.WatchModes.CALL = true;					
	}
	
	static void exitCall(Context context) {
				
		LEDIService.WatchModes.CALL = false;
				
		if (LEDIService.WatchModes.NOTIFICATION == true)
			Notification.toNotification(context);
		/*
		else if (LEDIService.WatchModes.APPLICATION == true)
			Application.toApp();
		*/
		else if (LEDIService.WatchModes.IDLE == true)
			Idle.toIdle(context);
	}
	
}


