package org.thunder.mpdcontrol.settings;

import org.thunder.mpdcontrol.R;
import org.thunder.mpdcontrol.models.SettingsFragment;

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
