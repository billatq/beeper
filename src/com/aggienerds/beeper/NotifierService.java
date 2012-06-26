package com.aggienerds.beeper;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class NotifierService extends Service {
	MediaPlayer mediaPlayer = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
	}

	@Override
	public void onDestroy() {
	}
	

	@Override
	public void onStart(Intent intent, int startid) {
		Context context = getApplicationContext();
		Bundle extras = intent.getExtras();
		
		String intentType = extras.getString("intentType");
		if (intentType != null)
		{
			if (intentType.equals("textMessage"))
			{
				// Pull out the subject and body from the extras and broadcast
				String textSubject = extras.getString("textSubject");
				String textBody = extras.getString("textBody");
				broadcastMessage(textSubject, textBody);
			}
			else if (intentType.equals("notificationTap"))
			{
				// Kill all notifications that are currently showing
				NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
				nm.cancelAll();
				
				// Kill any sounds that are currently playing
				if (mediaPlayer != null)
				{
					mediaPlayer.stop();
				}
				
				// Dispatch to the mms / sms app
				Intent messagingIntent = new Intent(Intent.ACTION_MAIN);
				messagingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				messagingIntent.addCategory(Intent.CATEGORY_DEFAULT);
				messagingIntent.setType("vnd.android-dir/mms-sms");
				startActivity(messagingIntent);
			}
		}
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
			//notification.sound = Uri.parse(alertSound);

			try {
				// Stop any existing media from playing
				if (mediaPlayer != null)
				{
					
					mediaPlayer.stop();
				}
				
				// Create new media player and set up a looping sound
				mediaPlayer = new MediaPlayer();
		        if (prefs.getBoolean("pref_alarmvol", false))
		        {
		        	mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
		        }
				
				mediaPlayer.setDataSource(this,Uri.parse(alertSound));
				mediaPlayer.setLooping(true);
				mediaPlayer.prepare();
				mediaPlayer.start();
				
				// TODO: Should we be vibrating from here instead of the notification?
				
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
        
		// Create a notification
        Intent returnIntent = new Intent(context, NotifierService.class);
        returnIntent.putExtra("intentType", "notificationTap");
        PendingIntent notificationIntent = PendingIntent.getService(context, 0, returnIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLatestEventInfo(context, subject, body, notificationIntent);
        
        // Vibrate, overriding default settings
        if (prefs.getBoolean("pref_vibrate", false))
        {
            if (prefs.getBoolean("vibrate", true)){
                notification.vibrate = new long[] {0, 800, 500, 800};
            }
        }
        
        nm.notify(0, notification);
	}
}