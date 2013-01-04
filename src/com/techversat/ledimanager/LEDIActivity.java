package com.techversat.ledimanager;

import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.util.Log;

// import org.techversat.ledimanager.R;
import com.techversat.ledimanager.LEDIService;
import com.techversat.ledimanager.Protocol;
import com.techversat.ledimanager.Settings;
// import org.techversat.ledimanager.Test;
import com.techversat.ledimanager.Utils;
import com.techversat.ledimanager.DeviceSelection;
// import com.techversat.ledimanager.LEDIService.Preferences;
import com.techversat.ledimanager.LEDIService.ConnectionState;
import com.techversat.lediview.VirtualLEDIActivity;


@SuppressLint("DefaultLocale")
public class LEDIActivity extends Activity {

	public static final String TAG = "LEDI";
	public static TextView textView = null;	
	public static ToggleButton toggleButton = null;
	static volatile Context context;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		setContentView(R.layout.activity_main);
		setTitle(getString(R.string.app_name));
		
		textView = (TextView) findViewById(R.id.textView1);	
		
		toggleButton = (ToggleButton) findViewById(R.id.toggleButton1);
		toggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(toggleButton.isChecked())
            		startService();
            	else
            		stopService();            	
            }
        });
		
		Log.i(TAG, "LEDIActivity onCreate");
	}
	
    @Override
    protected void onResume() {
    	super.onResume();	      
        /*	
        textView = (TextView) findViewById(R.id.textView1);	
  		toggleButton = (ToggleButton) findViewById(R.id.toggleButton1);

		toggleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if(toggleButton.isChecked())
            		startService();
            	else
            		stopService();
            }
        });*/
		displayStatus(this);
    }
    
    static void displayStatus(Context context) {
    	setButtonState(context);
    	
    	if(textView==null)
    		return;
    	
    	Resources res = context.getResources();
    	textView.setText("");
		Utils.appendColoredText(textView,
				res.getString(R.string.app_name),
				Color.GRAY);
    	textView.append("\n\n");
    	
    	switch (LEDIService.connectionState) {
	    	case LEDIService.ConnectionState.DISCONNECTED:
	    		Utils.appendColoredText(textView,
	    				res.getString(R.string.connection_disconnected).toUpperCase(),
	    				Color.RED);
	    		break;
	    	case LEDIService.ConnectionState.CONNECTING:
	    		Utils.appendColoredText(textView,
	    				res.getString(R.string.connection_connecting).toUpperCase(), 
	    				Color.GRAY);
	    		break;
	    	case LEDIService.ConnectionState.CONNECTED:
	    		Utils.appendColoredText(textView, 
	    				res.getString(R.string.connection_connected).toUpperCase(), 
	    				Color.GREEN);
	    		break;
	    	case LEDIService.ConnectionState.DISCONNECTING:
	    		Utils.appendColoredText(textView, 
	    				res.getString(R.string.connection_disconnecting).toUpperCase(), 
	    				Color.YELLOW);
	    		break;
    	}
    	textView.append("\n");
    }
    
	private static void setButtonState(Context context) {
		if (toggleButton!=null)
			toggleButton.setChecked(LEDIService.isRunning());
	}

	public void startSearchActivity(View view) {
	    Intent intent = new Intent(this, DeviceSelection.class);
	    startActivity(intent);
	}

	public void startVirtualLEDIActivity(View view) {
		textView = (TextView) findViewById(R.id.textView1);
		if(LEDIService.connectionState != ConnectionState.CONNECTED)
		{
			Utils.appendColoredText(textView,
					"Cannot start virtual LEDI.\nPlease establish connection first",
    				Color.GRAY);
			return;
		}
		Intent intent = new Intent(this, VirtualLEDIActivity.class);
	    startActivity(intent);
	}

	public void setLEDITime(View view)
	{
		textView = (TextView) findViewById(R.id.textView1);
		if(LEDIService.connectionState != ConnectionState.CONNECTED)
		{
			Utils.appendColoredText(textView,
					"Cannot set time.\nPlease establish connection first",
    				Color.GRAY);
			return;
		}
		textView.setText("setting Time");
		Protocol.sendRtcNow(this);
	}
	
	public void sendLEDIText(View view)
	{
		EditText editText = (EditText) findViewById(R.id.editText1);
		String message = editText.getText().toString();
		Protocol.sendText(this, message);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();

		LEDIService.loadPreferences(this);
		
		/*
		if (Preferences.idleMusicControls)
			Protocol.enableMediaButtons();
		//else 
			//Protocol.disableMediaButtons();
		
		if (Preferences.idleReplay)
			Protocol.enableReplayButton();
		//else
			//Protocol.disableReplayButton();
		*/
		
		// Protocol.configureMode();
		Log.i(TAG, "onStart");
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.start:
	    	startService();
	        return true;
	    case R.id.stop:	   
	    	stopService();
	        return true;
	    case R.id.test:
	    	// startActivity(new Intent(this, Test.class));
	        return true;
	    case R.id.settings:	 
	    	startActivity(new Intent(this, Settings.class));
	        return true;  
	    case R.id.about:
	    	showAbout();
	        return true;
	    case R.id.exit:	        
	    	exit();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
    
    
	void startService() {
		Context context = getApplicationContext();
		
		// we want the service to run even when the client unbinds
		context.startService(new Intent(this, LEDIService.class));
		
		// and then we bind the client to the service
		if(!LEDIService.isRunning()) {
			context.bindService(new Intent(LEDIActivity.this, 
					LEDIService.class), LEDIActivity.mConnection, Context.BIND_AUTO_CREATE);
		}
        setButtonState(context);
	}
	
    void stopService() {
		Context context = getApplicationContext();
        try {
        	context.unbindService(LEDIActivity.mConnection);   
        	context.stopService(new Intent(this, LEDIService.class));        	
        }
        catch(Throwable e) {
        	// The service wasn't running
        	// if (Preferences.logging) Log.d(MetaWatch.TAG, e.getMessage());          	
        }
        setButtonState(context);
    }
    
    void exit() {
    	System.exit(0);
    }
    
    void showAbout() {
    	
    	WebView webView = new WebView(this);
		String html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /><title>About</title></head><body>" + 
						"<h1>LEDI</h1>" +
						"<p>Version " + Utils.getVersion(this) + ".</p>" +
						"<p>&copy; Copyright 2012 TechVersat LLC.</p>" +
						"</body></html>";
        webView.loadData(html, "text/html", "utf-8");
        
        new AlertDialog.Builder(this).setView(webView).setCancelable(true).setPositiveButton("OK", new DialogInterface.OnClickListener() {			
			//@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).show();        
    }
    
    
    /**
     * Handler of incoming messages from service.
     */
    static class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Log.i(TAG,"received msg from LEDIService");
            switch (msg.what) {
                case LEDIService.Msg.UPDATE_STATUS:
                    LEDIActivity.displayStatus(context);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    public static Messenger mService = null;  
    final static IncomingHandler mIncomingHandler = new IncomingHandler();
    final static Messenger mMessenger = new Messenger(mIncomingHandler);

    /**
     * Class for interacting with the main interface of the service.
     */
    public static ServiceConnection mConnection = new ServiceConnection() {
    	   	
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);

            Log.i(TAG, "connecting to service");
            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                        LEDIService.Msg.REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };
}
