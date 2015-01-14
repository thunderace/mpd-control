package org.thunder.mpdcontrol.settings;

import org.thunder.mpdcontrol.R;
import org.thunder.mpdcontrol.models.SettingsFragment;

import android.os.Bundle;
import android.preference.PreferenceCategory;

public class ConnectionsSettingsFragment extends SettingsFragment
{
	
	private WifiConnectionSettingsFragment fragmentWifi = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_connections);
		
		PreferenceCategory defaul = (PreferenceCategory) getPreferenceScreen().findPreference("default");
		if (defaul != null) 
		{
			ConnectionSettingsFragment.init(defaul, getActivity());
		}
				
		PreferenceCategory wifi = (PreferenceCategory) getPreferenceScreen().findPreference("wifi");
		if (wifi != null) 
		{
			fragmentWifi = new WifiConnectionSettingsFragment();
			fragmentWifi.initWifi(wifi, getActivity());
		}
	}
	
	@Override
	public void onDestroy() 
	{
		if (fragmentWifi != null)
		{
			fragmentWifi.doneWifi(getActivity());
			fragmentWifi = null;
		}
		super.onDestroy();
	}
}
