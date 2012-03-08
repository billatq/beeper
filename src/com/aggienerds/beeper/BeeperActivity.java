package com.aggienerds.beeper;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class BeeperActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        MessageRetriever mr = new MessageRetriever(this.getApplicationContext());
        List<Message> messages = mr.getConversationsLast(true);
        
        for (Message m : messages)
        {
        	Log.d("BeeperActivity", m.getBody());
        }
    }
}