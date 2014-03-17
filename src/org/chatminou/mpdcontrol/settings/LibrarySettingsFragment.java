package org.chatminou.mpdcontrol.settings;

import org.chatminou.mpdcontrol.R;
import org.chatminou.mpdcontrol.models.SettingsFragment;

import android.os.Bundle;

public class LibrarySettingsFragment extends SettingsFragment
{
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_library);
	}
		
}
