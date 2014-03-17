package org.chatminou.mpdcontrol.settings;

import java.util.List;

import org.chatminou.mpdcontrol.MainApplication;
import org.chatminou.mpdcontrol.R;
import org.chatminou.mpdcontrol.helpers.SettingsHelper;
import org.chatminou.mpdcontrol.models.SettingsFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.util.Log;

public class WifiConnectionSettingsFragment extends SettingsFragment
{
	
	private BroadcastReceiver receiver = null;
	
	///////////////////////////////////////////////////////////////////////////
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_connection_wifi);
		
		PreferenceScreen pref = (PreferenceScreen) getPreferenceScreen();
		if (pref != null) initWifi(pref, getActivity());
	}
	
	@Override
    public void onDestroy() 
	{
		doneWifi(getActivity());
        super.onDestroy();
    }
	
	///////////////////////////////////////////////////////////////////////////
	
	protected void initWifi(final Preference pref, Context context)
	{
		receiver = new BroadcastReceiver()
		{
			@Override
			public void onReceive(final Context context, Intent intent) 
			{
				String action = intent.getAction();
				
		        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) 
		        {
		        	int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
		            int oldstate = intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
		            if (oldstate != state)
		            {
		            	emptyList(pref);
		            	if (state == WifiManager.WIFI_STATE_ENABLED)
			            {
			        		fillList(pref, context);
			            }
		            }
		        } 
		        else if (action.equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)) 
		        {
			         SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
			         if (state == SupplicantState.COMPLETED)
			         {
			        	 selectActive(pref, context);
			         }
		        }
			}
		};		
		
		context.registerReceiver(receiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
		context.registerReceiver(receiver, new IntentFilter(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION));
	}
	
	protected void doneWifi(Context context)
	{
		if (receiver != null)
		{
			context.unregisterReceiver(receiver);
			receiver = null;
		}
	}
	
	///////////////////////////////////////////////////////////////////////////

	private void emptyList(Preference pref)
	{
		if (pref.getClass() == PreferenceScreen.class)
		{
			PreferenceScreen p = (PreferenceScreen) pref;
			p.removeAll();
		}
		else if (pref.getClass() == PreferenceCategory.class)
		{
			PreferenceCategory p = (PreferenceCategory) pref;
			p.removeAll();
		}
	}
	
	private void fillList(Preference pref, Context context)
	{
		WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		List<WifiConfiguration> wifiList = manager.getConfiguredNetworks();
		if (wifiList == null) return;
		
		for (WifiConfiguration wifi : wifiList) 
		{
			String ssid = wifi.SSID.replaceAll("\"", "");
			
			String status;
			if (WifiConfiguration.Status.CURRENT == wifi.status) status = context.getResources().getString(R.string.settings_wifi_connected);
            else status = context.getResources().getString(R.string.settings_wifi_not_in_range);
			
			Preference p1 = new Preference(context);
			Intent intent = new Intent(context, ConnectionSettingsActivity.class);
			intent.putExtra("SSID", ssid);
			p1.setIntent(intent);
			p1.setTitle(ssid);
			p1.setSummary(status);
			
			if (pref.getClass() == PreferenceScreen.class)
			{
				PreferenceScreen p = (PreferenceScreen) pref;
				p.addPreference(p1);
			}
			else if (pref.getClass() == PreferenceCategory.class)
			{
				PreferenceCategory p = (PreferenceCategory) pref;
				p.addPreference(p1);
			}
		}
	}
	
	private void selectActive(Preference pref, Context context)
	{
		String ssid = SettingsHelper.getCurrentSSID((WifiManager) context.getSystemService(Context.WIFI_SERVICE));
		if (ssid == null) return;
		
		if (pref.getClass() == PreferenceScreen.class)
		{
			PreferenceScreen p = (PreferenceScreen) pref;
			for (int i = 0; i < p.getPreferenceCount(); i++)
			{
				Preference f = p.getPreference(i);
				if (f.getTitle().equals(ssid))
					f.setSummary(context.getResources().getString(R.string.settings_wifi_connected));
			}
		}
		else if (pref.getClass() == PreferenceCategory.class)
		{
			PreferenceCategory p = (PreferenceCategory) pref;
			for (int i = 0; i < p.getPreferenceCount(); i++)
			{
				Preference f = p.getPreference(i);
				if (f.getTitle().equals(ssid))
					f.setSummary(context.getResources().getString(R.string.settings_wifi_connected));
			}
		}

	}
		
}