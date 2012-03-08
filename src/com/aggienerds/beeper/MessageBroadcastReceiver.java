package com.aggienerds.beeper;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
			// TODO: Notify or something, per preferences
			Log.d("MessageBroadcastReceiver", "Got a message!");
			
			MessageRetriever mr = new MessageRetriever(context);
			
			List<Message> messages = null;
			
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
				
				// Got messages, log it for now
				for (Message m : messages)
				{
					Log.d("MessageBroadcastReceiver", m.getBody());
					break;
				}
				
				// Sleep about a second before trying again
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// We shouldn't get one of these here
					e.printStackTrace();
				}
			}
		}
	}
}
