package com.aggienerds.beeper;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class BeeperActivity extends PreferenceActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		addPreferencesFromResource(R.xml.preferences);
	}
}