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
	
	/***
	 * Work out what we'd like to say are the subject and body and pass it along to a service
	 * that specializes in making sure that the notification is received appropriately.
	 * @param context
	 * @param preferences
	 * @param message
	 */
	private void broadcastMessage(Context context, SharedPreferences preferences, Message message)
	{
		Log.d("MessageBroadcastRecieiver", "Setting up notification message");
        
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
        
        // Shove the subject and body that we'd like to broadcast into extras
        // and pass off to the notifier service
		Intent myIntent = new Intent(context, NotifierService.class);
		myIntent.putExtra("textSubject", subject);
		myIntent.putExtra("textBody", body);
		context.startService(myIntent);

	}
}
