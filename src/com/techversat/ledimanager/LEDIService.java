                                                                     
                                                                   
package com.techversat.ledimanager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.UUID;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class LEDIService extends Service {

	static volatile Context context;
	private static Service instance = null;
	public static BluetoothAdapter bluetoothAdapter;
	//BluetoothServerSocket bluetoothServerSocket;
	BluetoothSocket bluetoothSocket;
	static InputStream inputStream;
	static OutputStream outputStream;

	private boolean lastConnectionState = false;
	TelephonyManager telephonyManager;
	AudioManager audioManager;
	NotificationManager notificationManager;
	RemoteViews remoteViews;
	android.app.Notification notification;
	
	public static PowerManager powerManger;
	public static PowerManager.WakeLock wakeLock;
	
	static int connectionState;
	public static int watchState;
	
	public static TestSmsLoop testSmsLoop;
	static ServiceThread serviceThread;
		
	final class ConnectionState {
		static final int DISCONNECTED = 0;
		static final int CONNECTING = 1;
		static final int CONNECTED = 2;
		static final int DISCONNECTING = 3;
	}
	
	final class WatchBuffers {
		static final int IDLE = 0;
		static final int APPLICATION = 1;
		static final int NOTIFICATION = 2;
	}
	
	final class LEDIStates {
		static final int OFF = 0;
		static final int IDLE = 1;
		static final int APPLICATION = 2;
		static final int NOTIFICATION = 3;
		static final int CALL = 3;		
	}
	
	static class WatchModes {
		public static boolean IDLE = false;
		public static boolean APPLICATION = false;
		public static boolean NOTIFICATION = false;
		public static boolean CALL = false;
	}
	
	static class Preferences {
		public static boolean startOnBoot = false;
		public static boolean notifyCall = true;
		public static boolean notifySMS = true;
		public static boolean notifyGmail = true;
		public static boolean notifyK9 = true;
		public static boolean notifyAlarm = true;
		public static boolean notifyMusic = true;
		public static String watchMacAddress = "";
		public static int packetWait = 5;
		public static boolean skipSDP = false;
		public static boolean invertLCD = false;
		public static String weatherCity = "Dallas,US";
		public static boolean weatherCelsius = false;
		public static int fontSize = 2;
		public static int smsLoopInterval = 15;
		public static boolean idleMusicControls = false;
		public static boolean idleReplay = false;
		public static boolean insecureBtSocket = false;
		public static boolean logging = true;
	}
	
	final static class Msg {
		static final int REGISTER_CLIENT = 0;
		static final int UNREGISTER_CLIENT = 1;
		static final int UPDATE_STATUS = 2;
		static final int SEND_TOAST = 3;
		static final int DISCONNECT = 4;
	}
	


	public static void loadPreferences(Context context) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
			
		Preferences.startOnBoot = sharedPreferences.getBoolean("StartOnBoot", Preferences.startOnBoot);
		Preferences.notifyCall = sharedPreferences.getBoolean("NotifyCall", Preferences.notifyCall);
		Preferences.notifySMS = sharedPreferences.getBoolean("NotifySMS", Preferences.notifySMS);
		Preferences.notifyGmail = sharedPreferences.getBoolean("NotifyGmail", Preferences.notifyGmail);
		Preferences.notifyK9 = sharedPreferences.getBoolean("NotifyK9", Preferences.notifyK9);
		Preferences.notifyAlarm = sharedPreferences.getBoolean("NotifyAlarm", Preferences.notifyAlarm);
		Preferences.notifyMusic = sharedPreferences.getBoolean("NotifyMusic", Preferences.notifyMusic);
		Preferences.watchMacAddress = sharedPreferences.getString("MAC", Preferences.watchMacAddress);		
		Preferences.skipSDP = sharedPreferences.getBoolean("SkipSDP", Preferences.skipSDP);
		Preferences.invertLCD = sharedPreferences.getBoolean("InvertLCD", Preferences.invertLCD);
		Preferences.weatherCity = sharedPreferences.getString("WeatherCity", Preferences.weatherCity);
		Preferences.weatherCelsius = sharedPreferences.getBoolean("WeatherCelsius", Preferences.weatherCelsius);
		Preferences.idleMusicControls = sharedPreferences.getBoolean("IdleMusicControls", Preferences.idleMusicControls);
		Preferences.idleReplay = sharedPreferences.getBoolean("IdleReplay", Preferences.idleReplay);
		Preferences.insecureBtSocket = sharedPreferences.getBoolean("InsecureBtSocket", Preferences.insecureBtSocket);
		try {
			Preferences.fontSize = Integer.valueOf(sharedPreferences.getString("FontSize", Integer.toString(Preferences.fontSize)));
			Preferences.packetWait = Integer.valueOf(sharedPreferences.getString("PacketWait", Integer.toString(Preferences.packetWait)));
			Preferences.smsLoopInterval = Integer.valueOf(sharedPreferences.getString("SmsLoopInterval", Integer.toString(Preferences.smsLoopInterval)));
		} catch (NumberFormatException e) {			
		}
		
	}
	
	public static void saveMac(Context context, String mac) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Editor editor = sharedPreferences.edit();
		
		editor.putString("MAC", mac);
		editor.commit();
	}
	

	private PendingIntent createNotificationPendingIntent() {
		return PendingIntent.getActivity(this, 0, new Intent(this,
				LEDIActivity.class), 0);
	}

	public void createNotification() {
		notification = new android.app.Notification(R.drawable.notification_disconnected, null, System.currentTimeMillis());
		notification.flags |= android.app.Notification.FLAG_ONGOING_EVENT;

		remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
		remoteViews.setImageViewResource(R.id.image, R.drawable.notification_disconnected);
		remoteViews.setTextViewText(R.id.text, "LEDIActivity service is running");
		notification.contentView = remoteViews;

		Intent notificationIntent = new Intent(this, LEDIActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		notification.contentIntent = contentIntent;

		startForeground(1, notification);
	}
	
	public void updateNotification() {
		if (connectionState == ConnectionState.CONNECTED) {
			notification.icon = R.drawable.notification_connected;
			remoteViews.setImageViewResource(R.id.image, R.drawable.notification_connected);
			broadcastConnection(true);
		} else {
			notification.icon = R.drawable.notification_disconnected;
			remoteViews.setImageViewResource(R.id.image, R.drawable.notification_disconnected);
			broadcastConnection(false);
		}
		startForeground(1, notification);
		notifyClients();
	}
	
	
	public void removeNotification() {
		stopForeground(true);
	}
	
	public static boolean isRunning() {
		return instance != null;
	}
	
	/*
	 * Service life-cycle callback routines
	 */

	@Override
	public void onCreate() {
		super.onCreate();
		context = this;
		instance = this;
		initialize();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
	    //handleCommand(intent);
	    // We want this service to continue running until it is explicitly
	    // stopped, so return sticky.
		if (connectionState == ConnectionState.DISCONNECTED)
			initialize();
		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
	    return START_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		Toast.makeText(this, "service binding", Toast.LENGTH_SHORT).show();
		return mMessenger.getBinder();
	}
	
	private void initialize() {
		createNotification();
		
		connectionState = ConnectionState.CONNECTING;
		watchState = LEDIStates.OFF;
		
		if (bluetoothAdapter == null)
			bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				
		powerManger = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManger.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LEDIActivity");
		
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		Monitors.start(this, telephonyManager);				
		
		start();	
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Toast.makeText(this, "client unbinding", Toast.LENGTH_SHORT).show();
		return false; // if returns true, it will call onRebind()
	}

	@Override
	public void onRebind(Intent intent) {	
	}
	
	@Override
	public void onDestroy() {
		Toast.makeText(this, "destroying service", Toast.LENGTH_SHORT).show();
		disconnectExit();			
		super.onDestroy();
		serviceThread.quit();
		instance=null;
//		if (Preferences.logging) Log.d(MetaWatch.TAG,
//				"MetaWatchService.onDestroy()");
//		PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(prefChangeListener);	
		Monitors.stop();
		removeNotification();
		notifyClients();
		mClients.clear();
	}
	
	void connect(Context context) {
		try {
			
			Log.d(LEDIActivity.TAG, "Remote device address: " + Preferences.watchMacAddress);
			if (Preferences.watchMacAddress.equals(""))
				loadPreferences(context);
			BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(Preferences.watchMacAddress);
			
			/*
			Log.d(LEDIActivity.TAG, "remote device name: " + bluetoothDevice.getName());
			int bondState = bluetoothDevice.getBondState();
			String bond = "";
			switch (bondState) {
				case BluetoothDevice.BOND_BONDED:
					bond = "bonded";
					break;
				case BluetoothDevice.BOND_BONDING:
					bond = "bonding";
					break;
				case BluetoothDevice.BOND_NONE:
					bond = "none";
					break;					
			}
			Log.d(LEDIActivity.TAG, "bond state: " + bond);
			*/
			int currentapiVersion = android.os.Build.VERSION.SDK_INT;			
			if (Preferences.skipSDP) {
//				Method method = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
//				bluetoothSocket = (BluetoothSocket) method.invoke(bluetoothDevice, 1);
			    Method method;
			    if (Preferences.insecureBtSocket && currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
			    	method = bluetoothDevice.getClass().getMethod("createInsecureRfcommSocket", new Class[] { int.class });
			    } else {
			    	method = bluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
			    }
			    bluetoothSocket = (BluetoothSocket) method.invoke(bluetoothDevice, 1);
			} else {
				UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
//				bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
				if (Preferences.insecureBtSocket && currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
					bluetoothSocket = bluetoothDevice
							.createInsecureRfcommSocketToServiceRecord(uuid);
				} else {
					bluetoothSocket = bluetoothDevice
							.createRfcommSocketToServiceRecord(uuid);
				}				
			}
			
			//Log.d(LEDIActivity.TAG, "got Bluetooth socket");
			//if (bluetoothSocket == null)
				//Log.d(LEDIActivity.TAG, "Bluetooth socket is null");
			
			bluetoothSocket.connect();
			inputStream = bluetoothSocket.getInputStream();
			outputStream = bluetoothSocket.getOutputStream();
			
			connectionState = ConnectionState.CONNECTED;		
			updateNotification();
			
			Protocol.sendRtcNow(context);			
		
		} catch (IOException ioexception) {
			Log.d(LEDIActivity.TAG, ioexception.toString());
			sendToast(ioexception.toString());
		} catch (SecurityException e) {
			Log.d(LEDIActivity.TAG, e.toString());
		} catch (NoSuchMethodException e) {
			Log.d(LEDIActivity.TAG, e.toString());
		} catch (IllegalArgumentException e) {
			Log.d(LEDIActivity.TAG, e.toString());
		} catch (IllegalAccessException e) {
			Log.d(LEDIActivity.TAG, e.toString());
		} catch (InvocationTargetException e) {
			Log.d(LEDIActivity.TAG, e.toString());
		}	
	}
	
	public void sendToast(String text) {
		Message m = new Message();
		m.what = 1;
		m.obj = text;
		messageHandler.sendMessage(m);
	}
	

    /** Keeps track of all current registered clients. */
    static ArrayList<Messenger> mClients = new ArrayList<Messenger>();
  
	private static Handler messageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			// if (Preferences.logging) Log.d(LEDIActivity.TAG, "handleMessage "+msg);
			switch (msg.what) {
            case Msg.REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case Msg.UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case Msg.SEND_TOAST:
            	Toast.makeText(context, 
            			(CharSequence) msg.obj,
            			Toast.LENGTH_SHORT).show();
                break;
            default:
                super.handleMessage(msg);
			}
		}

	};

	
	/**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final static Messenger mMessenger = new Messenger(messageHandler);

    public static void notifyClients() {
    	for (int i=mClients.size()-1; i>=0; i--) {
            try {
                mClients.get(i).send(Message.obtain(null,
                        Msg.UPDATE_STATUS, 0, 0));
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                mClients.remove(i);
            } catch (NullPointerException e) {
                mClients.remove(i);
            }
        }
    }
	
	
	void disconnect() {
		try {
			if (outputStream != null)
				outputStream.close();
		} catch (IOException e) {
		}
		try {
			if (inputStream != null)
			inputStream.close();
		} catch (IOException e) {
		}
		try {
			if (bluetoothSocket != null)
				bluetoothSocket.close();
		} catch (IOException e) {
		}		
	}
	
	void disconnectExit() {
		connectionState = ConnectionState.DISCONNECTING;		
		updateNotification();
		disconnect();
	}
	
	public static void nap(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}
	
	private class ServiceThread extends Thread
	{			
		private Handler handler;
		private Looper looper;
		
		public ServiceThread(String name) {
			super(name);
		}
		@Override
		public void run() {
			
			try {						
				Looper.prepare();
				looper = Looper.myLooper();
				handler = new Handler();
				
				Runnable ProcessState = new Runnable() {
						public void run() { 
							int delay = processState();
							if(delay >= 0) {
								handler.postDelayed(this, delay);
							} else {
								connectionState = ConnectionState.DISCONNECTED;
								updateNotification();
								handler.removeCallbacks(this);
								looper.quit();
							}
						}
				};
					
				handler.post(ProcessState);
				Looper.loop();			
			} 
			catch(Throwable T)
			{
				// if (Preferences.logging) Log.d(LEDIActivity.TAG, "serviceThread: " + T.getMessage());	
			}
			finally
			{				
				connectionState = ConnectionState.DISCONNECTED;
				updateNotification();		
				if (instance != null) {
					instance.stopSelf();
				}
			}
		}
		
		public void quit() {
			if (looper != null)
				looper.quit();
		}		
	}	
	
	int processState()
	{
		int result = 0;
		switch (connectionState) {
		case ConnectionState.DISCONNECTED:
			// if (Preferences.logging) Log.d(LEDIActivity.TAG, "state: disconnected");
			break;
		case ConnectionState.CONNECTING:
			// if (Preferences.logging) Log.d(LEDIActivity.TAG, "state: connecting");
			// create initial connection or reconnect
			updateNotification();					
			connect(LEDIService.this);
//			if(powerManager.isScreenOn()) {
//				result = 10000; //try to reconnect in 10s 
//			} else {
//				result = 30000; //try to reconnect in 30s				
//			}
			result = 10000;
			break;
		case ConnectionState.CONNECTED:
			// if (Preferences.logging) Log.d(LEDIActivity.TAG, "state: connected");
			// read from input stream
			readFromDevice();
			// result = 10000; // 10 seconds
			break;
		case ConnectionState.DISCONNECTING:
			// if (Preferences.logging) Log.d(LEDIActivity.TAG, "state: disconnecting");
			// exit			
			result = -1;
			break;
		}	
		
		return result;
	}
	
	void start() {
		serviceThread = new ServiceThread("LEDI Service Thread"); 		
		serviceThread.start();
	}
	
	void start_old() {
		Thread thread = new Thread() {
			public void run() {
				boolean run = true;
				Looper.prepare();				

				while (run) {
					switch (connectionState) {
					case ConnectionState.DISCONNECTED:
						Log.d(LEDIActivity.TAG, "state: disconnected");
						break;
					case ConnectionState.CONNECTING:
						Log.d(LEDIActivity.TAG, "state: connecting");
						// create initial connection or reconnect
						updateNotification();
						connect(context);
						nap(2000);
						break;
					case ConnectionState.CONNECTED:
						Log.d(LEDIActivity.TAG, "state: connected");
						// read from input stream
						readFromDevice();
						break;
					case ConnectionState.DISCONNECTING:
						Log.d(LEDIActivity.TAG, "state: disconnecting");
						// exit
						run = false;
						break;
					}
				}
			}
		};
		thread.start();
	}
	
	void readFromDevice() {
		try {
			byte[] bytes = new byte[16]; // small number of bytes
			Log.d(LEDIActivity.TAG, "before blocking read");
			inputStream.read(bytes);
			wakeLock.acquire(5000);
			
			// let's see what we receive
			String str = "received: ";
			for (int i = 0; i < 4; i ++) {
				str += (char) bytes[i] + " ";
				// str+= Byte.toString(bytes[i]) + ", ";
				// str+= "0x" + Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1) + ", ";
			}
			Log.d(LEDIActivity.TAG, str);
			/*
			if (bytes[2] == 0x34) { // button press
				Log.d(LEDIActivity.TAG, "button event");
				pressedButton(bytes[3]);
			}
			*/	
		} catch (IOException e) {
			wakeLock.acquire(5000);
			if (connectionState != ConnectionState.DISCONNECTING)
				connectionState = ConnectionState.CONNECTING;			
		}
		
	}
	
	void pressedButton(byte button) {
		Log.d(LEDIActivity.TAG, "button code: " + Byte.toString(button));
		/*
		switch (watchState) {
			case LEDIStates.IDLE: {
				switch (button) { 
					case MediaControl.VOLUME_UP:
						MediaControl.volumeUp(audioManager);
						break;
					case MediaControl.VOLUME_DOWN:
						MediaControl.volumeDown(audioManager);
						break;
					case MediaControl.NEXT:
						MediaControl.next(this);
						break;
					case MediaControl.PREVIOUS:
						MediaControl.previous(this);
						break;
					case MediaControl.TOGGLE:
						MediaControl.togglePause(this);
						break;
					case Protocol.REPLAY:
						Notification.replay(this);
						break;
				}
				
				if (Idle.isIdleButtonOverriden(button)) {
						Log.d(LEDIActivity.TAG, "this button is overriden");
						broadcastButton(button, watchState);
				}
								
			}
				break;
			case LEDIStates.APPLICATION:
				broadcastButton(button, watchState);
				break;
			case LEDIStates.NOTIFICATION:				
				break;
		}
		*/
		
	}
	
	
	void broadcastConnection(boolean connected) {
		if (connected != lastConnectionState) {
			lastConnectionState = connected;
			Intent intent = new Intent(
					"com.techversat.ledimanager.CONNECTION_CHANGE");
			intent.putExtra("state", connected);
			sendBroadcast(intent);
			notifyClients();
			// if (Preferences.logging) Log.d(LEDIActivity.TAG,
			// 		"LEDIActivityService.broadcastConnection(): Broadcast connection change: state='"
			//				+ connected + "'");
			// Protocol.resetLCDDiffBuffer();
		}
	}
	

}
