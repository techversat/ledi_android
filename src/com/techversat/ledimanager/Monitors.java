                                                                     
package com.techversat.ledimanager;

import java.net.URL;
import java.util.Hashtable;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
/*
import org.anddev.android.weatherforecast.weather.GoogleWeatherHandler;
import org.anddev.android.weatherforecast.weather.WeatherCurrentCondition;
import org.anddev.android.weatherforecast.weather.WeatherForecastCondition;
import org.anddev.android.weatherforecast.weather.WeatherSet;
import org.anddev.android.weatherforecast.weather.WeatherUtils;
*/
import com.techversat.ledimanager.LEDIService.Preferences;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Monitors {
	
	public static AlarmManager alarmManager;
	public static Intent intent;
	public static PendingIntent sender;
	
	static GmailMonitor gmailMonitor;
	
	private static ContentObserverMessages contentObserverMessages;
	static ContentResolver contentResolverMessages;
	
	private static ContentObserverCalls contentObserverCalls;
	static ContentResolver contentResolverCalls;
	
	//public static int gmailCount = 0;
	static Hashtable<String, Integer> gmailUnreadCounts = new Hashtable<String, Integer>();

	
	public static class WeatherData {
		public static boolean received = false;
		public static String icon;
		public static String tempHigh;
		public static String tempLow;
		public static String temp;
		public static String condition;
		public static String city;
	}
	
	public static void updateGmailUnreadCount_old(String account, int count) {
		gmailUnreadCounts.put(account, count);
	}
	
	public static int getGmailUnreadCount_old() {
		int count = 0;
		for (int i : gmailUnreadCounts.values())
			count += i;
		return count;
	}
	
	
	public static void updateGmailUnreadCount(String account, int count) {
		if (Preferences.logging) Log.d(LEDIActivity.TAG, "Monitors.updateGmailUnreadCount(): account='"
				+ account + "' count='" + count + "'");
		gmailUnreadCounts.put(account, count);
		if (Preferences.logging) Log.d(LEDIActivity.TAG,
				"Monitors.updateGmailUnreadCount(): new unread count is: "
						+ gmailUnreadCounts.get(account));
	}
	
	public static int getGmailUnreadCount() {
		if (Preferences.logging) Log.d(LEDIActivity.TAG, "Monitors.getGmailUnreadCount()");
		int totalCount = 0;
		for (String key : gmailUnreadCounts.keySet()) {
			Integer accountCount = gmailUnreadCounts.get(key);
			totalCount += accountCount.intValue();
			if (Preferences.logging) Log.d(LEDIActivity.TAG, "Monitors.getGmailUnreadCount(): account='"
					+ key + "' accountCount='" + accountCount
					+ "' totalCount='" + totalCount + "'");
		}
		return totalCount;
	}
	
	public static int getGmailUnreadCount(String account) {
		int count = gmailUnreadCounts.get(account);
		if (Preferences.logging) Log.d(LEDIActivity.TAG, "Monitors.getGmailUnreadCount('"+account+"') returning " + count);
		return count;
	}
	
	
	public static void start(Context context, TelephonyManager telephonyManager) {
		// start weather updater
		
		// temporary one time update
		// updateWeatherData(context);
		Log.i(LEDIActivity.TAG, "start Monitor");
		
		CallStateListener phoneListener = new CallStateListener(context);
		
		telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		int phoneEvents = PhoneStateListener.LISTEN_CALL_STATE;
		telephonyManager.listen(phoneListener, phoneEvents);
		
		
		gmailMonitor = Utils.getGmailMonitor(context);
		if (gmailMonitor != null) {
			gmailMonitor.startMonitor();
		}
			
		try {
			contentObserverMessages = new ContentObserverMessages(context);
			Uri uri = Uri.parse("content://mms-sms/conversations/");
			contentResolverMessages = context.getContentResolver();
			contentResolverMessages.registerContentObserver(uri, true, contentObserverMessages);
		} catch (Exception x) {
		}
		
		try {
			contentObserverCalls = new ContentObserverCalls(context);
			//Uri uri = Uri.parse("content://mms-sms/conversations/");
			contentResolverCalls = context.getContentResolver();
			contentResolverCalls.registerContentObserver(android.provider.CallLog.Calls.CONTENT_URI, true, contentObserverCalls);
		} catch (Exception x) {
		}
		
		startAlarmTicker(context);
	}
	
	public static void stop() {
		contentResolverMessages.unregisterContentObserver(contentObserverMessages);
		stopAlarmTicker();		
	}

	/*
	public static void updateWeatherData(Context context) {
		try {
						
			URL url;
			String queryString = "http://www.google.com/ig/api?weather=" + Preferences.weatherCity;
			url = new URL(queryString.replace(" ", "%20"));

			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			
			GoogleWeatherHandler gwh = new GoogleWeatherHandler();
			xr.setContentHandler(gwh);
			
			InputSource src = new InputSource(url.openStream());
			src.setEncoding("ISO-8859-1");
			xr.parse(src);
			
			WeatherSet ws = gwh.getWeatherSet();						
			WeatherCurrentCondition wcc = ws.getWeatherCurrentCondition();
			
			// IndexOutOfBoundsException: Invalid index 0, size is 0
			WeatherForecastCondition wfc = ws.getWeatherForecastConditions().get(0);
			
			WeatherData.city = Preferences.weatherCity;
			
			String cond = wcc.getCondition();
			String temp;
			if (Preferences.weatherCelsius)
			{
				WeatherData.tempHigh = "High " + Integer.toString(wfc.getTempMaxCelsius());
				WeatherData.tempLow = "Low " + Integer.toString(wfc.getTempMinCelsius());
				temp = Integer.toString(wcc.getTempCelcius()) + (char) 0x00B0 + "C";
			}
			else 
			{
				WeatherData.tempHigh = "High " + Integer.toString(WeatherUtils.celsiusToFahrenheit(wfc.getTempMaxCelsius()));
				WeatherData.tempLow = "Low " + Integer.toString(WeatherUtils.celsiusToFahrenheit(wfc.getTempMinCelsius()));
				temp = Integer.toString(wcc.getTempFahrenheit()) + (char) 0x00B0 + "F";
			}
			//String place = gwh.city;
			
			WeatherData.condition = cond;
			WeatherData.temp = temp;
			
			cond = cond.toLowerCase();
			
			if (cond.equals("clear") || cond.equals("mostly sunny") || cond.equals("partly sunny") || cond.equals("sunny"))
				WeatherData.icon = "weather_sunny.bmp";
			else if (cond.equals("cloudy") || cond.equals("mostly cloudy") || cond.equals("overcast") || cond.equals("partly cloudy"))
				WeatherData.icon = "weather_cloudy.bmp";
			else if (cond.equals("light rain") || cond.equals("rain") || cond.equals("rain showers") || cond.equals("showers") || cond.equals("chance of showers") || cond.equals("scattered showers") || cond.equals("freezing rain") || cond.equals("freezing drizzle") || cond.equals("rain and snow"))
				WeatherData.icon = "weather_rain.bmp";	
			else if (cond.equals("thunderstorm") || cond.equals("chance of storm") || cond.equals("isolated thunderstorms"))
				WeatherData.icon = "weather_thunderstorm.bmp";
			else if (cond.equals("chance of snow") || cond.equals("snow showers") || cond.equals("ice/snow") || cond.equals("flurries"))
				WeatherData.icon = "weather_snow.bmp";
			else
				WeatherData.icon = "weather_cloudy.bmp";
			
			WeatherData.received = true;
			
			Idle.updateLcdIdle(context);
			
			} catch (Exception e) {
				Log.e(LEDIActivity.TAG, e.toString());
			} 
	}
	*/
	
	static void startAlarmTicker(Context context) {		
		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra("action_update", "update");
		sender = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 0, 30 * 60 * 1000, sender);		
	}
	
	static void stopAlarmTicker() {
		alarmManager.cancel(sender);
	}
	
	
	private static class ContentObserverMessages extends ContentObserver {

		Context context;
		
		public ContentObserverMessages(Context context) {
			super(null);
			this.context = context;			
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);			
			// change in SMS/MMS database			
			Idle.updateLcdIdle(context);
		}
	}
	
	private static class ContentObserverCalls extends ContentObserver {

		Context context;
		
		public ContentObserverCalls(Context context) {
			super(null);
			this.context = context;			
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);			
			// change in call history database
			Log.d(LEDIActivity.TAG, "call history change");
			Idle.updateLcdIdle(context);
		}
	}

	
}
