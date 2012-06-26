package com.aggienerds.beeper;

import java.util.ArrayList;
import java.util.List;

import com.aggienerds.beeper.androidpdu.*;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class TelephonyMessageRetriever {
	
	@SuppressWarnings("unused")
	private static final String TAG = "TelephonyMessageRetriever";
	
	@SuppressWarnings("unused")
	private Context context;
	private Intent intent;
	
	public TelephonyMessageRetriever(Context context, Intent intent)
	{
	    this.context = context;
	    this.intent = intent;
	}

	public List<Message> getMessages()
	{
		String action = intent.getAction();
		String type = intent.getType();
		
		if (action.contains("WAP_PUSH_RECEIVED")
				&& type.contains("application/vnd.wap.mms-message"))
		{
			return getMMSMessages();
		}
		else if (action.contains("SMS_RECEIVED"))
		{
			return getSMSMessages();
		}
		return null;
	}
	
	private List<Message> getSMSMessages() {
		List<Message> messages = new ArrayList<Message>();
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			Object[] pdus = (Object[]) bundle.get("pdus");
			for (Object pdu : pdus)
			{
				SmsMessage sms = SmsMessage.createFromPdu((byte[])pdu);
				Message m = new Message();
				m.setAddress(sms.getOriginatingAddress());
				m.setBody(sms.getMessageBody());
				m.setDate(sms.getTimestampMillis());
				messages.add(m);
			}
		}
		return messages;
	}
	
	private List<Message> getMMSMessages()
	{
		List<Message> messages = new ArrayList<Message>();
		PduParser parser = new PduParser();
		byte[] intentByteArray = intent.getByteArrayExtra("data");
		PduHeaders headers = parser.parseHeaders(intentByteArray);
		if (headers == null) {
			return null;
		}

		int messageType = headers.getMessageType();

		// Check if it's a MMS notification
		if (messageType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
			Message m = new Message();
			
			
			EncodedStringValue encodedFrom = headers.getFrom();
			String fromStr = null;
			if (encodedFrom != null) {
				fromStr = encodedFrom.getString();
				m.setAddress(fromStr);
				messages.add(m);
			}
			
		}
		return messages;
	}
}
