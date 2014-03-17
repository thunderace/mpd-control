package org.chatminou.mpdcontrol.models;

import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

public class SettingsFragment extends PreferenceFragment
{

	protected void addPreferencesFromResource (int id, PreferenceGroup newParent) 
	{
	    PreferenceScreen screen = getPreferenceScreen();
	    int last = screen.getPreferenceCount();
	    addPreferencesFromResource(id);
	    while (screen.getPreferenceCount () > last) 
	    {
	        Preference p = screen.getPreference(last);
	        screen.removePreference(p);
	        newParent.addPreference(p);
	    }
	}
		
}
