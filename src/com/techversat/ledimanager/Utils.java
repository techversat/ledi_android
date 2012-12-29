                                                                     
                                                                     
                                                                     
                                             
 /*****************************************************************************
  *  Copyright (c) 2011 Meta Watch Ltd.                                       *
  *  www.MetaWatch.org                                                        *
  *                                                                           *
  =============================================================================
  *                                                                           *
  *  Licensed under the Apache License, Version 2.0 (the "License");          *
  *  you may not use this file except in compliance with the License.         *
  *  You may obtain a copy of the License at                                  *
  *                                                                           *
  *    http://www.apache.org/licenses/LICENSE-2.0                             *
  *                                                                           *
  *  Unless required by applicable law or agreed to in writing, software      *
  *  distributed under the License is distributed on an "AS IS" BASIS,        *
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
  *  See the License for the specific language governing permissions and      *
  *  limitations under the License.                                           *
  *                                                                           *
  *****************************************************************************/

 /*****************************************************************************
  * Utils.java                                                                *
  * Utils                                                                     *
  * Different utils                                                           *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package com.techversat.ledimanager;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract.PhoneLookup;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

public class Utils {

	public static String getContactNameFromNumber(Context context, String number) {
		
		if (number.equals(""))
			return "Private number";

		String[] projection = new String[] { PhoneLookup.DISPLAY_NAME, PhoneLookup.NUMBER };
		Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
		Cursor c = context.getContentResolver().query(contactUri, projection, null, null, null);
		
		if (c.moveToFirst()) {
			String name = c.getString(c.getColumnIndex(PhoneLookup.DISPLAY_NAME));

			if (name.length() > 0)
				return name;
			else
				return number;
		}
		
		return number;		 
	}
	
	public static int getUnreadSmsCount(Context context) {

		int count = 0;

		Cursor cursor = context.getContentResolver().query(
				Uri.withAppendedPath(Uri.parse("content://sms"), "inbox"), 
				new String[] { "_id" }, 
				"read=0", 
				null, 
				null
			);
		
		if (cursor != null) {
			try {
				count = cursor.getCount();
			} finally {
				cursor.close();
			}
		}
		return count;
	}
	
	public static int getMissedCallsCount(Context context) {
		int missed = 0;
		try {
			Cursor cursor = context.getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI, null, null, null, null);
			cursor.moveToFirst();

			while (true) {
				if (cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE)) == 3)
					missed += cursor.getInt(cursor.getColumnIndex(CallLog.Calls.NEW));

				if (cursor.isLast())
					break;

				cursor.moveToNext();
			}

		} catch (Exception x) {
		}
		return missed;
	}
	
	public static int getUnreadGmailCount(Context context, String account, String label) {
		try {
			int nameColumn = 0;

			Cursor c = context.getContentResolver().query(Uri.parse("content://gmail-ls/labels/" + account), null, null, null, null);
			c.moveToFirst();

			for (int i = 0; i < c.getColumnCount(); i++)
				if (c.getColumnName(i).equals("canonicalName")) {
					nameColumn = i;
					break;
				}

			while (true) {
				if (c.getString(nameColumn).equals(label))
					for (int i = 0; i < c.getColumnCount(); i++) {
						if (c.getColumnName(i).equals("numUnreadConversations")) {
							return Integer.parseInt(c.getString(i));
						}
					}

				c.moveToNext();

				if (c.isLast()) {
					break;
				}
			}
		} catch (Exception x) {
			Log.d(LEDIActivity.TAG, x.toString());
		}

		return 0;
	}
	
	public static Bitmap loadBitmapFromAssets(Context context, String path) {
		try {
			InputStream inputStream = context.getAssets().open(path);
	        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
	        inputStream.close();
	        //Log.d(MetaWatch.TAG, "ok");
	        return bitmap;
		} catch (IOException e) {
			//Log.d(MetaWatch.TAG, e.toString());
			return null;
		}
	}
	/*
	public static Bitmap loadBitmapFromPath(Context context, String path) {
			return BitmapFactory.decodeFile(path);
	}
	*/
	
	public static String getVersion(Context context) {		
		try {
			PackageManager packageManager = context.getPackageManager();
			PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionName;
		} catch (NameNotFoundException e) {
		}
		return "unknown";
	}
	
	
	public static void appendColoredText(TextView tv, String text, int color) {
    	int start = tv.getText().length();
    	tv.append(text);
    	int end = tv.getText().length();
    	
    	Spannable spannableText = (Spannable) tv.getText();
    	spannableText.setSpan(new ForegroundColorSpan(color), start, end, 0);
    }
	
	public static boolean isGmailAccessSupported(Context context) {
		return (
			GmailAPIMonitor.isSupported(context) ||
			GmailLSMonitor.isSupported(context)
		);
	}
	
	private static GmailMonitor gmailMonitor = null;
	public static GmailMonitor getGmailMonitor(Context context) {
		if (gmailMonitor == null) {
			try {
				if (GmailAPIMonitor.isSupported(context)) {
					Log.i(LEDIActivity.TAG, "returning GmailAPIMonitor");
					gmailMonitor = new GmailAPIMonitor(context);
				} else if (GmailLSMonitor.isSupported(context)) {
					Log.i(LEDIActivity.TAG, "returning GmailLSMonitor");
					gmailMonitor = new GmailLSMonitor(context);
				}
			} catch (Exception e) {
				gmailMonitor = null;
			}
		}
		
		return gmailMonitor;
	}


	public static int getUnreadGmailCount(Context context) {
		GmailMonitor monitor = getGmailMonitor(context);
		if (monitor != null) {
			return monitor.getUnreadCount();
		}
		
		// Fallback to our own counter (based on notifications).
		return Monitors.getGmailUnreadCount();
	}
	
	public static String getGoogleAccountName(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccounts();

		for (Account account : accounts) {
			if (account.type.equals("com.google")) {
				return account.name;
			}
		}
		return null;
	}
	
	/**
	 * Return a lists of configured Gmail account, rather than the first one
	 * 
	 * @param context
	 * @return List<String> the list of the account names
	 */
	public static List<String> getGoogleAccountsNames(Context context) {
		AccountManager accountManager = AccountManager.get(context);
		Account[] accounts = accountManager.getAccounts();
		
		final List<String> accNames = new ArrayList<String>();
		
		for (Account account : accounts) {
			if (account.type.equals("com.google")) {
				accNames.add(account.name);
			}
		}
		
		return accNames;
	}
	
	
	public static class CursorHandler {
		private List<Cursor> cursors = new ArrayList<Cursor>();
		
		public Cursor add(Cursor c) {
			if (c!=null)
				cursors.add(c);
			return c;
		}
		
		public void closeAll() {
			for(Cursor c : cursors) {
				if(!c.isClosed())
					c.close();
			}
		}
	}
	

}
