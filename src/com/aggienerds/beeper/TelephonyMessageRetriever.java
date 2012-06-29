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

import com.aggienerds.beeper.androidpdu.*;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

/***
 * Simple class to provide known data from a text message event provided to a BroadcastReceiver.
 * It makes a best effort attempt to get as much data as is available from the event and then
 * passes back the text message as a Message.
 */
public class TelephonyMessageRetriever {

    @SuppressWarnings("unused")
    private static final String TAG = "TelephonyMessageRetriever";

    @SuppressWarnings("unused")
    private Context context;
    private Intent intent;

    public TelephonyMessageRetriever(Context context, Intent intent) {
        this.context = context;
        this.intent = intent;
    }

    public Message getMessage() {
        String action = intent.getAction();
        String type = intent.getType();

        if (action.contains("WAP_PUSH_RECEIVED")
                && type.contains("application/vnd.wap.mms-message")) {
            return getMMSMessage();
        } else if (action.contains("SMS_RECEIVED")) {
            return getSMSMessage();
        }
        return null;
    }

    private Message getSMSMessage() {
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            // There will only be a single PDU here, even though it's an array
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                Message m = new Message();
                m.setAddress(sms.getOriginatingAddress());
                m.setBody(sms.getMessageBody());
                m.setDate(sms.getTimestampMillis());
                return m;
            }
        }
        return null;
    }

    private Message getMMSMessage() {
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
                return m;
            }

        }
        return null;
    }
}
