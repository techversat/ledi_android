package com.techversat.ledimanager;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public class Notification {
	
	static NotificationType lastNotification = null;
	
	public static ArrayList<NotificationType> notificationQueue = new ArrayList<Notification.NotificationType>();
	public static boolean isSending = false;
	
	public static final int notificationTimeout = 5000;
	
	public static Object scrollRequest = new Object();
	
	public static class NotificationType {
		Bitmap bitmap;
		String msg;
		int[] array;
		byte[] buffer;
		
		byte[] oledTop;
		byte[] oledBottom;
		byte[] oledScroll;
		
		int scrollLength;
		int timeout;
		
		VibratePattern vibratePattern;
	}
	
	public static class VibratePattern {
		boolean vibrate = false;
		int on;
		int off;
		int cycles;
				
		public VibratePattern(boolean vibrate, int on, int off, int cycles) {
			this.vibrate = vibrate;
			this.on = on;
			this.off = off;
			this.cycles = cycles;
		}
	}
	
	public static void processNotificationQueue(final Context context) {
		if (isSending)
			return;
		else
			isSending = true;
		
		Thread thread = new Thread() {
			public void run() {
				
				if (notificationQueue.size() > 0) {
					LEDIService.watchState = LEDIService.LEDIStates.NOTIFICATION;
					LEDIService.WatchModes.NOTIFICATION = true;
				}
				
				while (notificationQueue.size() > 0) {
					NotificationType notification = notificationQueue.get(0);
					
					//Protocol.loadTemplate(2);
						
						Protocol.sendText(null, notification.msg);
						// Protocol.sendOledDisplay(notification.oledBottom, false, false);
						
						/*
						 * replace with buzzer beep functionality here
						if (notification.vibratePattern.vibrate)
							Protocol.vibrate(notification.vibratePattern.on, notification.vibratePattern.off, notification.vibratePattern.cycles);
						*/
						/*
						if (notification.oledScroll != null) {
							if (notification.scrollLength < 1)
								return;
							if (notification.scrollLength >= 240) {
								Protocol.sendOledBufferPart(notification.oledScroll, 0, 240, true, false);
								// wait continue with scroll 
																
								for (int i = 240; i < notification.scrollLength; i += 80) {
									// wait for next request
									try {
										synchronized (Notification.scrollRequest) {
												Notification.scrollRequest.wait(60000);									
										}
									} catch (InterruptedException e) {
									}
									
									if (i+80 >= notification.scrollLength)
										Protocol.sendOledBufferPart(notification.oledScroll, i, 80, false, true);
									else
										Protocol.sendOledBufferPart(notification.oledScroll, i, 80, false, false);
								}								
								
								// wait for scroll to finish
								try {
									synchronized (Notification.scrollRequest) {
											Notification.scrollRequest.wait(60000);									
									}
								} catch (InterruptedException e) {
								}
								
							} else {
								int len = notification.scrollLength / 20 + 1;
								Protocol.sendOledBufferPart(notification.oledScroll, 0, len*20, true, true);
							}							
						}
							*/
					
										
					LEDIService.nap(notification.timeout);
					
					if (LEDIService.WatchModes.CALL == true) {
						isSending = false;
						return;
					}						
					
					notificationQueue.remove(0);
				}				
				isSending = false;
				
				exitNotification(context);
			}
		};
		thread.start();
	}
	
	public static void addTextNotification(Context context, String text, int timeout) {
		NotificationType notification = new NotificationType();
		notification.msg = text;

		notificationQueue.add(notification);
		lastNotification = notification;
		processNotificationQueue(context);
	}
	
	public static void addTextNotification_old(Context context, String text, VibratePattern vibratePattern, int timeout) {
		NotificationType notification = new NotificationType();
		notification.bitmap = Protocol.createTextBitmap(context, text);
		notification.timeout = timeout;
		if (vibratePattern == null)
			notification.vibratePattern = new VibratePattern(false, 0, 0, 0);
		else
			notification.vibratePattern	= vibratePattern;
		notificationQueue.add(notification);
		lastNotification = notification;
		processNotificationQueue(context);
	}
	
	public static void addBitmapNotification(Context context, Bitmap bitmap, VibratePattern vibratePattern, int timeout) {
		NotificationType notification = new NotificationType();
		notification.bitmap = bitmap;
		notification.timeout = timeout;		
		if (vibratePattern == null)
			notification.vibratePattern = new VibratePattern(false, 0, 0, 0);
		else
			notification.vibratePattern	= vibratePattern;	
		notificationQueue.add(notification);
		lastNotification = notification;
		processNotificationQueue(context);
	}
	
	public static void addArrayNotification(Context context, int[] array, VibratePattern vibratePattern) {
		NotificationType notification = new NotificationType();
		notification.array = array;
		notification.timeout = notificationTimeout;
		if (vibratePattern == null)
			notification.vibratePattern = new VibratePattern(false, 0, 0, 0);
		else
			notification.vibratePattern	= vibratePattern;
		notificationQueue.add(notification);
		lastNotification = notification;
		processNotificationQueue(context);
	}
	
	public static void addBufferNotification(Context context, byte[] buffer, VibratePattern vibratePattern) {
		NotificationType notification = new NotificationType();
		notification.buffer = buffer;
		notification.timeout = notificationTimeout;
		if (vibratePattern == null)
			notification.vibratePattern = new VibratePattern(false, 0, 0, 0);
		else
			notification.vibratePattern	= vibratePattern;
		notificationQueue.add(notification);
		lastNotification = notification;
		processNotificationQueue(context);
	}
	
	
	public static void addOledNotification(Context context, byte[] top, byte[] bottom, byte[] scroll, int scrollLength, VibratePattern vibratePattern) {
		NotificationType notification = new NotificationType();
		notification.oledTop = top;
		notification.oledBottom = bottom;
		notification.oledScroll = scroll;
		notification.scrollLength = scrollLength;
		notification.timeout = notificationTimeout;
		if (vibratePattern == null)
			notification.vibratePattern = new VibratePattern(false, 0, 0, 0);
		else
			notification.vibratePattern	= vibratePattern;	
		notificationQueue.add(notification);
		lastNotification = notification;
		processNotificationQueue(context);
	}
	

	
	public static void toNotification(Context context) {
		LEDIService.watchState = LEDIService.LEDIStates.NOTIFICATION;
		LEDIService.WatchModes.NOTIFICATION = true;
		
		processNotificationQueue(context);
	}
	
	public static void exitNotification(Context context) {
		// disable notification mode
		LEDIService.WatchModes.NOTIFICATION = false;
		
		if (LEDIService.WatchModes.CALL == true)
			return;
		/*
		else if (LEDIService.WatchModes.APPLICATION == true)
			Application.toApp();
		*/
		else if (LEDIService.WatchModes.IDLE == true)
			Idle.toIdle(context);
	}
	
	public static void replay(Context context) {
		if (lastNotification != null) {
			lastNotification.vibratePattern.vibrate = false;
			notificationQueue.add(lastNotification);
			toNotification(context);		
		}
	}
	
}
