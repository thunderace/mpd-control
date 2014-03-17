package org.chatminou.mpdcontrol.settings;

import org.chatminou.mpdcontrol.TabPagerAdapter;
import org.chatminou.mpdcontrol.models.SettingsActivity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

public class ConnectionSettingsActivity extends SettingsActivity
{
		
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
			
		TabPagerAdapter adapter = new TabPagerAdapter(this);
		if (!adapter.isTablet()) setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		getFragmentManager().beginTransaction()
			.replace(android.R.id.content, new ConnectionSettingsFragment())
			.commit();
	}
	
}
