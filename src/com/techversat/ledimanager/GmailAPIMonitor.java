
package com.techversat.ledimanager;

import java.util.ArrayList;
import java.util.List;

import com.techversat.ledimanager.LEDIService.Preferences;

import com.google.android.gm.contentprovider.GmailContract;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class GmailAPIMonitor implements GmailMonitor {
	public static boolean isSupported(Context context) {
		return GmailContract.canReadLabels(context);
	}

	/**
	 * Describe a Gmail account. Used to store how many unread messages are in which account
	 * 
	 * @author tschork
	 * 
	 */
	public class GmailAccount {
		public int		unreadCount	= 0;
		public String	accountName	= null;
		public Uri		uri			= null;
		
		public GmailAccount(String pAccountName) {
			accountName = pAccountName;
		}
		
		public GmailAccount(String pAccountName, int pUnreadCount) {
			accountName = pAccountName;
			unreadCount = pUnreadCount;
		}
		
		public GmailAccount(String pAccountName, int pUnreadCount, Uri pUri) {
			accountName = pAccountName;
			unreadCount = pUnreadCount;
			uri = pUri;
		}
		
		public GmailAccount(int pUnreadCount) {
			unreadCount = pUnreadCount;
		}
		
		public GmailAccount(Uri pUri) {
			uri = pUri;
		}
	}
	
	Context context;
	
	MyContentObserver contentObserver = new MyContentObserver();
	
	public static List<GmailAccount> ListAccounts=new ArrayList<GmailAccount>();
	public static int lastUnreadCount = 0;
	String account = null;
	
	public GmailAPIMonitor(Context ctx) {
		super();		
		context = ctx;
		Utils.CursorHandler ch = new Utils.CursorHandler();
		try {
			final List<String> accounts = Utils.getGoogleAccountsNames(ctx);
			if (accounts.size() == 0) {
				throw new IllegalArgumentException("No account found.");
			}
			
			for (String account : accounts) {
				// find labels for the account.
				Cursor c = ch.add(context.getContentResolver().query(GmailContract.Labels.getLabelsUri(account), null, null, null, null));
				// loop through the cursor and find the Inbox.
				if (c != null) {
					// Technically, you can choose any label here, including priority inbox and all mail.
					// Make a setting for it later?
					final String inboxCanonicalName = GmailContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX;
					final int canonicalNameIndex = c.getColumnIndexOrThrow(GmailContract.Labels.CANONICAL_NAME);
					while (c.moveToNext()) {
						if (inboxCanonicalName.equals(c.getString(canonicalNameIndex))) {
							Uri uri = Uri.parse(c.getString(c.getColumnIndexOrThrow(GmailContract.Labels.URI)));
							ListAccounts.add(new GmailAccount(account, 0, uri));
						}
					}
				}
				if (ListAccounts.size() == 0) {
					throw new IllegalArgumentException("Label not found.");
				}
				
				lastUnreadCount = getUnreadCount();
			}
		} catch (Exception e) {
			// handle exception
			Log.e(LEDIActivity.TAG, e.getMessage());
		}
		finally {
			ch.closeAll();
		}
	}

	public void startMonitor() {
		for (GmailAccount objAccnt : ListAccounts) {
			try {
				context.getContentResolver().registerContentObserver(objAccnt.uri, true, contentObserver);
			} catch (Exception x) {
				if (Preferences.logging)
					Log.e(LEDIActivity.TAG, x.toString());
			}
		}
	}


	private class MyContentObserver extends ContentObserver {
		public MyContentObserver() {
			super(null);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			if (Preferences.logging) Log.d("ow", "onChange observer - unread");

			int currentUnreadCount = getUnreadCount();

			//if (Preferences.logging) Log.d("ow", "current gmail unread count: " + Integer.toString(currentGmailUnreadCount));

			if (Preferences.notifyGmail && currentUnreadCount > lastUnreadCount)
			{
				if (Preferences.logging) Log.d("ow", Integer.toString(currentUnreadCount) + " > " + Integer.toString(lastUnreadCount));

				// TODO: when the length of the recipients is too big, split the notification on 2 screens?
				String recipient = "";
				for (GmailAccount objGmailUnread : ListAccounts) {
					StringBuilder sb = new StringBuilder();
					if (objGmailUnread.unreadCount > 0) {
						if (!recipient.equals("")) {
							sb.append(recipient).append(", ");
						}
						recipient = sb.append(objGmailUnread.accountName).append(" (").append(objGmailUnread.unreadCount).append(")").toString();
						
					}
				}
				NotificationBuilder.createGmailBlank(context, recipient, currentUnreadCount);
			}
//			
//			if (currentUnreadCount != lastUnreadCount)
//			{
//				Idle.updateIdle(context, true);
//			}
			
			lastUnreadCount = currentUnreadCount;
		}
	}
	
	/***
	 * Returns how many unread Gmail messages are in a given account
	 * 
	 * @param accountName
	 *            The account name
	 * @return integer The number of unread messages
	 */
	private int getUnreadCount(String accountName) {
		int unreadCnt = 0;
		Utils.CursorHandler ch = new Utils.CursorHandler();
		try {
			Cursor c = ch.add(context.getContentResolver().query(GmailContract.Labels.getLabelsUri(accountName), null, null, null, null));
			c.moveToFirst();
			unreadCnt += c.getInt(c.getColumnIndexOrThrow(GmailContract.Labels.NUM_UNREAD_CONVERSATIONS));
		} catch (Exception x) {
			if (Preferences.logging)
				Log.d(LEDIActivity.TAG, "GmailAPIMonitor.getUnreadCount(): caught exception: " + x.toString());
		} finally {
			ch.closeAll();
		}
		return unreadCnt;
	}
	
	/**
	 * Updates unreadList to record how many unread Gmail messages are in which account(s)
	 * 
	 * @param account
	 *            The account name
	 * @param unreadMsg
	 *            The number of unread messages
	 */
	private void saveUnreadCount(String pAccount, int unreadMsg) {
		for (GmailAccount objGmailUnread : ListAccounts) {
			if (objGmailUnread.accountName.contentEquals(pAccount)) {
				objGmailUnread.unreadCount = unreadMsg;
				return;
			}
		}
	}
	
	public int getUnreadCount() {
		int unreadCnt = 0;
		int accUnread = 0;
		try {
			for (String account : Utils.getGoogleAccountsNames(context)) {
				accUnread = getUnreadCount(account);
				saveUnreadCount(account, accUnread);
				unreadCnt += accUnread;
			}
		} catch (Exception x) {
			if (Preferences.logging)
				Log.d(LEDIActivity.TAG, "GmailAPIMonitor.getUnreadCount(): caught exception: " + x.toString());
		}
		
		if (Preferences.logging)
			Log.d(LEDIActivity.TAG, "GmailAPIMonitor.getUnreadCount(): found " + unreadCnt + " unread messages");
		return unreadCnt;
	}
}
