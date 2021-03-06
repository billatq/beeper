/*
 * Copyright (C) 2012 William Reading 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aggienerds.beeper;

import java.io.IOException;

import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.provider.Telephony;
import android.util.Log;

@TargetApi(19)
public class NotifierService extends Service {

    private static final String TAG = "NotifierService";

    MediaPlayer mediaPlayer = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startid) {
        // The service is simply being restarted, nothing to do
        if (intent == null) {
            Log.d(TAG, "Null intent received. Ignoring.");
            return;
        }

        Context context = getApplicationContext();
        Bundle extras = intent.getExtras();

        String intentType = extras.getString("intentType");
        if (intentType != null) {
            if (intentType.equals("textMessage")) {
                // Pull out the subject and body from the extras and broadcast
                String textSubject = extras.getString("textSubject");
                String textBody = extras.getString("textBody");
                broadcastMessage(textSubject, textBody);
            } else if (intentType.equals("notificationTap")) {
                // Kill the notification that is currently showing (id=0)
                NotificationManager nm = (NotificationManager) context
                        .getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(0);

                // Kill any sounds that are currently playing
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }

                // Kill the vibrator
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.cancel();

                // Dispatch to the mms / sms app
                Intent messagingIntent = new Intent(Intent.ACTION_MAIN);
                messagingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Log.d(TAG, "We're on Android 4.4+, opening the default sms application.");
                    messagingIntent.setPackage(Telephony.Sms.getDefaultSmsPackage(context));
                } else {
                    // Pre 4.4 behavior
                    Log.d(TAG, "We're on Android 4.3 or lower, opening the mms-sms handler.");
                    messagingIntent.addCategory(Intent.CATEGORY_DEFAULT);
                    messagingIntent.setType("vnd.android-dir/mms-sms");
                }
                try {
                    startActivity(messagingIntent);
                } catch (Exception e) {
                    Log.d(TAG, "Couldn't start the messaging activity");
                    Log.d(TAG, e.getMessage());
                }
            } else {
                String message = String.format("Got an unknown intentType of type %s", intentType);
                Log.d(TAG, message);
            }
        } else {
            Log.d(TAG, "Got a null string for the intentType");
        }
    }

    private void broadcastMessage(String subject, String body) {
        Context context = getApplicationContext();
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        SharedPreferences prefs = context.getSharedPreferences("com.aggienerds.beeper_preferences", 0);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(
                R.drawable.status_icon,
                "Incoming Page",
                System.currentTimeMillis());

        // Set up the sound, if set
        String alertSound = prefs.getString("pref_alertsound", "");
        if ((alertSound != null) && (alertSound.length() > 0)) {
            try {
                // Stop any existing media from playing
                if (mediaPlayer != null) {

                    mediaPlayer.stop();
                }

                // Create new media player and set up a looping sound
                mediaPlayer = new MediaPlayer();

                // Play at alarm volume if need be, otherwise treat like a ringtone
                if (prefs.getBoolean("pref_alarmvol", false)) {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                } else {
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
                }

                mediaPlayer.setDataSource(this, Uri.parse(alertSound));
                mediaPlayer.setLooping(true);
                mediaPlayer.prepare();
                mediaPlayer.start();

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

        if (prefs.getBoolean("pref_vibrate", false)) {
            // Vibrate, if requested
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            long[] vibratePattern = new long[] { 0, 800, 500, 800 };
            v.vibrate(vibratePattern, 0);
        }

        // Create a notification
        Intent returnIntent = new Intent(context, NotifierService.class);
        returnIntent.putExtra("intentType", "notificationTap");

        PendingIntent notificationIntent = PendingIntent.getService(
                context,
                0,
                returnIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setLatestEventInfo(context, subject, body,
                notificationIntent);

        // Point back to this same class in the event of a delete.
        // We don't want to require a force kill to stop audio.
        notification.deleteIntent = PendingIntent.getService(
                context,
                0,
                returnIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Red flashy light, if we support one
        notification.ledARGB = 0xFFff0000;
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.ledOnMS = 100;
        notification.ledOffMS = 100;

        // Create a notification with an id=0
        nm.notify(0, notification);
    }
}
