package com.aggienerds.beeper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class NotifierService extends Service {
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(context, "NotifierService started!", duration);
		toast.show();
	}

	@Override
	public void onDestroy() {
		// code to execute when the service is shutting down
	}

	@Override
	public void onStart(Intent intent, int startid) {
		Bundle extras = intent.getExtras();
		
		// Pull out the subject and body from the extras and broadcast
		String textSubject = extras.getString("textSubject");
		String textBody = extras.getString("textBody");
		broadcastMessage(textSubject, textBody);

	}
	
	private void broadcastMessage(String subject, String body)
	{
		Context context = getApplicationContext();
		NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		SharedPreferences prefs = context.getSharedPreferences("com.aggienerds.beeper_preferences", 0);
		
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.status_icon, "Incoming Page", System.currentTimeMillis());
        
        // Set up the sound
		String alertSound = prefs.getString("pref_alertsound", "");
		if ((alertSound != null) && (alertSound.length() > 0))
		{
			notification.sound = Uri.parse(alertSound);
		}
		
		// Have the intent launch the mms/sms application
		Intent messagingIntent = new Intent(Intent.ACTION_MAIN);
		messagingIntent.addCategory(Intent.CATEGORY_DEFAULT);
		messagingIntent.setType("vnd.android-dir/mms-sms");

        // The PendingIntent to launch our activity if the user selects this notification
		// TODO: Make notification ids unique?
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, messagingIntent, Intent.FLAG_ACTIVITY_NEW_TASK);
        
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