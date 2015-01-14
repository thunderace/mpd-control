package org.thunder.mpdcontrol.settings;

import java.util.List;

import org.thunder.mpdcontrol.R;
import org.thunder.mpdcontrol.TabPagerAdapter;
import org.thunder.mpdcontrol.helpers.SettingsHelper;
import org.thunder.mpdcontrol.models.SettingsActivity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

public class ConnectionsSettingsActivity extends SettingsActivity
{
		
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		TabPagerAdapter adapter = new TabPagerAdapter(this);
		if (!adapter.isTablet()) setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		if (SettingsHelper.isSimplePreferences(this))
		{
			getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new ConnectionsSettingsFragment())
				.commit();
		}
	}
	
	@Override
	public void onBuildHeaders(List<Header> target) 
	{
		super.onBuildHeaders(target);
		if (!SettingsHelper.isSimplePreferences(this))
		{
			loadHeadersFromResource(R.xml.settings_header_connections, target);
		}
	}
			
}
