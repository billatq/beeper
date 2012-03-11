package com.aggienerds.beeper;

import java.util.List;
import java.util.regex.PatternSyntaxException;

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
		
		SharedPreferences prefs = context.getSharedPreferences("com.aggienerds.beeper_preferences", 0);
		if (prefs.getBoolean("pref_oncall", false))
		{
			TelephonyMessageRetriever retriever = new TelephonyMessageRetriever(context, intent);
			List<Message> messages = retriever.getMessages();
			if ((messages != null) && (messages.size() > 0))
			{
				for (Message message : messages)
				{
					if (doesMessageMatch(prefs, message))
					{
						broadcastMessage(context, prefs, message);
					}
				}
			}
			else
			{
				Log.d("MessageBroadcastReceiver", "Told we got a message, but couldn't find it.");
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
			try
			{
				if (comparison.matches("^.*" + matchText + ".*$")) { return true; }
			}
		    catch(PatternSyntaxException e)
		    {
		    	Log.d("MessageBroadcastReceiver", "Invalid regex in match pattern. Ignoring.");
		    }
		}
		
		return false;
	}
	
	private void broadcastMessage(Context context, SharedPreferences preferences, Message message)
	{
		Log.d("MessageBroadcastRecieiver", "Setting up notification message");
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		SharedPreferences prefs = preferences;
		

		
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.status_icon, "Incoming Page", System.currentTimeMillis());
        
		String alertSound = prefs.getString("pref_alertsound", "");
		if ((alertSound != null) && (alertSound.length() > 0))
		{
			notification.sound = Uri.parse(alertSound);
		}
		
		Intent messagingIntent = new Intent(Intent.ACTION_MAIN);
		messagingIntent.addCategory(Intent.CATEGORY_DEFAULT);
		messagingIntent.setType("vnd.android-dir/mms-sms");

        // The PendingIntent to launch our activity if the user selects this notification
		// TODO: Make notification ids unique?
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
}
