package org.chatminou.mpdcontrol.settings;

import org.chatminou.mpdcontrol.R;
import org.chatminou.mpdcontrol.helpers.SettingsHelper;
import org.chatminou.mpdcontrol.models.SettingsFragment;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;

public class MainSettingsFragment extends SettingsFragment
{
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_main);
			
		//////
		
		PreferenceCategory server = (PreferenceCategory) getPreferenceScreen().findPreference("server");
		if (server != null) addPreferencesFromResource(R.xml.settings_server, server);
			
		Preference pConn = (Preference) findPreference("server_connections");
		MultiSelectListPreference pOutputs = (MultiSelectListPreference) findPreference("server_outputs");
		Preference pUpdate = (Preference) findPreference("server_update");
		Preference pInfos = (Preference) findPreference("server_infos");
		ServerSettingsFragment.init(pConn, pOutputs, pUpdate, pInfos, getActivity());
		
		//////
		
		PreferenceCategory interfac = (PreferenceCategory) getPreferenceScreen().findPreference("interface");
		if (interfac != null) addPreferencesFromResource(R.xml.settings_interface, interfac);
		
		EditTextPreference pCacheUsage = (EditTextPreference) findPreference("cover_cache_usage");
		Preference pCacheClear = (Preference) findPreference("cover_cache_clear");
		CheckBoxPreference pCoverLocal = (CheckBoxPreference) findPreference(SettingsHelper.COVER_USE_LOCAL);
		EditTextPreference pCoverLocalPath = (EditTextPreference) findPreference(SettingsHelper.COVER_USE_LOCAL_PATH);
		EditTextPreference pCoverLocalfilename = (EditTextPreference) findPreference(SettingsHelper.COVER_USE_LOCAL_FILENAME);
		InterfaceSettingsFragment.init(pCacheUsage, pCacheClear, pCoverLocal, pCoverLocalPath, pCoverLocalfilename, getActivity());
		
		//////
		
		PreferenceCategory library = (PreferenceCategory) getPreferenceScreen().findPreference("library");
		if (library != null) addPreferencesFromResource(R.xml.settings_library, library);
	}
			
}
