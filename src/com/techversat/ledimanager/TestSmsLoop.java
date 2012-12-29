
package com.techversat.ledimanager;

import android.content.Context;

public class TestSmsLoop implements Runnable {

	Context context;
	boolean runLoop;
	
	public TestSmsLoop(Context context) {
		this.context = context;
	}

	public void run() {
		runLoop = true;
		for (int i = 1; runLoop; i++) {
			// NotificationBuilder.createSMS(context, "123-456-789", "\n  Test SMS #" + i);
			try {
				Thread.sleep(LEDIService.Preferences.smsLoopInterval*1000);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public void stop() {
		runLoop = false;
	}

}
