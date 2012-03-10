package com.aggienerds.beeper;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class MessageRetriever {

	private Context context;

	public MessageRetriever(Context context) {
		this.context = context;
	}

	public List<Message> getConversationsLast(Boolean unreadOnly) {
		List<Message> messages = new ArrayList<Message>();
		ContentResolver contentResolver = context.getContentResolver();
		final String[] projection = new String[] { "*" };
		
		String selection;
		if (unreadOnly == true)
		{
			selection = "read=0";
		}
		else
		{
			selection = "";
		}
		
		Uri uri = Uri.parse("content://mms-sms/conversations/");
		Cursor cursor = contentResolver.query(
				uri,
				projection,
				selection,
				null,
				null);

		if (cursor.moveToFirst())
		{
			do {
				String type = cursor.getString(cursor.getColumnIndex("ct_t"));
				String _id = cursor.getString(cursor.getColumnIndex("_id"));
				
				if (_id != null)
				{
					Message m = new Message();
					
					// Debugging
					String columns[] = cursor.getColumnNames();
					String values[] = new String[columns.length];
					for (int i = 0; i < columns.length; ++i)
					{
						values[i] = cursor.getString(cursor.getColumnIndex(columns[i]));
					}
				
					// MMS. Sometimes, right after it's been delivered, the mime type hasn't
					// yet been extracted. If we have a subject, we know it can't be a SMS.
					if ("application/vnd.wap.multipart.related".equals(type)
					    || (cursor.getString(cursor.getColumnIndex("sub")) != null))
					{
						m.setBody(getMmsText(_id));
						m.setAddress(getMmsAddress(_id));
						m.setSubject(cursor.getString(cursor.getColumnIndex("sub")));
	
					// SMS
					} else {
						m.setBody(getSmsText(_id));
						m.setAddress(cursor.getString(cursor.getColumnIndex("address")));
					}
					
					String date = cursor.getString(cursor.getColumnIndex("normalized_date"));
					if (date != null)
					{
						m.setDate(Long.parseLong(date));
					}
					
					m.setId(Integer.parseInt(_id));
					
					String thread = cursor.getString(cursor.getColumnIndex("tid"));
					if (thread != null)
					{
						m.setThread_id(Integer.parseInt(thread));
					}
					
					messages.add(m);
				}
			} while (cursor.moveToNext());
		}
		return messages;
	}
	
	private String getMmsText(String id) {
		Cursor cursor = context.getContentResolver().query(
				Uri.parse("content://mms/" + id + "/part"),
				null,
				null,
				null,
				null);

		while (cursor.moveToNext()) {
			String type = cursor.getString(cursor.getColumnIndex("ct"));
			if (type.contains("text/plain")) {
				return cursor.getString(cursor.getColumnIndex("text"));
			}
		}
		return new String();
	}
	
	private String getMmsAddress(String id) {
	    String selectionAdd = new String("msg_id=" + id);
	    String uriStr = MessageFormat.format("content://mms/{0}/addr", id);
	    Uri uriAddress = Uri.parse(uriStr);
	    Cursor cursor = context.getContentResolver().query(
	    		uriAddress,
	    		null,
	    		selectionAdd,
	    		null,
	    		null);
	    
	    if (cursor.moveToFirst()) {
	        do {
	            String address = cursor.getString(cursor.getColumnIndex("address"));
	            if (address != null) {
	                return address;
	            }
	        } while (cursor.moveToNext());
	    }
	    if (cursor != null) {
	        cursor.close();
	    }
	    return null;
	}


	private String getSmsText(String id) {
		String selection = "_id = " + id;
		Uri uri = Uri.parse("content://sms");
		Cursor cursor = context.getContentResolver().query(
				uri,
				null,
				selection,
				null,
				null);
		if (cursor.moveToFirst()) {
			return cursor.getString(cursor.getColumnIndex("body"));
		}
		return null;
	}

}
