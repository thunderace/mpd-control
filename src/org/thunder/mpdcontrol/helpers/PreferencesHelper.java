package org.thunder.mpdcontrol.helpers;

import android.content.Context;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class PreferencesHelper 
{
	
	private Context context = null;
	private PreferenceManager manager = null;
	
	public PreferencesHelper(PreferenceManager manager, Context context) 
	{
		this.manager = manager;
		this.context = context;
	}
	
	private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() 
	{
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) 
        {
            String stringValue = value.toString();
            if (preference instanceof ListPreference) 
            {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
            } 
            else 
            {
            	Bundle b = preference.getExtras();
            	String summary = b.getString("summary");
            	if (summary == null) summary = "";
            	if (!stringValue.isEmpty())	preference.setSummary(stringValue);
            	else preference.setSummary(summary);
            }
            return true;
        }
    };
	
    public static void bindPreferenceSummaryToValue(Preference preference) 
	{
		Bundle b = preference.getExtras();
		b.putString("summary", preference.getSummary().toString());
		
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager.getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }
	
    public PreferenceScreen createPreferenceScreen()
	{
		return manager.createPreferenceScreen(context);
	}
	    
}
