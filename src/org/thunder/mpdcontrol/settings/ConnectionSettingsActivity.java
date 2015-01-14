package org.thunder.mpdcontrol.settings;

import org.thunder.mpdcontrol.TabPagerAdapter;
import org.thunder.mpdcontrol.models.SettingsActivity;

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
