package com.aggienerds.beeper;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

public class MessageBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		String type = intent.getType();
		
		if (action == null) { action = new String(); }
		if (type == null) { type = new String(); }
		
		// We got an MMS or SMS
		if ((action.contains("WAP_PUSH_RECEIVED")
				&& type.contains("application/vnd.wap.mms-message"))
				|| action.contains("SMS_RECEIVED"))
		{
			SharedPreferences prefs = context.getSharedPreferences("com.aggienerds.beeper_preferences", 0);
			
			if (prefs.getBoolean("pref_oncall", false))
			{
				Log.d("MessageBroadcastReceiver", "Got a message event!");
				Message message = getMessage(context);
				if (message != null)
				{
					if (doesMessageMatch(prefs, message))
					{
						broadcastMessage(context, prefs, message);
					}
				}
				else
				{
					Log.d("MessageBroadcastReceiver", "Told we got a message, but couldn't find it.");
				}
			}
		}
	}
	
	private boolean doesMessageMatch(SharedPreferences prefs, Message message)
	{
		String matchText = prefs.getString("pref_match", "");
		
		String comparison = message.getAddress() + " " + message.getSubject() + " " + message.getBody();
		
		if (prefs.getBoolean("pref_regex", false) != true)
		{
			// Nothing provided. Match everything.
			if (comparison == "") { return true; }
			if (comparison.contains(matchText)) { return true; }
		}
		else
		{
			// Run it through the regex
			if (comparison.matches("^.*" + matchText + ".*$")) { return true; }
		}
		
		return false;
	}
	
	private void broadcastMessage(Context context, SharedPreferences preferences, Message message)
	{
		Log.d("MessageBroadcastRecieiver", "Setting up notification message");
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		SharedPreferences prefs = preferences;
		
		// TODO: Make notification ids unique?
		Log.d("MessageBroadcastRecieiver", "Trying to broadcast out!");
		//nm.notify(0, n);
		
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(android.R.drawable.stat_notify_error, "", System.currentTimeMillis());
        
		String alertSound = prefs.getString("pref_alertsound", "");
		if ((alertSound != null) && (alertSound.length() > 0))
		{
			notification.sound = Uri.parse(alertSound);
		}
        
        Intent messagingIntent = new Intent(Intent.ACTION_VIEW); 
        messagingIntent.setData(Uri.parse("content://mms-sms/conversations/" + message.getThread_id()));  


        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, messagingIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
        
        boolean haveSubject = false;
        if ((message.getSubject() != null) && (message.getSubject().length() > 0))
        {
        	haveSubject = true;
        }
        
        boolean haveBody = false;
        if ((message.getBody() != null) && (message.getBody().length() > 0))
        {
        	haveBody = true;
        }
        
        String subject = "";
        String body = "";
        
        if (haveSubject && haveBody)
        {
        	subject = message.getSubject();
        	body = message.getBody();
        }
        else if (haveSubject && !haveBody)
        {
        	subject = message.getAddress();
        	body = message.getSubject();
        }
        else if (haveBody && !haveSubject)
        {
        	subject = message.getAddress();
        	body = message.getBody();
        }
        else
        {
        	subject = "Incoming Page";
        	body = message.getAddress();
        }
        

        notification.setLatestEventInfo(context, subject, body, contentIntent);
        
        // Have it cancel when it's pressed, but also loop audio, etc. until it's handled.
        notification.flags |= (Notification.FLAG_AUTO_CANCEL | Notification.FLAG_INSISTENT);
        
        // Vibrate, overriding default settings
        if (prefs.getBoolean("pref_vibrate", false))
        {
            if (prefs.getBoolean("vibrate", true)){
                notification.vibrate = new long[] {0, 800, 500, 800};
            }
        }
        
        if (prefs.getBoolean("pref_alarmvol", false))
        {
        	notification.audioStreamType = AudioManager.STREAM_ALARM;
        }
        
        nm.notify(0, notification);

	}
	
	/**
	 * Make the best guess at the newest message shown
	 * @param context
	 * @return
	 */
	private Message getMessage(Context context)
	{
		MessageRetriever mr = new MessageRetriever(context);
		
		List<Message> messages = null;
		Message toReturn = null;
		
		// TODO: Better algorithm than that
		// Perhaps look at the time on the message returned?
		// Can we get the sender address from the notification?
		
		// Try to find messages
		long startTime = System.currentTimeMillis();
		while((messages == null) || (messages.size() == 0))
		{
			// Don't loop for longer than a minute
			long curTime = System.currentTimeMillis();
			if (((curTime - startTime)/1000) > 60)
			{
				break;
			}
			
			messages = mr.getConversationsLast(true);
			
			for (Message m : messages)
			{
				if (toReturn == null)
				{
					toReturn = m;
				}
				
				// If this date is greater than what we have,
				// then make it what we have
				if (m.getDate().compareTo(toReturn.getDate()) == 1)
				{
					toReturn = m;
				}
			}
			
			// Sleep about a second before trying again
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// We shouldn't get one of these here
				e.printStackTrace();
			}
		}
		return toReturn;
	}
}
